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
package com.startraveler.rootbound.blocktransformer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlockTransformerData {
    public static final Codec<TagKey<Block>> TAG_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("location").forGetter(TagKey::location))
            .apply(instance, location -> TagKey.create(Registries.BLOCK, location)));
    // Codec for TransformerData
    public static final Codec<BlockTransformerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("transformer")
                    .forGetter(data -> data.transformer == null ? Optional.empty() : Optional.of(data.transformer)),
            Identifier.CODEC.optionalFieldOf("result")
                    .forGetter(data -> data.result == null ? Optional.empty() : Optional.of(data.result)),
            Codec.list(BlockTransformerResultOption.CODEC)
                    .optionalFieldOf("results")
                    .forGetter(data -> data.results == null ? Optional.empty() : Optional.of(data.results)),
            TAG_CODEC.optionalFieldOf("tag")
                    .forGetter(data -> data.tag == null ? Optional.empty() : Optional.of(data.tag)),
            Identifier.CODEC.optionalFieldOf("block")
                    .forGetter(data -> data.block == null ? Optional.empty() : Optional.of(data.block))
    ).apply(instance, BlockTransformerData::new));
    public final Identifier transformer;
    public final Identifier result;
    public final List<BlockTransformerResultOption> results;
    public final TagKey<Block> tag;
    public final Identifier block;

    public BlockTransformerData(Optional<Identifier> transformer, Optional<Identifier> result, Optional<List<BlockTransformerResultOption>> results, Optional<TagKey<Block>> tag, Optional<Identifier> block) {
        this(transformer.orElse(null), result.orElse(null), results.orElse(null), tag.orElse(null), block.orElse(null));
    }

    public BlockTransformerData(Identifier transformer, Identifier result, List<BlockTransformerResultOption> results, TagKey<Block> tag, Identifier block) {
        this.transformer = transformer;
        this.result = result;
        this.results = results;
        this.tag = tag;
        this.block = block;

        validateRequiredFields(this);
    }

    // Method to validate that at least one of transformer, result, or results is present
    public static void validateRequiredFields(BlockTransformerData data) {
        boolean defersToParent = (data.transformer != null) && (data.result == null && data.results == null) && (data.tag == null && data.block == null);
        boolean specifiesResult = (data.transformer == null) && (data.result != null || data.results != null) && (data.tag != null || data.block != null) && !(data.tag != null && data.block != null);
        if (!(defersToParent || specifiesResult)) {
            StringBuilder message = new StringBuilder();
            message.append("Block transformer data failed to validate!\n");
            if (data.transformer != null) {
                message.append("The data contains both a data transformer fallback (")
                        .append(data.transformer)
                        .append(") and ");
                List<String> reasonsForInvalid = new ArrayList<>();
                if (data.result != null) {
                    reasonsForInvalid.add(" the direct result " + data.result + ";");
                }
                if (data.results != null) {
                    reasonsForInvalid.add(" a list of " + data.results.size() + "results;");
                }
                if (data.tag != null) {
                    reasonsForInvalid.add(" an input tag " + data.tag + ";");
                }
                if (reasonsForInvalid.size() > 1) {
                    for (String s : reasonsForInvalid) {
                        message.append(s);
                    }
                } else {
                    message.append(reasonsForInvalid.getFirst());
                }
                message.append(" if a fallback transformer is given, the other data must not be included.");
            }
            // TODO finish writing exception message.
            throw new IllegalStateException(message.toString());
        }

    }


}
