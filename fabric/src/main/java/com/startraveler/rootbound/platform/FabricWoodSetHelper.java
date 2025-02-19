package com.startraveler.rootbound.platform;

import com.startraveler.rootbound.platform.services.IWoodSetHelper;
import com.startraveler.rootbound.woodset.WoodSet;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;

public class FabricWoodSetHelper implements IWoodSetHelper {
    @Override
    public void registerStrippables(WoodSet woodSet) {
        StrippableBlockRegistry.register(woodSet.getLog().get(), woodSet.getStrippedLog().get());
        StrippableBlockRegistry.register(woodSet.getWood().get(), woodSet.getStrippedWood().get());
    }
}
