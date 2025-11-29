package com.darksoldier1404.dprc.obj;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.data.DataCargo;
import com.darksoldier1404.dprc.enums.RandomType;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.darksoldier1404.dprc.RewardChest.plugin;

public class Chest implements DataCargo {
    private String name;
    private RandomType randomType = RandomType.SIMPLE;
    private Location location;
    private Location offset;
    private DInventory inventory;
    private ItemStack keyItem;
    private final List<ChestWeight> weightList = new ArrayList<>();

    public Chest() {
    }

    public Chest(String name, RandomType randomType, Location location, Location offset, DInventory inventory, ItemStack keyItem) {
        this.name = name;
        this.randomType = randomType;
        this.location = location;
        this.offset = offset;
        this.inventory = inventory;
        this.inventory.applyDefaultPageTools();
        this.keyItem = keyItem;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RandomType getRandomType() {
        return randomType;
    }

    public void setRandomType(RandomType randomType) {
        this.randomType = randomType;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getOffset() {
        return offset;
    }

    public void setOffset(Location offset) {
        this.offset = offset;
    }

    public DInventory getInventory() {
        return inventory;
    }

    public void setInventory(DInventory inventory) {
        this.inventory = inventory;
    }

    public ItemStack getKeyItem() {
        return keyItem;
    }

    public void setKeyItem(ItemStack keyItem) {
        this.keyItem = keyItem;
    }

    public List<ChestWeight> getWeightList() {
        return weightList;
    }

    public int findWeight(int page, int slot) {
        for (ChestWeight chestWeight : weightList) {
            if (chestWeight.getPage() == page && chestWeight.getSlot() == slot) {
                return chestWeight.getWeight();
            }
        }
        return 0;
    }

    public ChestWeight findchestWeight(int page, int slot) {
        for (ChestWeight chestWeight : weightList) {
            if (chestWeight.getPage() == page && chestWeight.getSlot() == slot) {
                return chestWeight;
            }
        }
        return new ChestWeight(page, slot, 0);
    }

    @Override
    public YamlConfiguration serialize() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("name", this.name);
        data.set("randomType", this.randomType.toString());
        data.set("location", this.location);
        data.set("offset", this.offset);
        for (ChestWeight chestWeight : weightList) {
            data = chestWeight.serialize(data);
        }
        data = inventory.serialize(data);
        data.set("keyItem", this.keyItem);
        return data;
    }

    @Override
    public Chest deserialize(YamlConfiguration data) {
        this.name = data.getString("name");
        this.randomType = RandomType.valueOf(data.getString("randomType"));
        this.location = data.getLocation("location");
        this.offset = data.getLocation("offset");
        if (data.contains("ChestWeight")) {
            for (String pageKey : data.getConfigurationSection("ChestWeight").getKeys(false)) {
                int page = Integer.parseInt(pageKey);
                for (String slotKey : data.getConfigurationSection("ChestWeight." + pageKey).getKeys(false)) {
                    int slot = Integer.parseInt(slotKey);
                    int weight = data.getInt("ChestWeight." + pageKey + "." + slotKey + ".weight");
                    weightList.add(new ChestWeight(page, slot, weight));
                }
            }
        }
        this.inventory = new DInventory(name, 54, true, true, plugin).deserialize(data);
        this.keyItem = data.getItemStack("keyItem");
        return this;
    }
}