package net.chauvedev.woodencog.recipes.heatedRecipes.recipes;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.chauvedev.woodencog.blockEntities.BasinBlockEntityExtended;
import net.chauvedev.woodencog.config.WoodenCogCommonConfigs;
import net.chauvedev.woodencog.recipes.heatedRecipes.AllHeatedRecipeTypes;
import net.chauvedev.woodencog.recipes.heatedRecipes.HeatedProcessingRecipeParams;
import net.chauvedev.woodencog.utils.CogUtil;
import net.createmod.catnip.data.Iterate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HeatedBasinRecipe extends HeatedStandardProcessingRecipe<RecipeInput> {

    protected HeatedBasinRecipe(IRecipeTypeInfo type, HeatedProcessingRecipeParams params) {
        super(type, params);
    }

    public HeatedBasinRecipe(HeatedProcessingRecipeParams params) {
        this(AllHeatedRecipeTypes.HEATED_BASIN, params);
    }

    @Override
    protected int getMaxInputCount() {
        return 9;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 4;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 2;
    }

    @Override
    protected boolean canRequireHeat() {
        return true;
    }

    @Override
    protected boolean canRequireExtraHeat() {
        return true;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    // Static helper methods for matching and applying
    public static boolean match(BasinBlockEntity basin, Recipe<?> recipe) {
        FilteringBehaviour filter = basin.getFilter();

        if (CogUtil.logConditional(basin.getLevel() == null, HeatedBasinRecipe.class, "basin level is null"))
            return false;
        if (CogUtil.logConditional(filter == null, HeatedBasinRecipe.class, "filter is null"))
            return false;

        boolean filterTest = filter.test(recipe.getResultItem(basin.getLevel().registryAccess()));

        if (recipe instanceof HeatedBasinRecipe basinRecipe) {
            if (basinRecipe.getRollableResults().isEmpty() &&
                    !basinRecipe.getFluidResults().isEmpty()) {
                filterTest = filter.test(basinRecipe.getFluidResults().get(0));
            }
        }

        if (!filterTest) {
            return false;
        }

        return apply(basin, recipe, true);
    }

    public static boolean apply(BasinBlockEntity basin, Recipe<?> recipe) {
        boolean success = apply(basin, recipe, false);
        if (success && basin instanceof BasinBlockEntityExtended extended) {
            extended.autoChainRecipes();
        }
        return success;
    }

    private static boolean apply(BasinBlockEntity basin, Recipe<?> recipe, boolean test) {
        if (!(recipe instanceof HeatedBasinRecipe heatedRecipe)) {
            return false;
        }

        Level level = basin.getLevel();
        if (level == null) return false;

        IItemHandler availableItems = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                basin.getBlockPos(),
                null
        );
        if (availableItems == null) return false;

        IFluidHandler availableFluids = level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                basin.getBlockPos(),
                null
        );
        if (availableFluids == null) return false;

        if (basin instanceof BasinBlockEntityExtended extended) {
            try {
                float temp = extended.getHeatSourceTemperature();
                if (!heatedRecipe.testTemperature(temp)) {
                    return false;
                }
            } catch (NullPointerException e) {
                return false;
            }
        }

        List<ItemStack> recipeOutputItems = new ArrayList<>();
        List<FluidStack> recipeOutputFluids = new ArrayList<>();

        List<Ingredient> ingredients = new ArrayList<>(heatedRecipe.getHeatedIngredients());
        List<SizedFluidIngredient> fluidIngredients = heatedRecipe.getFluidIngredients();

        for (boolean simulate : Iterate.trueAndFalse) {
            if (!simulate && test) {
                return true;
            }

            int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
            int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];

            // Match item ingredients
            ingredientLoop:
            for (Ingredient ingredient : ingredients) {
                for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                    if (simulate && availableItems.getStackInSlot(slot).getCount() <= extractedItemsFromSlot[slot])
                        continue;

                    ItemStack extracted = availableItems.extractItem(slot, 1, true);
                    if (!ingredient.test(extracted)) {
                        continue;
                    }

                    if (!simulate) {
                        availableItems.extractItem(slot, 1, false);
                    }
                    extractedItemsFromSlot[slot]++;
                    continue ingredientLoop;
                }
                // Ingredient not found
                return false;
            }

            // Match fluid ingredients
            boolean fluidsAffected = false;
            fluidIngredientLoop:
            for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
                int amountRequired = fluidIngredient.amount();

                for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                    FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                    if (simulate && fluidStack.getAmount() <= extractedFluidsFromTank[tank])
                        continue;
                    if (!fluidIngredient.test(fluidStack))
                        continue;

                    int drainedAmount = Math.min(amountRequired, fluidStack.getAmount());
                    if (!simulate) {
                        fluidStack.shrink(drainedAmount);
                        fluidsAffected = true;
                    }
                    amountRequired -= drainedAmount;
                    if (amountRequired == 0) {
                        extractedFluidsFromTank[tank] += drainedAmount;
                        continue fluidIngredientLoop;
                    }
                }
                // Fluid ingredient not found
                return false;
            }

            if (fluidsAffected) {
                basin.getBehaviour(SmartFluidTankBehaviour.INPUT)
                        .forEach(SmartFluidTankBehaviour.TankSegment::onFluidStackChanged);
                basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT)
                        .forEach(SmartFluidTankBehaviour.TankSegment::onFluidStackChanged);
            }

            if (simulate) {
                // Roll outputs
                if (WoodenCogCommonConfigs.HANDLE_TEMPERATURE.get()) {
                    List<ItemStack> extractedItems = new ArrayList<>();
                    for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                        int amountUsed = extractedItemsFromSlot[slot];
                        if (amountUsed > 0) {
                            ItemStack original = availableItems.getStackInSlot(slot);
                            ItemStack used = original.copy();
                            used.setCount(amountUsed);
                            extractedItems.add(used);
                        }
                    }
                    recipeOutputItems.addAll(heatedRecipe.rollResults(extractedItems, level.random));
                } else {
                    recipeOutputItems.addAll(heatedRecipe.rollResults(null, level.random));
                }

                // Get remainder items
                CraftingInput remainderContainer = new DummyCraftingContainer(availableItems, extractedItemsFromSlot)
                        .asCraftInput();

                for (FluidStack fluidStack : heatedRecipe.getFluidResults()) {
                    if (!fluidStack.isEmpty()) {
                        recipeOutputFluids.add(fluidStack);
                    }
                }

                for (ItemStack stack : heatedRecipe.getRemainingItems(remainderContainer)) {
                    if (!stack.isEmpty()) {
                        recipeOutputItems.add(stack);
                    }
                }
            }

            if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate)) {
                return false;
            }
        }

        return true;
    }
}