package com.darksoldier1404.dprc.events;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.events.dinventory.DInventoryClickEvent;
import com.darksoldier1404.dppc.events.dinventory.DInventoryCloseEvent;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppc.utils.Tuple;
import com.darksoldier1404.dprc.functions.DPRCFunction;
import com.darksoldier1404.dprc.obj.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static com.darksoldier1404.dprc.RewardChest.*;

public class DPRCEvent implements Listener {
    @EventHandler
    public void onInventoryClose(DInventoryCloseEvent e) {
        DInventory inv = e.getDInventory();
        if (inv.isValidHandler(plugin)) {
            if (inv.getChannel() == 1) { // item set
                DPRCFunction.saveRewardChestItems((String) inv.getObj(), inv);
                return;
            }
            if (inv.getChannel() == 3) { // key set
                DPRCFunction.saveRewardChestKey((Player) e.getPlayer(), inv);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(DInventoryClickEvent e) {
        DInventory inv = e.getDInventory();
        if (inv.isValidHandler(plugin)) {
            ItemStack currentItem = e.getCurrentItem();
            if (currentItem == null || currentItem.getType().isAir()) {
                return;
            }
            if (NBT.hasTagKey(currentItem, "dprc_pane")) {
                e.setCancelled(true);
                return;
            }
            if (inv.getChannel() == 2) { // weight set
                if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
                    e.setCancelled(true);
                    return;
                }
                if (e.getCurrentItem() == null && e.getCurrentItem().getType().isAir()) {
                    e.setCancelled(true);
                    return;
                }
                int slot = e.getSlot();
                Chest chest = (Chest) inv.getObj();
                currentChanceEdit.put(e.getWhoClicked().getUniqueId(), Tuple.of(inv, slot));
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(plugin.getPrefix() + plugin.getLang().getWithArgs("reward_chest_weight_edit", chest.getName()));
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (currentChanceEdit.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
            String message = e.getMessage();
            Tuple<DInventory, Integer> data = currentChanceEdit.get(p.getUniqueId());
            DInventory inv = data.getA();
            int slot = data.getB();
            if (!message.matches("\\d+")) {
                p.sendMessage(plugin.getPrefix() + plugin.getLang().get("reward_chest_weight_format_error"));
                return;
            }
            currentChanceEdit.remove(p.getUniqueId());
            DPRCFunction.setRewardChestWeight(p, inv, slot, Integer.parseInt(e.getMessage()));
        }
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        if (e.getClickedBlock() != null && DPRCFunction.isRewardChestBlock(e.getClickedBlock()).getA()) {
            e.setCancelled(true);
            Player p = e.getPlayer();
            DPRCFunction.startGiveRewardChestKeyTask(p, DPRCFunction.isRewardChestBlock(e.getClickedBlock()).getB());
        }
    }
}
