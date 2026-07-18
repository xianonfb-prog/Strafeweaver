package com.strafeweaver;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

public class StrafeweaverManager {

    private final StrafeweaverPlugin plugin;
    private final NamespacedKey isStrafeKey;
    private final NamespacedKey uniqueIdKey;
    private final NamespacedKey speedLevelKey;
    
    private String trueStrafeUUID = null;

    public StrafeweaverManager(StrafeweaverPlugin plugin) {
        this.plugin = plugin;
        this.isStrafeKey = new NamespacedKey(plugin, "is_strafeweaver");
        this.uniqueIdKey = new NamespacedKey(plugin, "unique_id");
        this.speedLevelKey = new NamespacedKey(plugin, "speed_level");

        plugin.saveDefaultConfig();
        if (plugin.getConfig().contains("true_strafeweaver_uuid")) {
            trueStrafeUUID = plugin.getConfig().getString("true_strafeweaver_uuid");
        }
    }

    public ItemStack createStrafeweaver() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Strafeweaver");
        meta.setLore(Arrays.asList(
            ChatColor.DARK_GRAY + "Unique Artifact",
            "",
            ChatColor.GRAY + "A blade forged for the relentless.",
            "",
            ChatColor.GREEN + "Passive: " + ChatColor.GRAY + "Permanent Speed II when held.",
            ChatColor.GREEN + "Combo: " + ChatColor.GRAY + "Hit 3 times without taking damage to level up.",
            ChatColor.YELLOW + "Ability: " + ChatColor.GRAY + "/strafeweaver ability"
        ));

        // FIXED: Using GENERIC_ attributes to guarantee compilation
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
            UUID.randomUUID(), "strafeweaver_damage", 8.0, AttributeModifier.Operation.ADD_NUMBER));
            
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
            UUID.randomUUID(), "strafeweaver_speed", 1.6, AttributeModifier.Operation.ADD_NUMBER));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(isStrafeKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(speedLevelKey, PersistentDataType.INTEGER, 0);
        
        String newUUID = UUID.randomUUID().toString();
        pdc.set(uniqueIdKey, PersistentDataType.STRING, newUUID);

        sword.setItemMeta(meta);
        
        trueStrafeUUID = newUUID;
        plugin.getConfig().set("true_strafeweaver_uuid", newUUID);
        plugin.saveConfig();
        
        return sword;
    }

    public boolean isStrafeweaver(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(isStrafeKey);
    }

    public int getSpeedLevel(ItemStack item) {
        if (!isStrafeweaver(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(speedLevelKey, PersistentDataType.INTEGER, 0);
    }

    public void setSpeedLevel(ItemStack item, int level) {
        if (!isStrafeweaver(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(speedLevelKey, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
    }

    public boolean doesStrafeExistInWorld() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : p.getInventory().getContents()) {
                if (isStrafeweaver(item)) return true;
            }
        }
        return false;
    }
}
