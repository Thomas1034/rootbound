package com.startraveler.rootbound.tiling.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.startraveler.rootbound.util.Misc;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record ChunkLinkAttachment(ResourceLocation north,
        ResourceLocation south,
        ResourceLocation east,
        ResourceLocation west,
        ResourceLocation up,
        ResourceLocation down) {


    public static final Codec<ChunkLinkAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("north").forGetter(Misc.optionalWrapper(ChunkLinkAttachment::north)),
            ResourceLocation.CODEC.optionalFieldOf("south").forGetter(Misc.optionalWrapper(ChunkLinkAttachment::south)),
            ResourceLocation.CODEC.optionalFieldOf("east").forGetter(Misc.optionalWrapper(ChunkLinkAttachment::east)),
            ResourceLocation.CODEC.optionalFieldOf("west").forGetter(Misc.optionalWrapper(ChunkLinkAttachment::west)),
            ResourceLocation.CODEC.optionalFieldOf("up").forGetter(Misc.optionalWrapper(ChunkLinkAttachment::up)),
            ResourceLocation.CODEC.optionalFieldOf("down").forGetter(Misc.optionalWrapper(ChunkLinkAttachment::down))
    ).apply(instance, ChunkLinkAttachment::fromOptionals));

    public static ChunkLinkAttachment empty() {
        return new ChunkLinkAttachment(null, null, null, null, null, null);
    }


    public static ChunkLinkAttachment fromOptionals(Optional<ResourceLocation> north, Optional<ResourceLocation> south, Optional<ResourceLocation> east, Optional<ResourceLocation> west, Optional<ResourceLocation> up, Optional<ResourceLocation> down) {
        return new ChunkLinkAttachment(
                north.orElse(null),
                south.orElse(null),
                east.orElse(null),
                west.orElse(null),
                up.orElse(null),
                down.orElse(null)
        );
    }

    public ChunkLinkAttachment set(Direction direction, ResourceLocation location) {
        return switch (direction) {
            case NORTH -> new ChunkLinkAttachment(location, this.south, this.east, this.west, this.up, this.down);
            case SOUTH -> new ChunkLinkAttachment(this.north, location, this.east, this.west, this.up, this.down);
            case EAST -> new ChunkLinkAttachment(this.north, this.south, location, this.west, this.up, this.down);
            case WEST -> new ChunkLinkAttachment(this.north, this.south, this.east, location, this.up, this.down);
            case UP -> new ChunkLinkAttachment(this.north, this.south, this.east, this.west, location, this.down);
            case DOWN -> new ChunkLinkAttachment(this.north, this.south, this.east, this.west, this.up, location);
        };
    }

    public ResourceLocation get(Direction direction) {
        return switch (direction) {
            case NORTH -> this.north;
            case SOUTH -> this.south;
            case EAST -> this.east;
            case WEST -> this.west;
            case UP -> this.up;
            case DOWN -> this.down;
        };
    }
}
