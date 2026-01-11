package net.chauvedev.woodencog.recipes.heatedRecipes.recipes;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.chauvedev.woodencog.recipes.heatedRecipes.HeatedProcessingRecipe;
import net.chauvedev.woodencog.recipes.heatedRecipes.HeatedProcessingRecipeParams;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class HeatedStandardProcessingRecipe<T extends RecipeInput> extends HeatedProcessingRecipe<T, HeatedProcessingRecipeParams> {

    public HeatedStandardProcessingRecipe(IRecipeTypeInfo typeInfo, HeatedProcessingRecipeParams params) {
        super(typeInfo, params);
    }

    @FunctionalInterface
    public interface Factory<R extends HeatedStandardProcessingRecipe<?>>
            extends HeatedProcessingRecipe.Factory<HeatedProcessingRecipeParams, R> {
        R create(HeatedProcessingRecipeParams params);
    }

    public static class Serializer<R extends HeatedStandardProcessingRecipe<?>> implements RecipeSerializer<R> {
        private final Factory<R> factory;
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(Factory<R> factory) {
            this.factory = factory;
            this.codec = HeatedProcessingRecipe.codec(factory, HeatedProcessingRecipeParams.CODEC);
            this.streamCodec = HeatedProcessingRecipe.streamCodec(factory, HeatedProcessingRecipeParams.STREAM_CODEC);
        }

        @Override
        public MapCodec<R> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return streamCodec;
        }

        public Factory<R> factory() {
            return factory;
        }
    }
}