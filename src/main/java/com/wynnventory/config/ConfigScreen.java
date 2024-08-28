package com.wynnventory.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen {

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.wynnventory.config"));

        // Categories
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.wynnventory.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigManager configManager = ConfigManager.getInstance();
        // Entries
        general.addEntry(entryBuilder.startIntSlider(Component.translatable("option.wynnventory.api_delay.get"), configManager.getFetchUserSetting(), ConfigManager.FETCH_MIN_DELAY_MINS, ConfigManager.FETCH_MAX_DELAY_MINS)
                .setDefaultValue(ConfigManager.FETCH_DEFAULT_DELAY_MINS)
//                .setTooltip(Component.translatable("option.wynnventory.api_delay.tooltip"))
                .setSaveConsumer(configManager::setFetchUserSetting)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("option.wynnventory.api_delay.post"), configManager.getSendUserSetting(), ConfigManager.SEND_MIN_DELAY_MINS, ConfigManager.SEND_MAX_DELAY_MINS)
                .setDefaultValue(ConfigManager.SEND_DEFAULT_DELAY_MINS)
//                .setTooltip(Component.translatable("option.wynnventory.api_delay.tooltip"))
                .setSaveConsumer(configManager::setSendUserSetting)
                .build());

        builder.setSavingRunnable(ConfigManager.getInstance()::saveConfig);

        return builder.build();
    }
}
