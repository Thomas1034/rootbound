package com.startraveler.rootbound.data;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RootboundLootTableProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput.PathProvider pathProvider;
    private final Set<ResourceKey<LootTable>> requiredTables;
    private final List<LootTableProvider.SubProviderEntry> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public RootboundLootTableProvider(PackOutput output, Set<ResourceKey<LootTable>> requiredTables, List<LootTableProvider.SubProviderEntry> subProviders, CompletableFuture<HolderLookup.Provider> registries) {
        this.pathProvider = output.createRegistryElementsPathProvider(Registries.LOOT_TABLE);
        this.subProviders = subProviders;
        this.requiredTables = requiredTables;
        this.registries = registries;
    }

    private static ResourceLocation sequenceIdForLootTable(ResourceKey<LootTable> lootTable) {
        return lootTable.location();
    }

    public CompletableFuture<?> run(CachedOutput output) {
        return this.registries.thenCompose((p_323117_) -> this.run(output, p_323117_));
    }

    public final String getName() {
        return "Rootbound Loot Tables";
    }

    private CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider provider) {
        WritableRegistry<LootTable> writableRegistry = new MappedRegistry<>(
                Registries.LOOT_TABLE,
                Lifecycle.experimental()
        );
        Map<RandomSupport.Seed128bit, ResourceLocation> map = new Object2ObjectOpenHashMap<>();
        this.getTables().forEach((entry) -> (entry.provider().apply(provider)).generate((table, builder) -> {
            ResourceLocation sequenceId = sequenceIdForLootTable(table);
            ResourceLocation overwrittenSequenceId = map.put(RandomSequence.seedForKey(sequenceId), sequenceId);
            if (overwrittenSequenceId != null) {
                String locationAsString = String.valueOf(overwrittenSequenceId);
                Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + locationAsString + " and " + table.location());
            }

            builder.setRandomSequence(sequenceId);
            LootTable loottable = builder.setParamSet(entry.paramSet()).build();
            writableRegistry.register(table, loottable, RegistrationInfo.BUILT_IN);
        }));
        writableRegistry.freeze();
        ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
        HolderGetter.Provider frozenRegistries = (new RegistryAccess.ImmutableRegistryAccess(List.of(writableRegistry))).freeze();
        ValidationContext validationcontext = new ValidationContext(
                problemCollector,
                LootContextParamSets.ALL_PARAMS,
                frozenRegistries
        );
        this.validate(writableRegistry, validationcontext, problemCollector);
        Multimap<String, String> multimap = problemCollector.get();
        if (!multimap.isEmpty()) {
            multimap.forEach((p_124446_, p_124447_) -> LOGGER.warn(
                    "Found validation problem in {}: {}",
                    p_124446_,
                    p_124447_
            ));
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        } else {
            return CompletableFuture.allOf(writableRegistry.entrySet().stream().map((entry) -> {
                ResourceKey<LootTable> resourceKey = entry.getKey();
                LootTable loottable = entry.getValue();
                Path path = this.pathProvider.json(resourceKey.location());
                return DataProvider.saveStable(output, provider, LootTable.DIRECT_CODEC, loottable, path);
            }).toArray(CompletableFuture[]::new));
        }
    }

    public List<LootTableProvider.SubProviderEntry> getTables() {
        return this.subProviders;
    }

    protected void validate(WritableRegistry<LootTable> writableregistry, ValidationContext validationcontext, ProblemReporter.Collector problemreporter$collector) {

        for (ResourceKey<LootTable> resourcekey : Sets.difference(
                this.requiredTables,
                writableregistry.registryKeySet()
        )) {
            problemreporter$collector.report("Missing built-in table: " + resourcekey.location());
        }

        writableregistry.listElements()
                .forEach((entry) -> entry.value()
                        .validate(validationcontext.setContextKeySet(entry.value().getParamSet())
                                .enterElement("{" + entry.key().location() + "}", entry.key())));
    }

}