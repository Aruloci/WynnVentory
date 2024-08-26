package com.wynnventory.util;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.trademarket.type.TradeMarketPriceInfo;
import com.wynntils.utils.mc.LoreUtils;
import com.wynnventory.Wynnventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TradeMarketPriceParser {
    private static final Pattern PRICE_STR = Pattern.compile("§6Price:");
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "§[67] ?- (?:§f(?<amount>[\\d,]+) §7x )?§(?:(?:(?:c✖|a✔) §f)|f§m|f)(?<price>[\\d,]+)§7(?:§m)?²(?:§b ✮ (?<silverbullPrice>[\\d,]+)§3²)?(?: .+)?");

    public static TradeMarketPriceInfo calculateItemPriceInfo(ItemStack itemStack) {
        List<StyledText> loreLines = LoreUtils.getLore(itemStack);
        if (loreLines.size() < 2) return TradeMarketPriceInfo.EMPTY;
        StyledText priceLine = loreLines.get(1);
        if (priceLine != null && priceLine.matches(PRICE_STR)) {
            StyledText priceValueLine = loreLines.get(2);
            Matcher matcher = priceValueLine.getMatcher(PRICE_PATTERN);
            if (!matcher.matches()) {
                Wynnventory.warn("Trade Market item had an unexpected price value line: " + priceValueLine);
                return TradeMarketPriceInfo.EMPTY;
            } else {
                int price = Integer.parseInt(matcher.group("price").replace(",", ""));
                String silverbullPriceStr = matcher.group("silverbullPrice");
                int silverbullPrice = silverbullPriceStr == null ? price : Integer.parseInt(silverbullPriceStr.replace(",", ""));
                String amountStr = matcher.group("amount");
                int amount = amountStr == null ? 1 : Integer.parseInt(amountStr.replace(",", ""));
                return new TradeMarketPriceInfo(price, silverbullPrice, amount);
            }
        } else {
            Wynnventory.warn("Trade Market item had an unexpected price line: " + priceLine);
            return TradeMarketPriceInfo.EMPTY;
        }
    }
}
