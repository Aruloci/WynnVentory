package com.wynnventory.model.item;

import com.wynntils.core.components.Models;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.WynnItemData;
import com.wynntils.models.items.items.game.*;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.models.rewards.type.TomeInfo;
import com.wynntils.utils.mc.McUtils;
import com.wynnventory.WynnventoryMod;
import com.wynnventory.util.ItemStackUtils;
import com.wynnventory.util.RegionDetector;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class LootpoolItem {
    private String itemType;
    private int amount;
    private String name;
    private String rarity;
    private boolean shiny;
    private String type;
    public static final List<Class<? extends WynnItem>> LOOT_CLASSES = Arrays.asList(
            GearItem.class,
            InsulatorItem.class,
            SimulatorItem.class,
            EmeraldItem.class,
            MiscItem.class,
            RuneItem.class,
            DungeonKeyItem.class,
            AspectItem.class,
            AmplifierItem.class,
            PowderItem.class,
            GearBoxItem.class
    );

    public LootpoolItem() { }

    public LootpoolItem(String itemType, int amount, String name, String rarity, boolean shiny, String type) {
        this.itemType = itemType;
        this.amount = amount;
        this.name = name;
        this.rarity = rarity;
        this.shiny = shiny;
        this.type = type;
    }

    public LootpoolItem(WynnItem wynnItem) {
        this.itemType = wynnItem.getClass().getSimpleName();
        this.name = Objects.requireNonNull(ItemStackUtils.getWynntilsOriginalName(wynnItem.getData().get(WynnItemData.ITEMSTACK_KEY))).getLastPart().getComponent().getString();
        this.amount = ((ItemStack) wynnItem.getData().get(WynnItemData.ITEMSTACK_KEY)).getCount();
        name = name.replace("Unidentified ", "");
        type = wynnItem.getClass().getSimpleName().replace("Item", "");
        rarity = "Common";

        if (wynnItem instanceof GearItem gearItem) {
            shiny = name.contains("Shiny");
            name = gearItem.getName();
            rarity = gearItem.getGearTier().getName();
            type = gearItem.getGearType().name();
        }

        else if (wynnItem instanceof SimulatorItem || wynnItem instanceof InsulatorItem) {
            rarity = ((GearTierItemProperty) wynnItem).getGearTier().getName();
        }

/*        else if(wynnItem instanceof TomeItem tomeItem) {
            name = tomeItem.getName();
            rarity = tomeItem.getGearTier().getName();
            type = tomeItem.getItemInfo().type().name();
        }*/

        else if(wynnItem instanceof AspectItem aspectItem) {
            rarity = aspectItem.getGearTier().getName();

            String classReq = aspectItem.getClassType().getName();
            if(classReq != null && !classReq.isEmpty()) {
                type = classReq + type;
            }
        }

        else if (wynnItem instanceof EmeraldItem emeraldItem) {
            type = emeraldItem.getUnit().name();
        }

        else if (wynnItem instanceof RuneItem runeItem) {
            type = runeItem.getType().name();
        }

        else if (wynnItem instanceof PowderItem powderItem) {
            name = powderItem.getName().replaceAll("[✹✦❉❋✤]", "").trim();
            type = powderItem.getPowderProfile().element().getName() + type;
        }

        else if(wynnItem instanceof AmplifierItem amplifierItem) {
            rarity = amplifierItem.getGearTier().getName();
            String[] nameParts = name.split(" ");

            if(nameParts.length > 1) {
                type = nameParts[0] + nameParts[1];
            }
        }

        else if(wynnItem instanceof MiscItem && name.contains("Tome")) {
            TomeInfo info = Models.Rewards.getTomeInfoFromDisplayName(name);
            if(info != null) {
                type = info.type().name();
                rarity = info.tier().getName();
            }
        }
    }

    public static List<LootpoolItem> createLootpoolItemsFromWynnItem(List<WynnItem> wynnItems) {
        List<LootpoolItem> lootpoolItems = new ArrayList<>();

        for(WynnItem wynnItem : wynnItems) {
            lootpoolItems.addAll(createLootpoolItemFromWynnItem(wynnItem));
        }

        return lootpoolItems;
    }

    public static List<LootpoolItem> createLootpoolItemsFromItemStack(List<ItemStack> items) {
        List<WynnItem> wynnItems = new ArrayList<>();
        items.forEach(item -> Models.Item.getWynnItem(item).ifPresent(wynnItems::add));

        return createLootpoolItemsFromWynnItem(wynnItems);
    }

    public static List<LootpoolItem> createLootpoolItemFromWynnItem(WynnItem wynnItem) {
        List<LootpoolItem> lootpoolItems = new ArrayList<>();

        if(wynnItem instanceof GearBoxItem gearBoxItem) {
            List<GearInfo> possibleGear = Models.Gear.getPossibleGears(gearBoxItem);

            String name, rarity, type;
            for(GearInfo gearInfo : possibleGear) {
                if(gearInfo.requirements().quest().isPresent()) {
                    continue;
                }

                name = gearInfo.name();
                rarity = gearInfo.tier().name();
                type = gearInfo.type().name();

                lootpoolItems.add(new LootpoolItem("GearItem", 1, name, rarity, false, type));
            }

            return lootpoolItems;
        }

        if (LootpoolItem.LOOT_CLASSES.contains(wynnItem.getClass())) {
            lootpoolItems.add(new LootpoolItem(wynnItem));
        } else {
            WynnventoryMod.error("Unknown class: " + wynnItem.getClass());
        }


        return lootpoolItems;
    }

    public static List<LootpoolItem> createLootpoolItemFromItemStack(ItemStack item) {
        Optional<WynnItem> wynnItem = Models.Item.getWynnItem(item);
        return wynnItem.map(LootpoolItem::createLootpoolItemFromWynnItem).orElseGet(ArrayList::new);

    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public void setShiny(boolean shiny) {
        this.shiny = shiny;
    }

    public boolean isShiny() {
        return shiny;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        // If the object is compared with itself, return true
        if (this == o) return true;

        // Check if o is an instance of LootpoolItem or return false
        if (o == null || getClass() != o.getClass()) return false;

        // Typecast o to LootpoolItem to compare the attributes
        LootpoolItem that = (LootpoolItem) o;

        // Compare each field of the class
        return amount == that.amount &&
                Objects.equals(itemType, that.itemType) &&
                Objects.equals(name, that.name) &&
                Objects.equals(rarity, that.rarity) &&
                Objects.equals(shiny, that.shiny) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemType, amount, name, rarity, shiny, type);
    }
}