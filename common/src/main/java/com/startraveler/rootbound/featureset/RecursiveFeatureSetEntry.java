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
package com.startraveler.rootbound.featureset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.startraveler.rootbound.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public class RecursiveFeatureSetEntry extends FeatureSet.Entry{

    public static final Codec<RecursiveFeatureSetEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("configured_feature").forGetter(RecursiveFeatureSetEntry::getFeatureSetLocation),
            Codec.INT.fieldOf("weight").forGetter(RecursiveFeatureSetEntry::getWeight)
    ).apply(instance, RecursiveFeatureSetEntry::new));

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "recursive");

    private final ResourceLocation featureSetLocation;
    private FeatureSet featureSet;

    public RecursiveFeatureSetEntry(ResourceLocation location, int weight) {
        super(weight);
        this.featureSetLocation = location;
    }

    public ResourceLocation getFeatureSetLocation() {
        return this.featureSetLocation;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public void place(ServerLevel level, BlockPos pos) {
        if (this.featureSet == null) {
            this.featureSet = level.registryAccess().lookupOrThrow(FeatureSet.KEY).get(this.featureSetLocation).orElseThrow().value();
        }
        this.featureSet.place(level, pos);
    }
}

