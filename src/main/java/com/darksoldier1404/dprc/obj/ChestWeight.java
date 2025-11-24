package com.darksoldier1404.dprc.obj;

import org.bukkit.configuration.file.YamlConfiguration;

public class ChestWeight {
    private int page;
    private int slot;
    private int weight;

    public ChestWeight(int page, int slot, int weight) {
        this.page = page;
        this.slot = slot;
        this.weight = weight;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public YamlConfiguration serialize(YamlConfiguration data) {
        data.set("ChestWeight." + page + "." + slot + ".weight", weight);
        return data;
    }
}
