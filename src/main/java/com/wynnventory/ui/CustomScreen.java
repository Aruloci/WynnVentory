package com.wynnventory.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.guides.WynntilsGuideScreen;
import com.wynntils.screens.guides.gear.GuideGearItemStack;
import com.wynntils.services.itemfilter.type.ItemProviderType;
import com.wynntils.services.itemfilter.type.ItemSearchQuery;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynnventory.WynnventoryMod;
import com.wynnventory.api.WynnventoryAPI;
import com.wynnventory.model.item.Lootpool;
import com.wynnventory.model.item.LootpoolItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CustomScreen extends WynntilsGuideScreen<GuideGearItemStack, WynnventoryButton> {
    private static final int ELEMENTS_COLUMNS = 7;
    private static final int ELEMENT_ROWS = 7;

    protected List<GuideGearItemStack> allGearItems = List.of();

    private List<Lootpool> raidpools;
    private List<Lootpool> lootrunpools;

    protected Renderable hovered = null;

    public CustomScreen(Component title) {
        super(title, List.of(ItemProviderType.GENERIC, ItemProviderType.GEAR));

        WynnventoryAPI api = new WynnventoryAPI();
        raidpools = api.getLootpools("raid");
        lootrunpools = api.getLootpools("lootrun");

        getAllGearItems();
    }

    public static Screen create() {
        return new CustomScreen(Component.literal("Lootpools"));
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();
        poseStack.translate(translationX, translationY, 1f);

        renderTitle(poseStack, "Lootruns");

        renderDescription(
                poseStack,
                "See all the lootpools here",
                "Simply amazing");

        renderVersion(poseStack);

        renderItemsHeader(poseStack);

        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);

        renderPageInfo(poseStack, currentPage + 1, maxPage + 1);

        poseStack.popPose();

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hovered instanceof WynnventoryButton wynnventoryButton) {
            guiGraphics.renderTooltip(
                    FontRenderer.getInstance().getFont(), wynnventoryButton.getItemStack(), mouseX, mouseY);
        }

        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderItemsHeader(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.wynntilsGuides.itemGuide.available")),
                        Texture.CONTENT_BOOK_BACKGROUND.width() * 0.75f,
                        30,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.TOP,
                        TextShadow.NONE);
    }

    protected WynnventoryButton getButtonFromElement(int i) {
        int xOffset = (i % ELEMENTS_COLUMNS) * 20;
        int yOffset = ((i % getElementsPerPage()) / ELEMENTS_COLUMNS) * 20;

        return new WynnventoryButton(
                xOffset + Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 13,
                yOffset + 43,
                18,
                18,
                elements.get(i),
                this);
    }

    protected void reloadElementsList(ItemSearchQuery searchQuery) {
        List<GuideGearItemStack> itemStacks = new ArrayList<>();
        List<LootpoolItem> items = new ArrayList<>(lootrunpools.get(0).getItems());
        for(LootpoolItem item : items) {
            for (GuideGearItemStack stack : allGearItems) {
                if (stack.getGearInfo().name().equals(item.getName())) {
                    itemStacks.add(stack);
                }
            }
        }

        elements.addAll(Services.ItemFilter.filterAndSort(searchQuery, itemStacks));
    }

    private List<GuideGearItemStack> getAllGearItems() {
        if (allGearItems.isEmpty()) {
            allGearItems = Models.Gear.getAllGearInfos().map(GuideGearItemStack::new).toList();
        }

        return allGearItems;
    }

    @Override
    protected int getElementsPerPage() {
        return ELEMENT_ROWS * ELEMENTS_COLUMNS;
    }

    @Override
    protected void renderVersion(PoseStack poseStack) {
        super.renderVersion(poseStack);

        poseStack.pushPose();
        String version = WynnventoryMod.isDev() ? "Development Build" : WynnventoryMod.WYNNVENTORY_VERSION;
        poseStack.scale(0.7f, 0.7f, 0);
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(version),
                        59f * 1.3f,
                        (Texture.CONTENT_BOOK_BACKGROUND.width() / 2f - 30f) * 1.3f,
                        Texture.CONTENT_BOOK_BACKGROUND.height() * 1.3f - 6f,
                        0,
                        CommonColors.YELLOW,
                        HorizontalAlignment.CENTER,
                        TextShadow.NORMAL);
        poseStack.popPose();
    }
}
