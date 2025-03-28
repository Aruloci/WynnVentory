package com.wynnventory.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearRestrictions;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearBoxItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.mc.McUtils;
import com.wynnventory.WynnventoryMod;
import com.wynnventory.accessor.ItemQueueAccessor;
import com.wynnventory.api.WynnventoryAPI;
import com.wynnventory.config.ConfigManager;
import com.wynnventory.model.item.TradeMarketItem;
import com.wynnventory.model.item.TradeMarketItemPriceHolder;
import com.wynnventory.model.item.TradeMarketItemPriceInfo;
import com.wynnventory.util.EmeraldPrice;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(AbstractContainerScreen.class)
public abstract class TooltipMixin {

    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type);
    private static final String MARKET_TITLE = "󏿨";
    private static final String TITLE_TEXT = "Trade Market Price Info";
    private static final long EXPIRE_MINS = 2;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final EmeraldPrice EMERALD_PRICE = new EmeraldPrice();
    private static final WynnventoryAPI API = new WynnventoryAPI();

    private static final TradeMarketItemPriceInfo FETCHING = new TradeMarketItemPriceInfo();
    private static final TradeMarketItemPriceInfo UNTRADABLE = new TradeMarketItemPriceInfo();
    private static HashMap<String, TradeMarketItemPriceHolder> fetchedPrices = new HashMap<>();

    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private ConfigManager config = ConfigManager.getInstance();
    private ItemQueueAccessor accessor = (ItemQueueAccessor) McUtils.mc().getConnection();


    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("RETURN"))
    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        Screen currentScreen = Minecraft.getInstance().screen;

        if(currentScreen == null) {
            return;
        }

        Slot hoveredSlot = ((AbstractContainerScreenAccessor) this).getHoveredSlot();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;

        ItemStack item = hoveredSlot.getItem();
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(item);

        String screenTitle = currentScreen.getTitle().getString();
        if (screenTitle.equals(MARKET_TITLE)) {
            submitTrademarketItem(item);
        }

        if(config.isShowTooltips() && wynnItemOptional.isPresent()) {
            WynnItem wynnItem = wynnItemOptional.get();

            List<Component> tooltips = new ArrayList<>();
            if(wynnItem instanceof GearItem gearItem) {
                tooltips.add(Component.literal(TITLE_TEXT).withStyle(ChatFormatting.GOLD));

                fetchPricesForGear(gearItem.getItemInfo());

                tooltips.addAll(getTooltipsForGear(gearItem.getItemInfo()));

                // remove price if expired
                if (fetchedPrices.get(gearItem.getName()).isPriceExpired(EXPIRE_MINS)) fetchedPrices.remove(gearItem.getName());
            } else if(wynnItem instanceof GearBoxItem gearBoxItem && config.isShowBoxedItemTooltips()) {
                tooltips.add(Component.literal(TITLE_TEXT).withStyle(ChatFormatting.GOLD));

                List<GearInfo> possibleGear = Models.Gear.getPossibleGears(gearBoxItem);
                for(GearInfo gear : possibleGear) {
                    fetchPricesForGear(gear);

                    tooltips.addAll(getTooltipsForGear(gear));
                    tooltips.add(Component.literal(""));

                    // remove price if expired
                    if (fetchedPrices.get(gear.name()).isPriceExpired(EXPIRE_MINS)) fetchedPrices.remove(gear.name());
                }
            }

            renderPriceInfoTooltip(guiGraphics, mouseX, mouseY, item, tooltips);
        }
    }

    private void submitTrademarketItem(ItemStack item) {
        if (item.getItem() == Items.AIR || item.getItem() == Items.COMPASS || item.getItem() == Items.POTION) return;

        TradeMarketItem marketItem = TradeMarketItem.createTradeMarketItem(item);

        if(marketItem != null) {
            accessor.getQueuedMarketItems().add(marketItem);
            WynnventoryMod.info("Submitted item: " + marketItem.getItem().getName());
        }
    }

    @Unique
    private void renderPriceInfoTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, ItemStack item, List<Component> tooltipLines) {
        Font font = McUtils.mc().font;
        Window window = McUtils.window();

        // Adjust mouseX and mouseY within the screen bounds
        mouseX = Math.min(mouseX, guiGraphics.guiWidth() - 10);
        mouseY = Math.max(mouseY, 10);

        int guiScaledWidth = window.getGuiScaledWidth();
        int guiScaledHeight = window.getGuiScaledHeight();
        int guiHeight = window.getHeight();
        int screenHeight = window.getScreenHeight();
        int guiScale = (int) window.getGuiScale();
        int gap = 5 * guiScale;

        // Dimension and bounds for tooltip
        Dimension priceTooltipDimension = calculateTooltipDimension(tooltipLines);
        int priceTooltipMaxWidth = mouseX - gap;
        int priceTooltipMaxHeight = Math.round(window.getGuiScaledHeight() * 0.8f);
        float scaleFactor = calculateScaleFactor(tooltipLines, priceTooltipMaxHeight, priceTooltipMaxWidth, 0.4f, 1.0f);
        priceTooltipDimension = new Dimension(Math.round(priceTooltipDimension.width * scaleFactor), Math.round(priceTooltipDimension.height * scaleFactor));

        Dimension primaryTooltipDimension = calculateTooltipDimension(Screen.getTooltipFromItem(McUtils.mc(), item));

        int spaceToRight = guiScaledWidth - (mouseX + primaryTooltipDimension.width + gap);
        int spaceToLeft = mouseX - gap;

        float minY = (priceTooltipDimension.height / 4f) / scaleFactor;
        float maxY = (guiScaledHeight / 2f) / scaleFactor;
        float scaledTooltipY = ((guiScaledHeight / 2f) - (priceTooltipDimension.height / 2f)) / scaleFactor;

        float posX = 0;
        float posY = 0;
        if(config.isAnchorTooltips()) {
            if(spaceToRight > spaceToLeft * 1.3f) {
                posX = guiScaledWidth - (float) priceTooltipDimension.width - (gap / scaleFactor);
            }

            posY = Math.clamp(scaledTooltipY, minY, maxY);
            //WynnventoryMod.debug("Scaled Screenbounds: MinY: " + minY + " | MaxY: " + maxY + " | TTPosY: " + posY + " | TTHeight: " + priceTooltipDimension.height + " | ScaleFactor: " + scaleFactor);
        } else {
            if (priceTooltipDimension.width > spaceToRight) {
                posX = mouseX - gap - (float) priceTooltipDimension.width; // Position tooltip on the left
            } else {
                posX = mouseX + gap + (float) primaryTooltipDimension.width; // Position tooltip on the right
            }

            if(mouseY + priceTooltipDimension.height > guiScaledHeight) {
                posY = Math.clamp(scaledTooltipY, minY, maxY);
            } else {
                posY = mouseY;
            }
        }

        // Apply scaling to the PoseStack
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(posX, posY, 0);
        poseStack.scale(scaleFactor, scaleFactor, 1.0f);

        guiGraphics.renderComponentTooltip(font, tooltipLines, 0, 0);
        poseStack.popPose();
    }

    private Dimension calculateTooltipDimension(List<Component> tooltipLines) {
        Font font = McUtils.mc().font;

        int tooltipHeight = tooltipLines.size() * font.lineHeight;

        int priceTooltipWidth = tooltipLines.stream()
                .map(font::width)
                .max(Integer::compareTo)
                .orElse(0);

        return new Dimension(priceTooltipWidth, tooltipHeight);
    }

    private float calculateScaleFactor(List<Component> tooltipLines, int maxHeight, int maxWidth, float minScaleFactor, float maxScaleFactor) {
        Dimension tooltipDimension = calculateTooltipDimension(tooltipLines);

        float heightScaleFactor = maxHeight / (float) tooltipDimension.height;
        float scaleFactor = Math.clamp(heightScaleFactor, minScaleFactor, maxScaleFactor);

        tooltipDimension.width = Math.round(tooltipDimension.width * scaleFactor);
        if(tooltipDimension.width > maxWidth) {
            float widthScaleFactor = (float) maxWidth / tooltipDimension.width;

            if(widthScaleFactor < scaleFactor) {
                scaleFactor = Math.clamp(widthScaleFactor, minScaleFactor, maxScaleFactor);
            }
        }

        return scaleFactor;
    }

    @Unique
    private List<Component> createPriceTooltip(GearInfo info, TradeMarketItemPriceInfo priceInfo) {
        final ConfigManager config = ConfigManager.getInstance();
        final List<Component> tooltipLines = new ArrayList<>();

        tooltipLines.add(formatText(info.name(), info.tier().getChatFormatting()));

        if (priceInfo == null) {
            tooltipLines.add(formatText("No price data available yet!", ChatFormatting.RED));
        } else {
            if (config.isShowMaxPrice() && priceInfo.getHighestPrice() > 0) {
                tooltipLines.add(formatPrice("Max: ", priceInfo.getHighestPrice()));
            }
            if (config.isShowMinPrice() && priceInfo.getLowestPrice() > 0) {
                tooltipLines.add(formatPrice("Min: ", priceInfo.getLowestPrice()));
            }
            if (config.isShowAveragePrice() && priceInfo.getAveragePrice() != null) {
                tooltipLines.add(formatPrice("Avg: ", priceInfo.getAveragePrice().intValue()));
            }

            if (config.isShowAverage80Price() && priceInfo.getAverage80Price() != null) {
                tooltipLines.add(formatPrice("Avg 80%: ", priceInfo.getAverage80Price().intValue()));
            }

            if (config.isShowUnidAveragePrice() && priceInfo.getUnidentifiedAveragePrice() != null) {
                tooltipLines.add(formatPrice("Unidentified Avg: ", priceInfo.getUnidentifiedAveragePrice().intValue()));
            }

            if (config.isShowUnidAverage80Price() && priceInfo.getUnidentifiedAverage80Price() != null) {
                tooltipLines.add(formatPrice("Unidentified Avg 80%: ", priceInfo.getUnidentifiedAverage80Price().intValue()));
            }
        }
        return tooltipLines;
    }

    private void fetchPricesForGear(GearInfo info) {
        if (!fetchedPrices.containsKey(info.name())) {
            TradeMarketItemPriceHolder requestedPrice = new TradeMarketItemPriceHolder(FETCHING, info);
            fetchedPrices.put(info.name(), requestedPrice);

            if (info.metaInfo().restrictions() == GearRestrictions.UNTRADABLE) {
                // ignore untradable
                requestedPrice.setPriceInfo(UNTRADABLE);
            } else {
                // fetch price async
                CompletableFuture.supplyAsync(() -> API.fetchItemPrices(info.name()), executorService)
                        .thenAccept(requestedPrice::setPriceInfo);
            }
        }
    }

    private List<Component> getTooltipsForGear(GearInfo info) {
        TradeMarketItemPriceInfo price = fetchedPrices.get(info.name()).getPriceInfo();
        List<Component> tooltips = new ArrayList<>();

        if (price == FETCHING) { // Display retrieving info
            tooltips.add(formatText("Retrieving price information...", ChatFormatting.WHITE));
        } else if (price == UNTRADABLE) { // Display untradable
            tooltips.add(formatText("Item is untradable.", ChatFormatting.RED));
        } else { // Display fetched price
            tooltips = createPriceTooltip(info, price);
        }

        return tooltips;
    }

    @Unique
    private static MutableComponent formatPrice(String label, int price) {
        if (price > 0) {
            String formattedPrice = NUMBER_FORMAT.format(price) + EmeraldUnits.EMERALD.getSymbol();
            String formattedEmeralds = EMERALD_PRICE.getFormattedString(price, false);
            return Component.literal(label + formattedPrice)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + formattedEmeralds + ")")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
        }
        return null;
    }

    @Unique
    private static MutableComponent formatText(String text, ChatFormatting color) {
            return Component.literal(text)
                    .withStyle(Style.EMPTY.withColor(color));
    }
}
