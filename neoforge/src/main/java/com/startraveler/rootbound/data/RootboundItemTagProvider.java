package com.startraveler.rootbound.data;

import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RootboundItemTagProvider extends ItemTagsProvider {
    private final Set<WoodSet> woodSets;

    public RootboundItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, Set<WoodSet> woodSets) {
        super(output, lookupProvider, blockTags, Constants.MOD_ID);
        this.woodSets = woodSets;
    }


    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (WoodSet woodSet : this.woodSets) {
            generateFor(woodSet);
        }

    }

    public void generateFor(WoodSet woodSet) {
        this.tag(woodSet.getLogItems()).add(
                woodSet.getLog().get().asItem(),
                woodSet.getWood().get().asItem(),
                woodSet.getStrippedLog().get().asItem(),
                woodSet.getStrippedWood().get().asItem()
        );
        this.tag(ItemTags.WOODEN_SLABS).add(woodSet.getSlab().get().asItem());
        this.tag(ItemTags.WOODEN_STAIRS).add(woodSet.getStairs().get().asItem());
        this.tag(ItemTags.WOODEN_BUTTONS).add(woodSet.getButton().get().asItem());
        this.tag(ItemTags.WOODEN_PRESSURE_PLATES).add(woodSet.getPressurePlate().get().asItem());
        this.tag(ItemTags.WOODEN_FENCES).add(woodSet.getFence().get().asItem());
        this.tag(ItemTags.PLANKS).add(woodSet.getPlanks().get().asItem());
        this.tag(ItemTags.LOGS).add(
                woodSet.getLog().get().asItem(),
                woodSet.getWood().get().asItem(),
                woodSet.getStrippedLog().get().asItem(),
                woodSet.getStrippedWood().get().asItem()
        );
        if (woodSet.isFlammable()) {
            this.tag(ItemTags.LOGS_THAT_BURN).add(
                    woodSet.getLog().get().asItem(),
                    woodSet.getWood().get().asItem(),
                    woodSet.getStrippedLog().get().asItem(),
                    woodSet.getStrippedWood().get().asItem()
            );
        }
        this.tag(ItemTags.SIGNS).add(woodSet.getSignItem().get());
        this.tag(ItemTags.HANGING_SIGNS).add(woodSet.getHangingSignItem().get());
        this.tag(ItemTags.WOODEN_DOORS).add(woodSet.getDoor().get().asItem());
        this.tag(ItemTags.WOODEN_TRAPDOORS).add(woodSet.getTrapdoor().get().asItem());
        this.tag(Tags.Items.FENCE_GATES_WOODEN).add(woodSet.getFenceGate().get().asItem());
        this.tag(Tags.Items.FENCES_WOODEN).add(woodSet.getFence().get().asItem());
    }
}
