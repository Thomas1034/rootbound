package com.startraveler.rootbound;


import com.startraveler.rootbound.data.*;
import com.startraveler.rootbound.registration.RegistryObject;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class RootboundClient {

    public RootboundClient(@SuppressWarnings("unused") IEventBus modBus) {

    }

    @SuppressWarnings({"unused", "deprecation"})
    public static void initializeWoodSets(IEventBus modBus, Set<WoodSet> sets) {

        final Map<RegistryObject<EntityType<?>, EntityType<? extends Boat>>, ModelLayerLocation> locationForBoat = new HashMap<>();
        final Map<RegistryObject<EntityType<?>, EntityType<? extends ChestBoat>>, ModelLayerLocation> locationForChestBoat = new HashMap<>();

        for (WoodSet woodSet : sets) {
            ModelLayerLocation boat = new ModelLayerLocation(
                    Identifier.withDefaultNamespace("boat/" + woodSet.getName()),
                    "main"
            );
            ModelLayerLocation chestBoat = new ModelLayerLocation(
                    Identifier.withDefaultNamespace("chest_boat/" + woodSet.getName()),
                    "main"
            );
            locationForBoat.put(woodSet.getBoat(), boat);
            locationForChestBoat.put(woodSet.getChestBoat(), chestBoat);
        }

        modBus.addListener(
                FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
                    for (WoodSet woodSet : sets) {
                        EntityRenderers.register(
                                woodSet.getBoat().get(),
                                (context) -> new BoatRenderer(context, locationForBoat.get(woodSet.getBoat()))
                        );
                        EntityRenderers.register(
                                woodSet.getChestBoat().get(),
                                (context) -> new BoatRenderer(context, locationForChestBoat.get(woodSet.getChestBoat()))
                        );

                        ItemBlockRenderTypes.setRenderLayer(woodSet.getTrapdoor().get(), ChunkSectionLayer.CUTOUT);
                        ItemBlockRenderTypes.setRenderLayer(woodSet.getDoor().get(), ChunkSectionLayer.CUTOUT);
                    }

                })
        );

        modBus.addListener(
                EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
                    for (WoodSet woodSet : sets) {
                        event.registerLayerDefinition(
                                locationForBoat.get(woodSet.getBoat()),
                                BoatModel::createBoatModel
                        );
                        event.registerLayerDefinition(
                                locationForChestBoat.get(woodSet.getChestBoat()),
                                BoatModel::createChestBoatModel
                        );
                    }
                }
        );

    }

    @SuppressWarnings("unused")
    public static void gatherData(final GatherDataEvent.Client event, Set<WoodSet> woodSets) {
        try {
            // Store some frequently-used fields for later use.
            DataGenerator generator = event.getGenerator();
            PackOutput packOutput = generator.getPackOutput();
            CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

            // Loot tables.
            generator.addProvider(
                    true, new RootboundLootTableProvider(
                            packOutput, Collections.emptySet(), List.of(new LootTableProvider.SubProviderEntry(
                            registries -> new RootboundBlockLootTableProvider(registries, woodSets),
                            LootContextParamSets.BLOCK
                    )), lookupProvider
                    ) {
                        @Override
                        protected void validate(WritableRegistry<LootTable> lootTableRegistry, ValidationContext validationContext, ProblemReporter.Collector collector) {
                            // Do not validate at all, per what people online said.
                        }
                    }
            );

            // Generate data for the recipes
            generator.addProvider(true, new RootboundRecipeProvider.Runner(packOutput, lookupProvider, woodSets));

            // Generate block and item models.
            generator.addProvider(true, new RootboundModelProvider(packOutput, woodSets));

            // Generate data maps for furnace fuel, composters, and such; only used on the NeoForge side.
            generator.addProvider(true, new RootboundDataMapProvider(packOutput, lookupProvider, woodSets));

        } catch (RuntimeException e) {
            Constants.LOG.error("Failed to generate data.", e);
        }
    }


}