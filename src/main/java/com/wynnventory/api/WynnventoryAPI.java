package com.wynnventory.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.wynntils.core.components.Models;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.mc.McUtils;
import com.wynnventory.WynnventoryMod;
import com.wynnventory.model.item.Lootpool;
import com.wynnventory.model.item.TradeMarketItem;
import com.wynnventory.model.item.TradeMarketItemPriceInfo;
import com.wynnventory.util.HttpUtil;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WynnventoryAPI {
    private static final String BASE_URL = "https://www.wynnventory.com";
    private static final String API_IDENTIFIER = "api";
    private static final URI API_BASE_URL = createApiBaseUrl();
    private static final ObjectMapper objectMapper = createObjectMapper();

    public void sendTradeMarketResults(List<TradeMarketItem> marketItems) {
        if (marketItems.isEmpty()) return;

        URI endpointURI;
        if (WynnventoryMod.isDev()) {
            WynnventoryMod.info("Sending market data to DEV endpoint.");
            endpointURI = getEndpointURI("https://wynn-ventory-dev-2a243523ab77.herokuapp.com/api/trademarket/items?env=dev2");
        } else {
            endpointURI = getEndpointURI("trademarket/items");
        }
        HttpUtil.sendHttpPostRequest(endpointURI, serializeData(marketItems));
    }

    public void sendLootpoolData(List<Lootpool> lootpools) {
        if (lootpools.isEmpty()) return;

        URI endpointURI;
        if (WynnventoryMod.isDev()) {
            WynnventoryMod.info("Sending lootpool data to DEV endpoint.");
            endpointURI = URI.create("https://wynn-ventory-dev-2a243523ab77.herokuapp.com/api/lootpool/items?env=dev2");
        } else {
            endpointURI = getEndpointURI("lootpool/items");
        }

        for(Lootpool lootpool : lootpools) {
            HttpUtil.sendHttpPostRequest(endpointURI, serializeData(lootpool));
        }
    }

    public void sendRaidpoolData(List<Lootpool> lootpools) {
        if (lootpools.isEmpty()) return;

        URI endpointURI;
        if (WynnventoryMod.isDev()) {
            WynnventoryMod.info("Sending raidpool data to DEV endpoint.");
            endpointURI = URI.create("https://wynn-ventory-dev-2a243523ab77.herokuapp.com/api/raidpool/items?env=dev2");
        } else {
            endpointURI = getEndpointURI("raidpool/items");
        }

        for(Lootpool lootpool : lootpools) {
            HttpUtil.sendHttpPostRequest(endpointURI, serializeData(lootpool));
        }
    }

    public TradeMarketItemPriceInfo fetchItemPrices(ItemStack item) {
        return Models.Item.asWynnItem(item, GearItem.class)
                .map(gearItem -> fetchItemPrices(gearItem.getName()))
                .orElse(null);
    }

    public TradeMarketItemPriceInfo fetchItemPrices(String itemName) {
        String playerName = McUtils.playerName();
        try {
            final String encodedItemName = URLEncoder.encode(itemName, StandardCharsets.UTF_8).replace("+", "%20");

            URI endpointURI;
            if (WynnventoryMod.isDev()) {
                WynnventoryMod.info("Fetching market data from DEV endpoint.");
                endpointURI = getEndpointURI("https://wynn-ventory-dev-2a243523ab77.herokuapp.com/api/trademarket/item/" + encodedItemName + "/price?env=dev2&playername=" + playerName);
            } else {
                endpointURI = getEndpointURI("trademarket/item/" + encodedItemName + "/price?playername=" + playerName);
            }

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

    public List<Lootpool> getLootpools(String type) {
        try {
            URI endpointURI;
            if (WynnventoryMod.isDev()) {
                WynnventoryMod.info("Fetching " + type + " lootpools from DEV endpoint.");
                endpointURI = getEndpointURI("https://wynn-ventory-dev-2a243523ab77.herokuapp.com/api/lootpool/" + type + "/");
            } else {
                endpointURI = getEndpointURI("api/lootpool/" + type + "/");
            }

            WynnventoryMod.error("URL: " + endpointURI);

            HttpResponse<String> response = HttpUtil.sendHttpGetRequest(endpointURI);

            if (response.statusCode() == 200) {
                return parseLootpoolResponse(response.body());
            } else if (response.statusCode() == 404) {
                return null;
            } else {
                WynnventoryMod.error("Failed to fetch " + type + " lootpools: " + response.body());
                return null;
            }
        } catch (Exception e) {
            WynnventoryMod.error("Failed to initiate lootpool fetch {}", e);
            return null;
        }
    }

    private String serializeData(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            WynnventoryMod.LOGGER.error("Failed to serialize data", e);
            return "[]";
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

    private List<Lootpool> parseLootpoolResponse(String responseBody) {
        try {
            List<Lootpool> lootpools = objectMapper.readValue(responseBody, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            return lootpools.isEmpty() ? new ArrayList<>() : lootpools;
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