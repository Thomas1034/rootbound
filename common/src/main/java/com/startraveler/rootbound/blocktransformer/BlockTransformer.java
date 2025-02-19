/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you modify this file, please include a notice stating the changes:
 * Example: "Modified by [Your Name] on [Date] - [Short Description of Changes]"
 */
package com.startraveler.rootbound.blocktransformer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.blocktransformer.data.BlockTransformerData;
import com.startraveler.rootbound.blocktransformer.data.BlockTransformerResultOption;
import com.startraveler.rootbound.util.AliasBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class BlockTransformer {

    public static final ResourceKey<Registry<BlockTransformer>> KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
            "block_transformer"
    ));

    public static final Codec<List<BlockTransformerData>> DATA_LIST_CODEC = Codec.list(BlockTransformerData.CODEC).xmap(
            list -> {
                // Validation: Ensure that all objects in the list pass the required field check
                list.forEach(data -> {
                    if (!BlockTransformerData.validateRequiredFields(data)) {
                        throw new IllegalStateException("Block Transformer Data failed to validate");
                    }
                });
                return list;
            }, list -> list
    );
    public static final Codec<BlockTransformer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DATA_LIST_CODEC.fieldOf("values").forGetter(BlockTransformer::asData),
            ResourceLocation.CODEC.fieldOf("name").forGetter(bt -> bt.name)
    ).apply(instance, BlockTransformer::new));
    public final ResourceLocation name;
    private final Map<TagKey<Block>, Function<RandomSource, Block>> tagMap;
    private final Object2IntMap<TagKey<Block>> tagPriorityMap;
    private final Map<Block, Function<RandomSource, Block>> directMap;
    private final List<ResourceLocation> fallbacks;
    private final List<BlockTransformerData> rawData;
    private final Map<ResourceLocation, BlockTransformer> cachedFallbacks;
    private int numTagsAdded;

    public BlockTransformer(List<BlockTransformerData> values, ResourceLocation name) {
        this.tagMap = new HashMap<>();
        this.tagPriorityMap = new Object2IntOpenHashMap<>();
        this.tagPriorityMap.defaultReturnValue(-1);
        this.numTagsAdded = 0;
        this.directMap = new HashMap<>();
        this.fallbacks = new ArrayList<>();
        this.cachedFallbacks = new HashMap<>();
        this.rawData = values;
        this.name = name;

        this.fillData(this.rawData);

        // Reverse the list of fallbacks; this makes ones added last have higher priority.
        List<ResourceLocation> reversedCallbacks = new ArrayList<>(this.fallbacks.reversed());
        this.fallbacks.clear();
        this.fallbacks.addAll(reversedCallbacks);
    }

    private static Block getBlock(ResourceLocation location) {
        Block block = BuiltInRegistries.BLOCK.get(location).orElseThrow().value();
        Objects.requireNonNull(block, "Unrecognized block " + location + "in BlockTransformer");
        return block;
    }

    // Copies the properties of one block state onto the default state of another block, to whatever degree is possible.
    @SuppressWarnings("unchecked")
    public static BlockState copyProperties(BlockState input, Block to) {
        // Don't do unnecessary work.
        if (to == null || input.is(to)) {
            return input;
        }
        // Get the default state to copy properties onto.
        BlockState output = to.defaultBlockState();
        // Copy every property, if applicable.
        // Properties have type-parameters, but that sufficiently confuses the compiler here such that
        // raw types must be used.
        for (@SuppressWarnings("rawtypes") Property property : input.getProperties()) {
            // Skip if the output state does not have the desired property.
            if (output.hasProperty(property)) {
                // If it does, set it.
                output = output.trySetValue(property, input.getValue(property));
            }
        }

        return output;
    }

    private void fillData(List<BlockTransformerData> values) {
        for (BlockTransformerData toLoad : values) {
            // First, check if it's giving a transformer override.
            if (toLoad.transformer != null) {
                this.fallbacks.add(toLoad.transformer);
            }
            // If it's adding a block as the key, it could either be a direct mapping or a random chance.
            else if (toLoad.block != null) {
                if (toLoad.result != null) {
                    // It is a direct mapping.
                    this.addDirectMapping(getBlock(toLoad.block), getBlock(toLoad.result));
                } else if (toLoad.results != null) {
                    // It is not a direct mapping, which means this will be harder.
                    Object2IntMap<Block> temporaryMap = new Object2IntOpenHashMap<>();
                    // Load all the options into a map.
                    for (BlockTransformerResultOption option : toLoad.results) {
                        temporaryMap.put(getBlock(option.name()), option.weight());
                    }
                    // Add the map.
                    this.addProbabilisticMapping(getBlock(toLoad.block), temporaryMap);
                }
            }
            // If it's adding a tag as the key, it could either be a direct mapping or a random chance.
            else if (toLoad.tag != null) {
                if (toLoad.result != null) {
                    // It is a direct mapping.
                    this.addDirectMapping(toLoad.tag, getBlock(toLoad.result));
                } else if (toLoad.results != null) {
                    // It is not a direct mapping, which means this will be harder.
                    Object2IntMap<Block> temporaryMap = new Object2IntOpenHashMap<>();
                    // Load all the options into a map.
                    for (BlockTransformerResultOption option : toLoad.results) {
                        temporaryMap.put(getBlock(option.name()), option.weight());
                    }
                    // Add the map.
                    this.addProbabilisticMapping(toLoad.tag, temporaryMap);
                }
            }
            // Otherwise, we have a problem.
            else {
                throw new IllegalStateException("Unable to parse BlockTransformerData");
            }
        }
    }

    private void addDirectMapping(Block input, Block output) {
        this.directMap.put(input, (randomSource) -> output);
    }

    private void addDirectMapping(TagKey<Block> input, Block output) {
        this.tagMap.put(input, (randomSource) -> output);
        this.tagPriorityMap.put(input, this.numTagsAdded++);
    }

    private void addProbabilisticMapping(Block input, Map<Block, Integer> probabilities) {
        this.directMap.put(input, AliasBuilder.build(probabilities));
    }

    private void addProbabilisticMapping(TagKey<Block> input, Map<Block, Integer> probabilities) {
        this.tagMap.put(input, AliasBuilder.build(probabilities));
        this.tagPriorityMap.put(input, this.numTagsAdded++);
    }

    private Function<RandomSource, Block> getRaw(Block input, RegistryAccess access) {

        Function<RandomSource, Block> result = this.directMap.get(input);

        if (result == null) {
            result = this.getHighestPriorityTagMapping(input);
        }
        // If no result was found yet,
        if (result == null) {
            for (ResourceLocation fallback : this.fallbacks) {
                // Iterate until a valid option is found; then stop.
                result = this.getFallback(access, fallback).getRaw(input, access);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    // Storing the set of previously visited block transformers prevents infinite loops.
    public Block get(Block input, RegistryAccess access, RandomSource random) {
        Function<RandomSource, Block> raw = this.getRaw(input, access);
        return raw == null ? null : raw.apply(random);
    }

    public BlockState get(BlockState input, RegistryAccess access, RandomSource random) {
        return copyProperties(input, this.get(input.getBlock(), access, random));
    }

    private Function<RandomSource, Block> getHighestPriorityTagMapping(Block block) {
        int highestPriority = -1;
        Function<RandomSource, Block> result = null;

        for (Object2IntMap.Entry<TagKey<Block>> entry : this.tagPriorityMap.object2IntEntrySet()) {
            int tagPriority = entry.getIntValue();
            TagKey<Block> tag = entry.getKey();
            if (tagPriority > highestPriority && block.builtInRegistryHolder().is(tag)) {
                result = this.tagMap.get(tag);
                highestPriority = tagPriority;
            }
        }

        return result;
    }

    public boolean isValidInput(RegistryAccess access, @NotNull BlockState input) {
        return this.isValidInput(access, input.getBlock());
    }

    public boolean isValidInput(RegistryAccess access, Block input) {
        return this.directMap.containsKey(input) || this.hasValidTagMapping(input) || this.hasValidFallbackMapping(access,
                input
        );
    }

    private boolean hasValidTagMapping(@NotNull Block input) {
        Set<TagKey<Block>> tags = this.tagMap.keySet();
        for (TagKey<Block> tag : tags) {
            if (input.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasValidFallbackMapping(RegistryAccess access, @NotNull Block input) {
        for (ResourceLocation location : this.fallbacks) {
            if (this.getFallback(access, location).isValidInput(access, input)) {
                return true;
            }
        }
        return false;
    }

    // Note: this could cause an infinite loop if a fallback of this registry at any point lists this as a fallback.
    private BlockTransformer getFallback(RegistryAccess access, ResourceLocation location) {
        BlockTransformer cached = this.cachedFallbacks.get(location);
        if (cached != null) {
            return cached;
        }

        Registry<BlockTransformer> transformers = access.lookupOrThrow(BlockTransformer.KEY);
        BlockTransformer transformer = transformers.get(location).orElseThrow().value();
        this.cachedFallbacks.put(location, transformer);
        return transformer;
    }

    public List<BlockTransformerData> asData() {
        return this.rawData;
    }

}
