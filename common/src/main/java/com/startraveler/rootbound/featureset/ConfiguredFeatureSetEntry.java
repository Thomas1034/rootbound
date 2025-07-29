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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ConfiguredFeatureSetEntry extends FeatureSet.Entry {

    public static final Codec<ConfiguredFeatureSetEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("configured_feature").forGetter(ConfiguredFeatureSetEntry::getConfiguredFeatureLocation),
            Codec.INT.fieldOf("weight").forGetter(ConfiguredFeatureSetEntry::getWeight)
    ).apply(instance, ConfiguredFeatureSetEntry::new));

    public static final ResourceLocation TYPE = Constants.location("configured_feature");

    private final ResourceLocation configuredFeatureLocation;
    private ConfiguredFeature<?, ?> configuredFeature;

    public ConfiguredFeatureSetEntry(ResourceLocation configuredFeature, int weight) {
        super(weight);
        this.configuredFeatureLocation = configuredFeature;
    }

    public ResourceLocation getConfiguredFeatureLocation() {
        return this.configuredFeatureLocation;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    @Override
    public boolean place(ServerLevel level, BlockPos pos) {
        if (this.configuredFeature == null) {
            this.configuredFeature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(this.configuredFeatureLocation).orElseThrow().value();
        }
        // If it's still null, throw.
        return this.configuredFeature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), pos);
    }


}

