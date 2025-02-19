package com.startraveler.rootbound;

import com.startraveler.rootbound.blocktransformer.BlockTransformer;
import com.startraveler.rootbound.featureset.FeatureSet;
import com.startraveler.rootbound.woodset.WoodSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public class Rootbound implements ModInitializer {

    public static void initializeWoodSets(Set<WoodSet> sets) {
        // Add wood sets.
        for (WoodSet woodSet : sets) {
            BlockEntityType.SIGN.addSupportedBlock(woodSet.getSign().get());
            BlockEntityType.SIGN.addSupportedBlock(woodSet.getWallSign().get());
            BlockEntityType.HANGING_SIGN.addSupportedBlock(woodSet.getHangingSign().get());
            BlockEntityType.HANGING_SIGN.addSupportedBlock(woodSet.getWallHangingSign().get());
            FuelRegistryEvents.BUILD.register((builder, context) -> woodSet.registerFuels((builder::add)));
            StrippableBlockRegistry.register(woodSet.getLog().get(), woodSet.getStrippedLog().get());
            StrippableBlockRegistry.register(woodSet.getWood().get(), woodSet.getStrippedWood().get());
            woodSet.registerDispenserBehaviors();
            woodSet.registerFlammability(FlammableBlockRegistry.getDefaultInstance()::add);
        }
    }

    @Override
    public void onInitialize() {

        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        CommonClass.init();

        DynamicRegistries.registerSynced(BlockTransformer.KEY, BlockTransformer.CODEC);
        DynamicRegistries.registerSynced(FeatureSet.KEY, FeatureSet.CODEC);
    }
}
