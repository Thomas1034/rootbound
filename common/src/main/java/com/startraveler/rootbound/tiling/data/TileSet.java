package com.startraveler.rootbound.tiling.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.util.AliasBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TileSet {

    public static final Codec<TileSet> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(TileSet::name),
            Codec.INT.fieldOf("internal_weight").forGetter(TileSet::internalWeight),
            Codec.list(Codec.mapPair(ResourceLocation.CODEC.fieldOf("location"), Codec.INT.fieldOf("weight")).codec())
                    .fieldOf("tile_sets")
                    .forGetter(TileSet::tileSets),
            Codec.list(Codec.mapPair(ResourceLocation.CODEC.fieldOf("location"), Codec.INT.fieldOf("weight")).codec())
                    .fieldOf("tiles")
                    .forGetter(TileSet::tiles)
    ).apply(instance, TileSet::new));
    public static final ResourceKey<Registry<TileSet>> KEY = ResourceKey.createRegistryKey(Constants.location("tile_set"));
    protected final List<Pair<TileSet, Integer>> cachedTileSets;
    protected final List<Pair<StructureTile, Integer>> cachedTiles;
    protected final List<Pair<StructureTile, Integer>> resolvedTiles;
    private final ResourceLocation name;
    private final int internalWeight;
    private final List<Pair<ResourceLocation, Integer>> tileSets;
    private final List<Pair<ResourceLocation, Integer>> tiles;

    public TileSet(ResourceLocation name, int internalWeight, List<Pair<ResourceLocation, Integer>> tileSets, List<Pair<ResourceLocation, Integer>> tiles) {
        this.name = name;
        this.internalWeight = internalWeight;
        this.tileSets = tileSets;
        this.tiles = tiles;
        this.cachedTileSets = new ArrayList<>();
        this.cachedTiles = new ArrayList<>();
        this.resolvedTiles = new ArrayList<>();
    }

    public Function<RandomSource, Triple<StructureTile, Rotation, Mirror>> getTilesForNeighbors(RegistryAccess access, EnumMap<Direction, TileConnection> connections) {

        Map<Triple<StructureTile, Rotation, Mirror>, Integer> filteredEntries = this.resolve(access)
                .stream()
                .flatMap(pair -> pair.getFirst()
                        .getMatchingContexts(access, connections)
                        .stream()
                        .map(ctx -> new Pair<>(
                                new ImmutableTriple<>(pair.getFirst(), ctx.getFirst(), ctx.getSecond()),
                                pair.getSecond()
                        )))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        System.out.println("There are " + filteredEntries.size() + " entries.");
        return AliasBuilder.build(filteredEntries);
    }

    protected List<Pair<StructureTile, Integer>> resolve(RegistryAccess access) {
        return this.resolve(access, null);
    }

    protected List<Pair<StructureTile, Integer>> resolve(RegistryAccess access, Deque<ResourceLocation> parentStack) {

        if (this.resolvedTiles.isEmpty()) {
            if (parentStack == null) {
                parentStack = new ArrayDeque<>();
            }
            // Make Java happy; external variables in a lambda expression
            // must be final or effectively final.
            final Deque<ResourceLocation> parents = parentStack;
            // Get registries for both objects in question.
            Registry<TileSet> tileSetRegistry = access.lookupOrThrow(TileSet.KEY);
            Registry<StructureTile> tileRegistry = access.lookupOrThrow(StructureTile.KEY);

            // Reset the cache. This should be unnecessary, since this should only ever
            // be called once, but it makes me feel slightly better, so I'm keeping it.
            this.cachedTileSets.clear();
            // Look up every tile set by name and cache it.
            this.cachedTileSets.addAll(this.tileSets.stream()
                    .map(pair -> new Pair<>(
                            tileSetRegistry.getOptional(pair.getFirst()).orElseThrow(),
                            pair.getSecond()
                    ))
                    .collect(Collectors.toSet()));
            // Reset the cache.
            this.cachedTiles.clear();
            // Look up every individual tile by name and cache it.
            this.cachedTiles.addAll(this.tiles.stream()
                    .map(pair -> new Pair<>(tileRegistry.getOptional(pair.getFirst()).orElseThrow(), pair.getSecond()))
                    .collect(Collectors.toSet()));

            // Scale all cached tiles by the internal weight of this set, and add them
            // to the list of resolved tiles.
            this.cachedTiles.stream()
                    .map(pair -> new Pair<>(pair.getFirst(), pair.getSecond() * this.internalWeight))
                    .forEach(this.resolvedTiles::add);

            // Enter a new context for this tile set, to prevent recursion.
            parents.push(this.name);
            // Recursively add all elements from all the nested tile sets.
            // This is somewhat complicated.
            this.cachedTileSets.forEach(tilePair -> (parents.contains(tilePair.getFirst()
                    .name()) ? ((Supplier<Pair<TileSet, Integer>>) (() -> {
                // Prevent infinite recursion by throwing an exception if
                // the nested set is a duplicate of one higher in the hierarchy.
                throw new IllegalStateException("Detected recursion in TileSet " + this.name + ", exiting to prevent infinite loop");
            })).get() : tilePair).getFirst()
                    .resolve(access, parents)
                    .stream()
                    .map(pair -> new Pair<>(pair.getFirst(), pair.getSecond() * tilePair.getSecond()))
                    .forEach(this.resolvedTiles::add));
            // Exit the context for this tile set.
            parents.pop();
        }
        return this.resolvedTiles;
    }

    public ResourceLocation name() {
        return name;
    }

    public int internalWeight() {
        return internalWeight;
    }

    public List<Pair<ResourceLocation, Integer>> tileSets() {
        return tileSets;
    }

    public List<Pair<ResourceLocation, Integer>> tiles() {
        return tiles;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tileSets, tiles);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (TileSet) obj;
        return Objects.equals(this.name, that.name) && Objects.equals(
                this.internalWeight,
                that.internalWeight
        ) && Objects.equals(this.tileSets, that.tileSets) && Objects.equals(this.tiles, that.tiles);
    }

    @Override
    public String toString() {
        return "TileSet[" + "name=" + name + ", " + "internalWeight=" + internalWeight + ", " + "tileSets=" + tileSets + ", " + "tiles=" + tiles + ']';
    }


}
