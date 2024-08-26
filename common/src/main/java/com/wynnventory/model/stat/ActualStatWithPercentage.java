package com.wynnventory.model.stat;

import com.wynntils.models.stats.StatCalculator;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.utils.type.RangedValue;

public class ActualStatWithPercentage {
    private StatActualValue statActualValue;
    private StatPossibleValues possibleValues;

    public ActualStatWithPercentage(StatActualValue statActualValue, StatPossibleValues possibleValues) {
        this.statActualValue = statActualValue;
        this.possibleValues = possibleValues;
    }

    public String getStatName() {
        return statActualValue.statType().getKey();
    }

    public RangedValue getRange() {
        return possibleValues.range();
    }

    public int getValue() {
        return statActualValue.value();
    }

    public float getActualRollPercentage() {
        return StatCalculator.getPercentage(statActualValue, possibleValues);
    }

    @Override
    public String toString() {
        return "statName=" + statActualValue.statType().getKey() + ", actualValue=" + statActualValue.value() + ", actualValuePercent=" + getActualRollPercentage() + ", minRange=" + possibleValues.range().low() + ", maxRange=" + possibleValues.range().high();
    }
}