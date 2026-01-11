package net.chauvedev.woodencog.recipes.heatedRecipes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.codec.CreateCodecs;
import net.chauvedev.woodencog.recipes.heatedRecipes.output.DynamicProcessingOutput;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class HeatedProcessingRecipeParams {

    public static final MapCodec<HeatedProcessingRecipeParams> CODEC = codec(HeatedProcessingRecipeParams::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, HeatedProcessingRecipeParams> STREAM_CODEC =
            streamCodec(HeatedProcessingRecipeParams::new);

    protected NonNullList<Ingredient> ingredients;
    protected NonNullList<DynamicProcessingOutput<?>> results;
    protected NonNullList<SizedFluidIngredient> fluidIngredients;
    protected NonNullList<FluidStack> fluidResults;
    protected int processingDuration;
    protected HeatCondition requiredHeat;
    protected WoodenCogHeatCondition extraHeatCondition;

    protected HeatedProcessingRecipeParams() {
        this.ingredients = NonNullList.create();
        this.results = NonNullList.create();
        this.fluidIngredients = NonNullList.create();
        this.fluidResults = NonNullList.create();
        this.processingDuration = 0;
        this.requiredHeat = HeatCondition.NONE;
        this.extraHeatCondition = WoodenCogHeatCondition.of(0);
    }

    protected static <P extends HeatedProcessingRecipeParams> MapCodec<P> codec(Supplier<P> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.either(CreateCodecs.SIZED_FLUID_INGREDIENT, Ingredient.CODEC)
                        .listOf()
                        .fieldOf("ingredients")
                        .forGetter(HeatedProcessingRecipeParams::ingredients),
                Codec.either(FluidStack.CODEC, DynamicProcessingOutput.CODEC)
                        .listOf()
                        .fieldOf("results")
                        .forGetter(HeatedProcessingRecipeParams::results),
                Codec.INT.optionalFieldOf("processing_time", 0)
                        .forGetter(HeatedProcessingRecipeParams::processingDuration),
                HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE)
                        .forGetter(HeatedProcessingRecipeParams::requiredHeat),
                Codec.INT.optionalFieldOf("extra_heat_temperature", 0)
                        .forGetter(params -> params.extraHeatCondition.getTemperature())
        ).apply(instance, (ingredients, results, processingDuration, requiredHeat, extraTemp) -> {
            P params = factory.get();

            // Process ingredients
            ingredients.forEach(either -> either
                    .ifRight(params.ingredients::add)
                    .ifLeft(params.fluidIngredients::add));

            // Process results
            results.forEach(either -> either
                    .ifRight(params.results::add)
                    .ifLeft(params.fluidResults::add));

            params.processingDuration = processingDuration;
            params.requiredHeat = requiredHeat;
            params.extraHeatCondition = WoodenCogHeatCondition.of(extraTemp);

            return params;
        }));
    }

    protected static <P extends HeatedProcessingRecipeParams> StreamCodec<RegistryFriendlyByteBuf, P> streamCodec(
            Supplier<P> factory
    ) {
        return StreamCodec.of(
                (buffer, params) -> params.encode(buffer),
                buffer -> {
                    P params = factory.get();
                    params.decode(buffer);
                    return params;
                }
        );
    }

    // Helper methods for codec
    protected final List<Either<SizedFluidIngredient, Ingredient>> ingredients() {
        List<Either<SizedFluidIngredient, Ingredient>> list =
                new ArrayList<>(this.ingredients.size() + this.fluidIngredients.size());
        this.ingredients.forEach(ingredient -> list.add(Either.right(ingredient)));
        this.fluidIngredients.forEach(ingredient -> list.add(Either.left(ingredient)));
        return list;
    }

    protected final List<Either<FluidStack, DynamicProcessingOutput<?>>> results() {
        List<Either<FluidStack, DynamicProcessingOutput<?>>> list =
                new ArrayList<>(this.results.size() + this.fluidResults.size());
        this.results.forEach(result -> list.add(Either.right(result)));
        this.fluidResults.forEach(result -> list.add(Either.left(result)));
        return list;
    }

    protected final int processingDuration() {
        return processingDuration;
    }

    protected final HeatCondition requiredHeat() {
        return requiredHeat;
    }

    protected void encode(RegistryFriendlyByteBuf buffer) {
        CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).encode(buffer, ingredients);
        CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).encode(buffer, fluidIngredients);
        CatnipStreamCodecBuilders.nonNullList(DynamicProcessingOutput.STREAM_CODEC).encode(buffer, results);
        CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).encode(buffer, fluidResults);
        ByteBufCodecs.VAR_INT.encode(buffer, processingDuration);
        HeatCondition.STREAM_CODEC.encode(buffer, requiredHeat);
        ByteBufCodecs.VAR_INT.encode(buffer, extraHeatCondition.serialize());
    }

    protected void decode(RegistryFriendlyByteBuf buffer) {
        ingredients = CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).decode(buffer);
        fluidIngredients = CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).decode(buffer);
        results = CatnipStreamCodecBuilders.nonNullList(DynamicProcessingOutput.STREAM_CODEC).decode(buffer);
        fluidResults = CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).decode(buffer);
        processingDuration = ByteBufCodecs.VAR_INT.decode(buffer);
        requiredHeat = HeatCondition.STREAM_CODEC.decode(buffer);
        int temp = ByteBufCodecs.VAR_INT.decode(buffer);
        extraHeatCondition = WoodenCogHeatCondition.of(temp);
    }
}