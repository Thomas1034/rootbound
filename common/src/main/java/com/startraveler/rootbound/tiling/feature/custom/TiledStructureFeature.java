package com.startraveler.rootbound.tiling.feature.custom;

import com.mojang.serialization.Codec;
import com.startraveler.rootbound.platform.Services;
import com.startraveler.rootbound.tiling.data.StructureTile;
import com.startraveler.rootbound.tiling.data.TileConnection;
import com.startraveler.rootbound.tiling.data.TileSet;
import com.startraveler.rootbound.tiling.feature.configuration.TilingFeatureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.tuple.Triple;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TiledStructureFeature extends ChunkAlignedFeature<TilingFeatureConfiguration> {

    protected final Map<ResourceLocation, TileSet> tileSetCache;

    public TiledStructureFeature(Codec<TilingFeatureConfiguration> codec) {
        super(codec);
        this.tileSetCache = new HashMap<>();
    }

    @Override
    protected boolean place(FeatureConfiguration config, WorldGenLevel level, RandomSource random, ChunkAccess chunk, BlockPos origin) {
        TilingFeatureConfiguration tilingConfig = (TilingFeatureConfiguration) config;
        ResourceLocation tileSetLocation = tilingConfig.tileSet();


        TileSet tileSet = this.getTileSet(level.registryAccess(), tileSetLocation);

        ChunkPos pos = chunk.getPos();
        // Get the y of the subchunk.
        int y = origin.getY() >> 4;

        // Get the tile connection registry.
        Registry<TileConnection> tileConnectionRegistry = level.registryAccess().lookupOrThrow(TileConnection.KEY);

        EnumMap<Direction, TileConnection> neighboringConnections = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            ChunkAccess neighborAccess = level.getChunk(pos.x + direction.getStepX(), pos.z + direction.getStepZ());
            int localY = y + direction.getStepY();

            ResourceLocation link = Services.CHUNK_LINK_GETTER.get(neighborAccess, localY, direction);
            if (link != null) {
                neighboringConnections.put(direction, tileConnectionRegistry.getValue(link));
            }
        }
        Function<RandomSource, Triple<StructureTile, Rotation, Mirror>> tiles = tileSet.getTilesForNeighbors(
                level.registryAccess(),
                neighboringConnections
        );

        Triple<StructureTile, Rotation, Mirror> tileWithSettings = tiles.apply(level.getRandom());
        StructureTile tile = tileWithSettings.getLeft();

        StructurePlaceSettings settings = new StructurePlaceSettings().setRandom(level.getRandom())
                .setRotation(tileWithSettings.getMiddle())
                .setMirror(tileWithSettings.getRight());

        EnumMap<Direction, TileConnection> connectionsToPut = tile.getConnectionsFor(
                level.registryAccess(),
                tileWithSettings.getMiddle(),
                tileWithSettings.getRight()
        );
        connectionsToPut.forEach((d, c) -> Services.CHUNK_LINK_GETTER.set(chunk, y, d, c.name()));

        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
        StructureTemplate structureTemplate = templateManager.getOrCreate(tile.structure());

        structureTemplate.placeInWorld(
                level,
                BlockPos.ZERO,
                origin,
                settings,
                level.getRandom(),
                4 /* no idea what this means */
        );

        return true;
    }

    protected TileSet getTileSet(RegistryAccess access, ResourceLocation tileSetLocation) {

        TileSet tileSet = this.tileSetCache.get(tileSetLocation);

        if (tileSet != null) {
            return tileSet;
        }

        Registry<TileSet> tileSetRegistry = access.lookupOrThrow(TileSet.KEY);
        tileSet = tileSetRegistry.getOptional(tileSetLocation).orElseThrow();

        this.tileSetCache.put(tileSetLocation, tileSet);
        return tileSet;
    }
}
