package com.startraveler.rootbound.platform;

import com.startraveler.rootbound.platform.services.IWoodSetHelper;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public class NeoForgeWoodSetHelper implements IWoodSetHelper {

    @Override
    public void registerStrippables(WoodSet woodSet) {
        NeoForge.EVENT_BUS.addListener(
                BlockEvent.BlockToolModificationEvent.class, event -> {
                    ItemStack stack = event.getHeldItemStack();
                    ItemAbility ability = event.getItemAbility();
                    // If it isn't stripping, or if it can't do the action, return.
                    if (ability != ItemAbilities.AXE_STRIP || !stack.canPerformAction(ability)) {
                        return;
                    }
                    BlockState state = event.getState();
                    BlockState finalState = null;
                    // Check if this is a strippable log from a wood set.
                    // If so, set the result and return.
                    if (state.is(woodSet.getLog().get())) {
                        finalState = woodSet.getStrippedLog().get().defaultBlockState();
                    } else if (state.is(woodSet.getWood().get())) {
                        finalState = woodSet.getStrippedWood()
                                .get()
                                .defaultBlockState()
                                .setValue(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
                    }
                    // If the final state was changed, set it.
                    if (finalState != null) {
                        finalState = finalState.setValue(
                                BlockStateProperties.AXIS,
                                state.getValue(BlockStateProperties.AXIS)
                        );
                        event.setFinalState(finalState);
                    }

                }
        );
    }
}
