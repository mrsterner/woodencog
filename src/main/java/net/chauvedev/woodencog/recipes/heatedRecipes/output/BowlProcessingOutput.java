package net.chauvedev.woodencog.recipes.heatedRecipes.output;

//import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.common.blocks.BowlBlock;
import net.dries007.tfc.common.component.Bowl;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodData;
import net.dries007.tfc.common.component.food.IFood;
import net.dries007.tfc.common.component.food.Nutrient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//import net.minecraftforge.registries.RegistryObject;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class BowlProcessingOutput extends DynamicProcessingOutput<List<ItemStack>> {

    public static final int HUNGER_VALUE = 4;

    public BowlProcessingOutput(Item outputBowl, int count, float chance) {
        super(new ItemStack(outputBowl, count), chance);
    }

    public BowlProcessingOutput(ItemStack outputBowl, float chance) {
        super(outputBowl,chance);
    }

    /**
     * Used only for JEI
     * @return all posible results for this recipe
     */
    public abstract List<ItemStack> getStacks();

    public ItemStack getBowlItem(Map<Nutrient, DeferredHolder<Item, Item>> map, float decayModifier){
        List<ItemStack> usedItems = this.getDynamicData();
        usedItems.sort(Comparator.comparing(ItemStack::getCount)
                .thenComparing((itemx) -> BuiltInRegistries.ITEM.getKey(itemx.getItem())));

        int ingredientCount = 0;
        float water = 20, saturation = 2;
        float[] nutrition = new float[Nutrient.TOTAL];
        ItemStack resultStack = ItemStack.EMPTY;

        // ingredientes recibidos
        final List<ItemStack> itemIngredients = new ArrayList<>();

        for (ItemStack stack : usedItems) {
            final @Nullable IFood food = FoodCapability.get(stack);
            if (food != null) {
                itemIngredients.add(stack);
                if (food.isRotten()) {
                    ingredientCount = 0;
                    break;
                }
                final FoodData data = food.getData();
                water += data.water();
                saturation += data.saturation();
                for (Nutrient nutrient : Nutrient.VALUES) {
                    nutrition[nutrient.ordinal()] += data.nutrient(nutrient);
                }
                ingredientCount++;
            }
        }

        if (ingredientCount > 0) {
            float multiplier = 1 - (0.05f * ingredientCount);
            water *= multiplier;
            saturation *= multiplier;

            Nutrient maxNutrient = Nutrient.GRAIN;
            float maxNutrientValue = 0.0F;

            for (Nutrient nutrient : Nutrient.VALUES) {
                final int idx = nutrient.ordinal();
                nutrition[idx] *= multiplier;

                if (nutrition[idx] > maxNutrientValue) {
                    maxNutrientValue = nutrition[idx];
                    maxNutrient = nutrient;
                }
            }

            FoodData data = FoodData.ofFood(HUNGER_VALUE, saturation, water, decayModifier);
            for (Nutrient nutrient : Nutrient.VALUES) {
                data.with(Nutrient.values()[nutrient.ordinal()], nutrition[nutrient.ordinal()]);
            }

            long created = FoodCapability.getRoundedCreationDate();

            resultStack = new ItemStack(map.get(maxNutrient).get(), getStack().getCount());

            final @Nullable IFood food = FoodCapability.get(resultStack);

            /*TODO
            if (food instanceof DynamicBowlHandler handler) {
                handler.setCreationDate(created);
                handler.setIngredients(itemIngredients);
                handler.setFood(data);
            }

             */

            CompoundTag bowlTag = new CompoundTag();
            bowlTag.putString("id", BuiltInRegistries.ITEM.getKey(getStack().getItem()).toString());
            bowlTag.putByte("Count", (byte) 1);
/*TODO
            CompoundTag custom = resultStack.getOrCreateTag();
            custom.put("bowl", bowlTag);

 */
        }

        return resultStack;
    }
}
