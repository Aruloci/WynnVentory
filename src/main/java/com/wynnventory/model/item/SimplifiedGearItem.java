package com.wynnventory.model.item;

import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.stats.type.ShinyStat;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynnventory.model.stat.ActualStatWithPercentage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SimplifiedGearItem {
    private String name;
    private int level;
    private String rarity;
    private int powderSlots;
    private int rerollCount;
    private float overallPercentage;
    private boolean unidentified;
    private Optional<ShinyStat> shinyStat;
    private List<ActualStatWithPercentage> actualStatsWithPercentage = new ArrayList<>();

    public SimplifiedGearItem(GearItem item) {
        this.name = item.getName();
        this.level = item.getLevel();
        this.rarity = item.getGearTier().getName();
        this.powderSlots = item.getPowderSlots();
        this.rerollCount = item.getRerollCount();
        this.overallPercentage = item.getOverallPercentage();
        this.unidentified = item.isUnidentified();
        this.shinyStat = item.getShinyStat();

        final List<StatActualValue> actualValues = item.getIdentifications();
        final List<StatPossibleValues> possibleValues = item.getPossibleValues();

        for(StatActualValue actual : actualValues) {
            StatPossibleValues possibleValue = possibleValues.stream().filter(p -> p.statType().getKey().equals(actual.statType().getKey())).findFirst().orElse(null);
            actualStatsWithPercentage.add(new ActualStatWithPercentage(actual, possibleValue));
        }
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public String getRarity() {
        return rarity;
    }

    public int getPowderSlots() {
        return powderSlots;
    }

    public int getRerollCount() {
        return rerollCount;
    }

    public float getOverallPercentage() {
        return overallPercentage;
    }

    public boolean isUnidentified() {
        return unidentified;
    }

    public Optional<ShinyStat> getShinyStat() {
        return shinyStat;
    }

    public List<ActualStatWithPercentage> getActualStatsWithPercentage() {
        return actualStatsWithPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof SimplifiedGearItem other) {
            return level == other.level &&
                    powderSlots == other.powderSlots &&
                    rerollCount == other.rerollCount &&
                    Float.compare(other.overallPercentage, overallPercentage) == 0 &&
                    unidentified == other.unidentified &&
                    Objects.equals(name, other.name) &&
                    Objects.equals(rarity, other.rarity) &&
                    Objects.equals(shinyStat, other.shinyStat) &&
                    Objects.equals(actualStatsWithPercentage, other.actualStatsWithPercentage);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, level, rarity, powderSlots, rerollCount,
                overallPercentage, unidentified, shinyStat,
                actualStatsWithPercentage);
    }

}

