package com.startraveler.rootbound.registrycache;


import com.startraveler.rootbound.blocktransformer.BlockTransformer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ReloadableRegistryCache<T> extends SimplePreparableReloadListener<Integer> {

    private final Map<Identifier, T> cachedValues;
    private final ResourceKey<? extends Registry<T>> key;
    private Registry<T> registry;

    public ReloadableRegistryCache(ResourceKey<? extends Registry<T>> key) {
        this.cachedValues = new HashMap<>();
        this.key = key;
    }

    public T get(RegistryAccess access, Identifier name) {
        if (this.cachedValues.containsKey(name)) {
            return this.cachedValues.get(name);
        }
        if (this.registry == null) {
            this.registry = access.lookupOrThrow(this.key);
        }
        T result = this.registry.getValue(name);
        this.cachedValues.put(name, result);
        return result;
    }

    public void clear() {
        this.cachedValues.clear();
        this.registry = null;
    }

    @Override
    protected @NotNull Integer prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        return 0;
    }

    @Override
    protected void apply(@NotNull Integer i, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        this.clear();
    }

    public static class Transformers extends ReloadableRegistryCache<BlockTransformer> {
        public Transformers() {
            super(BlockTransformer.KEY);
        }
    }
}
