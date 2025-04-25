package com.startraveler.rootbound.platform;

import com.startraveler.rootbound.Rootbound;
import com.startraveler.rootbound.platform.services.IChunkLinkHandler;
import com.startraveler.rootbound.tiling.attachment.ChunkLinkAttachment;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;

public class FabricChunkLinkHandler implements IChunkLinkHandler {

    @Override
    public ResourceLocation get(ChunkAccess chunk, int subchunk, Direction direction) {
        if (null == chunk || null == direction) {
            return null;
        }
        return this.get(chunk, subchunk).get(direction);
    }

    @Override
    public ChunkLinkAttachment get(ChunkAccess chunk, int subchunk) {
        if (null == chunk) {
            return null;
        }
        return chunk.getAttachedOrCreate(Rootbound.CHUNK_LINK).get(subchunk);
    }

    @Override
    public void set(ChunkAccess chunk, int subchunk, Direction direction, ResourceLocation value) {
        if (null == chunk || null == direction || null == value) {
            return;
        }
        chunk.setAttached(
                Rootbound.CHUNK_LINK,
                chunk.getAttachedOrCreate(Rootbound.CHUNK_LINK).set(subchunk, direction, value)
        );
    }
}
