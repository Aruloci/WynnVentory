package com.wynnventory.model.item;

import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.items.items.game.GearItem;

import java.time.Instant;

public class TradeMarketItemPriceHolder {
    private TradeMarketItemPriceInfo priceInfo;
    private final GearInfo info;
    private final Instant timestamp;
    private final boolean isShiny;

    public TradeMarketItemPriceHolder(TradeMarketItemPriceInfo priceInfo, GearInfo info, boolean isShiny) {
        this.priceInfo = priceInfo;
        this.info = info;
        this.timestamp = Instant.now();
        this.isShiny = isShiny;
    }

    public void setPriceInfo(TradeMarketItemPriceInfo priceInfo) {
        this.priceInfo = priceInfo;
    }

    public GearInfo getInfo() {
        return info;
    }

    public TradeMarketItemPriceInfo getPriceInfo() {
        return priceInfo;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isShiny() {
        return isShiny;
    }

    public boolean isPriceExpired(long minutes) {
        Instant now = Instant.now();
        return now.isAfter(timestamp.plusSeconds(minutes * 60));
    }
}
