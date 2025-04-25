package com.startraveler.rootbound.tiling.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.startraveler.rootbound.Constants;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class StructureTile {

    public static final ResourceKey<Registry<StructureTile>> KEY = ResourceKey.createRegistryKey(Constants.location(
            "structure_tile"));


    public static final Codec<StructureTile> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(StructureTile::name),
            ResourceLocation.CODEC.fieldOf("structure").forGetter(StructureTile::structure),
            Codec.unboundedMap(Direction.CODEC, ResourceLocation.CODEC)
                    .fieldOf("connections")
                    .forGetter(StructureTile::connections)
    ).apply(instance, StructureTile::new));
    protected static final Rotation[] COUNTERCLOCKWISE_ROTATIONS = new Rotation[]{Rotation.NONE,
            Rotation.COUNTERCLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.CLOCKWISE_90};
    protected final Map<Direction, TileConnection> cachedConnections;
    private final ResourceLocation name;
    private final ResourceLocation structure;
    private final Map<Direction, ResourceLocation> connections;
    protected StructureTemplate template;

    public StructureTile(ResourceLocation name, ResourceLocation structure, Map<Direction, ResourceLocation> connections) {
        this.name = name;
        this.structure = structure;
        this.connections = connections;
        this.cachedConnections = new HashMap<>();
    }

    /**
     * Rotates the given direction-to-connection map by the specified number of 90-degree steps.
     * Returns a new rotated map without modifying the original.
     *
     * @param map   The original direction-to-connection map.
     * @param steps The number of 90-degree steps to rotate (positive for clockwise, negative for counterclockwise).
     * @return A new EnumMap with rotated keys.
     * <p>
     * Credit: ChatGPT
     */
    public static EnumMap<Direction, TileConnection> rotateMap(EnumMap<Direction, TileConnection> map, int steps) {
        EnumMap<Direction, TileConnection> rotatedMap = new EnumMap<>(Direction.class);

        // Normalize steps to be within {0, 1, 2, 3} (mod 4)
        int normalizedSteps = Math.floorMod(steps, 4);

        for (Map.Entry<Direction, TileConnection> entry : map.entrySet()) {
            Direction rotatedDirection = rotateDirection(entry.getKey(), normalizedSteps);
            rotatedMap.put(rotatedDirection, entry.getValue());
        }

        return rotatedMap;
    }

    public static EnumMap<Direction, TileConnection> rotateMap(EnumMap<Direction, TileConnection> map, Rotation rotation) {
        EnumMap<Direction, TileConnection> mirroredMap = new EnumMap<>(Direction.class);

        mirroredMap.putAll(map);

        if (rotation == Rotation.CLOCKWISE_90) {
            mirroredMap.put(Direction.NORTH, map.get(Direction.EAST));
            mirroredMap.put(Direction.EAST, map.get(Direction.SOUTH));
            mirroredMap.put(Direction.SOUTH, map.get(Direction.WEST));
            mirroredMap.put(Direction.WEST, map.get(Direction.NORTH));
        } else if (rotation == Rotation.CLOCKWISE_180) {
            mirroredMap.put(Direction.NORTH, map.get(Direction.SOUTH));
            mirroredMap.put(Direction.EAST, map.get(Direction.WEST));
            mirroredMap.put(Direction.SOUTH, map.get(Direction.NORTH));
            mirroredMap.put(Direction.WEST, map.get(Direction.EAST));
        } else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
            mirroredMap.put(Direction.NORTH, map.get(Direction.WEST));
            mirroredMap.put(Direction.EAST, map.get(Direction.NORTH));
            mirroredMap.put(Direction.SOUTH, map.get(Direction.EAST));
            mirroredMap.put(Direction.WEST, map.get(Direction.SOUTH));
        }

        return mirroredMap;
    }

    public static EnumMap<Direction, TileConnection> mirrorMap(EnumMap<Direction, TileConnection> map, Mirror mirror) {
        EnumMap<Direction, TileConnection> mirroredMap = new EnumMap<>(Direction.class);

        mirroredMap.putAll(map);

        if (mirror == Mirror.LEFT_RIGHT) {
            mirroredMap.put(Direction.EAST, map.get(Direction.WEST));
            mirroredMap.put(Direction.WEST, map.get(Direction.EAST));
        } else if (mirror == Mirror.FRONT_BACK) {
            mirroredMap.put(Direction.NORTH, map.get(Direction.SOUTH));
            mirroredMap.put(Direction.SOUTH, map.get(Direction.NORTH));
        }

        return mirroredMap;
    }


    /**
     * Rotates a direction within the horizontal plane.
     * UP and DOWN remain unchanged.
     *
     * @param direction The original direction.
     * @param steps     The number of 90-degree rotations (0-3).
     * @return The rotated direction.
     * <p>
     * Credit: ChatGPT
     */
    private static Direction rotateDirection(Direction direction, int steps) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return direction; // Keep UP and DOWN unchanged
        }

        return switch (steps) {
            case 1 -> switch (direction) { // CLOCKWISE_90
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> throw new IllegalStateException("Unexpected value: " + direction);
            };
            case 2 -> switch (direction) { // CLOCKWISE_180
                case NORTH -> Direction.SOUTH;
                case EAST -> Direction.WEST;
                case SOUTH -> Direction.NORTH;
                case WEST -> Direction.EAST;
                default -> throw new IllegalStateException("Unexpected value: " + direction);
            };
            case 3 -> switch (direction) { // COUNTERCLOCKWISE_90
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                default -> throw new IllegalStateException("Unexpected value: " + direction);
            };
            default -> direction; // No rotation (0 steps)
        };
    }

    protected TileConnection getConnection(RegistryAccess access, Direction direction) {
        if (this.cachedConnections.get(direction) instanceof TileConnection tileConnection) {
            return tileConnection;
        }

        System.out.println("Looking up " + this.connections.get(direction));
        TileConnection tileConnection = TileConnection.lookup(access, this.connections.get(direction));
        System.out.println("Found " + tileConnection);
        this.cachedConnections.put(direction, tileConnection);
        return tileConnection;
    }

    public EnumMap<Direction, TileConnection> getConnectionsFor(RegistryAccess access, Rotation rotation, Mirror mirror /* on the wall */) {
        EnumMap<Direction, TileConnection> original = new EnumMap<>(Direction.class);
        original.putAll(Arrays.stream(Direction.values())
                .map(dir -> new Pair<>(dir, this.getConnection(access, dir)))
                .filter(p -> p.getSecond() != null)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
        return mirrorMap(rotateMap(original, rotation), mirror);
    }

    protected boolean matches(RegistryAccess access, EnumMap<Direction, TileConnection> connections) {

        for (Map.Entry<Direction, TileConnection> entry : connections.entrySet()) {
            if (!this.getConnection(access, entry.getKey()).matches(access, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public Set<Pair<Rotation, Mirror>> getMatchingContexts(RegistryAccess access, EnumMap<Direction, TileConnection> connections) {
        Set<Pair<Rotation, Mirror>> validContexts = new HashSet<>();
        System.out.println("Getting matching contexts for " + this.name + " with the map " + this.connections);
        for (int i = 0; i < 3; i++) {

            // Rotate the map backwards!
            EnumMap<Direction, TileConnection> rotated = rotateMap(connections, 4 - i);
            for (Mirror mirror : Mirror.values()) {
                EnumMap<Direction, TileConnection> mirrored = mirrorMap(rotated, mirror);
                if (this.matches(access, mirrored)) {
                    validContexts.add(Pair.of(COUNTERCLOCKWISE_ROTATIONS[i], mirror));
                }
            }

        }
        return validContexts;
    }

    public StructureTemplate getTemplate(MinecraftServer server) {
        if (this.template != null) {
            return this.template;
        }
        this.template = server.getStructureManager().getOrCreate(this.structure);

        return this.template;
    }

    public ResourceLocation name() {
        return name;
    }

    public ResourceLocation structure() {
        return structure;
    }

    public Map<Direction, ResourceLocation> connections() {
        return connections;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, structure, connections);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (StructureTile) obj;
        return Objects.equals(this.name, that.name) && Objects.equals(this.structure, that.structure) && Objects.equals(this.connections,
                that.connections
        );
    }

    @Override
    public String toString() {
        return "StructureTile[" + "name=" + name + ", " + "structure=" + structure + ", " + "connections=" + connections + ']';
    }


}
