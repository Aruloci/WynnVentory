package com.wynnventory;

import com.sun.tools.javac.Main;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

public final class Wynnventory {
	public static final String 	MOD_ID = "wynnventory";
	private static String	 	MOD_NAME;
	private static ModLoader 	MOD_LOADER;
	private static String 		MOD_VERSION = "";
	private static File 		MOD_FILE;
	private static boolean 		DEV_MODE;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static void init(ModLoader loader, String modVersion, String modName, boolean isDevelopmentEnvironment, File modFile) {
		MOD_LOADER = loader;
		MOD_VERSION = modVersion;
		MOD_NAME = modName;
		DEV_MODE = isDevelopmentEnvironment;
		MOD_FILE = modFile;

		if (isDevelopmentEnvironment) {
			warn("WynnVentory is running in dev environment. Mod will behave differently in non-dev environment.");
		}

		LOGGER.info("Initialized WynnVentoryMod with version {}", MOD_VERSION);
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

	public static ModLoader getModLoader() {
		return MOD_LOADER;
	}

	public static String getModVersion() {
		return MOD_VERSION;
	}

	public static String getModName() {
		return MOD_NAME;
	}

	public static File getModFile() {
		return MOD_FILE;
	}

	public static boolean isDevelopmentEnvironment() {
		return DEV_MODE;
	}

	public enum ModLoader {
		FORGE,
		FABRIC
	}
}