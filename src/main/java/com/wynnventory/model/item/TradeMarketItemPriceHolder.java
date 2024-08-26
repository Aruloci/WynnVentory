package com.wynnventory.model.item;

import com.wynntils.models.items.WynnItem;

import java.time.Instant;

public class TradeMarketItemPriceHolder {
    private TradeMarketItemPriceInfo priceInfo;
    private final WynnItem item;
    private final Instant timestamp;

    public TradeMarketItemPriceHolder(TradeMarketItemPriceInfo priceInfo, WynnItem item) {
        this.priceInfo = priceInfo;
        this.item = item;
        this.timestamp = Instant.now();
    }

    public void setPriceInfo(TradeMarketItemPriceInfo priceInfo) {
        this.priceInfo = priceInfo;
    }

    public WynnItem getItem() {
        return item;
    }

    public TradeMarketItemPriceInfo getPriceInfo() {
        return priceInfo;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isPriceExpired(long minutes) {
        Instant now = Instant.now();
        return now.isAfter(timestamp.plusSeconds(minutes * 60));
    }
}
