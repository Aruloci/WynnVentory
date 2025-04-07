package com.wynnventory;

import com.sun.tools.javac.Main;
import com.wynntils.utils.mc.McUtils;
import com.wynnventory.api.WynnventoryAPI;
import com.wynnventory.api.WynnventoryScheduler;
import com.wynnventory.config.ConfigManager;
import com.wynnventory.model.item.Lootpool;
import com.wynnventory.model.keymapping.StickyKeyMapping;
import com.wynnventory.ui.LootpoolScreen;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WynnventoryMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("wynnventory");
	public static final Optional<ModContainer> WYNNVENTORY_INSTANCE = FabricLoader.getInstance().getModContainer("wynnventory");
	public static String WYNNVENTORY_VERSION;
	public static String WYNNVENTORY_MOD_NAME;

	private static final WynnventoryAPI API = new WynnventoryAPI();
	private static final ExecutorService executorService = Executors.newCachedThreadPool();
	private static List<Lootpool> RAID_POOLS;
	private static List<Lootpool> LOOT_POOLS;

	private static boolean IS_DEV = false;

	public static KeyMapping KEY_OPEN_POOLS;

	@Override
	public void onInitializeClient() {
		if (WYNNVENTORY_INSTANCE.isEmpty()) {
			error("Could not find Wynnventory in Fabric Loader!");
			return;
		}
		WYNNVENTORY_VERSION = WYNNVENTORY_INSTANCE.get().getMetadata().getVersion().getFriendlyString();
		WYNNVENTORY_MOD_NAME = WYNNVENTORY_INSTANCE.get().getMetadata().getName();

		// Start WynnventoryScheduler
		WynnventoryScheduler.startScheduledTask();

		AutoConfig.register(ConfigManager.class, GsonConfigSerializer::new);
		registerKeyBinds();

		loadPools(); // Load all lootpools on load

		try {
			IS_DEV = Main.class.getClassLoader().loadClass("com.intellij.rt.execution.application.AppMainV2") != null;
		} catch (NoClassDefFoundError | Exception ignored) {
			IS_DEV = WYNNVENTORY_VERSION.contains("dev");
		}

		if (isDev()) warn("Wynnventory is running in dev environment. Mod will behave differently in non-dev environment.");
		LOGGER.info("Initialized Wynnventory with version {}", WYNNVENTORY_VERSION);
	}

	private static void registerKeyBinds() {
		KEY_OPEN_POOLS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.wynnventory.open_config",
				GLFW.GLFW_KEY_N,
				"category.wynnventory.keybinding"
		));

		StickyKeyMapping priceTooltipKey = (StickyKeyMapping) KeyBindingHelper.registerKeyBinding(new StickyKeyMapping(
				"key.wynnventory.toggle_tooltips",
				GLFW.GLFW_KEY_PERIOD,
				"category.wynnventory.keybinding",
				() -> true
		));

		StickyKeyMapping boxedPriceTooltipKey = (StickyKeyMapping) KeyBindingHelper.registerKeyBinding(new StickyKeyMapping(
				"key.wynnventory.toggle_boxed_item_tooltips",
				GLFW.GLFW_KEY_COMMA,
				"category.wynnventory.keybinding",
				() -> true
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
//			if (KEY_OPEN_CONFIG.consumeClick()) {
//				Minecraft.getInstance().setScreen(
//						AutoConfig.getConfigScreen(ConfigManager.class, Minecraft.getInstance().screen).get()
//				);
			if (KEY_OPEN_POOLS.consumeClick()) {
				client.setScreen(new LootpoolScreen(Component.literal("Lootruns"), LOOT_POOLS, RAID_POOLS));
			}
		});


		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (priceTooltipKey.hasStateChanged()) {
				ConfigManager config = ConfigManager.getInstance();
				config.setShowTooltips(!config.isShowTooltips());
				// Instruct AutoConfig to save the changes
				AutoConfig.getConfigHolder(ConfigManager.class).save();

				Component message;
				if (config.isShowTooltips()) {
					message = Component.literal("[Wynnventory] Trade Market item tooltips enabled").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
				} else {
					message = Component.literal("[Wynnventory] Trade Market item tooltips disabled").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
				}
				McUtils.sendMessageToClient(message);
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (boxedPriceTooltipKey.hasStateChanged()) {
				ConfigManager config = ConfigManager.getInstance();
				config.setShowBoxedItemTooltips(!config.isShowBoxedItemTooltips());
				// Save the changes using AutoConfig's save method
				AutoConfig.getConfigHolder(ConfigManager.class).save();

				Component message;
				if (config.isShowBoxedItemTooltips()) {
					message = Component.literal("[Wynnventory] Trade Market boxed item tooltips enabled").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
				} else {
					message = Component.literal("[Wynnventory] Trade Market boxed item tooltips disabled").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
				}

				McUtils.sendMessageToClient(message);
			}
		});
	}

	private static void loadPools() {
		CompletableFuture.supplyAsync(() -> API.getLootpools("lootrun"), executorService)
				.thenAccept(result -> LOOT_POOLS = result);
		CompletableFuture.supplyAsync(() -> API.getLootpools("raidpool"), executorService)
				.thenAccept(result -> RAID_POOLS = result);
	}

	public static void debug(String msg) {
		LOGGER.debug(msg);
	}

	public static void info(String msg) {
		LOGGER.info(msg);
	}

	public static void warn(String msg) {
		LOGGER.warn(msg);
	}

	public static void warn(String msg, Throwable t) {
		LOGGER.warn(msg, t);
	}

	public static void error(String msg) {
		LOGGER.error(msg);
	}

	public static void error(String msg, Throwable t) {
		LOGGER.error(msg, t);
	}

	public static boolean isDev() {
		return IS_DEV;
	}
}