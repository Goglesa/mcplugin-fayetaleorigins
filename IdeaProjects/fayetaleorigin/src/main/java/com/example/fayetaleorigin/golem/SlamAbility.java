package com.example.fayetaleorigin.golem;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.CooldownAbility; // Import CooldownAbility
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.cooldowns.Cooldowns; // Import Cooldowns
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.Collection;


// Implement CooldownAbility alongside VisibleAbility and Listener
public class SlamAbility implements VisibleAbility, Listener, CooldownAbility {

    private static final double SLAM_RADIUS = 5.0;
    private static final double SLAM_DAMAGE = 6.0;
    private static final double KNOCKBACK_STRENGTH = 1.5;
    // Cooldown duration defined in getCooldownInfo now
    // private static final int COOLDOWN_TICKS = 20 * 20;

    // No manual cooldown map needed

    public SlamAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "slam");
    }

    @Override
    public String description() {
        return "Left-click the air to slam the ground, damaging and knocking back nearby entities (20s Cooldown).";
    }

    @Override
    public String title() {
        return "Ground Slam";
    }

    // Implement method required by CooldownAbility
    @Override
    public Cooldowns.CooldownInfo getCooldownInfo() {
        // duration (ticks), unique cooldown ID string (MUST match icon filename without .png)
        // Changed "slam_cooldown" to "split" to match user's icon file "split.png"
        return new Cooldowns.CooldownInfo(400, "split");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR) {
            Player player = event.getPlayer();
            runForAbility(player, p -> {
                // Use hasCooldown() provided by CooldownAbility
                if (this.hasCooldown(p)) {
                    // Cooldown is active, do nothing
                    // Origins Reborn might provide feedback based on CooldownAbility implementation
                    return;
                }

                // Not on cooldown: set cooldown using built-in method, perform slam
                // setCooldown() uses the duration from getCooldownInfo()
                this.setCooldown(p);
                performSlam(p);
                event.setCancelled(true); // Prevent arm swing
            });
        }
    }

    private void performSlam(Player player) {
        Location slamCenter = player.getLocation();
        player.getWorld().playSound(slamCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);

        try {
            player.getWorld().spawnParticle(Particle.CRIT, slamCenter, 30, 1.5, 0.5, 1.5, 0.1);
            player.getWorld().spawnParticle(Particle.valueOf("SMOKE_NORMAL"), slamCenter, 20, 1.0, 0.2, 1.0, 0.05);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, slamCenter.clone().add(0, 0.5, 0), 15, 0.5, 0.5, 0.5, 0.2);
        } catch (IllegalArgumentException e) {
            FayetaleOrigins.getInstance().getLogger().warning("Failed to spawn SMOKE_NORMAL particle by string name: " + e.getMessage());
        } catch (Exception e) {
            FayetaleOrigins.getInstance().getLogger().warning("Failed to spawn slam particles: " + e.getMessage());
        }

        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
                slamCenter, SLAM_RADIUS, SLAM_RADIUS, SLAM_RADIUS,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())
        );

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.damage(SLAM_DAMAGE, player);
                Vector direction = livingEntity.getLocation().toVector().subtract(slamCenter.toVector()).normalize();
                direction.setY(Math.max(0.3, direction.getY() * 0.5 + 0.3));
                livingEntity.setVelocity(direction.multiply(KNOCKBACK_STRENGTH));
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}