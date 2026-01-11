package net.chauvedev.woodencog.mixin.recipes;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import net.chauvedev.woodencog.recipes.advancedProcessingRecipe.baseRecipes.SetItemStackProvider;
//import net.dries007.tfc.common.capabilities.MoldLike;
import net.dries007.tfc.util.Metal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
//import net.minecraftforge.fluids.FluidStack;
//import net.minecraftforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;

@Mixin(value = FillingBySpout.class, remap = false)
public class MixinFillingBySpout {
    /*TODO

    @Redirect(
            method = "canItemBeFilled",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/AllRecipeTypes;find(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private static<I extends RecipeInput, R extends Recipe<I>> Optional<R> canItemBeFilled(AllRecipeTypes instance, I inv, Level world) {
        if (instance.find(inv, world).isPresent()){
            FillingRecipe recipe = (FillingRecipe) instance.find(inv, world).get();

            boolean is_advanced_recipe = AllAdvancedRecipeTypes.CACHES.containsKey(recipe.getTypeInfo().getId().toString());
            if(is_advanced_recipe && !(Objects.equals(recipe.getIngredients().get(0).getItems()[0].getTag(), stack.getTag())
                    || stack.getTag() == null || stack.getTag().isEmpty())) {
                return Optional.empty();
            }
        }
        return instance.find(inv, world);
    }


    @Redirect(method = "fillItem",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FillingRecipe;rollResults(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private static List<ItemStack> fillItem(FillingRecipe instance, RandomSource randomSource) {
        List<ItemStack> results = instance.rollResults(randomSource);

        boolean is_advanced_recipe = AllAdvancedRecipeTypes.CACHES.containsKey(instance.getTypeInfo().getId().toString());
        if(is_advanced_recipe) {
            ArrayList<ItemStack> newStacks = new ArrayList<>();

            FluidStack toFill = availableFluid.copy();
            toFill.setAmount(requiredAmount);

            SetItemStackProvider provider = AllAdvancedRecipeTypes.CACHES.get(fillingRecipe.getId().toString());
            (results).forEach(o -> {
                ItemStack baseItem = provider.onResultStackSingle(stack.copyWithCount(o.getCount()),o);
                var mold = MoldLike.get(baseItem);
                if (mold != null) {
                    mold.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
                    var metal = Metal.get(mold.getFluidInTank(0).getFluid());
                    if (metal != null) mold.setTemperature(metal.getMeltTemperature());
                }
                newStacks.add(baseItem);
            });

            return newStacks;
        }
        return results;
    }
    */
}
