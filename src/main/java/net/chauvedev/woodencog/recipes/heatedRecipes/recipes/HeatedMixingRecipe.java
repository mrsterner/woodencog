package net.chauvedev.woodencog.recipes.heatedRecipes.recipes;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.chauvedev.woodencog.recipes.heatedRecipes.AllHeatedRecipeTypes;
import net.chauvedev.woodencog.recipes.heatedRecipes.HeatedProcessingRecipeParams;
import net.minecraft.world.item.crafting.Recipe;

public class HeatedMixingRecipe extends HeatedBasinRecipe {

    public HeatedMixingRecipe(HeatedProcessingRecipeParams params) {
        super(AllHeatedRecipeTypes.HEATED_MIXING, params);
    }

    public static boolean match(BasinBlockEntity basin, Recipe<?> recipe) {
        return HeatedBasinRecipe.match(basin, recipe);
    }

    public static boolean apply(BasinBlockEntity basin, Recipe<?> recipe) {
        return HeatedBasinRecipe.apply(basin, recipe);
    }
}