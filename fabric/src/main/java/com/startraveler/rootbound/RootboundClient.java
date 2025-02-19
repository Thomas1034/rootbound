package com.startraveler.rootbound;

import com.startraveler.rootbound.woodset.WoodSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BoatRenderer;

import java.util.Set;

public class RootboundClient implements ClientModInitializer {

    public static void initializeWoodSets(Set<WoodSet> sets) {

        for (WoodSet woodSet : sets) {
            BlockRenderLayerMap.INSTANCE.putBlock(woodSet.getDoor().get(), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(woodSet.getTrapdoor().get(), RenderType.cutout());
            ModelLayerLocation boat = ModelLayers.register("boat/" + woodSet.getName());
            ModelLayerLocation chestBoat = ModelLayers.register("chest_boat/" + woodSet.getName());
            EntityModelLayerRegistry.registerModelLayer(boat, BoatModel::createBoatModel);
            EntityModelLayerRegistry.registerModelLayer(chestBoat, BoatModel::createChestBoatModel);
            EntityRendererRegistry.register(woodSet.getBoat().get(), (context) -> new BoatRenderer(context, boat));
            EntityRendererRegistry.register(
                    woodSet.getChestBoat().get(),
                    (context) -> new BoatRenderer(context, chestBoat)
            );
        }
    }

    // Handles client-only code.
    @Override
    public void onInitializeClient() {
    }


}
