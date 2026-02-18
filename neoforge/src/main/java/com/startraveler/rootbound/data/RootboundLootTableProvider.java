package com.startraveler.rootbound.data;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.NotNull;
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

    private static Identifier sequenceIdForLootTable(ResourceKey<LootTable> lootTable) {
        return lootTable.identifier();
    }

    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        return this.registries.thenCompose((provider) -> this.run(output, provider));
    }

    public @NotNull String getName() {
        return "Rootbound Loot Tables";
    }

    private CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider provider) {
        WritableRegistry<LootTable> lootTableRegistry = new MappedRegistry<>(
                Registries.LOOT_TABLE,
                Lifecycle.experimental()
        );
        Map<RandomSupport.Seed128bit, Identifier> randomSeedForEachIdentifier = new Object2ObjectOpenHashMap<>();
        getTables().forEach(subProviderEntry -> subProviderEntry.provider()
                .apply(provider)
                .generate((lootTableResourceKey, builder) -> {
                    Identifier lootTableId = sequenceIdForLootTable(lootTableResourceKey);
                    Identifier displacedLootTableId = randomSeedForEachIdentifier.put(
                            RandomSequence.seedForKey(
                                    lootTableId), lootTableId
                    );
                    if (displacedLootTableId != null) {
                        Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + displacedLootTableId + " and " + lootTableResourceKey.identifier());
                    }

                    builder.setRandomSequence(lootTableId);
                    LootTable loottable = builder.setParamSet(subProviderEntry.paramSet()).build();
                    lootTableRegistry.register(lootTableResourceKey, loottable, RegistrationInfo.BUILT_IN);
                }));
        lootTableRegistry.freeze();
        ProblemReporter.Collector problemReporterCollector = new ProblemReporter.Collector();
        HolderGetter.Provider lootTableRegistryAccess = new RegistryAccess.ImmutableRegistryAccess(List.of(
                lootTableRegistry)).freeze();
        ValidationContext lootContextValidationContext = new ValidationContext(
                problemReporterCollector,
                LootContextParamSets.ALL_PARAMS,
                lootTableRegistryAccess
        );

        validate(lootTableRegistry, lootContextValidationContext, problemReporterCollector);

        if (!problemReporterCollector.isEmpty()) {
            problemReporterCollector.forEach((s, problem) -> LOGGER.warn(
                    "Found validation problem in {}: {}",
                    s,
                    problem.description()
            ));
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        } else {
            return CompletableFuture.allOf(lootTableRegistry.entrySet().stream().map(keyLootTablePair -> {
                ResourceKey<LootTable> key = keyLootTablePair.getKey();
                LootTable lootTable = keyLootTablePair.getValue();
                Path path = this.pathProvider.json(key.identifier());
                return DataProvider.saveStable(output, provider, LootTable.DIRECT_CODEC, lootTable, path);
            }).toArray(CompletableFuture[]::new));
        }
    }

    public List<LootTableProvider.SubProviderEntry> getTables() {
        return this.subProviders;
    }

    protected void validate(WritableRegistry<LootTable> lootTableRegistry, ValidationContext validationContext, ProblemReporter.Collector problemreporter$collector) {
        for (ResourceKey<LootTable> resourcekey : Sets.difference(
                this.requiredTables,
                lootTableRegistry.registryKeySet()
        )) {
            problemreporter$collector.report(new LootTableProvider.MissingTableProblem(resourcekey));
        }

        lootTableRegistry.listElements()
                .forEach(lootTableReference -> lootTableReference.value()
                        .validate(validationContext.setContextKeySet(lootTableReference.value().getParamSet())
                                .enterElement(
                                        new ProblemReporter.RootElementPathElement(lootTableReference.key()),
                                        lootTableReference.key()
                                )));
    }

}