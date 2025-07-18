package com.darksoldier1404.dprc;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.utils.PluginUtil;
import com.darksoldier1404.dppc.utils.Tuple;
import com.darksoldier1404.dprc.commands.DPRCCommand;
import com.darksoldier1404.dprc.events.DPRCEvent;
import com.darksoldier1404.dprc.functions.DPRCFunction;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RewardChest extends JavaPlugin {
    public static RewardChest plugin;
    public static YamlConfiguration config;
    public static String prefix;
    public static DLang lang;
    public static Map<String, YamlConfiguration> rewardChests = new HashMap<>();
    public static Map<UUID, Tuple<DInventory, Integer>> currentChanceEdit = new HashMap<>();
    public static Set<UUID> currentlyRoll = new HashSet<>();
    public static Location defaultOffset;

    public static RewardChest getInstance() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        PluginUtil.addPlugin(plugin, 26191);
    }

    @Override
    public void onEnable() {
        DPRCFunction.init();
        plugin.getServer().getPluginManager().registerEvents(new DPRCEvent(), plugin);
        getCommand("dprc").setExecutor(new DPRCCommand().getExecuter());
        DPRCFunction.cleanupFakeItems();
    }

    @Override
    public void onDisable() {
        DPRCFunction.saveConfig();
        DPRCFunction.cleanupFakeItems();
    }
}