package com.wynnventory.model.item;

import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.mc.McUtils;

public class TradeMarketItem {
    private SimplifiedGearItem item;
    private int listingPrice;
    private int amount;
    private String playerName;

    public TradeMarketItem(GearItem item, int listingPrice, int amount) {
        this.item = new SimplifiedGearItem(item);
        this.listingPrice = listingPrice;
        this.amount = amount;
        this.playerName = McUtils.playerName();
    }

    public SimplifiedGearItem getItem() {
        return item;
    }

    public int getListingPrice() {
        return listingPrice;
    }

    public int getAmount() {
        return amount;
    }

    public String getPlayerName() { return playerName; }
}
