package com.startraveler.rootbound.data;

import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.registration.RegistryObject;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RootboundModelProvider extends ModelProvider {

    protected final Set<WoodSet> woodSets;
    protected final Set<Block> knownBlocks;
    protected final Set<Item> knownItems;
    private BlockModelGenerators blockModels;
    private ItemModelGenerators itemModels;

    public RootboundModelProvider(PackOutput output, Set<WoodSet> woodSets) {
        super(output, Constants.MOD_ID);
        this.woodSets = woodSets;
        this.knownBlocks = woodSets.stream()
                .map(WoodSet::getBlockProvider)
                .flatMap(provider -> provider.getEntries().stream())
                .map(
                        RegistryObject::get)
                .collect(Collectors.toSet());
        this.knownItems = woodSets.stream()
                .map(WoodSet::getItemProvider)
                .flatMap(provider -> provider.getEntries().stream())
                .map(
                        RegistryObject::get)
                .collect(Collectors.toSet());
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        this.blockModels = blockModels;
        this.itemModels = itemModels;


        for (WoodSet woodSet : this.woodSets) {
            generateFor(woodSet);
        }
    }

    @Override
    public Stream<? extends Holder<Block>> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.listElements().filter((holder) -> this.knownBlocks.contains(holder.value()));
    }

    @Override
    public Stream<? extends Holder<Item>> getKnownItems() {
        return BuiltInRegistries.ITEM.listElements().filter((holder) -> this.knownItems.contains(holder.value()));
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