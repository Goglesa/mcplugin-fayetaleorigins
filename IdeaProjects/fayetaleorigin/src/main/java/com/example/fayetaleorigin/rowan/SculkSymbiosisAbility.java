package com.example.fayetaleorigin.rowan;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle; // <-- Import Particle
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SculkSymbiosisAbility implements VisibleAbility, Listener {

    private final Map<UUID, Boolean> isEmpowered = new HashMap<>();
    private static final int CHECK_RADIUS = 2;
    private static final int EFFECT_AMPLIFIER = 1; // Strength II / Regen II
    private static final int EFFECT_DURATION = 60;
    private static final int PARTICLE_COUNT = 1;
    private static final double PARTICLE_OFFSET = 0.4;

    private static final Set<Material> SCULK_MATERIALS = EnumSet.of(
            Material.SCULK, Material.SCULK_VEIN, Material.SCULK_CATALYST,
            Material.SCULK_SHRIEKER, Material.SCULK_SENSOR
    );

    private static final PotionEffect STRENGTH_EFFECT = new PotionEffect(
            PotionEffectType.STRENGTH, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);
    private static final PotionEffect REGEN_EFFECT = new PotionEffect(
            PotionEffectType.REGENERATION, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);

    public SculkSymbiosisAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "sculk_symbiosis");
    }

    @Override
    public String description() {
        return "Being near sculk empowers you, granting passive Strength II and Regeneration II and causing you to emit sculk particles.";
    }

    @Override
    public String title() {
        return "Sculk Symbiosis";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        // Check every second (20 ticks)
        if (event.getTickNumber() % 20 == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                runForAbility(player, this::updateSymbiosis);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeEmpowerment(event.getPlayer(), "Player quit");
        isEmpowered.remove(event.getPlayer().getUniqueId());
    }

    private void updateSymbiosis(Player player) {
        UUID playerId = player.getUniqueId();
        boolean nearSculk = isNearSculk(player.getLocation());
        boolean currentlyEmpowered = isEmpowered.getOrDefault(playerId, false);

        if (nearSculk) {
            if (!currentlyEmpowered) {
                player.addPotionEffect(STRENGTH_EFFECT);
                player.addPotionEffect(REGEN_EFFECT);
                isEmpowered.put(playerId, true);
            } else {
                player.addPotionEffect(STRENGTH_EFFECT);
                player.addPotionEffect(REGEN_EFFECT);
            }
            // Spawn particles if near sculk (regardless of whether state changed this tick)
            player.getWorld().spawnParticle(
                    Particle.SCULK_SOUL,
                    player.getLocation().add(0, 1, 0), // Spawn near player center
                    PARTICLE_COUNT,
                    PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET,
                    0.01
            );
        } else {
            if (currentlyEmpowered) {
                removeEmpowerment(player, "Moved away from sculk");
            }
        }
    }

    private boolean isNearSculk(Location location) {
        int px = location.getBlockX();
        int py = location.getBlockY();
        int pz = location.getBlockZ();
        World world = location.getWorld();
        if (world == null) return false;

        for (int x = -CHECK_RADIUS; x <= CHECK_RADIUS; x++) {
            for (int y = -CHECK_RADIUS; y <= CHECK_RADIUS; y++) {
                for (int z = -CHECK_RADIUS; z <= CHECK_RADIUS; z++) {
                    Block block = world.getBlockAt(px + x, py + y, pz + z);
                    if (SCULK_MATERIALS.contains(block.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void removeEmpowerment(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        if (isEmpowered.getOrDefault(playerId, false)) {
            if(player.hasPotionEffect(PotionEffectType.STRENGTH)) player.removePotionEffect(PotionEffectType.STRENGTH);
            if(player.hasPotionEffect(PotionEffectType.REGENERATION)) player.removePotionEffect(PotionEffectType.REGENERATION);
            isEmpowered.put(playerId, false);
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}