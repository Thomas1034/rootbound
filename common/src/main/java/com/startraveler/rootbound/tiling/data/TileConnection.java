package com.startraveler.rootbound.tiling.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.startraveler.rootbound.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

public class TileConnection {


    public static final ResourceKey<Registry<TileConnection>> KEY = ResourceKey.createRegistryKey(Constants.location(
            "tile_connection"));

    public static final Codec<TileConnection> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(TileConnection::name),
            TagKey.codec(KEY).listOf().fieldOf("tag_connections").forGetter(TileConnection::tagConnections),
            ResourceLocation.CODEC.listOf().fieldOf("direct_connections").forGetter(TileConnection::directConnections)
    ).apply(instance, TileConnection::new));
    public static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("empty");
    public static final TileConnection EMPTY_CONNECTION = new TileConnection(EMPTY, List.of(), List.of()) {
        @Override
        public boolean matches(RegistryAccess access, TileConnection connection) {
            return true;
        }
    };
    protected final Map<TileConnection, Boolean> cache;
    private final ResourceLocation name;
    private final List<TagKey<TileConnection>> tagConnections;
    private final List<ResourceLocation> directConnections;

    public TileConnection(ResourceLocation name, List<TagKey<TileConnection>> tagConnections, List<ResourceLocation> directConnections) {
        this.name = name;
        this.tagConnections = tagConnections;
        this.directConnections = directConnections;
        this.cache = new HashMap<>();
    }

    public static TileConnection lookup(RegistryAccess access, ResourceLocation location) {
        if (access == null || location == null) {
            return null;
        }
        if (location.equals(EMPTY)) {
            return EMPTY_CONNECTION;
        }
        Registry<TileConnection> registry = access.lookupOrThrow(TileConnection.KEY);
        return registry.getValue(location);
    }

    public boolean matches(RegistryAccess access, TileConnection connection) {
        if (null == connection) {
            return true;
        } else if (connection.name.equals(EMPTY)) {
            if (this.cache.get(connection) instanceof Boolean bool) {
                return bool;
            }
        }
        Registry<TileConnection> registry = access.lookupOrThrow(KEY);
        boolean matches = this.name.equals(EMPTY) || connection.name.equals(EMPTY) || this.directConnections.contains(
                connection.name) || this.tagConnections.stream()
                .anyMatch(tag -> StreamSupport.stream(registry.getTagOrEmpty(tag).spliterator(), false)
                        .anyMatch(holder -> holder.value().equals(this)));
        this.cache.put(connection, matches);
        return matches;
    }

    public ResourceLocation name() {
        return name;
    }

    public List<TagKey<TileConnection>> tagConnections() {
        return tagConnections;
    }

    public List<ResourceLocation> directConnections() {
        return directConnections;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tagConnections, directConnections);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (TileConnection) obj;
        return Objects.equals(this.name, that.name) && Objects.equals(
                this.tagConnections,
                that.tagConnections
        ) && Objects.equals(
                this.directConnections,
                that.directConnections
        );
    }

    @Override
    public String toString() {
        return "TileConnection[" + "name=" + name + ", " + "tagConnections=" + tagConnections + ", " + "directConnections=" + directConnections + ']';
    }


}
