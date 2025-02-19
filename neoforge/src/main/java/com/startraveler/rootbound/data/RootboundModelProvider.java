package com.startraveler.rootbound.data;

import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class RootboundModelProvider extends ModelProvider {

    protected final Set<WoodSet> woodSets;
    private BlockModelGenerators blockModels;
    private ItemModelGenerators itemModels;

    public RootboundModelProvider(PackOutput output, Set<WoodSet> woodSets) {
        super(output, Constants.MOD_ID);
        this.woodSets = woodSets;
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        this.blockModels = blockModels;
        this.itemModels = itemModels;

        for (WoodSet woodSet : this.woodSets) {
            generateFor(woodSet);
        }
    }

    protected void generateFor(WoodSet woodSet) {
        Block planks = woodSet.getPlanks().get();

        blockModels.family(woodSet.getPlanks().get()).generateFor(woodSet.getFamily());
        blockModels.createHangingSign(planks, woodSet.getHangingSign().get(), woodSet.getWallHangingSign().get());
        blockModels.createAxisAlignedPillarBlock(woodSet.getLog().get(), TexturedModel.COLUMN);
        blockModels.createAxisAlignedPillarBlock(woodSet.getStrippedLog().get(), TexturedModel.COLUMN);
        blockModels.createAxisAlignedPillarBlock(woodSet.getWood().get(), TexturedModel.COLUMN);
        blockModels.createAxisAlignedPillarBlock(woodSet.getStrippedWood().get(), TexturedModel.COLUMN);

        basicItem(woodSet.getBoatItem().get());
        basicItem(woodSet.getChestBoatItem().get());
    }

    private void basicItem(Item item) {
        itemModels.generateFlatItem(item, item, ModelTemplates.FLAT_ITEM);
    }

}