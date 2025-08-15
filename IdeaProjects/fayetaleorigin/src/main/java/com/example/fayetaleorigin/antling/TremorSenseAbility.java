package com.example.fayetaleorigin.antling;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.Collection;

public class TremorSenseAbility implements VisibleAbility, Listener {

    private static final double SENSE_RADIUS = 20.0; // How far the sense reaches (blocks)
    private static final int GLOW_DURATION = 40;

    // No constructor needed

    @Override
    public @NotNull Key getKey() {
        // Assumes Main.getInstance() is available and working
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "tremor_sense"); // New key
    }

    @Override
    public String description() {
        return "While sneaking, you can briefly sense nearby creatures through the earth.";
    }

    @Override
    public String title() {
        return "Tremor Sense";
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        // Trigger only when starting to sneak
        if (event.isSneaking()) {
            // Check if the player has this ability active
            runForAbility(player, p -> {
                // Find nearby living entities (excluding self and other players)
                Collection<Entity> nearbyEntities = p.getWorld().getNearbyEntities(
                        p.getLocation(),
                        SENSE_RADIUS, SENSE_RADIUS, SENSE_RADIUS,
                        entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !entity.getUniqueId().equals(p.getUniqueId())
                );

                // Apply glowing effect
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.addPotionEffect(new PotionEffect(
                                PotionEffectType.GLOWING,
                                GLOW_DURATION,
                                0, // Amplifier
                                true, // Ambient
                                false, // No particles
                                false  // Don't show icon (optional)
                        ));
                    }
                }
            });
        }
    }

    // Assumes OR handles listener registration automatically
    // Removed initialize method

    // Removed getPlugin helper method
}