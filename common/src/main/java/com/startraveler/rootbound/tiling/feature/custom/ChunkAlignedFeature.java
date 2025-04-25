package com.startraveler.rootbound.tiling.feature.custom;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public abstract class ChunkAlignedFeature<T extends FeatureConfiguration> extends Feature<T> {

    public ChunkAlignedFeature(Codec<T> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<T> featurePlaceContext) {
        WorldGenLevel level = featurePlaceContext.level();
        RandomSource random = featurePlaceContext.random();
        BlockPos origin = featurePlaceContext.origin();
        ChunkAccess chunk = level.getChunk(origin);

        int y = origin.getY();
        return this.place(
                featurePlaceContext.config(),
                level,
                random,
                chunk,
                chunk.getPos().getBlockAt(0, (y >> 4) << 4, 0)
        );
    }

    protected abstract boolean place(FeatureConfiguration config, WorldGenLevel level, RandomSource random, ChunkAccess chunk, BlockPos origin);

}