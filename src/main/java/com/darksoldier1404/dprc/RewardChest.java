package com.darksoldier1404.dprc;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.data.DataContainer;
import com.darksoldier1404.dppc.data.DataType;
import com.darksoldier1404.dppc.utils.PluginUtil;
import com.darksoldier1404.dppc.utils.Tuple;
import com.darksoldier1404.dprc.commands.DPRCCommand;
import com.darksoldier1404.dprc.events.DPRCEvent;
import com.darksoldier1404.dprc.functions.DPRCFunction;
import com.darksoldier1404.dprc.obj.Chest;
import org.bukkit.Location;

import java.util.*;


@DPPCoreVersion(since = "5.3.0")
public class RewardChest extends DPlugin {
    public static RewardChest plugin;
    public static DataContainer<String, Chest> data;
    public static Map<UUID, Tuple<DInventory, Integer>> currentChanceEdit = new HashMap<>();
    public static Set<UUID> currentlyRoll = new HashSet<>();
    public static Location defaultOffset;

    public RewardChest() {
        super(true);
        plugin = this;
        init();
    }

    public static RewardChest getInstance() {
        return plugin;
    }

    @Override
    public void onLoad() {
        PluginUtil.addPlugin(plugin, 26191);
    }

    @Override
    public void onEnable() {
        data = loadDataContainer(new DataContainer<>(this, DataType.CUSTOM, "data"), Chest.class);
        DPRCFunction.init();
        plugin.getServer().getPluginManager().registerEvents(new DPRCEvent(), plugin);
        getCommand("dprc").setExecutor(new DPRCCommand().getExecuter());
        DPRCFunction.cleanupFakeItems();
    }

    @Override
    public void onDisable() {
        saveAllData();
        DPRCFunction.cleanupFakeItems();
    }
}