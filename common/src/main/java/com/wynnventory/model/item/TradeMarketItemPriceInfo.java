package com.wynnventory.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TradeMarketItemPriceInfo {
    @JsonProperty("average_price")
    private double averagePrice;

    @JsonProperty("highest_price")
    private int highestPrice;

    @JsonProperty("lowest_price")
    private int lowestPrice;

    @JsonProperty("unidentified_average_price")
    private Double unidentifiedAveragePrice;

    // Default constructor
    public TradeMarketItemPriceInfo() {}

    // Getters and Setters

    public int getAveragePrice() {
        return (int) averagePrice;
    }

    public void setAveragePrice(double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public int getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(int highestPrice) {
        this.highestPrice = highestPrice;
    }

    public int getLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(int lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public Double getUnidentifiedAveragePrice() {
        return unidentifiedAveragePrice;
    }

    public void setUnidentifiedAveragePrice(Double unidentifiedAveragePrice) {
        this.unidentifiedAveragePrice = unidentifiedAveragePrice;
    }

    @Override
    public String toString() {
        return "ItemPrice{" +
                "averagePrice=" + averagePrice +
                ", highestPrice=" + highestPrice +
                ", lowestPrice=" + lowestPrice +
                ", unidentifiedAveragePrice=" + unidentifiedAveragePrice +
                '}';
    }
}
