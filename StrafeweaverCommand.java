package com.yourserver.strafeweaver;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrafeweaverCommand implements CommandExecutor {

    private final StrafeweaverPlugin plugin;
    private final StrafeweaverManager manager;
    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();

    public StrafeweaverCommand(StrafeweaverPlugin plugin, StrafeweaverManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.AQUA + "Strafeweaver Commands:");
            player.sendMessage(ChatColor.GRAY + "/strafeweaver give [force]");
            player.sendMessage(ChatColor.GRAY + "/strafeweaver ability");
            return true;
        }

        // --- GIVE COMMAND ---
        if (args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("strafeweaver.give")) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }

            boolean force = args.length > 1 && args[1].equalsIgnoreCase("force");

            if (!force && manager.doesStrafeExistInWorld()) {
                player.sendMessage(ChatColor.RED + "The Strafeweaver already exists in this world! Use '/strafeweaver give force' to overwrite.");
                return true;
            }

            player.getInventory().addItem(manager.createStrafeweaver());
            player.sendMessage(ChatColor.AQUA + "You have forged the one and only Strafeweaver!");
            return true;
        }

        // --- ABILITY COMMAND ---
        if (args[0].equalsIgnoreCase("ability")) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!manager.isStrafeweaver(hand)) {
                player.sendMessage(ChatColor.RED + "You must be holding the Strafeweaver!");
                return true;
            }

            // Check Cooldown (120 seconds)
            long cooldownMs = 120_000;
            long now = System.currentTimeMillis();
            if (abilityCooldowns.containsKey(player.getUniqueId())) {
                long nextUse = abilityCooldowns.get(player.getUniqueId());
                if (now < nextUse) {
                    player.sendMessage(ChatColor.RED + "Ability on cooldown for " + ((nextUse - now) / 1000) + "s.");
                    return true;
                }
            }

            // Activate Ability: Haste 2 AND halves attack speed attribute (1.6 to 0.8)
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 12 * 20, 1, false, false, false));
            
            ItemMeta meta = hand.getItemMeta();
            meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                NamespacedKey.fromString("strafeweaver:speed_ability"), 0.8, 
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            hand.setItemMeta(meta);

            player.sendMessage(ChatColor.YELLOW + "Strafeweaver Overdrive! (12s)");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 2.0f);

            // Revert after 12 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (manager.isStrafeweaver(hand)) {
                        ItemMeta m = hand.getItemMeta();
                        m.removeAttributeModifier(Attribute.ATTACK_SPEED);
                        m.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                            NamespacedKey.fromString("strafeweaver:speed"), 1.6, 
                            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
                        hand.setItemMeta(m);
                        player.sendMessage(ChatColor.GRAY + "Overdrive faded.");
                    }
                }
            }.runTaskLater(plugin, 12 * 20);

            abilityCooldowns.put(player.getUniqueId(), now + cooldownMs);
            return true;
        }

        return false;
    }
}
