package com.cocktails;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
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
        if (player.level().isClientSide()
                || !player.hasEffect(SCREWDRIVER_EFFECT.get())
                || !player.isCrouching()
                || !(held.getItem() instanceof BlockItem)) {
            return false;
        }

        Level level = player.level();
        BlockPos targetPos = player.blockPosition().below();

        if (!level.getBlockState(targetPos).canBeReplaced()) {
            targetPos = targetPos.below();
        }

        // in this place you dont allow place blocks
        if (!level.getBlockState(targetPos).canBeReplaced()) {
            return false;
        }

        // check protect and player laws
        if (!level.mayInteract(player, targetPos)) {
            return false;
        }

        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(targetPos),
                Direction.UP,
                targetPos,
                false
        );

        UseOnContext context = new UseOnContext(player, hand, hitResult);

        int oldCount = held.getCount();
        InteractionResult result = ForgeHooks.onPlaceItemIntoWorld(context);

        // in creative mod blocks should not be spent
        if (player.getAbilities().instabuild) {
            held.setCount(oldCount);
        }

        return result.consumesAction();
    }

    private boolean wantsToPlaceBlockUnderPlayer(Player player, ItemStack held) {
        return player.hasEffect(SCREWDRIVER_EFFECT.get())
                && player.isCrouching()
                && held.getItem() instanceof BlockItem;
    }

    private void handleBlockPlacement(PlayerInteractEvent event) {
        Player player = event.getEntity();

        if (!wantsToPlaceBlockUnderPlayer(player, event.getItemStack())) {
            return;
        }

        if (player.level().isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        boolean placed = tryPlaceBlockUnderPlayer(player, event.getHand(), event.getItemStack());
        event.setCancellationResult(placed ? InteractionResult.CONSUME : InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleBlockPlacement(event);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleBlockPlacement(event);
    }
}
