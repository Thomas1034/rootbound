package com.startraveler.rootbound.data;


import com.startraveler.rootbound.woodset.WoodSet;
import com.startraveler.rootbound.registration.RegistryObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RootboundBlockLootTableProvider extends BlockLootSubProvider {

    protected final Set<Block> knownBlocks;
    protected final Set<WoodSet> woodSets;

    public RootboundBlockLootTableProvider(HolderLookup.Provider registries, Set<WoodSet> woodSets) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        this.knownBlocks = new HashSet<>();
        this.woodSets = woodSets;
    }

    @Override
    protected void generate() {

        // For the wood set
        for (WoodSet woodSet : woodSets) {
            generateFor(woodSet);
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return this.knownBlocks;
    }

    protected void generateFor(WoodSet woodSet) {

        this.knownBlocks.addAll(woodSet.getBlockProvider()
                .getEntries()
                .stream()
                .map(RegistryObject::get)
                .collect(Collectors.toSet()));

        this.dropSelf(woodSet.getPlanks().get());
        this.dropSelf(woodSet.getLog().get());
        this.dropSelf(woodSet.getWood().get());
        this.dropSelf(woodSet.getStrippedLog().get());
        this.dropSelf(woodSet.getStrippedWood().get());
        this.dropSelf(woodSet.getFence().get());
        this.dropSelf(woodSet.getFenceGate().get());
        this.dropSelf(woodSet.getStairs().get());
        this.dropSelf(woodSet.getButton().get());
        this.dropSelf(woodSet.getPressurePlate().get());
        this.dropSelf(woodSet.getTrapdoor().get());
        this.dropOther(woodSet.getWallSign().get(), woodSet.getSignItem().get());
        this.dropOther(woodSet.getSign().get(), woodSet.getSignItem().get());
        this.dropOther(woodSet.getWallHangingSign().get(), woodSet.getHangingSignItem().get());
        this.dropOther(woodSet.getHangingSign().get(), woodSet.getHangingSignItem().get());
        this.add(woodSet.getSlab().get(), this.createSlabItemTable(woodSet.getSlab().get()));
        this.add(woodSet.getDoor().get(), this.createDoorTable(woodSet.getDoor().get()));

        if (woodSet.hasMosaic()) {
            this.dropSelf(woodSet.getMosaic().get());
            this.dropSelf(woodSet.getMosaicStairs().get());
            this.add(woodSet.getMosaicSlab().get(), this.createSlabItemTable(woodSet.getMosaicSlab().get()));
        }
    }
}

