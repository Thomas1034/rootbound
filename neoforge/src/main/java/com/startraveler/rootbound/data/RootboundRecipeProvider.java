package com.startraveler.rootbound.data;

import com.startraveler.rootbound.Constants;
import com.startraveler.rootbound.woodset.WoodSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RootboundRecipeProvider extends RecipeProvider {
    protected final Set<WoodSet> woodSets;
    protected final String modid;

    public RootboundRecipeProvider(HolderLookup.Provider provider, RecipeOutput output, Set<WoodSet> woodSets) {
        super(provider, output);
        this.modid = Constants.MOD_ID;
        this.woodSets = woodSets;
    }

    @Override
    protected void buildRecipes() {
        for (WoodSet woodSet : this.woodSets) {
            this.generateFor(woodSet);
        }
    }

    protected void shaped(List<String> pattern, List<Character> tokens, List<Object> ingredients, RecipeCategory recipeCategory, ItemLike result, int count) {
        shaped(pattern, tokens, ingredients, recipeCategory, result, count, null);
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    protected void shaped(List<String> pattern, List<Character> tokens, List<Object> ingredients, RecipeCategory recipeCategory, ItemLike result, int count, String group) {

        ShapedRecipeBuilder recipe = shaped(recipeCategory, result, count);
        StringBuilder recipeName = new StringBuilder(this.modid + ":" + getItemName(result) + "_from");

        // Adds in the pattern.
        for (String row : pattern) {
            recipe = recipe.pattern(row);
        }

        // Check if the ingredients match the tokens.
        if (tokens.size() != ingredients.size()) {
            throw new IllegalArgumentException("Token count does not match ingredient count.");
        }

        // Defines the tokens.
        for (int i = 0; i < tokens.size(); i++) {
            if (ingredients.get(i) instanceof ItemLike) {
                recipeName.append("_").append(getItemName((ItemLike) ingredients.get(i)));
                recipe = recipe.define(tokens.get(i), (ItemLike) ingredients.get(i));
            } else if (ingredients.get(i) instanceof TagKey<?> tag) {
                Ingredient ingredient = Ingredient.of(this.registries.lookupOrThrow(Registries.ITEM)
                        .getOrThrow((TagKey<Item>) tag));
                recipeName.append("_tag_").append(tag.location().toDebugFileName());
                recipe = recipe.define(tokens.get(i), ingredient);
            } else {
                throw new IllegalArgumentException("Unrecognized item or tag type: " + ingredients.get(i));
            }
        }

        // Adds in the unlock trigger for the ingredients.
        for (Object ingredient : ingredients) {
            if (ingredient instanceof ItemLike) {
                String name = getHasName((ItemLike) ingredient);
                recipe = recipe.unlockedBy(name, has((ItemLike) ingredient));
            } else if (ingredient instanceof TagKey) {
                String name = "has_" + ((TagKey<Item>) ingredient).registry().registry().toDebugFileName();
                recipe = recipe.unlockedBy(name, has((TagKey<Item>) ingredient));
            } else {
                throw new IllegalArgumentException("Unrecognized item or tag type: " + ingredient);
            }
        }
        // Adds in the unlock trigger for the result.
        recipe = recipe.unlockedBy(getHasName(result), has(result));

        recipe.group(group == null ? group(result) : group);

        // Saves the recipe.
        recipe.save(this.output, recipeName.toString());
    }

    // Shapeless recipe. Item at n must correspond to item count at n.
    @SuppressWarnings({"unchecked"})
    protected void shapeless(List<Object> ingredients, List<Integer> counts, RecipeCategory recipeCategory, ItemLike result, int count, String group) {

        ShapelessRecipeBuilder recipe = shapeless(recipeCategory, result, count);
        StringBuilder recipeName = new StringBuilder(group(result) + "_from");

        // Check if the ingredients match the count length.
        if (counts.size() != ingredients.size()) {
            throw new IllegalArgumentException("Token count does not match ingredient count.");
        }

        // Adds in the ingredients.
        for (int i = 0; i < counts.size(); i++) {
            if (ingredients.get(i) instanceof ItemLike) {
                recipeName.append("_").append(getItemName((ItemLike) ingredients.get(i)));
                recipe = recipe.requires((ItemLike) ingredients.get(i), counts.get(i));
            } else if (ingredients.get(i) instanceof TagKey<?> tag) {
                recipeName.append("_tag_").append(tag.location().toDebugFileName());
                Ingredient ingredient = Ingredient.of(this.registries.lookupOrThrow(Registries.ITEM)
                        .getOrThrow((TagKey<Item>) tag));
                recipe = recipe.requires(ingredient, counts.get(i));
            } else {
                throw new IllegalArgumentException("Unrecognized item or tag type: " + ingredients.get(i));
            }
        }

        // Adds in the unlock triggers for the ingredients.
        for (Object ingredient : ingredients) {
            if (ingredient instanceof ItemLike) {
                recipe = recipe.unlockedBy(getHasName((ItemLike) ingredient), has((ItemLike) ingredient));
            } else if (ingredient instanceof TagKey) {
                String name = "has" + ((TagKey<Item>) ingredient).registry().registry().toDebugFileName();
                recipe = recipe.unlockedBy(name, has((TagKey<Item>) ingredient));
            } else {
                throw new IllegalArgumentException("Unrecognized item or tag type: " + ingredient);
            }
        }
        // Adds in the unlock trigger for the result.
        recipe = recipe.unlockedBy(getHasName(result), has(result));

        recipe.group(group == null ? group(result) : group);

        // Saves the recipe.
        recipe.save(this.output, recipeName.toString());

    }

    @SuppressWarnings("unused")
    protected void foodCooking(List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime) {
        String group = (namespace(result) + ":" + getItemName(result));
        campfire(ingredients, category, result, experience, 2 * cookingTime, group);
        smelting(ingredients, category, result, experience, cookingTime, group);
        smoking(ingredients, category, result, experience, cookingTime / 2, group);
    }

    protected void generateFor(WoodSet woodSet) {

        shapeless(
                List.of(woodSet.getLogItems()),
                List.of(1),
                RecipeCategory.BUILDING_BLOCKS,
                woodSet.getPlanks().get(),
                4,
                "planks"
        );

        shaped(
                List.of("ll", "ll"),
                List.of('l'),
                List.of(woodSet.getLog().get()),
                RecipeCategory.BUILDING_BLOCKS,
                woodSet.getWood().get(),
                3
        );

        stairBuilder(woodSet.getStairs().get(), Ingredient.of(woodSet.getPlanks().get())).group("wooden_stairs")
                .unlockedBy(hasPlanks(woodSet), has(woodSet.getPlanks().get()))
                .save(this.output);

        if (woodSet.hasMosaic()) {
            shaped(
                    List.of("s", "s"),
                    List.of('s'),
                    List.of(woodSet.getSlab().get()),
                    RecipeCategory.BUILDING_BLOCKS,
                    woodSet.getOrThrowMosaic().get(),
                    1
            );
            stairBuilder(woodSet.getOrThrowMosaicStairs().get(), Ingredient.of(woodSet.getOrThrowMosaic().get())).group(
                            "wooden_stairs")
                    .unlockedBy(hasPlanks(woodSet), has(woodSet.getOrThrowMosaic().get()))
                    .save(this.output);
            this.slabBuilder(
                            RecipeCategory.BUILDING_BLOCKS,
                            woodSet.getOrThrowMosaicSlab().get(),
                            Ingredient.of(woodSet.getOrThrowMosaic().get())
                    )
                    .unlockedBy(hasPlanks(woodSet), this.has(woodSet.getOrThrowMosaic().get()))
                    .group("wooden_slab")
                    .save(this.output);
        }

        this.slabBuilder(
                        RecipeCategory.BUILDING_BLOCKS,
                        woodSet.getSlab().get(),
                        Ingredient.of(woodSet.getPlanks().get())
                )
                .unlockedBy(hasPlanks(woodSet), this.has(woodSet.getPlanks().get()))
                .group("wooden_slab")
                .save(this.output);

        buttonBuilder(woodSet.getButton().get(), Ingredient.of(woodSet.getPlanks().get())).group("wooden_button")
                .unlockedBy(hasPlanks(woodSet), has(woodSet.getPlanks().get()))
                .save(this.output);

        shelf(woodSet.getShelf().get(), woodSet.getStrippedLog().get());

        pressurePlate(woodSet.getPressurePlate().get(), woodSet.getPlanks().get());

        fenceBuilder(woodSet.getFence().get(), Ingredient.of(woodSet.getPlanks().get())).group("wooden_fence")
                .unlockedBy(hasPlanks(woodSet), has(woodSet.getPlanks().get()))
                .save(this.output);

        fenceGateBuilder(woodSet.getFenceGate().get(), Ingredient.of(woodSet.getPlanks().get())).group(
                "wooden_fence_gate").unlockedBy(hasPlanks(woodSet), has(woodSet.getPlanks().get())).save(this.output);

        doorBuilder(woodSet.getDoor().get(), Ingredient.of(woodSet.getPlanks().get())).group("wooden_door")
                .unlockedBy(hasPlanks(woodSet), has(woodSet.getPlanks().get()))
                .save(this.output);

        trapdoorBuilder(woodSet.getTrapdoor().get(), Ingredient.of(woodSet.getPlanks().get())).group("wooden_trapdoor")
                .unlockedBy(hasPlanks(woodSet), has(woodSet.getPlanks().get()))
                .save(this.output);

        signBuilder(woodSet.getSignItem().get(), Ingredient.of(woodSet.getPlanks().get())).group("wooden_sign")
                .unlockedBy(hasPlanks(woodSet), has(woodSet.getPlanks().get()))
                .save(this.output);

        hangingSign(woodSet.getHangingSignItem().get(), woodSet.getStrippedLog().get());

        shaped(
                List.of("p p", "ppp"),
                List.of('p'),
                List.of(woodSet.getPlanks().get()),
                RecipeCategory.TRANSPORTATION,
                woodSet.getBoatItem().get(),
                1
        );

        shapeless(
                List.of(Items.CHEST, woodSet.getBoatItem().get()),
                List.of(1, 1),
                RecipeCategory.TRANSPORTATION,
                woodSet.getChestBoatItem().get(),
                1,
                "chest_boat"
        );
    }

    protected String hasPlanks(WoodSet woodSet) {
        return has(woodSet, "_planks");
    }

    @SuppressWarnings("SameParameterValue")
    protected String has(WoodSet woodSet, String suffix) {
        return "has_" + woodSet.getName() + "_" + suffix;
    }


    protected String group(ItemLike item) {
        return identifier(item).toString();
    }


    protected String namespace(ItemLike item) {
        return identifier(item).getNamespace();
    }

    @SuppressWarnings("deprecation")
    protected Identifier identifier(ItemLike item) {
        return item.asItem().builtInRegistryHolder().key().identifier();
    }

    protected void campfire(List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        cooking(
                RecipeSerializer.CAMPFIRE_COOKING_RECIPE,
                CampfireCookingRecipe::new,
                ingredients,
                category,
                result,
                experience,
                cookingTime * 3,
                group,
                "_from_campfire"
        );
    }

    protected void smoking(List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        cooking(
                RecipeSerializer.SMOKING_RECIPE,
                SmokingRecipe::new,
                ingredients,
                category,
                result,
                experience,
                cookingTime / 2,
                group,
                "_from_smoking"
        );
    }

    protected void smelting(List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        cooking(
                RecipeSerializer.SMELTING_RECIPE,
                SmeltingRecipe::new,
                ingredients,
                category,
                result,
                experience,
                cookingTime,
                group,
                "_from_smelting"
        );
    }

    @SuppressWarnings("unused")
    protected void blasting(List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        cooking(
                RecipeSerializer.BLASTING_RECIPE,
                BlastingRecipe::new,
                ingredients,
                category,
                result,
                experience,
                cookingTime / 2,
                group,
                "_from_blasting"
        );
    }

    protected <T extends AbstractCookingRecipe> void cooking(RecipeSerializer<T> cookingSerializer, AbstractCookingRecipe.Factory<T> factory, List<ItemLike> ingredients, RecipeCategory category, ItemLike pResult, float experience, int cookingTime, String group, String recipeName) {
        for (ItemLike itemlike : ingredients) {
            SimpleCookingRecipeBuilder.generic(
                            Ingredient.of(itemlike),
                            category,
                            pResult,
                            experience,
                            cookingTime,
                            cookingSerializer,
                            factory
                    )
                    .group(group)
                    .unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(
                            this.output,
                            this.modid + ":" + getItemName(pResult) + recipeName + "_" + getItemName(itemlike)
                    );
        }
    }

    // The runner to add to the data generator
    public static class Runner extends RecipeProvider.Runner {
        protected final Set<WoodSet> woodSets;

        // Get the parameters from GatherDataEvent.
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, Set<WoodSet> woodSets) {
            super(output, lookupProvider);
            this.woodSets = woodSets;
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider provider, @NotNull RecipeOutput output) {
            return new RootboundRecipeProvider(provider, output, woodSets);
        }

        @Override
        public @NotNull String getName() {
            return "Rootbound Recipe Provider";
        }
    }

}