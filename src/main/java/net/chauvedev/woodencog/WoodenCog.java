package net.chauvedev.woodencog;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.chauvedev.woodencog.block.generator.WoodenGeneratorRenderer;
import net.chauvedev.woodencog.block.transformer.CTTransformerRenderer;
import net.chauvedev.woodencog.block.WoodencogBlockEntityTypes;
import net.chauvedev.woodencog.compat.Compat;
import net.chauvedev.woodencog.config.WoodenCogCommonConfigs;
import net.chauvedev.woodencog.datagen.DataGenerators;
import net.chauvedev.woodencog.datapack.DataPackRegistries;
import net.chauvedev.woodencog.interaction.CustomArmInteractionPointTypes;
import net.chauvedev.woodencog.item.WoodencogItems;
import net.chauvedev.woodencog.ponder.WoodenCogPonderPlugin;
import net.chauvedev.woodencog.recipes.advancedProcessingRecipe.AllAdvancedRecipeTypes;
import net.chauvedev.woodencog.recipes.heatedRecipes.AllHeatedRecipeTypes;
import net.chauvedev.woodencog.block.WoodencogBlocks;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
//import net.minecraftforge.eventbus.api.IEventBus;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.event.lifecycle.*;
//import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//import net.minecraftforge.registries.RegisterEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.function.UnaryOperator;

@Mod(WoodenCog.MOD_ID)
public class WoodenCog {
    public static final String MOD_ID = "woodencog";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(WoodenCog.MOD_ID);

    public WoodenCog(IEventBus modEventBus, ModContainer modContainer) {
        Compat.init(); //Load addon compatibility

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.register(this);
        REGISTRATE.registerEventListeners(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, WoodenCogCommonConfigs.SPEC);

        WoodencogItems.register(modEventBus);
        WoodencogBlocks.register();
        WoodencogBlockEntityTypes.register();

        AllAdvancedRecipeTypes.register(modEventBus);
        AllHeatedRecipeTypes.register(modEventBus);

        modEventBus.addListener(WoodenCog::onRegister);
        modEventBus.addListener(DataGenerators::gatherData);
        modEventBus.addListener(DataPackRegistries::register);
        register(modEventBus);
        modEventBus.addListener(this::addCreative);
    }
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Create.ID);

    public static final DataComponentType<CompoundTag> GENERIC_TAG = register(
            "generic_tag",
            builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    @ApiStatus.Internal
    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    private void setup(final FMLCommonSetupEvent event) {
        DataGenerators.registerSerializers();
    }

    public static void onRegister(final RegisterEvent event) {
        CustomArmInteractionPointTypes.init();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey() == AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey()){
            event.accept(WoodencogBlocks.CT_TRANSFORMER.get());
            event.accept(WoodencogBlocks.WOODEN_GENERATOR.get());
        }
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        BlockEntityRenderers.register(WoodencogBlockEntityTypes.CT_TRANSFORMER.get(), CTTransformerRenderer::new);
        BlockEntityRenderers.register(WoodencogBlockEntityTypes.WOODEN_GENERATOR.get(), WoodenGeneratorRenderer::new);

        PonderIndex.addPlugin(new WoodenCogPonderPlugin());
    }

    public static ResourceLocation asResource(String path) {
        WoodenCog.LOGGER.info("ResourceLocation created: "+ ResourceLocation.tryBuild(WoodenCog.MOD_ID, path));
        return ResourceLocation.tryBuild(WoodenCog.MOD_ID, path);
    }

}


