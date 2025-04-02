package com.wynnventory.model.stat;

import com.wynntils.models.stats.StatCalculator;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.utils.type.RangedValue;

import java.util.Objects;

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

    public int getValue() {
        return statActualValue.value();
    }

    public String getActualRollPercentage() {
        if (possibleValues == null) return "NaN";
        float percent = StatCalculator.getPercentage(statActualValue, possibleValues);
        if (Float.isInfinite(percent) || Float.isNaN(percent)) {
            return "NaN";
        }
        return StatCalculator.getPercentage(statActualValue, possibleValues)+"%";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ActualStatWithPercentage other) {
            return Objects.equals(getStatName(), other.getStatName()) &&
                    Objects.equals(getActualRollPercentage(), other.getActualRollPercentage());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStatName(), getActualRollPercentage());
    }


    @Override
    public String toString() {
        return "statName=" + statActualValue.statType().getKey() + ", actualValue=" + statActualValue.value() + ", actualValuePercent=" + getActualRollPercentage() + ", minRange=" + possibleValues.range().low() + ", maxRange=" + possibleValues.range().high();
    }
}