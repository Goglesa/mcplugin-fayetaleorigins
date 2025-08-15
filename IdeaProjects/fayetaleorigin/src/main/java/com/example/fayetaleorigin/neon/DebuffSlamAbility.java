package com.example.fayetaleorigin.neon;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.CooldownAbility;
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.cooldowns.Cooldowns;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector; // <-- Import Vector
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class DebuffSlamAbility implements VisibleAbility, Listener, CooldownAbility {

    private static final double SLAM_RADIUS = 8.0;
    private static final int COOLDOWN_TICKS = 20 * 30;
    private static final int DEBUFF_DURATION_TICKS = 20 * 8;
    private static final double KNOCKBACK_STRENGTH = 2; // Added knockback strength
    private static final NamespacedKey DEBUFF_ITEM_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "debuff_slam_item");

    public DebuffSlamAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "debuff_slam");
    }

    @Override
    public String description() {
        // Updated description to mention knockback
        return "Right-click while holding 'Nature's Blessing' to release a debilitating wave, poisoning, slowing, and knocking back nearby entities (30s Cooldown). Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "Debilitating Slam";
    }

    @Override
    public Cooldowns.CooldownInfo getCooldownInfo() {
        return new Cooldowns.CooldownInfo(COOLDOWN_TICKS, "potion_action");
    }

    private boolean isDebuffItem(ItemStack item) {
        if (item == null || item.getType() != Material.VINE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(DEBUFF_ITEM_KEY, PersistentDataType.BYTE);
    }

    public ItemStack createDebuffItem() {
        ItemStack item = new ItemStack(Material.VINE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Nature's Blessing").color(NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Right-click to release a debilitating wave.").color(NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(DEBUFF_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    // --- Give Item Logic ---
    private void giveItemIfNotPresent(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isDebuffItem(item)) {
                return;
            }
        }
        ItemStack debuffItem = createDebuffItem();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(debuffItem);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), debuffItem);
            player.sendMessage(Component.text("Your Nature's Blessing couldn't fit in your inventory and was dropped!").color(NamedTextColor.YELLOW));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        runForAbility(player, this::giveItemIfNotPresent);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> {
            runForAbility(player, this::giveItemIfNotPresent);
        }, 1L);
    }

    // --- Prevent Item Loss ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isDebuffItem(event.getItemDrop().getItemStack())) {
            runForAbility(event.getPlayer(), p -> {
                event.setCancelled(true);
                p.sendMessage(Component.text("Nature's Blessing cannot be dropped.").color(NamedTextColor.RED));
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        runForAbility(event.getEntity(), p -> {
            event.getDrops().removeIf(this::isDebuffItem);
        });
    }

    // --- Activate Ability ---
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();

        if (isDebuffItem(heldItem)) {
            event.setCancelled(true);
            runForAbility(player, p -> {
                if (this.hasCooldown(p)) {
                    return;
                }
                this.setCooldown(p);
                performDebuffSlam(p);
            });
        }
    }

    // --- Slam Logic ---
    private void performDebuffSlam(Player player) {
        Location slamCenter = player.getLocation();
        player.getWorld().playSound(slamCenter, Sound.ENTITY_WITCH_THROW, 1.0f, 0.8f);
        player.getWorld().playSound(slamCenter, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.2f);

        try {

            player.getWorld().spawnParticle(Particle.EFFECT, slamCenter, 30, 1.5, 0.5, 1.5, 0);
        } catch (Exception e) {
            FayetaleOrigins.getInstance().getLogger().warning("Failed to spawn debuff slam particles: " + e.getMessage());
        }

        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
                slamCenter, SLAM_RADIUS, SLAM_RADIUS, SLAM_RADIUS,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())
        );

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity livingEntity) {
                // Apply Debuffs
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DEBUFF_DURATION_TICKS, 3));
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, DEBUFF_DURATION_TICKS, 10));

                // Apply Knockback (similar to Golem Slam)
                Vector direction = livingEntity.getLocation().toVector().subtract(slamCenter.toVector()).normalize();
                direction.setY(Math.max(0.3, direction.getY() * 0.5 + 0.3)); // Add slight upward push
                livingEntity.setVelocity(direction.multiply(KNOCKBACK_STRENGTH));
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}