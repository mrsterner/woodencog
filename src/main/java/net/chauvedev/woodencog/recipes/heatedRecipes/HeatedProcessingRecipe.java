package net.chauvedev.woodencog.recipes.heatedRecipes;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.chauvedev.woodencog.recipes.heatedRecipes.output.DynamicProcessingOutput;
import net.chauvedev.woodencog.utils.HeatHandlingUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.chauvedev.woodencog.recipes.heatedRecipes.HeatedProcessingRecipeParams;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class HeatedProcessingRecipe<I extends RecipeInput, P extends HeatedProcessingRecipeParams> implements Recipe<I> {

    protected P params;
    protected NonNullList<Ingredient> ingredients;
    protected NonNullList<DynamicProcessingOutput<?>> results;
    protected NonNullList<SizedFluidIngredient> fluidIngredients;
    protected NonNullList<FluidStack> fluidResults;
    protected int processingDuration;
    protected HeatCondition requiredHeat;
    protected WoodenCogHeatCondition extraHeatCondition;

    private RecipeType<?> type;
    private RecipeSerializer<?> serializer;
    private IRecipeTypeInfo typeInfo;
    private Supplier<ItemStack> forcedResult;

    public HeatedProcessingRecipe(IRecipeTypeInfo typeInfo, P params) {
        this.params = params;
        this.ingredients = params.ingredients;
        this.fluidIngredients = params.fluidIngredients;
        this.results = params.results;
        this.fluidResults = params.fluidResults;
        this.processingDuration = params.processingDuration;
        this.requiredHeat = params.requiredHeat;
        this.extraHeatCondition = params.extraHeatCondition;
        this.type = typeInfo.getType();
        this.serializer = typeInfo.getSerializer();
        this.typeInfo = typeInfo;
        this.forcedResult = null;
    }

    // Recipe type options
    protected abstract int getMaxInputCount();
    protected abstract int getMaxOutputCount();

    protected boolean canRequireHeat() {
        return false;
    }

    protected boolean canSpecifyDuration() {
        return false;
    }

    protected int getMaxFluidInputCount() {
        return 0;
    }

    protected int getMaxFluidOutputCount() {
        return 0;
    }

    protected boolean canRequireExtraHeat() {
        return false;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        int ingredientCount = ingredients.size();
        int outputCount = results.size();

        if (ingredientCount > getMaxInputCount())
            errors.add("Recipe has more item inputs (" + ingredientCount + ") than supported ("
                    + getMaxInputCount() + ").");

        if (outputCount > getMaxOutputCount())
            errors.add("Recipe has more item outputs (" + outputCount + ") than supported ("
                    + getMaxOutputCount() + ").");

        ingredientCount = fluidIngredients.size();
        outputCount = fluidResults.size();

        if (ingredientCount > getMaxFluidInputCount())
            errors.add("Recipe has more fluid inputs (" + ingredientCount + ") than supported ("
                    + getMaxFluidInputCount() + ").");

        if (outputCount > getMaxFluidOutputCount())
            errors.add("Recipe has more fluid outputs (" + outputCount + ") than supported ("
                    + getMaxFluidOutputCount() + ").");

        if (processingDuration > 0 && !canSpecifyDuration())
            errors.add("Recipe specified a duration. Durations have no impact on this type of recipe.");

        if (requiredHeat != HeatCondition.NONE && !canRequireHeat())
            errors.add("Recipe specified a heat condition. Heat conditions have no impact on this type of recipe.");

        if (extraHeatCondition.hasTemp() && !canRequireExtraHeat())
            errors.add("Recipe specified an extra heat condition. Extra heat conditions have no impact on this type of recipe.");

        return errors;
    }

    public P getParams() {
        return params;
    }

    /**
     * @deprecated Use getHeatedIngredients() instead
     */
    @Override
    @Deprecated
    public NonNullList<Ingredient> getIngredients() {
        // Return empty to avoid confusion, use getHeatedIngredients()
        return NonNullList.create();
    }

    public NonNullList<Ingredient> getHeatedIngredients() {
        return ingredients;
    }

    public NonNullList<SizedFluidIngredient> getFluidIngredients() {
        return fluidIngredients;
    }

    public List<DynamicProcessingOutput<?>> getRollableResults() {
        return results;
    }

    public NonNullList<FluidStack> getFluidResults() {
        return fluidResults;
    }

    public void enforceNextResult(Supplier<ItemStack> stack) {
        forcedResult = stack;
    }

    public List<ItemStack> rollResults(List<ItemStack> usedItems, RandomSource randomSource) {
        return rollResults(this.getRollableResults(), usedItems, randomSource);
    }

    public List<ItemStack> rollResults(List<DynamicProcessingOutput<?>> rollableResults, float temp) {
        List<ItemStack> results = new ArrayList<>();
        for(int i = 0; i < rollableResults.size(); ++i) {
            DynamicProcessingOutput<?> output = rollableResults.get(i);
            DynamicProcessingOutput.setDynamicData(output,temp);
            ItemStack stack = i == 0 && this.forcedResult != null ? this.forcedResult.get() : output.rollOutput();
            results.add(stack);
        }
        return results;
    }

    public List<ItemStack> rollResults(
            List<DynamicProcessingOutput<?>> rollableResults,
            List<ItemStack> usedItems,
            RandomSource randomSource
    ) {
        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < rollableResults.size(); i++) {
            DynamicProcessingOutput<?> output = rollableResults.get(i);

            if (usedItems != null) {
                if (output.getType() == DynamicProcessingOutput.ProcessingOutputTypes.HEATED) {
                    float temp = HeatHandlingUtil.computeThermalEquilibrium(usedItems);
                    DynamicProcessingOutput.setDynamicData(output, temp);
                } else {
                    DynamicProcessingOutput.setDynamicData(output, usedItems);
                }
            }

            ItemStack stack = i == 0 && forcedResult != null ? forcedResult.get() : output.rollOutput(randomSource);
            if (!stack.isEmpty())
                results.add(stack);
        }
        return results;
    }

    public int getProcessingDuration() {
        return processingDuration;
    }

    public HeatCondition getRequiredHeat() {
        return requiredHeat;
    }

    public WoodenCogHeatCondition getExtraHeatCondition() {
        return extraHeatCondition;
    }

    public boolean testTemperature(float sourceTemp) {
        return extraHeatCondition.testSourceTemp(sourceTemp);
    }

    // IRecipe<> paperwork
    @Override
    public ItemStack assemble(I input, HolderLookup.Provider provider) {
        return getResultItem(provider);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return getRollableResults().isEmpty() ? ItemStack.EMPTY
                : getRollableResults().get(0).getStack();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String getGroup() {
        return "heated_processing";
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }

    public IRecipeTypeInfo getTypeInfo() {
        return typeInfo;
    }

    public static <P extends HeatedProcessingRecipeParams, R extends HeatedProcessingRecipe<?, P>> MapCodec<R> codec(
            Factory<P, R> factory,
            MapCodec<P> paramsCodec
    ) {
        return paramsCodec.xmap(factory::create, recipe -> recipe.getParams())
                .validate(recipe -> {
                    var errors = recipe.validate();
                    if (errors.isEmpty())
                        return DataResult.success(recipe);
                    errors.add(recipe.getClass().getSimpleName() + " failed validation:");
                    return DataResult.error(() -> Joiner.on('\n').join(errors), recipe);
                });
    }

    public static <P extends HeatedProcessingRecipeParams, R extends HeatedProcessingRecipe<?, P>> StreamCodec<RegistryFriendlyByteBuf, R> streamCodec(
            Factory<P, R> factory,
            StreamCodec<RegistryFriendlyByteBuf, P> streamCodec
    ) {
        return streamCodec.map(factory::create, HeatedProcessingRecipe::getParams);
    }

    @FunctionalInterface
    public interface Factory<P extends HeatedProcessingRecipeParams, R extends HeatedProcessingRecipe<?, P>> {
        R create(P params);
    }
}