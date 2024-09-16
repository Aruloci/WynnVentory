package com.wynnventory.model.item;

import java.util.ArrayList;
import java.util.List;

public class Lootpool {

    private List<LootpoolItem> lootpoolItems = new ArrayList<>();
    private String region;
    private String playerName;
    private String modVersion;

    public Lootpool(String region, String playerName, String modVersion) {
        this.region = region;
        this.playerName = playerName;
        this.modVersion = modVersion;
    }

    public void addItem(LootpoolItem item) {
        lootpoolItems.add(item);
    }

    public void addItems(List<LootpoolItem> items) {
        lootpoolItems.addAll(items);
    }

    public void removeItem(LootpoolItem item) {
        lootpoolItems.remove(item);
    }

    public void removeItems(List<LootpoolItem> items) {
        lootpoolItems.removeAll(items);
    }

    public List<LootpoolItem> getItems() {
        return lootpoolItems;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getModVersion() {
        return modVersion;
    }

    public void setModVersion(String modVersion) {
        this.modVersion = modVersion;
    }
}