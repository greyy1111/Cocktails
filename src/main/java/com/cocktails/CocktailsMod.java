package com.cocktails;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

@Mod(CocktailsMod.MODID)
public class CocktailsMod {
    public static final String MODID = "cocktails";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NETWORK_VERSION = "1";
    private static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "main"))
            .networkProtocolVersion(() -> NETWORK_VERSION)
            .clientAcceptedVersions(NETWORK_VERSION::equals)
            .serverAcceptedVersions(NETWORK_VERSION::equals)
            .simpleChannel();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<MobEffect> YORSH_EFFECT = MOB_EFFECTS.register("yorsh_effect", YorshEffect::new);
    public static final RegistryObject<MobEffect> SCREWDRIVER_EFFECT = MOB_EFFECTS.register("screwdriver_effect", ScrewdriverEffect::new);
    public static final RegistryObject<MobEffect> SANGRIA_EFFECT = MOB_EFFECTS.register("sangria_effect", SangriaEffect::new);
    public static final RegistryObject<SoundEvent> SANGRIA_MUSIC = SOUND_EVENTS.register("sangria_music",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "sangria_music")));

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
    public static final RegistryObject<Item> SANGRIA = ITEMS.register("sangria", () -> new DrinkItem(drinkProperties(), () -> new MobEffectInstance(SANGRIA_EFFECT.get(), 6000, 0)));

    public CocktailsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NETWORK.registerMessage(
                    0,
                    SangriaMusicStatePacket.class,
                    SangriaMusicStatePacket::encode,
                    SangriaMusicStatePacket::decode,
                    SangriaMusicStatePacket::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT)
            );
        });
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
            event.accept(SANGRIA);
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        SangriaEffect.tick(event.getEntity());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sangriamusic")
                .requires(source -> source.hasPermission(2))
                .executes(context -> toggleSangriaMusic(context.getSource()))
                .then(Commands.literal("on")
                        .executes(context -> setSangriaMusicEnabled(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setSangriaMusicEnabled(context.getSource(), false)))
                .then(Commands.literal("status")
                        .executes(context -> showSangriaMusicStatus(context.getSource()))));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NETWORK.send(PacketDistributor.PLAYER.with(() -> player),
                    new SangriaMusicStatePacket(SangriaMusicSavedData.get(player.getServer()).isEnabled()));
        }
    }

    private static int toggleSangriaMusic(CommandSourceStack source) {
        boolean enabled = !SangriaMusicSavedData.get(source.getServer()).isEnabled();
        return setSangriaMusicEnabled(source, enabled);
    }

    private static int setSangriaMusicEnabled(CommandSourceStack source, boolean enabled) {
        SangriaMusicSavedData.get(source.getServer()).setEnabled(enabled);
        NETWORK.send(PacketDistributor.ALL.noArg(), new SangriaMusicStatePacket(enabled));
        source.sendSuccess(() -> Component.literal("Sangria music: " + (enabled ? "ON" : "OFF")), true);
        return 1;
    }

    private static int showSangriaMusicStatus(CommandSourceStack source) {
        boolean enabled = SangriaMusicSavedData.get(source.getServer()).isEnabled();
        source.sendSuccess(() -> Component.literal("Sangria music: " + (enabled ? "ON" : "OFF")), false);
        return enabled ? 1 : 0;
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

    private record SangriaMusicStatePacket(boolean enabled) {
        private static void encode(SangriaMusicStatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.enabled);
        }

        private static SangriaMusicStatePacket decode(FriendlyByteBuf buffer) {
            return new SangriaMusicStatePacket(buffer.readBoolean());
        }

        private static void handle(SangriaMusicStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> SangriaMusicClient.setEnabled(packet.enabled)
            ));
            context.setPacketHandled(true);
        }
    }
}
