package com.startraveler.rootbound.data;

import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class RootboundBlockTagProvider extends BlockTagsProvider {
    protected final Set<WoodSet> woodSets;

    public RootboundBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, Set<WoodSet> woodSets) {
        this(output, lookupProvider, woodSets, Constants.MOD_ID);
    }

    public RootboundBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, Set<WoodSet> woodSets, String modId) {
        super(output, lookupProvider, modId);
        this.woodSets = woodSets;
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        for (WoodSet woodSet : this.woodSets) {
            generateFor(woodSet);
        }
    }


    public void generateFor(WoodSet woodSet) {
        this.tag(woodSet.getLogs()).add(
                woodSet.getLog().get(),
                woodSet.getWood().get(),
                woodSet.getStrippedLog().get(),
                woodSet.getStrippedWood().get()
        );
        this.tag(BlockTags.MINEABLE_WITH_AXE).add(
                woodSet.getLog().get(),
                woodSet.getWood().get(),
                woodSet.getStrippedLog().get(),
                woodSet.getStrippedWood().get(),
                woodSet.getPlanks().get(),
                woodSet.getSlab().get(),
                woodSet.getStairs().get(),
                woodSet.getFence().get(),
                woodSet.getFenceGate().get(),
                woodSet.getSign().get(),
                woodSet.getWallSign().get(),
                woodSet.getHangingSign().get(),
                woodSet.getWallHangingSign().get(),
                woodSet.getButton().get(),
                woodSet.getPressurePlate().get(),
                woodSet.getDoor().get(),
                woodSet.getTrapdoor().get(),
                woodSet.getShelf().get()
        );
        this.tag(BlockTags.WOODEN_SHELVES).add(woodSet.getShelf().get());
        this.tag(BlockTags.WOODEN_TRAPDOORS).add(woodSet.getTrapdoor().get());
        this.tag(BlockTags.WOODEN_DOORS).add(woodSet.getDoor().get());
        this.tag(BlockTags.WOODEN_SLABS).add(woodSet.getSlab().get());
        this.tag(BlockTags.WOODEN_STAIRS).add(woodSet.getStairs().get());
        this.tag(BlockTags.WOODEN_BUTTONS).add(woodSet.getButton().get());
        this.tag(BlockTags.WOODEN_PRESSURE_PLATES).add(woodSet.getPressurePlate().get());
        this.tag(BlockTags.WOODEN_FENCES).add(woodSet.getFence().get());
        this.tag(BlockTags.PLANKS).add(woodSet.getPlanks().get());
        this.tag(BlockTags.LOGS).add(
                woodSet.getLog().get(),
                woodSet.getWood().get(),
                woodSet.getStrippedLog().get(),
                woodSet.getStrippedWood().get()
        );
        this.tag(Tags.Blocks.STRIPPED_LOGS).add(woodSet.getStrippedLog().get(), woodSet.getStrippedWood().get());
        if (woodSet.isFlammable()) {
            this.tag(BlockTags.LOGS_THAT_BURN).add(
                    woodSet.getLog().get(),
                    woodSet.getWood().get(),
                    woodSet.getStrippedLog().get(),
                    woodSet.getStrippedWood().get()
            );
        }
        this.tag(Tags.Blocks.FENCE_GATES_WOODEN).add(woodSet.getFenceGate().get());
        this.tag(Tags.Blocks.FENCES_WOODEN).add(woodSet.getFence().get());

        if (woodSet.hasMosaic()) {
            this.tag(BlockTags.MINEABLE_WITH_AXE).add(
                    woodSet.getOrThrowMosaic().get(),
                    woodSet.getOrThrowMosaicSlab().get(),
                    woodSet.getOrThrowMosaicStairs().get()
            );
            this.tag(BlockTags.WOODEN_SLABS).add(woodSet.getOrThrowMosaicSlab().get());
            this.tag(BlockTags.WOODEN_STAIRS).add(woodSet.getOrThrowMosaicStairs().get());
        }
    }
}
