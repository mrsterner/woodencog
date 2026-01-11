package net.chauvedev.woodencog.compat.jei.tfcGuiFix;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.chauvedev.woodencog.WoodenCog;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.IHeat;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public class HeatTypedItemStack implements ITypedIngredient<ItemStack>{

    private final Holder<Item> itemHolder;
    private final CompoundTag tag;
    private final int count;
    private final IHeat heat;

    public HeatTypedItemStack(ItemStack ingredient) {
        this.itemHolder = ingredient.getItemHolder();
        this.tag = ingredient.get(WoodenCog.GENERIC_TAG);
        this.count = ingredient.getCount();
        this.heat = HeatCapability.get(ingredient);
    }

    public static ITypedIngredient<ItemStack> create(ItemStack ingredient) {
        return (ingredient.getCount() == 1 ?
                HeatNormalizedTypedItemStack.create(ingredient) :
                new HeatTypedItemStack(ingredient));
    }

    public ItemStack getIngredient() {
        ItemStack itemStack = new ItemStack(this.itemHolder, this.count);
        if (this.tag != null) {
            itemStack.set(WoodenCog.GENERIC_TAG, this.tag);
        }
        if(this.heat != null){
            //WoodenCog.LOGGER.info("HeatTypedItemStack returned "+itemStack.getItem()+" ingredient with "+ heat.getTemperature());
            HeatCapability.setTemperature(itemStack, heat.getTemperature());
        }
        return itemStack;
    }

    public Optional<ItemStack> getItemStack() {
        return Optional.of(this.getIngredient());
    }

    public IIngredientType<ItemStack> getType() {
        return VanillaTypes.ITEM_STACK;
    }

    public String toString() {
        return "TypedItemStack{itemHolder=" + this.itemHolder + ", tag=" + this.tag + ", count=" + this.count + "}";
    }

    public Holder<Item> itemHolder() {
        return this.itemHolder;
    }

    @Nullable
    public CompoundTag tag() {
        return this.tag;
    }

    public int count() {
        return this.count;
    }

    public IHeat heat(){
        return this.heat;
    }

}
