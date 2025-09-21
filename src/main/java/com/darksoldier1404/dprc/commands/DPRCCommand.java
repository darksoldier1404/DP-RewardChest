package com.darksoldier1404.dprc.commands;

import com.darksoldier1404.dppc.builder.command.CommandBuilder;
import com.darksoldier1404.dprc.functions.DPRCFunction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.darksoldier1404.dprc.RewardChest.*;

public class DPRCCommand {
    private final CommandBuilder builder;

    public DPRCCommand() {
        builder = new CommandBuilder(plugin);

        builder.addSubCommand("reload", "dprc.reload", plugin.getLang().get("help_reload"), false, (p, args) -> {
            DPRCFunction.init();
            p.sendMessage(plugin.getPrefix() + plugin.getLang().get("reload_success"));
            return true;
        });

        builder.addSubCommand("create", "dprc.create", plugin.getLang().get("help_create"), true, (p, args) -> {
            if (args.length == 2) DPRCFunction.createRewardChest(p, args[1]);
            else p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_create"));
            return true;
        });

        builder.addSubCommand("items", "dprc.items", plugin.getLang().get("help_items"), true, (p, args) -> {
            if (args.length == 2) DPRCFunction.openRewardChestItems(p, args[1]);
            else p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_items"));
            return true;
        });

        builder.addSubCommand("weight", "dprc.weight", plugin.getLang().get("help_weight"), true, (p, args) -> {
            if (args.length == 2) DPRCFunction.openRewardChestChance(p, args[1]);
            else p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_weight"));
            return true;
        });

        builder.addSubCommand("delete", "dprc.delete", plugin.getLang().get("help_delete"), false, (p, args) -> {
            if (args.length == 2) DPRCFunction.deleteRewardChest(p, args[1]);
            else p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_delete"));
            return true;
        });

        builder.addSubCommand("key", "dprc.key", plugin.getLang().get("help_key"), true, (p, args) -> {
            if (args.length == 2) DPRCFunction.openRewardChestKey(p, args[1]);
            else p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_key"));
            return true;
        });

        builder.addSubCommand("givekey", "dprc.givekey", plugin.getLang().get("help_givekey"), false, (p, args) -> {
            if (args.length == 2) {
                if (!(p instanceof Player)) {
                    p.sendMessage(plugin.getPrefix() + plugin.getLang().get("not_player"));
                    return true;
                }
                DPRCFunction.giveRewardChestKey((Player) p, args[1]);
            } else if (args.length == 3) {
                DPRCFunction.giveRewardChestKey(Bukkit.getPlayer(args[2]), args[1]);
            } else p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_givekey"));
            return true;
        });

        builder.addSubCommand("block", "dprc.block", plugin.getLang().get("help_block"), true, (p, args) -> {
            if (args.length == 2) DPRCFunction.setRewardChestBlock(p, args[1]);
            else p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_block"));
            return true;
        });

        builder.addSubCommand("offset", "dprc.offset", plugin.getLang().get("help_offset"), true, (p, args) -> {
            if (args.length == 5) {
                DPRCFunction.setOffset(p, args[1], args[2], args[3], args[4]);
            } else {
                p.sendMessage(plugin.getPrefix() + plugin.getLang().get("help_offset"));
            }
            return true;
        });

        for (String c : builder.getSubCommandNames()) {
            builder.addTabCompletion(c, args -> {
                if (args.length == 2) return new ArrayList<>(rewardChests.keySet());
                if (args.length == 3 && c.equalsIgnoreCase("givekey")) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
                return null;
            });
        }
    }

    public CommandBuilder getExecuter() {
        return builder;
    }
}
