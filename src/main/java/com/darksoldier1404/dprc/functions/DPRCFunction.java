package com.darksoldier1404.dprc.functions;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

import static com.darksoldier1404.dprc.RewardChest.*;

public class DPRCFunction {
    public static void init() {
        config = ConfigUtils.loadDefaultPluginConfig(plugin);
        prefix = ColorUtils.applyColor(config.getString("Settings.prefix"));
        lang = new DLang(config.getString("Settings.Lang") == null ? "English" : config.getString("Settings.Lang"), plugin);
        for (YamlConfiguration rewardChestConfig : ConfigUtils.loadCustomDataList(plugin, "data")) {
            if (rewardChestConfig.contains("Settings.name")) {
                String name = rewardChestConfig.getString("Settings.name");
                rewardChests.put(name, rewardChestConfig);
            }
        }
        defaultOffset = new Location(null,
                config.getDouble("Settings.defaultOffset.x", 0),
                config.getDouble("Settings.defaultOffset.y", 0),
                config.getDouble("Settings.defaultOffset.z", 0));
    }

    public static void saveConfig() {
        ConfigUtils.savePluginConfig(plugin, config);
        for (String name : rewardChests.keySet()) {
            ConfigUtils.saveCustomData(plugin, rewardChests.get(name), name, "data");
        }
    }

    public static boolean isExistRewardChest(String name) {
        return rewardChests.containsKey(name);
    }

    public static void createRewardChest(CommandSender sender, String name) {
        if (isExistRewardChest(name)) {
            sender.sendMessage(prefix + lang.getWithArgs("reward_chest_exists", name));
            return;
        }
        YamlConfiguration rewardChestConfig = new YamlConfiguration();
        rewardChestConfig.set("Settings.name", name);
        rewardChests.put(name, rewardChestConfig);
        saveConfig();
        sender.sendMessage(prefix + lang.getWithArgs("reward_chest_created", name));
    }

