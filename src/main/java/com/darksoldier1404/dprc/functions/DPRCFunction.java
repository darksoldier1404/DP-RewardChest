package com.darksoldier1404.dprc.functions;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.utils.*;
import com.darksoldier1404.dprc.enums.RandomType;
import com.darksoldier1404.dprc.obj.Chest;
import com.darksoldier1404.dprc.obj.ChestWeight;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.darksoldier1404.dprc.RewardChest.*;

@SuppressWarnings("DataFlowIssue")
public class DPRCFunction {
    public static void init() {
        defaultOffset = new Location(null,
                plugin.getConfig().getDouble("Settings.defaultOffset.x", 0),
                plugin.getConfig().getDouble("Settings.defaultOffset.y", 0),
                plugin.getConfig().getDouble("Settings.defaultOffset.z", 0));
    }

    public static boolean isExistRewardChest(String name) {
        return data.containsKey(name);
    }

    public static void createRewardChest(CommandSender sender, String name) {
        if (isExistRewardChest(name)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_exists", name));
            return;
        }
        Chest chest = new Chest(name, RandomType.SIMPLE, null, null, new DInventory(name, 54, true, true, plugin), null);
        data.put(name, chest);
        data.save(name);
        sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_created", name));
    }

    public static void openRewardChestItems(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().get("not_player"));
            return;
        }
        Chest chest = data.get(name);
        DInventory inv = chest.getInventory();
        inv.setChannel(1);
        inv.setObj(chest);
        inv.openInventory((Player) sender);
    }

    public static void saveRewardChestItems(String name, DInventory inv) {
        if (!isExistRewardChest(name)) {
            return;
        }
        Chest chest = data.get(name);
        inv.applyChanges();
        chest.setInventory(inv);
        data.put(name, chest);
        data.save(name);
    }

    public static void deleteRewardChest(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        data.delete(name);
        data.remove(name);
        sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_deleted", name));
    }

    public static void openRewardChestChance(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().get("not_player"));
            return;
        }
        Chest chest = data.get(name);
        DInventory inv = chest.getInventory().clone();
        inv.setChannel(2);
        inv.setObj(chest);
        inv.openInventory((Player) sender);
    }

    public static void setRewardChestWeight(Player p, DInventory inv, int slot, int weight) {
        Chest chest = (Chest) inv.getObj();
        String name = chest.getName();
        if (!isExistRewardChest(name)) {
            return;
        }
        ChestWeight cw = chest.findchestWeight(inv.getCurrentPage(), slot);
        chest.getWeightList().remove(cw);
        if (weight == 0) {
            plugin.data.put(chest.getName(), chest);
            plugin.data.save(chest.getName());
            p.sendMessage(plugin.getPrefix() + "weight removed");
        } else {
            cw.setWeight(weight);
            chest.getWeightList().add(cw);
            plugin.data.put(chest.getName(), chest);
            plugin.data.save(chest.getName());
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_weight_set", String.valueOf(weight), String.valueOf(slot)));
        }
        inv = chest.getInventory().clone();
        inv.setChannel(2);
        inv.setObj(chest);
        DInventory finalInv = inv;
        Bukkit.getScheduler().runTaskLater(plugin, () -> finalInv.openInventory(p), 1L);
    }

    public static void updateChanceLore(DInventory inv) {
        Chest box = (Chest) inv.getObj();
        if (box == null) return;
        inv.applyAllItemChanges((pis -> {
            ItemStack item = pis.getItem();
            if (item == null || item.getType().isAir()) {
                return pis;
            }
            int totalWeight = 0;
            for (DInventory.PageItemSet pageItemSet : inv.getAllPageItemSets()) {
                totalWeight += box.findWeight(pageItemSet.getPage(), pageItemSet.getSlot());
            }
            int weight = box.findWeight(pis.getPage(), pis.getSlot());
            List<String> lore = item.getItemMeta() != null && item.getItemMeta().getLore() != null ? item.getItemMeta().getLore() : new ArrayList<>();
            if (weight > 0) {
                lore.add("§7Weight: §e" + weight);
                double chance = (double) weight / (double) totalWeight * 100.0;
                lore.add("§7Chance: §e" + String.format("%.2f", chance) + "%");
            }
            ItemMeta meta = item.getItemMeta();
            meta.setLore(lore);
            item.setItemMeta(meta);
            pis.setItem(item);
            return pis;
        }));
        inv.update();
    }

    public static void openRewardChestKey(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().get("not_player"));
            return;
        }
        Chest chest = data.get(name);
        DInventory inv = new DInventory("Reward Chest Key Edit - " + chest.getName(), 27, plugin);
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
        inv.setItem(13, chest.getKeyItem());
        inv.setChannel(3);
        inv.setObj(chest);
        inv.openInventory((Player) sender);
    }

    public static void saveRewardChestKey(Player p, DInventory inv) {
        Chest chest = (Chest) inv.getObj();
        String name = chest.getName();
        if (!isExistRewardChest(name)) {
            return;
        }
        ItemStack keyItem = inv.getItem(13);
        chest.setKeyItem(keyItem);
        data.put(name, chest);
        data.save(name);
        p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_key_set", name));
    }

    @Nullable
    public static ItemStack getRewardChestKey(String name) {
        if (!isExistRewardChest(name)) {
            return null;
        }
        Chest chest = data.get(name);
        ItemStack keyItem = chest.getKeyItem();
        if (keyItem == null) {
            return null;
        } else {
            return NBT.setStringTag(keyItem, "dprc_key", name);
        }
    }

    public static void giveRewardChestKey(Player player, String name) {
        ItemStack key = getRewardChestKey(name);
        if (key == null) {
            player.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_key_not_exists", name));
            return;
        }
        if (!InventoryUtils.hasEnoughSpace(player.getInventory().getStorageContents(), key)) {
            player.sendMessage(plugin.getPrefix() + plugin.getLang().get("inventory_full"));
            return;
        }
        player.getInventory().addItem(key);
        player.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_key_given", name));
    }

    public static void setRewardChestBlock(CommandSender sender, String name) {
        if (!isExistRewardChest(name)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().get("not_player"));
            return;
        }
        Player p = (Player) sender;
        Block b = p.getTargetBlockExact(10);
        if (b == null) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().get("no_target_block"));
            return;
        }
        World world = b.getWorld();
        if (!b.getType().isSolid()) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().get("block_not_solid"));
            return;
        }
        Chest chest = data.get(name);
        chest.setLocation(b.getLocation());
        data.put(name, chest);
        data.save(name);
        p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_block_set", name, world.getName(), b.getX() + ", " + b.getY() + ", " + b.getZ() + " (" + b.getType().name() + ")"));
    }

    public static Tuple<Boolean, String> isRewardChestBlock(Block block) {
        for (Map.Entry<String, Chest> entry : data.entrySet()) {
            Chest chest = entry.getValue();
            if (chest.getLocation() == null) continue;
            if (chest.getLocation().equals(block.getLocation())) {
                return Tuple.of(true, entry.getKey());
            }
        }
        return Tuple.of(false, null);
    }

    public static List<ItemStack> getRewardChestItems(String name) {
        if (!isExistRewardChest(name)) {
            return Collections.emptyList();
        }
        Chest chest = data.get(name);
        DInventory inv = chest.getInventory();
        return inv.getAllPageItems();
    }

    @Nullable
    public static Location getRewardChestLocation(String name) {
        if (!isExistRewardChest(name)) {
            return null;
        }
        Chest chest = data.get(name);
        return chest.getLocation().clone();
    }

    @Nullable
    public static ItemStack getReward(String name) {
        Chest chest = data.get(name);
        DInventory inv = chest.getInventory().clone();
        if (chest.getRandomType() == RandomType.SIMPLE) {
            List<ItemStack> items = inv.getAllPageItems().stream().filter(item -> item != null && item.getType() != Material.AIR).collect(Collectors.toList());
            if (items.isEmpty()) {
                return null;
            }
            int randomIndex = (int) (Math.random() * items.size());
            return items.get(randomIndex).clone();
        } else {
            List<DInventory.PageItemSet> allItems = inv.getAllPageItemSets();
            List<Integer> weights = new ArrayList<>();
            int totalWeight = 0;
            for (DInventory.PageItemSet pis : allItems) {
                int weight = chest.findWeight(pis.getPage(), pis.getSlot());
                weights.add(weight);
                totalWeight += weight;
            }
            int rand = new Random().nextInt(totalWeight);
            int sum = 0;
            for (int i = 0; i < allItems.size(); i++) {
                sum += weights.get(i);
                if (rand < sum) {
                    return allItems.get(i).getItem().clone();
                }
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
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
        }
        if (!hasKey(p, name)) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_key_not_exists", name));
            return;
        }
        if (currentlyRoll.contains(p.getUniqueId())) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().get("reward_chest_key_already_rolling"));
            return;
        }
        currentlyRoll.add(p.getUniqueId());
        Location loc = getRewardChestLocation(name);
        if (loc == null) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_block_not_set", name));
            return;
        }
        List<ItemStack> items = getRewardChestItems(name);
        ItemDisplay as = showFakeItem(p, name, loc, items.isEmpty() ? new ItemStack(Material.PAPER) : items.get(new Random().nextInt(items.size())));
        ItemStack item = getReward(name);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            as.setItemStack(items.isEmpty() ? new ItemStack(Material.PAPER) : items.get(new Random().nextInt(items.size())));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
        }, 0L, 2L);
        BukkitTask task2 = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!as.isDead()) {
                removeFakeItem(p);
                currentlyRoll.remove(p.getUniqueId());
            } else {
                p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_key_give_failed", name));
            }
        }, 80L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!as.isDead()) {
                p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_key_give", name));
                task.cancel();
                as.setItemStack(item);
                if (!InventoryUtils.hasEnoughSpace(p.getInventory().getStorageContents(), item)) {
                    p.sendMessage(plugin.getPrefix() + plugin.getLang().get("inventory_full"));
                    currentlyRoll.remove(p.getUniqueId());
                    task2.cancel();
                    removeFakeItem(p);
                    return;
                }
                for (ItemStack i : p.getInventory().getContents()) {
                    if (i == null) continue;
                    if (NBT.hasTagKey(i, "dprc_key") && NBT.getStringTag(i, "dprc_key").equals(name)) {
                        i.setAmount(i.getAmount() - 1);
                        p.getInventory().addItem(item);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1F);
                        currentlyRoll.remove(p.getUniqueId());
                        task2.cancel();
                        removeFakeItem(p);
                        return;
                    }
                }
            } else {
                p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_key_wait_failed", name));
                currentlyRoll.remove(p.getUniqueId());
                task2.cancel();
                removeFakeItem(p);
            }
        }, 60L);
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
        List<String> temp = plugin.getConfig().getStringList("Settings.fake_items");
        if (!temp.contains(stand.getUniqueId().toString())) {
            temp.add(stand.getUniqueId().toString());
            plugin.getConfig().set("Settings.fake_items", temp);
        }
        return stand;
    }

    public static void removeFakeItem(Player player) {
        ItemDisplay stand = fakeEntities.remove(player.getUniqueId());
        if (stand != null && !stand.isDead()) {
            stand.remove();
            List<String> temp = plugin.getConfig().getStringList("Settings.fake_items");
            temp.remove(stand.getUniqueId().toString());
            plugin.getConfig().set("Settings.fake_items", temp);
        }
    }

    public static void cleanupFakeItems() {
        List<String> temp = plugin.getConfig().getStringList("Settings.fake_items");
        for (String uuid : temp) {
            UUID id = UUID.fromString(uuid);
            ArmorStand stand = Bukkit.getEntity(id) instanceof ArmorStand ? (ArmorStand) Bukkit.getEntity(id) : null;
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        plugin.getConfig().set("Settings.fake_items", null);
    }

    public static Location getOffset(World world, String name) {
        if (!isExistRewardChest(name)) {
            return defaultOffset;
        }
        Chest chest = data.get(name);
        Location offset = chest.getOffset();
        if (offset != null) {
            offset.setWorld(world);
            return offset;
        } else {
            offset = defaultOffset.clone();
            offset.setWorld(world);
            return offset;
        }
    }

    public static void setOffset(CommandSender p, String name, String x, String y, String z) {
        if (!isExistRewardChest(name)) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        if (!(p instanceof Player)) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().get("not_player"));
            return;
        }
        Player player = (Player) p;
        try {
            double offsetX = Double.parseDouble(x);
            double offsetY = Double.parseDouble(y);
            double offsetZ = Double.parseDouble(z);
            Chest chest = data.get(name);
            Location offset = new Location(player.getWorld(), offsetX, offsetY, offsetZ);
            chest.setOffset(offset);
            data.put(name, chest);
            data.save(name);
            player.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_offset_set", name, x, y, z));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getPrefix() + plugin.getLang().get("invalid_number_format"));
        }
    }

    public static void setRewardChestRandomType(CommandSender p, String name, String sRandomType) {
        if (!isExistRewardChest(name)) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        RandomType randomType;
        try {
            randomType = RandomType.valueOf(sRandomType.toUpperCase());
        } catch (IllegalArgumentException e) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("invalid_random_type", Arrays.toString(RandomType.values())));
            return;
        }
        Chest chest = data.get(name);
        chest.setRandomType(randomType);
        data.put(name, chest);
        data.save(name);
        p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_random_type_set", name, randomType.name()));

    }

    public static void setRewardChestMaxPage(CommandSender p, String name, String sMaxPage) {
        if (!isExistRewardChest(name)) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_not_exists", name));
            return;
        }
        int maxPage;
        try {
            maxPage = Integer.parseInt(sMaxPage);
            if (maxPage < 1) {
                p.sendMessage(plugin.getPrefix() + plugin.getLang().get("invalid_max_page"));
                return;
            }
        } catch (NumberFormatException e) {
            p.sendMessage(plugin.getPrefix() + plugin.getLang().get("invalid_number_format"));
            return;
        }
        Chest chest = data.get(name);
        DInventory inv = chest.getInventory();
        inv.setPages(maxPage);
        chest.setInventory(inv);
        data.put(name, chest);
        data.save(name);
        p.sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_max_page_set", name, String.valueOf(maxPage)));
    }
}
