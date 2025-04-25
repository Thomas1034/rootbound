package com.startraveler.rootbound;

import com.startraveler.rootbound.tiling.attachment.ChunkLinkAttachment;
import com.startraveler.rootbound.tiling.attachment.ChunkLinkColumn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class AttachmentRegistry {

    // Create the DeferredRegister for attachment types
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,
            Constants.MOD_ID
    );

    public static final Supplier<AttachmentType<ChunkLinkColumn>> CHUNK_LINK = ATTACHMENT_TYPES.register(
            "chunk_link",
            () -> AttachmentType.builder(ChunkLinkColumn::empty).serialize(ChunkLinkColumn.CODEC).build()
    );


    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
