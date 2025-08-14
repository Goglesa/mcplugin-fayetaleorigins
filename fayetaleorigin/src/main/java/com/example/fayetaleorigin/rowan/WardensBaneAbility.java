package com.example.fayetaleorigin.rowan;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.CooldownAbility;
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.cooldowns.Cooldowns;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType; 
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;


import java.util.HashMap;

import java.util.List;



public class WardensBaneAbility implements VisibleAbility, Listener, CooldownAbility {

    private static final double BEAM_RANGE = 40.0;
    private static final double BEAM_DAMAGE = 17.0;
    private static final double WARDEN_BONUS_DAMAGE = 8.0;
    private static final int SLOWNESS_DURATION = 20 * 4;
    private static final int BLINDNESS_DURATION = 20 * 6;
    private static final int SLOWNESS_AMPLIFIER = 1;
    private static final int BLINDNESS_AMPLIFIER = 0;
    private static final int COOLDOWN_TICKS = 20 * 35;
    private static final NamespacedKey BANE_ITEM_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "wardens_bane_item");

    public WardensBaneAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "wardens_bane");
    }

    @Override
    public String description() {

        return "Right-click your Purifying Crystal to fire a long-range beam, damaging, slowing and blinding the first entity hit (Bonus vs Wardens, 35s Cooldown). Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "Debilitating Beam";
    }

    @Override
    public Cooldowns.CooldownInfo getCooldownInfo() {
        return new Cooldowns.CooldownInfo(COOLDOWN_TICKS, "wardens_bane");
    }

    // --- Item Handling ---
    private boolean isBaneItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(BANE_ITEM_KEY, PersistentDataType.BYTE);
    }

    private ItemStack createBaneItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Purifying Crystal").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Right-click to unleash a debilitating beam.").color(NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(BANE_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveItemIfNotPresent(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isBaneItem(item)) return;
        }
        ItemStack baneItem = createBaneItem();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(baneItem);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), baneItem);
        }
    }

    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) { runForAbility(event.getPlayer(), this::giveItemIfNotPresent); }
    @EventHandler public void onPlayerRespawn(PlayerRespawnEvent event) { Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> runForAbility(event.getPlayer(), this::giveItemIfNotPresent), 1L); }
    @EventHandler(priority = EventPriority.HIGH) public void onDropItem(PlayerDropItemEvent event) { if (isBaneItem(event.getItemDrop().getItemStack())) { runForAbility(event.getPlayer(), p -> event.setCancelled(true)); } }
    @EventHandler(priority = EventPriority.HIGH) public void onPlayerDeath(PlayerDeathEvent event) { runForAbility(event.getEntity(), p -> event.getDrops().removeIf(this::isBaneItem)); }

    // --- Activation ---
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();
        if (isBaneItem(heldItem)) {
            event.setCancelled(true);
            runForAbility(player, p -> {
                if (this.hasCooldown(p)) return;
                this.setCooldown(p);
                fireBaneBeam(p);
            });
        }
    }


    private void fireBaneBeam(Player player) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        world.playSound(start, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.2f);

        RayTraceResult result = world.rayTraceEntities(start, direction, BEAM_RANGE, 0.5,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId()));

        LivingEntity target = null;
        Location end = start.clone().add(direction.clone().multiply(BEAM_RANGE));

        if (result != null && result.getHitEntity() instanceof LivingEntity hitEntity) {
            target = hitEntity;
            end = target.getEyeLocation();
        }

        spawnBeamParticles(start, end);

        if (target != null) {

            double damage = BEAM_DAMAGE;
            if (target.getType() == EntityType.WARDEN) {
                damage += WARDEN_BONUS_DAMAGE;
            }
            target.damage(damage, player);

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_DURATION, BLINDNESS_AMPLIFIER));
            world.playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        }
    }

    private void spawnBeamParticles(Location start, Location end) {
        World world = start.getWorld();
        if (world == null) return;

        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        double step = 0.3;

        for (double d = 0; d < distance; d += step) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.SCULK_SOUL, particleLoc, 1, 0, 0, 0, 0);
            if (d % (step * 4) < step) {
                try {
                    Location shriekLoc = particleLoc.clone().add(direction.clone().multiply(0.1));
                    world.spawnParticle(Particle.SHRIEK, shriekLoc, 1, 0, 0, 0, 0, 0);
                } catch (Exception e) {
                    world.spawnParticle(Particle.SCULK_CHARGE_POP, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
