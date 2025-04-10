package com.wynnventory.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.items.WynnItem;
import com.wynntils.screens.guides.aspect.GuideAspectItemStack;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynnventory.accessor.ItemQueueAccessor;
import com.wynnventory.config.ConfigManager;
import com.wynnventory.enums.Region;
import com.wynnventory.enums.RegionType;
import com.wynnventory.model.item.GroupedLootpool;
import com.wynnventory.model.item.LootpoolItem;
import com.wynnventory.ui.WynnventoryItemButton;
import com.wynnventory.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(AbstractContainerScreen.class)
public abstract class TooltipMixin {
    private static final String MARKET_TITLE = "󏿨";

    private final ConfigManager config = ConfigManager.getInstance();
    private final ItemQueueAccessor accessor = (ItemQueueAccessor) McUtils.mc().getConnection();

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("RETURN"))
    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen == null) return;

        // Get the hovered slot using an accessor (assumed available)
        Slot hoveredSlot = ((AbstractContainerScreenAccessor) this).getHoveredSlot();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;

        ItemStack itemStack = hoveredSlot.getItem();
        Optional<WynnItem> maybeWynnItem = Models.Item.getWynnItem(itemStack);

        // If in the market screen, submit the item for market processing
        if (MARKET_TITLE.equals(currentScreen.getTitle().getString())) {
            accessor.queueItemForSubmit(itemStack);
        }

        if (config.isShowTooltips() && maybeWynnItem.isPresent()) {
            List<Component> tooltipComponents = ItemStackUtils.getTooltips(itemStack);
            PriceTooltipHelper.renderPriceInfoTooltip(guiGraphics, mouseX, mouseY, itemStack, tooltipComponents, config.isAnchorTooltips());
        }

        Component rawName = Objects.requireNonNull(ItemStackUtils.getWynntilsOriginalName(itemStack)).getComponent();
        String displayName = StyledText.fromComponent(rawName).getStringWithoutFormatting();
        Region region = Region.getRegionByName(displayName);

        if (region != null && region.getRegionType() == RegionType.RAID) {
            LootpoolManager.getRaidPools().stream()
                    .filter(p -> p.getRegion().equalsIgnoreCase(region.getShortName()))
                    .findFirst()
                    .ifPresent(pool -> AspectTooltipHelper.renderAspectTooltip(guiGraphics, mouseX, mouseY, pool));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void renderRaidAspects(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!RaidDisplayQueue.shouldShowRaidAspects()) return;

        // Cache aspect stack references for rendering
        Map<String, GuideAspectItemStack> aspectStacks = Models.Aspect.getAllAspectInfos()
                .map(info -> new GuideAspectItemStack(info, 1))
                .collect(Collectors.toMap(stack -> stack.getAspectInfo().name(), Function.identity()));

        List<GroupedLootpool> raidPools = LootpoolManager.getRaidPools();
        List<WynnventoryItemButton<GuideAspectItemStack>> tooltipButtons = new ArrayList<>();

        int x = 20;
        int y = 75;
        int itemSize = 16;
        int spacing = 22;

        for (GroupedLootpool pool : raidPools) {
            Region region = Region.getRegionByShortName(pool.getRegion());
            if (region == null) continue;

            // Draw section header
            Component title = Component.literal(region.getShortName() + " Mythic Aspects")
                    .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
            guiGraphics.drawString(McUtils.mc().font, title, x, y, 0xFFFFFF);

            final int[] buttonX = {x};
            int buttonY = y + 12;

            // Render each mythic aspect as a button
            pool.getGroupItems().stream()
                    .flatMap(group -> group.getLootItems().stream())
                    .filter(item -> "Mythic".equalsIgnoreCase(item.getRarity()) &&
                            "AspectItem".equalsIgnoreCase(item.getItemType()))
                    .sorted(Comparator.comparing(LootpoolItem::getName))
                    .forEach(lootItem -> {
                        GuideAspectItemStack stack = aspectStacks.get(lootItem.getName());
                        if (stack == null) return;

                        WynnventoryItemButton<GuideAspectItemStack> button =
                                new WynnventoryItemButton<>(buttonX[0], buttonY, itemSize, itemSize, stack, false);
                        button.setPosition(buttonX[0], buttonY);
                        button.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                        tooltipButtons.add(button);

                        buttonX[0] += spacing;
                    });

            y += 40;
        }

        // Render tooltip overlays
        for (WynnventoryItemButton<GuideAspectItemStack> button : tooltipButtons) {
            if (isMouseOver(button, mouseX, mouseY)) {
                guiGraphics.renderTooltip(
                        FontRenderer.getInstance().getFont(),
                        button.getItemStack(),
                        mouseX,
                        mouseY
                );

                PriceTooltipHelper.renderPriceInfoTooltip(
                        guiGraphics,
                        mouseX,
                        mouseY,
                        button.getItemStack(),
                        ItemStackUtils.getTooltips(button.getItemStack()),
                        false
                );
            }
        }
    }

    @Unique
    private boolean isMouseOver(WynnventoryItemButton<?> button, int mouseX, int mouseY) {
        return mouseX >= button.getX() && mouseX <= button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY <= button.getY() + button.getHeight();
    }
}