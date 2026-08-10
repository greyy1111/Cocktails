package com.cocktails;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(CocktailsMod.MODID)
public class CocktailsMod {
    public static final String MODID = "cocktails";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);

    public static final RegistryObject<MobEffect> YORSH_EFFECT = MOB_EFFECTS.register("yorsh_effect", YorshEffect::new);
    public static final RegistryObject<MobEffect> SCREWDRIVER_EFFECT = MOB_EFFECTS.register("screwdriver_effect", ScrewdriverEffect::new);

    private static Item.Properties drinkProperties() {
        return new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE)
                .stacksTo(16)
                .food(new FoodProperties.Builder()
                        .alwaysEat()
                        .nutrition(1)
                        .saturationMod(0.1f)
                        .build());
    }

    public static final RegistryObject<Item> WISKEY = ITEMS.register("wiskey", () -> new DrinkItem(drinkProperties()));
    public static final RegistryObject<Item> COGNAC = ITEMS.register("cognac", () -> new DrinkItem(drinkProperties()));
    public static final RegistryObject<Item> VODKA = ITEMS.register("vodka", () -> new DrinkItem(drinkProperties()));
    public static final RegistryObject<Item> JIN = ITEMS.register("jin", () -> new DrinkItem(drinkProperties()));
    public static final RegistryObject<Item> SPRITE = ITEMS.register("sprite", () -> new DrinkItem(drinkProperties()));
    public static final RegistryObject<Item> RUM = ITEMS.register("rum", () -> new DrinkItem(drinkProperties()));
    public static final RegistryObject<Item> YORSH = ITEMS.register("yorsh", () -> new DrinkItem(drinkProperties(), () -> new MobEffectInstance(YORSH_EFFECT.get(), 2400, 0)));
    public static final RegistryObject<Item> SCREWDRIVER = ITEMS.register("screwdriver", () -> new DrinkItem(drinkProperties(), () -> new MobEffectInstance(SCREWDRIVER_EFFECT.get(), 12000, 0)));

    public CocktailsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Cocktails Mod initialized!");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(WISKEY);
            event.accept(COGNAC);
            event.accept(VODKA);
            event.accept(JIN);
            event.accept(SPRITE);
            event.accept(RUM);
            event.accept(YORSH);
            event.accept(SCREWDRIVER);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (YorshEffect.isInActivePhase(event.getEntity())) {
            event.setAmount(event.getAmount() * 0.5f);
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (attacker.hasEffect(SCREWDRIVER_EFFECT.get())) {
                event.setAmount(event.getAmount() * 0.2f);
            }
        }
    }

    private boolean tryPlaceBlockUnderPlayer(Player player, InteractionHand hand, ItemStack held) {
        if (!player.level().isClientSide() && player.hasEffect(SCREWDRIVER_EFFECT.get()) && player.isCrouching()) {
            if (held.getItem() instanceof BlockItem blockItem) {
                Level level = player.level();
                BlockPos targetPos = player.blockPosition().below();
                if (!level.getBlockState(targetPos).canBeReplaced()) {
                    targetPos = targetPos.below();
                }
                if (level.getBlockState(targetPos).canBeReplaced()) {
                    BlockState stateToPlace = blockItem.getBlock().defaultBlockState();
                    if (level.setBlock(targetPos, stateToPlace, 3)) {
                        SoundType soundType = stateToPlace.getSoundType(level, targetPos, player);
                        level.playSound(null, targetPos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
                        if (!player.getAbilities().instabuild) {
                            held.shrink(1);
                        }
                        player.swing(hand, true);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (tryPlaceBlockUnderPlayer(event.getEntity(), event.getHand(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryPlaceBlockUnderPlayer(event.getEntity(), event.getHand(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }
}
