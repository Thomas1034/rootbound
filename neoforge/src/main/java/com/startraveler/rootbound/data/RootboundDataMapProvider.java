package com.startraveler.rootbound.data;

import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RootboundDataMapProvider extends DataMapProvider {
    protected final Set<WoodSet> woodSets;

    public RootboundDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, Set<WoodSet> woodSets) {
        super(packOutput, lookupProvider);
        this.woodSets = woodSets;
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        for (WoodSet woodSet : this.woodSets) {
            generateFor(woodSet);
        }
    }

    public void generateFor(WoodSet woodSet) {
        woodSet.registerFuels(this::addFurnaceFuel);
    }

    public void addFurnaceFuel(ItemLike itemLike, int burnTime) {
        this.builder(NeoForgeDataMaps.FURNACE_FUELS)
                .add(BuiltInRegistries.ITEM.wrapAsHolder(itemLike.asItem()), new FurnaceFuel(burnTime), false);
    }
}
