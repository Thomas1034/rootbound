package com.startraveler.rootbound.platform.services;

import com.startraveler.rootbound.tiling.attachment.ChunkLinkAttachment;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface IChunkLinkHandler {

    ResourceLocation get(ChunkAccess chunk, int subchunk, Direction direction);

    ChunkLinkAttachment get(ChunkAccess chunk, int subchunk);

    void set(ChunkAccess chunk, int subchunk, Direction direction, ResourceLocation value);
}
