/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you modify this file, please include a notice stating the changes:
 * Example: "Modified by [Your Name] on [Date] - [Short Description of Changes]"
 */
package com.startraveler.rootbound.woodset;


import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.platform.Services;
import com.startraveler.rootbound.registration.RegistrationProvider;
import com.startraveler.rootbound.registration.RegistryObject;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamily;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.PushReaction;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WoodSet {

    // The mod id
    protected final String modid;
    // The wood type name
    protected final String setName;
    // The wood type
    protected final WoodType woodType;
    // The block set type
    protected final BlockSetType setType;
    // The base block properties
    protected final Supplier<BlockBehaviour.Properties> base;
    // Registration helpers
    protected final RegistrationProvider<Block> blocks;
    protected final RegistrationProvider<Item> items;
    protected final RegistrationProvider<EntityType<?>> entities;
    protected final RegistrationProvider<CreativeModeTab> tabs;
    // Whether the set is flammable
    private final boolean isFlammable;
    // The burn time factor of the set
    private final float burnTimeFactor;
    private final boolean hasMosaic;
    // The blocks created
    protected RegistryObject<Block, RotatedPillarBlock> log;
    protected RegistryObject<Block, RotatedPillarBlock> wood;
    protected RegistryObject<Block, RotatedPillarBlock> strippedLog;
    protected RegistryObject<Block, RotatedPillarBlock> strippedWood;
    protected RegistryObject<Block, Block> planks;
    protected RegistryObject<Block, SlabBlock> slab;
    protected RegistryObject<Block, StairBlock> stairs;
    protected RegistryObject<Block, FenceBlock> fence;
    protected RegistryObject<Block, FenceGateBlock> fenceGate;
    protected RegistryObject<Block, ButtonBlock> button;
    protected RegistryObject<Block, PressurePlateBlock> pressurePlate;
    protected RegistryObject<Block, DoorBlock> door;
    protected RegistryObject<Block, TrapDoorBlock> trapdoor;
    protected RegistryObject<Block, SignBlock> sign;
    protected RegistryObject<Block, WallSignBlock> wallSign;
    protected RegistryObject<Block, CeilingHangingSignBlock> hangingSign;
    protected RegistryObject<Block, WallHangingSignBlock> wallHangingSign;
    protected RegistryObject<Block, ShelfBlock> shelf;
    // The items created
    protected RegistryObject<Item, Item> signItem;
    protected RegistryObject<Item, Item> hangingSignItem;
    protected RegistryObject<Item, BoatItem> boatItem;
    protected RegistryObject<Item, BoatItem> chestBoatItem;
    // The entities created
    protected RegistryObject<EntityType<?>, EntityType<? extends Boat>> boat;
    protected RegistryObject<EntityType<?>, EntityType<? extends ChestBoat>> chestBoat;
    // The tag for the logs
    protected TagKey<Block> logs;
    // The tag for the log items
    protected TagKey<Item> logItems;
    // The block family
    protected BlockFamily family = null;
    private BlockFamily mosaicFamily = null;
    private RegistryObject<Block, Block> mosaic;
    private RegistryObject<Block, SlabBlock> mosaicSlab;
    private RegistryObject<Block, StairBlock> mosaicStairs;

    public WoodSet(String modid, String setName, Supplier<BlockBehaviour.Properties> baseProperties, float burnTimeFactor, boolean isFlammable) {
        this(modid, setName, baseProperties, burnTimeFactor, isFlammable, false);
    }

    public WoodSet(String modid, String setName, Supplier<BlockBehaviour.Properties> baseProperties, float burnTimeFactor, boolean isFlammable, boolean hasMosaic) {
        this.modid = modid;
        this.setName = setName;
        this.base = baseProperties;
        this.blocks = RegistrationProvider.get(Registries.BLOCK, this.modid);
        this.items = RegistrationProvider.get(Registries.ITEM, this.modid);
        this.tabs = RegistrationProvider.get(Registries.CREATIVE_MODE_TAB, this.modid);
        this.entities = RegistrationProvider.get(Registries.ENTITY_TYPE, this.modid);
        this.setType = new BlockSetType(this.setName);
        this.woodType = WoodType.register(new WoodType(this.setName, this.setType));
        this.isFlammable = isFlammable;
        this.burnTimeFactor = burnTimeFactor;
        this.hasMosaic = hasMosaic;

        registerBlocks();
        registerEntities();
        registerItems();
        registerTabs();
        this.logs = TagKey.create(Registries.BLOCK, Constants.location(this.setName + "_logs"));
        this.logItems = TagKey.create(Registries.ITEM, Constants.location(this.setName + "_logs"));
        Services.WOOD_SET_HELPER.registerStrippables(this);
    }

    private static EntityType.EntityFactory<@NotNull Boat> boatFactory(Supplier<Item> itemSupplier) {
        return (type, level) -> new Boat(type, level, itemSupplier);
    }

    private static EntityType.EntityFactory<@NotNull ChestBoat> chestBoatFactory(Supplier<Item> itemSupplier) {
        return (type, level) -> new ChestBoat(type, level, itemSupplier);
    }

    public static <T> T throwIfNullForMosaics(T t, WoodSet w) {
        return Objects.requireNonNull(t, "The wood set " + w.getName() + " is marked as having mosaics, but does not!");
    }

    public void registerDispenserBehaviors() {
        DispenserBlock.registerBehavior(this.getBoatItem().get(), new BoatDispenseItemBehavior(this.getBoat().get()));
        DispenserBlock.registerBehavior(
                this.getChestBoatItem().get(),
                new BoatDispenseItemBehavior(this.getChestBoat().get())
        );
    }

    public BlockFamily getMosaicFamily() {
        if (this.mosaicFamily == null && this.hasMosaic) {
            BlockFamily.Builder builder = new BlockFamily.Builder(this.getOrThrowMosaic()
                    .get()).slab(this.getOrThrowMosaicSlab().get()).stairs(this.getOrThrowMosaicStairs().get());
            this.mosaicFamily = builder.getFamily();
        }
        return this.mosaicFamily;
    }

    public BlockFamily getFamily() {
        if (this.family == null) {
            BlockFamily.Builder builder = new BlockFamily.Builder(this.getPlanks().get()).fence(this.getFence().get())
                    .fenceGate(this.getFenceGate().get())
                    .slab(this.getSlab().get())
                    .stairs(this.getStairs().get())
                    .pressurePlate(this.getPressurePlate().get())
                    .sign(this.getSign().get(), this.getWallSign().get())
                    .button(this.getButton().get())
                    .door(this.getDoor().get())
                    .trapdoor(this.getTrapdoor().get());
            if (this.hasMosaic) {
                builder.mosaic(this.getOrThrowMosaic().get());
            }
            this.family = builder.getFamily();
        }

        return this.family;
    }

    public boolean isFlammable() {
        return isFlammable;
    }

    public void registerFlammability(TriConsumer<Block, Integer, Integer> registrar) {
        if (isFlammable) {
            registrar.accept(this.log.get(), 5, 5);
            registrar.accept(this.wood.get(), 5, 5);
            registrar.accept(this.strippedLog.get(), 5, 5);
            registrar.accept(this.strippedWood.get(), 5, 5);
            registrar.accept(this.planks.get(), 5, 20);
            registrar.accept(this.slab.get(), 5, 20);
            registrar.accept(this.stairs.get(), 5, 20);
            registrar.accept(this.fence.get(), 5, 20);
            registrar.accept(this.fenceGate.get(), 5, 20);
            registrar.accept(this.door.get(), 5, 20);
            registrar.accept(this.trapdoor.get(), 5, 20);
            registrar.accept(this.shelf.get(), 30, 20);
            if (this.hasMosaic) {
                registrar.accept(this.mosaic.get(), 5, 20);
                registrar.accept(this.mosaicSlab.get(), 5, 20);
                registrar.accept(this.mosaicStairs.get(), 5, 20);
            }
        }
    }

    public RegistryObject<EntityType<?>, EntityType<? extends Boat>> getBoat() {
        return boat;
    }

    public RegistryObject<EntityType<?>, EntityType<? extends ChestBoat>> getChestBoat() {
        return chestBoat;
    }

    public void registerTabs() {
        this.tabs.register(
                this.modid + ".wood_set." + this.setName,
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .icon(() -> new ItemStack(this.log.get()))
                        .displayItems((itemDisplayParameters, output) -> {
                            output.accept(this.log.get());
                            output.accept(this.wood.get());
                            output.accept(this.strippedLog.get());
                            output.accept(this.strippedWood.get());
                            output.accept(this.planks.get());
                            output.accept(this.slab.get());
                            output.accept(this.stairs.get());
                            if (this.hasMosaic) {
                                output.accept(this.mosaic.get());
                                output.accept(this.mosaicSlab.get());
                                output.accept(this.mosaicStairs.get());
                            }
                            output.accept(this.fence.get());
                            output.accept(this.fenceGate.get());
                            output.accept(this.door.get());
                            output.accept(this.trapdoor.get());
                            output.accept(this.button.get());
                            output.accept(this.pressurePlate.get());
                            output.accept(this.shelf.get());
                            output.accept(this.signItem.get());
                            output.accept(this.hangingSignItem.get());
                            output.accept(this.boatItem.get());
                            output.accept(this.chestBoatItem.get());
                        })
                        .title(Component.translatable("creativetab." + this.modid + ".wood_set." + this.setName))
                        .build()
        );
    }

    public void registerFuels(BiConsumer<ItemLike, Integer> registrar) {

        registrar.accept(this.log.get(), (int) (BurnTimes.LOG * this.burnTimeFactor));
        registrar.accept(this.wood.get(), (int) (BurnTimes.LOG * this.burnTimeFactor));
        registrar.accept(this.strippedLog.get(), (int) (BurnTimes.LOG * this.burnTimeFactor));
        registrar.accept(this.strippedWood.get(), (int) (BurnTimes.LOG * this.burnTimeFactor));
        registrar.accept(this.planks.get(), (int) (BurnTimes.PLANKS * this.burnTimeFactor));
        registrar.accept(this.stairs.get(), (int) (BurnTimes.STAIRS * this.burnTimeFactor));
        registrar.accept(this.slab.get(), (int) (BurnTimes.SLAB * this.burnTimeFactor));
        registrar.accept(this.fence.get(), (int) (BurnTimes.FENCE * this.burnTimeFactor));
        registrar.accept(this.fenceGate.get(), (int) (BurnTimes.FENCE_GATE * this.burnTimeFactor));
        registrar.accept(this.pressurePlate.get(), (int) (BurnTimes.PRESSURE_PLATE * this.burnTimeFactor));
        registrar.accept(this.button.get(), (int) (BurnTimes.BUTTON * this.burnTimeFactor));
        registrar.accept(this.door.get(), (int) (BurnTimes.DOOR * this.burnTimeFactor));
        registrar.accept(this.trapdoor.get(), (int) (BurnTimes.TRAPDOOR * this.burnTimeFactor));
        registrar.accept(this.signItem.get(), (int) (BurnTimes.SIGN * this.burnTimeFactor));
        registrar.accept(this.hangingSignItem.get(), (int) (BurnTimes.HANGING_SIGN * this.burnTimeFactor));
        if (this.hasMosaic) {
            registrar.accept(this.mosaic.get(), (int) (BurnTimes.PLANKS * this.burnTimeFactor));
            registrar.accept(this.mosaicStairs.get(), (int) (BurnTimes.STAIRS * this.burnTimeFactor));
            registrar.accept(this.mosaicSlab.get(), (int) (BurnTimes.SLAB * this.burnTimeFactor));
        }
    }

    public String getName() {
        return this.setName;
    }

    @SuppressWarnings("unused")
    public String getModid() {
        return this.modid;
    }

    @SuppressWarnings("unused")
    public WoodType getType() {
        return this.woodType;
    }

    protected void registerEntities() {
        this.boat = this.entities.register(
                typeName("_boat"),
                () -> EntityType.Builder.of(boatFactory(() -> this.boatItem.get()), MobCategory.MISC)
                        .noLootTable()
                        .sized(1.375F, 0.5625F)
                        .eyeHeight(0.5625F)
                        .clientTrackingRange(10)
                        .build(ResourceKey.create(
                                Registries.ENTITY_TYPE,
                                Identifier.fromNamespaceAndPath(this.modid, typeName("_boat"))
                        ))
        );
        this.chestBoat = this.entities.register(
                typeName("_chest_boat"),
                () -> EntityType.Builder.of(chestBoatFactory(() -> this.chestBoatItem.get()), MobCategory.MISC)
                        .noLootTable()
                        .sized(1.375F, 0.5625F)
                        .eyeHeight(0.5625F)
                        .clientTrackingRange(10)
                        .build(ResourceKey.create(
                                Registries.ENTITY_TYPE,
                                Identifier.fromNamespaceAndPath(this.modid, typeName("_chest_boat"))
                        ))
        );
    }

    protected void registerItems() {
        this.signItem = register(
                typeName("_sign"),
                () -> new SignItem(this.sign.get(), this.wallSign.get(), itemProperties(typeName("_sign")))
        );
        this.hangingSignItem = register(
                typeName("_hanging_sign"),
                () -> new HangingSignItem(
                        this.hangingSign.get(),
                        this.wallHangingSign.get(),
                        itemProperties(typeName("_hanging_sign"))
                )
        );
        this.boatItem = register(
                typeName("_boat"),
                () -> new BoatItem(this.boat.get(), itemProperties(typeName("_boat")))
        );
        this.chestBoatItem = register(
                typeName("_chest_boat"),
                () -> new BoatItem(this.chestBoat.get(), itemProperties(typeName("_chest_boat")))
        );
    }

    protected void registerBlocks() {
        this.log = registerBlockWithItem(
                typeName("_log"),
                () -> new RotatedPillarBlock(this.logProperties(typeName("_log")))
        );
        this.wood = registerBlockWithItem(
                typeName("_wood"),
                () -> new RotatedPillarBlock(this.logProperties(typeName("_wood")))
        );
        this.strippedLog = registerBlockWithItem(
                splitName("stripped_", "_log"),
                () -> new RotatedPillarBlock(this.logProperties(splitName("stripped_", "_log")))
        );
        this.strippedWood = registerBlockWithItem(
                splitName("stripped_", "_wood"),
                () -> new RotatedPillarBlock(this.logProperties(splitName("stripped_", "_wood")))
        );
        this.planks = registerBlockWithItem(
                typeName("_planks"),
                () -> new Block(this.planksProperties(typeName("_planks")))
        );
        this.slab = registerBlockWithItem(
                typeName("_slab"),
                () -> new SlabBlock(this.slabProperties(typeName("_slab")))
        );
        this.stairs = registerBlockWithItem(
                typeName("_stairs"),
                () -> new StairBlock(this.planks.get().defaultBlockState(), this.stairsProperties(typeName("_stairs")))
        );
        this.fence = registerBlockWithItem(
                typeName("_fence"),
                () -> new FenceBlock(this.fenceProperties(typeName("_fence")))
        );
        this.fenceGate = registerBlockWithItem(
                typeName("_fence_gate"),
                () -> new FenceGateBlock(this.woodType, this.fenceGateProperties(typeName("_fence_gate")))
        );
        this.button = registerBlockWithItem(
                typeName("_button"),
                () -> new ButtonBlock(this.setType, 30, this.buttonProperties(typeName("_button")))
        );
        this.pressurePlate = registerBlockWithItem(
                typeName("_pressure_plate"),
                () -> new PressurePlateBlock(this.setType, this.pressurePlateProperties(typeName("_pressure_plate")))
        );
        this.sign = registerBlockWithoutItem(
                typeName("_sign"),
                () -> new StandingSignBlock(this.woodType, this.signProperties(typeName("_sign")))
        );
        this.wallSign = registerBlockWithoutItem(
                typeName("_wall_sign"),
                () -> new WallSignBlock(this.woodType, this.wallSignProperties(typeName("_wall_sign")))
        );
        this.hangingSign = registerBlockWithoutItem(
                typeName("_hanging_sign"),
                () -> new CeilingHangingSignBlock(this.woodType, this.signProperties(typeName("_hanging_sign")))
        );
        this.wallHangingSign = registerBlockWithoutItem(
                typeName("_wall_hanging_sign"),
                () -> new WallHangingSignBlock(
                        this.woodType,
                        this.wallHangingSignProperties(typeName("_wall_hanging_sign"))
                )
        );
        this.trapdoor = registerBlockWithItem(
                typeName("_trapdoor"),
                () -> new TrapDoorBlock(this.setType, this.trapdoorProperties(typeName("_trapdoor")))
        );
        this.door = registerBlockWithItem(
                typeName("_door"),
                () -> new DoorBlock(this.setType, this.doorProperties(typeName("_door")))
        );
        this.shelf = registerBlockWithItem(
                typeName("_shelf"),
                () -> new ShelfBlock(this.shelfProperties(typeName("_shelf")))
        );
        if (this.hasMosaic) {
            this.mosaic = registerBlockWithItem(
                    typeName("_mosaic"),
                    () -> new Block(this.planksProperties(typeName("_mosaic")))
            );
            this.mosaicSlab = registerBlockWithItem(
                    typeName("_mosaic_slab"),
                    () -> new SlabBlock(this.slabProperties(typeName("_mosaic_slab")))
            );
            this.mosaicStairs = registerBlockWithItem(
                    typeName("_mosaic_stairs"),
                    () -> new StairBlock(
                            this.mosaic.get().defaultBlockState(),
                            this.stairsProperties(typeName("_mosaic_stairs"))
                    )
            );
        }
    }

    protected String typeName(String suffix) {
        return this.setName + suffix;
    }

    @SuppressWarnings("SameParameterValue")
    protected String splitName(String prefix, String suffix) {
        return prefix + this.setName + suffix;
    }

    protected <T extends Block> RegistryObject<Block, T> registerBlockWithItem(String name, Supplier<T> block) {
        return registerBlockWithItem(
                name,
                block,
                b -> () -> new BlockItem(b.get(), itemProperties(name).useBlockDescriptionPrefix())
        );
    }

    protected <T extends Block> RegistryObject<Block, T> registerBlockWithItem(String name, Supplier<T> block, Function<RegistryObject<Block, T>, Supplier<? extends BlockItem>> item) {
        var reg = this.blocks.register(name, block);
        this.register(name, () -> item.apply(reg).get());
        return reg;
    }

    protected <T extends Block> RegistryObject<Block, T> registerBlockWithoutItem(String name, Supplier<T> block) {
        return this.blocks.register(name, block);
    }

    protected <T extends Item> RegistryObject<Item, T> register(String name, Supplier<T> supplier) {
        return this.items.register(name, supplier);
    }

    protected Item.Properties itemProperties(String name) {
        return new Item.Properties().setId(ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(this.modid, name)
        ));
    }

    public TagKey<Block> getLogs() {
        return logs;
    }

    public TagKey<Item> getLogItems() {
        return logItems;
    }

    public RegistryObject<Block, RotatedPillarBlock> getLog() {
        return log;
    }

    public RegistryObject<Block, RotatedPillarBlock> getWood() {
        return wood;
    }

    public RegistryObject<Block, RotatedPillarBlock> getStrippedLog() {
        return strippedLog;
    }

    public RegistryObject<Block, RotatedPillarBlock> getStrippedWood() {
        return strippedWood;
    }

    public RegistryObject<Block, Block> getPlanks() {
        return planks;
    }

    public RegistryObject<Block, SlabBlock> getSlab() {
        return slab;
    }

    public RegistryObject<Block, StairBlock> getStairs() {
        return stairs;
    }

    public @Nullable RegistryObject<Block, Block> getMosaic() {
        return mosaic;
    }

    public @Nullable RegistryObject<Block, SlabBlock> getMosaicSlab() {
        return mosaicSlab;
    }

    public @Nullable RegistryObject<Block, StairBlock> getMosaicStairs() {
        return mosaicStairs;
    }

    public RegistryObject<Block, Block> getOrThrowMosaic() {
        return WoodSet.throwIfNullForMosaics(mosaic, this);
    }

    public RegistryObject<Block, SlabBlock> getOrThrowMosaicSlab() {
        return WoodSet.throwIfNullForMosaics(mosaicSlab, this);
    }

    public RegistryObject<Block, StairBlock> getOrThrowMosaicStairs() {
        return WoodSet.throwIfNullForMosaics(mosaicStairs, this);
    }

    public RegistryObject<Block, FenceBlock> getFence() {
        return fence;
    }

    public RegistryObject<Block, FenceGateBlock> getFenceGate() {
        return fenceGate;
    }

    public RegistryObject<Block, ShelfBlock> getShelf() {
        return shelf;
    }

    public RegistryObject<Block, SignBlock> getSign() {
        return sign;
    }


    public RegistryObject<Block, WallSignBlock> getWallSign() {
        return wallSign;
    }

    public RegistryObject<Item, Item> getSignItem() {
        return signItem;
    }

    public RegistryObject<Block, CeilingHangingSignBlock> getHangingSign() {
        return hangingSign;
    }

    public RegistryObject<Block, WallHangingSignBlock> getWallHangingSign() {
        return wallHangingSign;
    }

    public RegistryObject<Item, Item> getHangingSignItem() {
        return hangingSignItem;
    }

    public RegistryObject<Block, ButtonBlock> getButton() {
        return button;
    }

    public RegistryObject<Block, PressurePlateBlock> getPressurePlate() {
        return pressurePlate;
    }

    public RegistryObject<Block, DoorBlock> getDoor() {
        return door;
    }

    public RegistryObject<Block, TrapDoorBlock> getTrapdoor() {
        return trapdoor;
    }

    public RegistryObject<Item, BoatItem> getBoatItem() {
        return boatItem;
    }

    public RegistryObject<Item, BoatItem> getChestBoatItem() {
        return chestBoatItem;
    }

    public RegistrationProvider<Block> getBlockProvider() {
        return this.blocks;
    }

    public RegistrationProvider<Item> getItemProvider() {
        return this.items;
    }

    public BlockBehaviour.Properties blockProperties(String name) {
        return this.base.get().setId(id(name));
    }

    @SuppressWarnings("unused")
    public BlockBehaviour.Properties blockProperties(Block block, String name) {
        return BlockBehaviour.Properties.ofFullCopy(block).setId(id(name));
    }

    protected BlockBehaviour.Properties planksProperties(String name) {
        return this.blockProperties(name);
    }

    protected BlockBehaviour.Properties logProperties(String name) {
        return this.blockProperties(name);
    }

    protected BlockBehaviour.Properties slabProperties(String name) {
        return this.blockProperties(name);
    }

    protected BlockBehaviour.Properties stairsProperties(String name) {
        return this.blockProperties(name);
    }

    protected BlockBehaviour.Properties fenceProperties(String name) {
        return this.blockProperties(name).forceSolidOn();
    }

    protected BlockBehaviour.Properties fenceGateProperties(String name) {
        return this.blockProperties(name).forceSolidOn();
    }

    protected BlockBehaviour.Properties shelfProperties(String name) {
        return this.blockProperties(name)
                .sound(SoundType.SHELF)
                .ignitedByLava()
                .strength(2.0F, 3.0F);
    }

    protected BlockBehaviour.Properties signProperties(String name) {
        return this.blockProperties(name).forceSolidOn().noCollision().strength(1.0F);
    }

    protected BlockBehaviour.Properties wallSignProperties(String name) {
        return this.blockProperties(name).forceSolidOn().noCollision().strength(1.0F);
    }

    @SuppressWarnings("unused")
    protected BlockBehaviour.Properties hangingSignProperties(String name) {
        return this.blockProperties(name).forceSolidOn().noCollision().strength(1.0F);
    }

    protected BlockBehaviour.Properties wallHangingSignProperties(String name) {
        return this.blockProperties(name).forceSolidOn().noCollision().strength(1.0F);
    }

    protected BlockBehaviour.Properties buttonProperties(String name) {
        return this.blockProperties(name).noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY);
    }

    protected BlockBehaviour.Properties pressurePlateProperties(String name) {
        return this.blockProperties(name)
                .forceSolidOn()
                .noCollision()
                .strength(0.5F)
                .pushReaction(PushReaction.DESTROY);
    }

    protected BlockBehaviour.Properties trapdoorProperties(String name) {
        return this.blockProperties(name).noOcclusion();
    }

    protected BlockBehaviour.Properties doorProperties(String name) {
        return this.blockProperties(name).noOcclusion();
    }

    private ResourceKey<Block> id(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(this.modid, name));
    }

    public boolean hasMosaic() {
        return this.hasMosaic;
    }

    @SuppressWarnings("unused")
    public static class BurnTimes {
        public static final int SINGLE_ITEM = 200;
        // The burn times of common items, in ticks.
        public static final int LOG = 300;
        public static final int PLANKS = 300;
        public static final int BUTTON = 100;
        public static final int STICK = 100;
        public static final int FENCE = 300;
        public static final int FENCE_GATE = 300;
        public static final int SLAB = 150;
        public static final int DOOR = 200;
        public static final int TRAPDOOR = 300;
        public static final int STAIRS = 300;
        public static final int PRESSURE_PLATE = 300;
        public static final int COAL = 1600;
        public static final int SIGN = 200;
        public static final int HANGING_SIGN = 800;
    }

}

