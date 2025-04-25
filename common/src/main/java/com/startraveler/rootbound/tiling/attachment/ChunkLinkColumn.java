package com.startraveler.rootbound.tiling.attachment;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ChunkLinkColumn(Map<Integer, ChunkLinkAttachment> attachments) {

    public static final Codec<ChunkLinkColumn> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.pair(Codec.INT.fieldOf("subchunk").codec(),
                    ChunkLinkAttachment.CODEC.fieldOf("attachment").codec()
            )
            .listOf()
            .fieldOf("attachments")
            .forGetter(ChunkLinkColumn::toPairs)).apply(instance, ChunkLinkColumn::create));

    private static ChunkLinkColumn create(List<Pair<Integer, ChunkLinkAttachment>> entries) {
        Map<Integer, ChunkLinkAttachment> attachments = new HashMap<>();

        for (Pair<Integer, ChunkLinkAttachment> entry : entries) {
            attachments.put(entry.getFirst(), entry.getSecond());
        }

        return new ChunkLinkColumn(Collections.unmodifiableMap(attachments));
    }

    public static ChunkLinkColumn empty() {
        return new ChunkLinkColumn(Map.of());
    }

    private List<Pair<Integer, ChunkLinkAttachment>> toPairs() {
        return this.attachments.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList();
    }

    public ChunkLinkAttachment get(int subchunk) {
        return this.attachments.getOrDefault(subchunk, ChunkLinkAttachment.empty());
    }

    public ChunkLinkColumn set(int subchunk, Direction direction, ResourceLocation value) {
        Map<Integer, ChunkLinkAttachment> attachments = new HashMap<>(this.attachments);
        attachments.put(
                subchunk,
                this.attachments.getOrDefault(subchunk, ChunkLinkAttachment.empty()).set(direction, value)
        );
        return new ChunkLinkColumn(attachments);
    }

    public ChunkLinkColumn set(int subchunk, ChunkLinkAttachment attachment) {
        Map<Integer, ChunkLinkAttachment> attachments = new HashMap<>(this.attachments);
        attachments.put(subchunk, attachment);
        return new ChunkLinkColumn(attachments);
    }

}
