package com.startraveler.rootbound;


import com.startraveler.rootbound.blocktransformer.BlockTransformer;
import com.startraveler.rootbound.featureset.FeatureSet;
import com.startraveler.rootbound.woodset.WoodSet;
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
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

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


    public static void initializeWoodSets(IEventBus modBus, Set<WoodSet> sets) {

        modBus.addListener(
                RegisterCapabilitiesEvent.class, event -> {
                    // Boats, modified from CapabilityHooks.
                    List<? extends EntityType<? extends Container>> woodSetChestBoats = sets.stream()
                            .map(woodSet -> woodSet.getChestBoat().get())
                            .toList();
                    for (EntityType<? extends Container> entityType : woodSetChestBoats) {
                        event.registerEntity(
                                Capabilities.ItemHandler.ENTITY,
                                entityType,
                                (entity, ctx) -> new InvWrapper(entity)
                        );
                        event.registerEntity(
                                Capabilities.ItemHandler.ENTITY_AUTOMATION,
                                entityType,
                                (entity, ctx) -> new InvWrapper(entity)
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
                    }
                }
        );
        modBus.addListener(
                FMLCommonSetupEvent.class, event -> {
                    event.enqueueWork(() -> {
                        for (WoodSet woodSet : sets) {
                            woodSet.registerFlammability(((FireBlock) Blocks.FIRE)::setFlammable);
                            woodSet.registerDispenserBehaviors();
                        }
                    });
                }
        );
    }

    public void registerDatapackRegistries(final DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BlockTransformer.KEY, BlockTransformer.CODEC, BlockTransformer.CODEC);
        event.dataPackRegistry(FeatureSet.KEY, FeatureSet.CODEC, FeatureSet.CODEC);
    }
}