    public static void openRewardChestItems(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(prefix + lang.getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + lang.get("not_player"));
            return;
        }
        DInventory inv = new DInventory(lang.getWithArgs("reward_chest_items_title", name), 54, plugin);
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        if (rewardChestConfig.getConfigurationSection("Items") != null) {
            for (String itemKey : rewardChestConfig.getConfigurationSection("Items").getKeys(false)) {
                inv.setItem(Integer.parseInt(itemKey), rewardChestConfig.getItemStack("Items." + itemKey + ".item"));
            }
        }
        inv.setChannel(1);
        inv.setObj(name);
        inv.openInventory((Player) sender);
    }

    public static void saveRewardChestItems(String name, DInventory inv) {
        if (!isExistRewardChest(name)) {
            return;
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        for (int i = 0; i < inv.getSize(); i++) {
            rewardChestConfig.set("Items." + i + ".item", inv.getItem(i));
        }
        rewardChests.put(name, rewardChestConfig);
        saveConfig();
    }

    public static void deleteRewardChest(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(prefix + lang.getWithArgs("reward_chest_not_exists", name));
            return;
        }
        rewardChests.remove(name);
        new File(plugin.getDataFolder(), "data/" + name + ".yml").delete();
        sender.sendMessage(prefix + lang.getWithArgs("reward_chest_deleted", name));
    }

    public static void openRewardChestChance(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(prefix + lang.getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + lang.get("not_player"));
            return;
        }
        DInventory inv = new DInventory(lang.getWithArgs("reward_chest_weight_title", name), 54, plugin);
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        if (rewardChestConfig.getConfigurationSection("Items") != null) {
            for (String itemKey : rewardChestConfig.getConfigurationSection("Items").getKeys(false)) {
                ItemStack item = rewardChestConfig.getItemStack("Items." + itemKey + ".item");
                if (item != null) {
                    inv.setItem(Integer.parseInt(itemKey), item.clone());
                }
            }
        }
        inv.setChannel(2);
        inv.setObj(name);
        inv.openInventory((Player) sender);
    }

    public static void setRewardChestWeight(Player p, DInventory inv, int index, int weight) {
        String name = (String) inv.getObj();
        if (!isExistRewardChest(name)) {
            return;
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        rewardChestConfig.set("Items." + index + ".weight", weight);
        rewardChests.put(name, rewardChestConfig);
        saveConfig();
        updateChanceLore(inv, name);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            inv.openInventory(p);
            p.sendMessage(prefix + lang.getWithArgs("reward_chest_weight_set", String.valueOf(weight), String.valueOf(index)));
        }, 1L);
    }

    public static void updateChanceLore(DInventory inv, String name) { // need to update after setting weight
        if (!isExistRewardChest(name)) {
            return;
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        Map<String, Integer> itemWeights = new HashMap<>();
        Map<String, Double> itemChances = new HashMap<>();
        if (rewardChestConfig.getConfigurationSection("Items") != null) {
            for (String itemKey : rewardChestConfig.getConfigurationSection("Items").getKeys(false)) {
                int weight = rewardChestConfig.getInt("Items." + itemKey + ".weight", 0);
                itemWeights.put(itemKey, weight);
            }
        }
        int totalWeight = itemWeights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight == 0) {
            for (String itemKey : itemWeights.keySet()) {
                itemChances.put(itemKey, 1.0 / itemWeights.size());
            }
        } else {
            for (Map.Entry<String, Integer> entry : itemWeights.entrySet()) {
                double chance = (double) entry.getValue() / totalWeight;
                itemChances.put(entry.getKey(), chance);
            }
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) != null && rewardChestConfig.contains("Items." + i)) {
                ItemStack item = inv.getItem(i);
                ItemMeta im = item.getItemMeta();
                if (im != null) {
                    im.setLore(
                            Arrays.asList(
                                    lang.getWithArgs("reward_chest_item_lore_weight", String.valueOf(itemWeights.get(String.valueOf(i)))),
                                    lang.getWithArgs("reward_chest_item_lore_chance", String.format("%.2f%%", itemChances.getOrDefault(String.valueOf(i), 0.0) * 100))
                            )
                    );
                    item.setItemMeta(im);
                    inv.setItem(i, item);
                }
            }
        }
    }

    public static void openRewardChestKey(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(prefix + lang.getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + lang.get("not_player"));
            return;
        }
        DInventory inv = new DInventory(lang.getWithArgs("reward_chest_key_title", name), 27, plugin);
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            paneMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            pane.setItemMeta(paneMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, NBT.setStringTag(pane, "dprc_pane", "true"));
        }
        if (rewardChestConfig.getConfigurationSection("Key") != null) {
            ItemStack keyItem = rewardChestConfig.getItemStack("Key.item");
            inv.setItem(13, keyItem);
        } else {
            inv.setItem(13, null);
        }
        inv.setChannel(3);
        inv.setObj(name);
        inv.openInventory((Player) sender);
    }

    public static void saveRewardChestKey(Player p, DInventory inv) {
        String name = (String) inv.getObj();
        if (!isExistRewardChest(name)) {
            return;
        }
        ItemStack keyItem = inv.getItem(13);
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        rewardChestConfig.set("Key.item", keyItem);
        rewardChests.put(name, rewardChestConfig);
        saveConfig();
        p.sendMessage(prefix + lang.getWithArgs("reward_chest_key_set", name));
    }

    public static ItemStack getRewardChestKey(String name) {
        if (!isExistRewardChest(name)) {
            return null;
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        if (rewardChestConfig.contains("Key.item")) {
            return NBT.setStringTag(rewardChestConfig.getItemStack("Key.item"), "dprc_key", name);
        }
        return null;
    }

    public static void giveRewardChestKey(Player player, String name) {
        ItemStack key = getRewardChestKey(name);
        if (key == null) {
            player.sendMessage(prefix + lang.getWithArgs("reward_chest_key_not_exists", name));
            return;
        }
        if (!InventoryUtils.hasEnoughSpace(player.getInventory().getStorageContents(), key)) {
            player.sendMessage(prefix + lang.get("inventory_full"));
            return;
        }
        player.getInventory().addItem(key);
        player.sendMessage(prefix + lang.getWithArgs("reward_chest_key_given", name));
    }

    public static void setRewardChestBlock(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(prefix + lang.getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + lang.get("not_player"));
            return;
        }
        Player p = (Player) sender;
        Block b = p.getTargetBlockExact(10);
        if (b == null) {
            p.sendMessage(prefix + lang.get("no_target_block"));
            return;
        }
        World world = b.getWorld();
        if (!b.getType().isSolid()) {
            p.sendMessage(prefix + lang.get("block_not_solid"));
            return;
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        if (rewardChestConfig.contains("Settings.block.world") &&
                rewardChestConfig.getString("Settings.block.world").equals(world.getName()) &&
                rewardChestConfig.getInt("Settings.block.x") == b.getX() &&
                rewardChestConfig.getInt("Settings.block.y") == b.getY() &&
                rewardChestConfig.getInt("Settings.block.z") == b.getZ()) {
            rewardChestConfig.set("Settings.block", null);
            p.sendMessage(prefix + lang.getWithArgs("reward_chest_block_removed", name));
        } else {
            rewardChestConfig.set("Settings.block.world", world.getName());
            rewardChestConfig.set("Settings.block.x", b.getX());
            rewardChestConfig.set("Settings.block.y", b.getY());
            rewardChestConfig.set("Settings.block.z", b.getZ());
            p.sendMessage(prefix + lang.getWithArgs("reward_chest_block_set", name, world.getName(), b.getX() + ", " + b.getY() + ", " + b.getZ() + " (" + b.getType().name() + ")"));
        }
        rewardChests.put(name, rewardChestConfig);
        saveConfig();
    }

    public static Tuple<Boolean, String> isRewardChestBlock(Block block) {
        for (Map.Entry<String, YamlConfiguration> entry : rewardChests.entrySet()) {
            YamlConfiguration rewardChestConfig = entry.getValue();
            if (rewardChestConfig.contains("Settings.block")) {
                String worldName = block.getWorld().getName();
                World world = Bukkit.getWorld(worldName);
                if (world == null || !world.equals(block.getWorld())) {
                    continue;
                }
                int x = rewardChestConfig.getInt("Settings.block.x");
                int y = rewardChestConfig.getInt("Settings.block.y");
                int z = rewardChestConfig.getInt("Settings.block.z");
                if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                    return Tuple.of(true, entry.getKey());
                }
            }
        }
        return Tuple.of(false, null);
    }

    public static List<ItemStack> getRewardChestItems(String name) {
        if (!isExistRewardChest(name)) {
            return Collections.emptyList();
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        List<ItemStack> items = new ArrayList<>();
        if (rewardChestConfig.getConfigurationSection("Items") != null) {
            for (String itemKey : rewardChestConfig.getConfigurationSection("Items").getKeys(false)) {
                ItemStack item = rewardChestConfig.getItemStack("Items." + itemKey + ".item");
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    public static Location getRewardChestLocation(String name) {
        if (!isExistRewardChest(name)) {
            return null;
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        if (rewardChestConfig.contains("Settings.block")) {
            String worldName = rewardChestConfig.getString("Settings.block.world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                int x = rewardChestConfig.getInt("Settings.block.x");
                int y = rewardChestConfig.getInt("Settings.block.y");
                int z = rewardChestConfig.getInt("Settings.block.z");
                return new Location(world, x, y, z);
            }
        }
        return null;
    }

    public static ItemStack getReward(String name) {
        if (!isExistRewardChest(name)) {
            return null;
        }
        YamlConfiguration rewardChestConfig = rewardChests.get(name);
        if (rewardChestConfig.getConfigurationSection("Items") == null) {
            return null;
        }
        List<String> itemKeys = new ArrayList<>(rewardChestConfig.getConfigurationSection("Items").getKeys(false));
        List<ItemStack> items = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int totalWeight = 0;
        for (String key : itemKeys) {
            ItemStack item = rewardChestConfig.getItemStack("Items." + key + ".item");
            int weight = rewardChestConfig.getInt("Items." + key + ".weight", 0);
            if (item != null && weight > 0) {
                items.add(item);
                weights.add(weight);
                totalWeight += weight;
            }
        }
        if (items.isEmpty() || totalWeight == 0) {
            return null;
        }
        int rand = new Random().nextInt(totalWeight);
        int sum = 0;
        for (int i = 0; i < items.size(); i++) {
            sum += weights.get(i);
            if (rand < sum) {
                return items.get(i).clone();
            }
        }
        return null;
    }

    public static boolean hasKey(Player player, String name) {
        if (!isExistRewardChest(name)) {
            return false;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && NBT.hasTagKey(item, "dprc_key") && NBT.getStringTag(item, "dprc_key").equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static void startGiveRewardChestKeyTask(Player p, String name) {
        if (!isExistRewardChest(name)) {
            p.sendMessage(prefix + lang.getWithArgs("reward_chest_not_exists", name));
        }
        if (!hasKey(p, name)) {
            p.sendMessage(prefix + lang.getWithArgs("reward_chest_key_not_exists", name));
            return;
        }
        if (currentlyRoll.contains(p.getUniqueId())) {
            p.sendMessage(prefix + lang.get("reward_chest_key_already_rolling"));
            return;
        }
        currentlyRoll.add(p.getUniqueId());
        Location loc = getRewardChestLocation(name);
        if (loc == null) {
            p.sendMessage(prefix + lang.getWithArgs("reward_chest_block_not_set", name));
            return;
        }
        List<ItemStack> items = getRewardChestItems(name);
        ItemDisplay as = showFakeItem(p, name, loc, items.isEmpty() ? new ItemStack(Material.PAPER) : items.get(new Random().nextInt(items.size())));
        ItemStack item = getReward(name);
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            as.setItemStack(items.isEmpty() ? new ItemStack(Material.PAPER) : items.get(new Random().nextInt(items.size())));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
        }, 0L, 2L);
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!as.isDead()) {
                p.sendMessage(prefix + lang.getWithArgs("reward_chest_key_give", name));
                task.cancel();
                as.setItemStack(item);
                if (!InventoryUtils.hasEnoughSpace(p.getInventory().getStorageContents(), item)) {
                    p.sendMessage(prefix + lang.get("inventory_full"));
                    return;
                }
                for (ItemStack i : p.getInventory().getContents()) {
                    if (i == null) continue;
                    if (NBT.hasTagKey(i, "dprc_key") && NBT.getStringTag(i, "dprc_key").equals(name)) {
                        i.setAmount(i.getAmount() - 1);
                        p.getInventory().addItem(item);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1F);
                        return;
                    }
                }
            } else {
                p.sendMessage(prefix + lang.getWithArgs("reward_chest_key_wait_failed", name));
            }
        }, 60L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!as.isDead()) {
                removeFakeItem(p);
                currentlyRoll.remove(p.getUniqueId());
            } else {
                p.sendMessage(prefix + lang.getWithArgs("reward_chest_key_give_failed", name));
            }
        }, 80L);
    }

    private static final Map<UUID, ItemDisplay> fakeEntities = new HashMap<>();

    public static ItemDisplay showFakeItem(Player player, String name, Location location, ItemStack item) {
        ItemDisplay stand = location.add(getOffset(location.getWorld(), name)).getWorld().spawn(location, ItemDisplay.class);
        stand.setItemStack(item);
        stand.setVisibleByDefault(false);
        player.showEntity(plugin, stand);
        NamespacedKey key = new NamespacedKey(plugin, "dprc_fake_item");
        PersistentDataContainer dataContainer = stand.getPersistentDataContainer();
        dataContainer.set(key, org.bukkit.persistence.PersistentDataType.STRING, "dprc_fake_item");
        fakeEntities.put(player.getUniqueId(), stand);
        List<String> temp = config.getStringList("Settings.fake_items");
        if (!temp.contains(stand.getUniqueId().toString())) {
            temp.add(stand.getUniqueId().toString());
            config.set("Settings.fake_items", temp);
        }
        return stand;
    }

    public static void removeFakeItem(Player player) {
        ItemDisplay stand = fakeEntities.remove(player.getUniqueId());
        if (stand != null && !stand.isDead()) {
            stand.remove();
            List<String> temp = config.getStringList("Settings.fake_items");
            temp.remove(stand.getUniqueId().toString());
            config.set("Settings.fake_items", temp);
        }
    }

    public static void cleanupFakeItems() {
        List<String> temp = config.getStringList("Settings.fake_items");
        for (String uuid : temp) {
            UUID id = UUID.fromString(uuid);
            ArmorStand stand = Bukkit.getEntity(id) instanceof ArmorStand ? (ArmorStand) Bukkit.getEntity(id) : null;
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        config.set("Settings.fake_items", null);
    }

    public static Location getOffset(World world, String name) {
        if (!isExistRewardChest(name)) {
            return defaultOffset;
        }
        YamlConfiguration data = rewardChests.get(name);
        if (data.contains("Settings.offset")) {
            Location loc = new Location(
                    world,
                    data.getDouble("Settings.offset.x"),
                    data.getDouble("Settings.offset.y"),
                    data.getDouble("Settings.offset.z")
            );
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                return loc;
            } else {
                return defaultOffset;
            }
        }
        return defaultOffset;
    }

    public static void setOffset(CommandSender p, String name, String x, String y, String z) {
        if (!isExistRewardChest(name)) {
            p.sendMessage(prefix + lang.getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(p instanceof Player)) {
            p.sendMessage(prefix + lang.get("not_player"));
            return;
        }
        Player player = (Player) p;
        try {
            double offsetX = Double.parseDouble(x);
            double offsetY = Double.parseDouble(y);
            double offsetZ = Double.parseDouble(z);
            YamlConfiguration rewardChestConfig = rewardChests.get(name);
            rewardChestConfig.set("Settings.offset.x", offsetX);
            rewardChestConfig.set("Settings.offset.y", offsetY);
            rewardChestConfig.set("Settings.offset.z", offsetZ);
            rewardChests.put(name, rewardChestConfig);
            saveConfig();
            player.sendMessage(prefix + lang.getWithArgs("reward_chest_offset_set", name, x, y, z));
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + lang.get("invalid_number_format"));
        }
    }
}
