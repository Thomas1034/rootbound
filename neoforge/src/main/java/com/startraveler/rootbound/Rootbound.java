package com.startraveler.rootbound;


import com.startraveler.rootbound.blocktransformer.BlockTransformer;
import com.startraveler.rootbound.featureset.FeatureSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(Constants.MOD_ID)
public class Rootbound {

    public Rootbound(IEventBus eventBus) {

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        CommonClass.init();

        eventBus.addListener(Rootbound::registerDatapackRegistries);
    }

    public static void registerDatapackRegistries(final DataPackRegistryEvent.NewRegistry event) {

        event.dataPackRegistry(BlockTransformer.KEY, BlockTransformer.CODEC, BlockTransformer.CODEC);
        event.dataPackRegistry(FeatureSet.KEY, FeatureSet.CODEC, FeatureSet.CODEC);

    }
}