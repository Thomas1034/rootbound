package com.startraveler.rootbound.tiling.feature.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record TilingFeatureConfiguration(ResourceLocation tileSet) implements FeatureConfiguration {

    public static final Codec<TilingFeatureConfiguration> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                    ResourceLocation.CODEC.fieldOf("tile_set").forGetter(TilingFeatureConfiguration::tileSet))
            .apply(instance, TilingFeatureConfiguration::new));


}
