package com.startraveler.rootbound.tiling.feature.custom;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ChessboardFeature extends ChunkAlignedFeature<NoneFeatureConfiguration> {
    public ChessboardFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean place(FeatureConfiguration config, WorldGenLevel level, RandomSource random, ChunkAccess chunk, BlockPos origin) {
        BlockState state = ((Math.abs(chunk.getPos().x + chunk.getPos().z) % 2) == 1 ? Blocks.BLACK_CONCRETE : Blocks.WHITE_CONCRETE).defaultBlockState();
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                level.setBlock(origin.offset(i, 0, j), state, Block.UPDATE_CLIENTS);
            }
        }

        return true;
    }
}
