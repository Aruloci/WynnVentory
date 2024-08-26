package com.wynnventory.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.mc.extension.ItemStackExtension;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.gear.type.GearRestrictions;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.MiscItem;
import com.wynntils.utils.mc.McUtils;
import com.wynnventory.WynnventoryMod;
import com.wynnventory.api.WynnventoryAPI;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(AbstractContainerScreen.class)
public class TooltipMixin {

    private static final String TITLE_TEXT = "Trade Market Price Info";
    private static final long EXPIRE_MINS = 1;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final EmeraldPrice EMERALD_PRICE = new EmeraldPrice();
    private static final WynnventoryAPI API = new WynnventoryAPI();

    private static final TradeMarketItemPriceInfo FETCHING = new TradeMarketItemPriceInfo();
    private static final TradeMarketItemPriceInfo UNTRADABLE = new TradeMarketItemPriceInfo();
    private static HashMap<String, TradeMarketItemPriceHolder> fetchedPrices = new HashMap<>();

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("RETURN"))
    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
//        if(!Screen.hasAltDown()) { return; } @TODO: Move to Mod Config
        Slot hoveredSlot = ((AbstractContainerScreenAccessor) this).getHoveredSlot();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;

        ItemStack item = hoveredSlot.getItem();
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(item);

        if(wynnItemOptional.isEmpty()) { return; }
        WynnItem wynnItem = wynnItemOptional.get();

        // Exclude base WynnItems and the EmeraldPouch from sending sending fetch requests
        if(wynnItem.getClass() == WynnItem.class || wynnItem.getClass() == EmeraldPouchItem.class) {
            return;
        }

        ItemStackExtension extension = wynnItem.getData().get(WynnItemData.ITEMSTACK_KEY);
        String name = extension.getOriginalName().getStringWithoutFormatting();

        if (!fetchedPrices.containsKey(name)) {
            TradeMarketItemPriceHolder requestedPrice = new TradeMarketItemPriceHolder(FETCHING, wynnItem);
            fetchedPrices.put(name, requestedPrice);

            // Check for untradable items
            if(wynnItem instanceof GearItem) {
                Optional<GearItem> gearItemOptional = Models.Item.asWynnItem(item, GearItem.class);

                if(gearItemOptional.isPresent()) {
                    if (gearItemOptional.get().getItemInfo().metaInfo().restrictions() == GearRestrictions.UNTRADABLE) {
                        requestedPrice.setPriceInfo(UNTRADABLE);
                    } else {
                        // fetch price async
                        CompletableFuture.supplyAsync(() -> API.fetchItemPrices(item), executorService)
                                .thenAccept(requestedPrice::setPriceInfo);
                    }
                }
            } else if (wynnItem instanceof MiscItem) {
                Optional<MiscItem> miscItemOptional = Models.Item.asWynnItem(item, MiscItem.class);

                if(miscItemOptional.isPresent() && miscItemOptional.get().isUntradable()) {
                    requestedPrice.setPriceInfo(UNTRADABLE);
                } else {
                    // fetch price async
                    CompletableFuture.supplyAsync(() -> API.fetchItemPrices(item), executorService)
                            .thenAccept(requestedPrice::setPriceInfo);
                }
            }
        }

        TradeMarketItemPriceInfo price = fetchedPrices.get(name).getPriceInfo();
        List<Component> tooltips = new ArrayList<>();
        if (price == FETCHING) { // Display retrieving info
            tooltips.add(formatText(TITLE_TEXT, ChatFormatting.GOLD));
            tooltips.add(formatText("Retrieving price information...", ChatFormatting.WHITE));
        } else if (price == UNTRADABLE) { // Display untradable
            tooltips.add(formatText(TITLE_TEXT, ChatFormatting.GOLD));
            tooltips.add(formatText("Item is untradable.", ChatFormatting.RED));
        } else { // Display fetched price
            tooltips = createPriceTooltip(price);
        }
        renderPriceInfoTooltip(guiGraphics, mouseX, mouseY, item, tooltips);

        // remove price if expired
        if (fetchedPrices.get(name).isPriceExpired(EXPIRE_MINS)) fetchedPrices.remove(name);
    }

    @Unique
    private void renderPriceInfoTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, ItemStack item, List<Component> tooltipLines) {
        mouseX = Math.min(mouseX, guiGraphics.guiWidth() - 10);
        mouseY = Math.max(mouseY, 10);
        int guiScaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int guiScaleFactor = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int gap = 5 * guiScaleFactor;

        final PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(0, 0, 300);

        List<Component> primaryTooltip = Screen.getTooltipFromItem(McUtils.mc(), item);
        int primaryTooltipWidth = primaryTooltip.stream()
                .map(component -> McUtils.mc().font.width(component))
                .max(Integer::compareTo)
                .orElse(0);

        int priceTooltipWidth = tooltipLines.stream()
                .map(component -> McUtils.mc().font.width(component))
                .max(Integer::compareTo)
                .orElse(0);
        priceTooltipWidth+=gap;

        int spaceToRight = guiScaledWidth - (mouseX + primaryTooltipWidth + gap);

        Font font = Minecraft.getInstance().font;
        try {
            if (priceTooltipWidth > spaceToRight) {
                // Render on left
                guiGraphics.renderComponentTooltip(
                        font, tooltipLines, mouseX - priceTooltipWidth - gap, mouseY);
            } else {
                // Render on right
                guiGraphics.renderComponentTooltip(
                        font, tooltipLines, mouseX + primaryTooltipWidth + gap, mouseY);
            }
        } catch (Exception e) {
            WynnventoryMod.error("Failed to render price tooltip for " + item.getDisplayName());
        }
        poseStack.popPose();
    }

    @Unique
    private List<Component> createPriceTooltip(TradeMarketItemPriceInfo priceInfo) {
        List<Component> tooltipLines = new ArrayList<>();
        tooltipLines.add(Component.literal(TITLE_TEXT).withStyle(ChatFormatting.GOLD));

        if (priceInfo == null) {
            tooltipLines.add(formatText("No price data available yet!", ChatFormatting.RED));
        } else {
            if (priceInfo.getHighestPrice() > 0) {
                tooltipLines.add(formatPrice("Max: ", priceInfo.getHighestPrice()));
            }
            if (priceInfo.getLowestPrice() > 0) {
                tooltipLines.add(formatPrice("Min: ", priceInfo.getLowestPrice()));
            }
            if (priceInfo.getAveragePrice() > 0.0) {
                tooltipLines.add(formatPrice("Avg: ", priceInfo.getAveragePrice()));
            }
            if (priceInfo.getUnidentifiedAveragePrice() != null) {
                tooltipLines.add(formatPrice("Unidentified Avg: ", priceInfo.getUnidentifiedAveragePrice().intValue()));
            }
        }
        return tooltipLines;
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
