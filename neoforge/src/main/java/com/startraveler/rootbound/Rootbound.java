package com.startraveler.rootbound;


import com.startraveler.rootbound.blocktransformer.BlockTransformer;
import com.startraveler.rootbound.featureset.FeatureSet;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod(Constants.MOD_ID)
public class Rootbound {

    protected static Rootbound INSTANCE;
    protected final Set<WoodSet> woodSets;

    public Rootbound(IEventBus modBus) {
        assert (INSTANCE == null);
        INSTANCE = this;

        this.woodSets = new HashSet<>();

        // Use NeoForge to bootstrap the Common mod.
        CommonClass.init();

        modBus.addListener(this::registerDatapackRegistries);
    }

    @SuppressWarnings("unused")
    public static void initializeWoodSets(IEventBus modBus, Set<WoodSet> sets) {

        modBus.addListener(
                RegisterCapabilitiesEvent.class, event -> {
                    // Boats, modified from CapabilityHooks.
                    List<? extends EntityType<? extends Container>> woodSetChestBoats = sets.stream()
                            .map(woodSet -> woodSet.getChestBoat().get())
                            .toList();
                    for (EntityType<? extends Container> entityType : woodSetChestBoats) {
                        event.registerEntity(
                                Capabilities.Item.ENTITY,
                                entityType,
                                (entity, ctx) -> VanillaContainerWrapper.of(entity)
                        );
                        event.registerEntity(
                                Capabilities.Item.ENTITY_AUTOMATION,
                                entityType,
                                (entity, ctx) -> VanillaContainerWrapper.of(entity)
                        );
                    }
                }
        );

        modBus.addListener(
                BlockEntityTypeAddBlocksEvent.class, event -> {
                    for (WoodSet woodSet : sets) {
                        event.modify(BlockEntityType.SIGN, woodSet.getSign().get(), woodSet.getWallSign().get());
                        event.modify(
                                BlockEntityType.HANGING_SIGN,
                                woodSet.getHangingSign().get(),
                                woodSet.getWallHangingSign().get()
                        );
                        event.modify(BlockEntityType.SHELF, woodSet.getShelf().get());

                    }
                }
        );
        modBus.addListener(
                FMLCommonSetupEvent.class, event -> event.enqueueWork(() -> {
                    for (WoodSet woodSet : sets) {
                        woodSet.registerFlammability(((FireBlock) Blocks.FIRE)::setFlammable);
                        woodSet.registerDispenserBehaviors();
                    }
                })
        );

        modBus.addListener(AddServerReloadListenersEvent.class,
                event -> event.addListener(
                        Identifier.fromNamespaceAndPath(Constants.MOD_ID, "clear_cache"),
                        BlockTransformer.SAFE_CACHE
                )
        );
    }

    public void registerDatapackRegistries(final DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BlockTransformer.KEY, BlockTransformer.CODEC, BlockTransformer.CODEC);
        event.dataPackRegistry(FeatureSet.KEY, FeatureSet.CODEC, FeatureSet.CODEC);
    }
}