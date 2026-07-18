package com.yourserver.strafeweaver;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrafeweaverListener implements Listener {

    private final StrafeweaverPlugin plugin;
    private final StrafeweaverManager manager;
    private final Map<UUID, Map<UUID, Integer>> comboTracker = new HashMap<>();

    public StrafeweaverListener(StrafeweaverPlugin plugin, StrafeweaverManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ==========================================
    // PERMANENT SPEED II WHEN HELD
    // ==========================================
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());

        if (manager.isStrafeweaver(oldItem)) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
        if (manager.isStrafeweaver(newItem)) {
            // Base Speed II (Amplifier 1)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false, false));
        }
    }

    // ==========================================
    // COMBAT, COMBO & AUTO-CRIT LOGIC
    // ==========================================
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!manager.isStrafeweaver(weapon)) return;

        // ==========================================
        // CHARGED HIT CHECK (Prevents Spam Clicking)
        // ==========================================
        // A fully charged hit is 1.0F. We use 0.95F to ensure it's a real, timed swing.
        if (attacker.getAttackCooldown() < 0.95F) {
            return; // Ignore weak/spam hits. Combo does not increment.
        }
        // ==========================================

        UUID attUUID = attacker.getUniqueId();
        UUID vicUUID = victim.getUniqueId();

        // --- INCREMENT COMBO ---
        comboTracker.putIfAbsent(attUUID, new HashMap<>());
        Map<UUID, Integer> targets = comboTracker.get(attUUID);
        
        int currentHits = targets.getOrDefault(vicUUID, 0) + 1;
        targets.put(vicUUID, currentHits);

        // --- SOUND CUES (Increasing Pitch) ---
        float pitch = 1.0f + ((currentHits - 1) * 0.5f); 
        attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, Math.min(pitch, 2.0f));

        // --- 3-HIT COMBO RESOLUTION ---
        if (currentHits >= 3) {
            targets.put(vicUUID, 0); // Reset combo counter for this target

            // 1. Upgrade Sword Speed Level (Max 6)
            int currentSpeedLevel = manager.getSpeedLevel(weapon);
            if (currentSpeedLevel < 6) {
                int newLevel = currentSpeedLevel + 1;
                manager.setSpeedLevel(weapon, newLevel);
                
                // Apply extra speed (Base Speed II is amp 1. Each upgrade adds +1)
                int totalSpeedAmplifier = 1 + newLevel; 
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, totalSpeedAmplifier, false, false, false));
                attacker.sendMessage(ChatColor.AQUA + "Strafeweaver upgraded to " + ChatColor.YELLOW + "Speed " + (totalSpeedAmplifier + 1) + "!");
            }

            // 2. AUTO CRIT on the 3rd hit (1-tick delay to override vanilla KB)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!victim.isValid()) return;
                    
                    // Sprint knockback math (0.9 horizontal, 0.4 vertical)
                    Vector direction = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
                    direction.multiply(0.9);
                    direction.setY(0.4);
                    victim.setVelocity(direction);
                    
                    // Spawn crit particles & sound
                    victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    // ==========================================
    // RESET LOGIC (If they hit you back)
    // ==========================================
    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        UUID vicUUID = victim.getUniqueId();
        UUID attUUID = attacker.getUniqueId();

        // If the victim was attacking the attacker, reset the victim's combo on the attacker
        if (comboTracker.containsKey(vicUUID)) {
            Map<UUID, Integer> targets = comboTracker.get(vicUUID);
            if (targets.containsKey(attUUID)) {
                targets.put(attUUID, 0); // Reset the combo!
                victim.sendMessage(ChatColor.RED + "Strafeweaver combo interrupted!");
                victim.playSound(victim.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            }
        }
    }
}
