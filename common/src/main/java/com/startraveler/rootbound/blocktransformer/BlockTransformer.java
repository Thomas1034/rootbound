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
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class BlockTransformer {

    public static final ResourceKey<Registry<BlockTransformer>> KEY = ResourceKey.createRegistryKey(Constants.location(
            "block_transformer"));

    public static final Codec<List<BlockTransformerData>> DATA_LIST_CODEC = Codec.list(BlockTransformerData.CODEC).xmap(
            list -> {
                // Validation: Ensure that all objects in the list pass the required field check
                list.forEach(BlockTransformerData::validateRequiredFields);
                return list;
            }, list -> list
    );
    public static final Codec<BlockTransformer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DATA_LIST_CODEC.fieldOf("values").forGetter(BlockTransformer::asData),
            Identifier.CODEC.fieldOf("name").forGetter(bt -> bt.name)
    ).apply(instance, BlockTransformer::new));
    public final Identifier name;
    private final Map<TagKey<Block>, Function<RandomSource, Block>> tagMap;
    private final Object2IntMap<TagKey<Block>> tagPriorityMap;
    private final Map<Block, Function<RandomSource, Block>> directMap;
    private final List<Identifier> fallbacks;
    private final List<BlockTransformerData> rawData;
    private final Map<Identifier, BlockTransformer> cachedFallbacks;
    private final Map<Block, Function<RandomSource, Block>> cachedTagMappings;
    private final Map<Block, Function<RandomSource, Block>> cache;
    private final Map<Block, Boolean> cachedValidInputs;
    private int numTagsAdded;

    public BlockTransformer(List<BlockTransformerData> values, Identifier name) {
        this.tagMap = new HashMap<>();
        this.tagPriorityMap = new Object2IntOpenHashMap<>();
        this.tagPriorityMap.defaultReturnValue(-1);
        this.numTagsAdded = 0;
        this.directMap = new HashMap<>();
        this.fallbacks = new ArrayList<>();
        this.cachedFallbacks = new HashMap<>();
        this.cachedValidInputs = new Object2BooleanOpenHashMap<>();
        this.cachedTagMappings = new HashMap<>();
        this.rawData = values;
        this.name = name;
        this.cache = new IdentityHashMap<>();

        this.fillData(this.rawData);

        // Reverse the list of fallbacks; this makes ones added last have higher priority.
        List<Identifier> reversedCallbacks = new ArrayList<>(this.fallbacks.reversed());
        this.fallbacks.clear();
        this.fallbacks.addAll(reversedCallbacks);

    }

    private static Block getBlock(Identifier location) {
        Block block = BuiltInRegistries.BLOCK.get(location).orElseThrow().value();
        Objects.requireNonNull(block, "Unrecognized block " + location + "in BlockTransformer");
        return block;
    }

    // Copies the properties of one block state onto the default state of another block, to whatever degree is possible.
    public static BlockState copyProperties(BlockState input, Block to) {
        // Don't do unnecessary work.
        if (to == null || input.is(to)) {
            return input;
        }
        // Minecraft already handles this. Who knew.
        return to.withPropertiesOf(input);
    }

    private void fillData(List<BlockTransformerData> values) {
        for (BlockTransformerData toLoad : values) {
            // First, check if it's giving a transformer fallback.
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

        if (this.cache.containsKey(input)) {
            return this.cache.get(input);
        }
        Function<RandomSource, Block> result = this.computeRaw(input, access);
        this.cache.put(input, result);
        return result;
    }

    private Function<RandomSource, Block> computeRaw(Block block, RegistryAccess access) {
        Function<RandomSource, Block> result = this.directMap.getOrDefault(
                block,
                this.lookupHighestPriorityTagMapping(block)
        );
        if (result == null) {
            for (Identifier fallback : this.fallbacks) {
                result = this.getFallback(access, fallback).getRaw(block, access);
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

    private Function<RandomSource, Block> lookupHighestPriorityTagMapping(Block input) {
        if (this.cachedTagMappings.containsKey(input)) {
            return this.cachedTagMappings.get(input);
        }
        Function<RandomSource, Block> mapping = this.computeHighestPriorityTagMapping(input);
        this.cachedTagMappings.put(input, mapping);
        return mapping;
    }

    @SuppressWarnings("deprecation")
    private Function<RandomSource, Block> computeHighestPriorityTagMapping(Block block) {
        int highestPriority = -1;
        TagKey<Block> selectedTag = null;
        for (Object2IntMap.Entry<TagKey<Block>> entry : this.tagPriorityMap.object2IntEntrySet()) {
            int tagPriority = entry.getIntValue();
            TagKey<Block> tag = entry.getKey();
            if (tagPriority > highestPriority && block.builtInRegistryHolder().is(tag)) {
                selectedTag = tag;
                highestPriority = tagPriority;
            }
        }
        return null == selectedTag ? null : this.tagMap.get(selectedTag);
    }

    @SuppressWarnings("unused")
    public boolean isValidInput(RegistryAccess access, @NotNull BlockState input) {
        return this.isValidInput(access, input.getBlock());
    }

    public boolean isValidInput(RegistryAccess access, Block input) {
        if (this.cachedValidInputs.containsKey(input)) {
            return this.cachedValidInputs.get(input);
        }
        boolean isValid = this.computeIsValidInput(access, input);
        this.cachedValidInputs.put(input, isValid);
        return isValid;
    }

    private boolean computeIsValidInput(RegistryAccess access, Block block) {
        return this.directMap.containsKey(block) || this.hasValidTagMapping(block) || this.hasValidFallbackMapping(
                access,
                block
        );
    }

    @SuppressWarnings("deprecation")
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
        for (Identifier location : this.fallbacks) {
            if (this.getFallback(access, location).isValidInput(access, input)) {
                return true;
            }
        }
        return false;
    }

    // Note: this could cause an infinite loop if a fallback of this registry at any point lists this as a fallback.
    private BlockTransformer getFallback(RegistryAccess access, Identifier location) {
        if (this.cachedFallbacks.containsKey(location)) {
            return this.cachedFallbacks.get(location);
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
