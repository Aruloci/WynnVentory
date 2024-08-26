package com.wynnventory.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.wynntils.core.components.Models;
import com.wynntils.mc.extension.ItemStackExtension;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.trademarket.type.TradeMarketPriceInfo;
import com.wynnventory.WynnventoryMod;
import com.wynnventory.model.item.TradeMarketItem;
import com.wynnventory.model.item.TradeMarketItemPriceInfo;
import com.wynnventory.util.HttpUtil;
import com.wynnventory.util.TradeMarketPriceParser;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WynnventoryAPI {
    private static final String BASE_URL = "https://www.wynnventory.com";
    private static final String API_IDENTIFIER = "api";
    private static final URI API_BASE_URL = createApiBaseUrl();
    private static final ObjectMapper objectMapper = createObjectMapper();

    public void sendTradeMarketResults(ItemStack item) {
        sendTradeMarketResults(List.of(item));
    }

    public void sendTradeMarketResults(List<ItemStack> items) {
        if (items.isEmpty()) return;

        List<TradeMarketItem> marketItems = createTradeMarketItems(items);

        if (marketItems.isEmpty()) return;

        URI endpointURI;
        if (WynnventoryMod.isDev()) {
            WynnventoryMod.info("Sending item data to DEV endpoint.");
            endpointURI = getEndpointURI("trademarket/items?env=dev2");
        } else {
            endpointURI = getEndpointURI("trademarket/items");
        }
        HttpUtil.sendHttpPostRequest(endpointURI, serializeMarketItems(marketItems));
    }

    public TradeMarketItemPriceInfo fetchItemPrices(ItemStack item) {
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(item);

        if(wynnItemOptional.isPresent()) {
            WynnItem wynnItem = wynnItemOptional.get();

            ItemStackExtension extension = wynnItem.getData().get(WynnItemData.ITEMSTACK_KEY);
            String name = extension.getOriginalName().getStringWithoutFormatting();

            return fetchItemPrices(name);
        }

        return null;
    }

    public TradeMarketItemPriceInfo fetchItemPrices(String itemName) {
        try {
            String encodedItemName = URLEncoder.encode(itemName, StandardCharsets.UTF_8).replace("+", "%20");
            URI endpointURI = getEndpointURI("trademarket/item/" + encodedItemName + "/price");

            HttpResponse<String> response = HttpUtil.sendHttpGetRequest(endpointURI);

            if (response.statusCode() == 200) {
                return parsePriceInfoResponse(response.body());
            } else if (response.statusCode() == 404) {
                return null;
            } else {
                WynnventoryMod.error("Failed to fetch item price from API: " + response.body());
                return null;
            }
        } catch (Exception e) {
            WynnventoryMod.error("Failed to initiate item price fetch {}", e);
            return null;
        }
    }

    private List<TradeMarketItem> createTradeMarketItems(List<ItemStack> items) {
        List<TradeMarketItem> marketItems = new ArrayList<>();

        for (ItemStack item : items) {
            Optional<GearItem> gearItemOptional = Models.Item.asWynnItem(item, GearItem.class);

            gearItemOptional.ifPresent(gearItem -> {
                TradeMarketPriceInfo priceInfo = TradeMarketPriceParser.calculateItemPriceInfo(item);
                if (priceInfo != TradeMarketPriceInfo.EMPTY) {
                    marketItems.add(new TradeMarketItem(gearItem, priceInfo.price(), priceInfo.amount()));
                }
            });
        }

        return marketItems;
    }

    private String serializeMarketItems(List<TradeMarketItem> marketItems) {
        try {
            return objectMapper.writeValueAsString(marketItems);
        } catch (JsonProcessingException e) {
            WynnventoryMod.LOGGER.error("Failed to serialize market items ({})", marketItems.getFirst().getItem().getName());
//            WynnventoryMod.LOGGER.error("Failed to serialize market items ({})", marketItems.getFirst().getItem().getName(), e);
            return "{}";
        }
    }

    private TradeMarketItemPriceInfo parsePriceInfoResponse(String responseBody) {
        try {
            List<TradeMarketItemPriceInfo> priceInfoList = objectMapper.readValue(responseBody, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            return priceInfoList.isEmpty() ? null : priceInfoList.getFirst();
        } catch (JsonProcessingException e) {
            WynnventoryMod.error("Failed to parse item price response {}", e);
            return null;
        }
    }

    private static URI createApiBaseUrl() {
        try {
            return new URI(BASE_URL).resolve("/" + API_IDENTIFIER + "/");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URL format", e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        return mapper;
    }

    private static URI getEndpointURI(String endpoint) {
        return API_BASE_URL.resolve(endpoint);
    }

}
