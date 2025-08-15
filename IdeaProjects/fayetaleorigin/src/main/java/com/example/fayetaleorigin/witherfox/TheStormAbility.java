package com.example.fayetaleorigin.witherfox;

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
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TheStormAbility implements VisibleAbility, Listener, CooldownAbility {

    private static final double EFFECT_RADIUS = 10.0;
    private static final int BLINDNESS_DURATION = 20 * 15; // 15 seconds
    private static final int SLOWNESS_DURATION = 20 * 10;  // 10 seconds
    private static final int INSTANT_DAMAGE_AMPLIFIER = 0; // Level I
    private static final int SLOWNESS_AMPLIFIER = 1;       // Slowness II
    private static final int BLINDNESS_AMPLIFIER = 0;      // Blindness I
    private static final int COOLDOWN_TICKS = 20 * 75;     // 1 minute 15 seconds
    private static final NamespacedKey STORM_ITEM_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "the_storm_item");

    public TheStormAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "the_storm");
    }

    @Override
    public String description() {
        return "Right-click the Jigsaw block to unleash a storm, blinding, slowing and harming nearby entities (1m 15s Cooldown). Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "The Storm";
    }

    @Override
    public Cooldowns.CooldownInfo getCooldownInfo() {
        // Icon name should match your texture file, e.g., the_storm.png
        return new Cooldowns.CooldownInfo(COOLDOWN_TICKS, "the_storm");
    }

    // --- Item Handling ---
    private boolean isStormItem(ItemStack item) {
        if (item == null || item.getType() != Material.JIGSAW || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(STORM_ITEM_KEY, PersistentDataType.BYTE);
    }

    private ItemStack createStormItem() {
        ItemStack item = new ItemStack(Material.JIGSAW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Eye of the Storm").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Right-click to unleash the storm.").color(NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.LURE, 1, true); // Glow effect
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(STORM_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveItemIfNotPresent(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isStormItem(item)) return;
        }
        ItemStack stormItem = createStormItem();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stormItem);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), stormItem);
            player.sendMessage(Component.text("Your Eye of the Storm couldn't fit and was dropped!").color(NamedTextColor.YELLOW));
        }
    }

    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) { runForAbility(event.getPlayer(), this::giveItemIfNotPresent); }
    @EventHandler public void onPlayerRespawn(PlayerRespawnEvent event) { Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> runForAbility(event.getPlayer(), this::giveItemIfNotPresent), 1L); }
    @EventHandler(priority = EventPriority.HIGH) public void onDropItem(PlayerDropItemEvent event) { if (isStormItem(event.getItemDrop().getItemStack())) { runForAbility(event.getPlayer(), p -> event.setCancelled(true)); } }
    @EventHandler(priority = EventPriority.HIGH) public void onPlayerDeath(PlayerDeathEvent event) { runForAbility(event.getEntity(), p -> event.getDrops().removeIf(this::isStormItem)); }

    // --- Activation ---
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();

        if (isStormItem(heldItem)) {
            event.setCancelled(true);
            runForAbility(player, p -> {
                if (this.hasCooldown(p)) return;
                this.setCooldown(p);
                performStorm(p);
            });
        }
    }

    // --- Storm Logic ---
    private void performStorm(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();

        // Sounds and Particles
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        world.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.5f);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center, 100, EFFECT_RADIUS / 2.0, 1.0, EFFECT_RADIUS / 2.0, 0.1);



        // Apply effects to nearby entities
        Collection<Entity> nearbyEntities = world.getNearbyEntities(
                center, EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())
        );

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER));
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER));
                // Apply Instant Damage I (amplifier 0)
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, INSTANT_DAMAGE_AMPLIFIER));
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}