package net.chauvedev.woodencog.recipes.heatedRecipes;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.chauvedev.woodencog.WoodenCog;
import net.chauvedev.woodencog.recipes.heatedRecipes.recipes.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public enum AllHeatedRecipeTypes implements IRecipeTypeInfo {

    HEATED_BASIN(HeatedBasinRecipe::new),
    HEATED_PRESSING(HeatedPressingRecipe::new),
    HEATED_COMPACTING(HeatedCompactingRecipe::new),
    HEATED_MIXING(HeatedMixingRecipe::new);

    public static final Predicate<Recipe<?>> CAN_BE_AUTOMATED =
            (r) -> !r.getId().getPath().endsWith("_manual_only");

    private final ResourceLocation id;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
    private final @Nullable DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;
    private final Supplier<RecipeType<?>> type;

    AllHeatedRecipeTypes(
            Supplier<RecipeSerializer<?>> serializerSupplier,
            Supplier<RecipeType<?>> typeSupplier,
            boolean registerType
    ) {
        String name = this.name().toLowerCase();
        this.id = WoodenCog.asResource(name);
        this.serializerObject = Registers.SERIALIZER_REGISTER.register(name, serializerSupplier);

        if (registerType) {
            this.typeObject = Registers.TYPE_REGISTER.register(name, typeSupplier);
            this.type = () -> this.typeObject.get();
        } else {
            this.typeObject = null;
            this.type = typeSupplier;
        }
    }

    AllHeatedRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
        String name = this.name().toLowerCase();
        this.id = WoodenCog.asResource(name);
        this.serializerObject = Registers.SERIALIZER_REGISTER.register(name, serializerSupplier);
        this.typeObject = Registers.TYPE_REGISTER.register(name, () -> RecipeType.simple(this.id));
        this.type = () -> this.typeObject.get();
    }

    AllHeatedRecipeTypes(HeatedStandardProcessingRecipe.Factory<?> processingFactory) {
        this(() -> new HeatedStandardProcessingRecipe.Serializer<>(processingFactory));
    }

    public static void register(IEventBus modEventBus) {
        Registers.SERIALIZER_REGISTER.register(modEventBus);
        Registers.TYPE_REGISTER.register(modEventBus);
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) serializerObject.get();
    }

    @Override
    public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
        return (RecipeType<R>) type.get();
    }

    public <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(I inv, Level world) {
        return world.getRecipeManager().getRecipeFor(getType(), inv, world);
    }

    public static boolean shouldIgnoreInAutomation(Recipe<?> recipe) {
        RecipeSerializer<?> serializer = recipe.getSerializer();
        if (AllTags.AllRecipeSerializerTags.AUTOMATION_IGNORE.matches(serializer)) {
            return true;
        }
        return !CAN_BE_AUTOMATED.test(recipe);
    }

    private static class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER =
                DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, WoodenCog.MOD_ID);
        private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER =
                DeferredRegister.create(Registries.RECIPE_TYPE, WoodenCog.MOD_ID);
    }
}