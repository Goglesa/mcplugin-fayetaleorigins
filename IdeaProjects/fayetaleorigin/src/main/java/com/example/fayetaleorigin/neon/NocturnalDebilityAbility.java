package com.example.fayetaleorigin.neon;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent; // Requires Paper
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

public class NocturnalDebilityAbility implements VisibleAbility, Listener {

    private static final int EFFECT_AMPLIFIER = 1; // Weakness II
    private static final int EFFECT_DURATION = 60; // Apply for 3 seconds, refresh

    private static final PotionEffect WEAKNESS_EFFECT = new PotionEffect(
            PotionEffectType.WEAKNESS, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);

    public NocturnalDebilityAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "nocturnal_debility");
    }

    @Override
    public String description() {
        return "You grow weaker under the veil of night.";
    }

    @Override
    public String title() {
        return "Night's Toll";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        // Check every second (20 ticks)
        if (event.getTickNumber() % 20 != 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            runForAbility(player, this::updateDebility);
        }
    }

    private void updateDebility(Player player) {
        boolean isNight = !player.getWorld().isDayTime(); // isDayTime() is true from tick 0 to 12999

        if (isNight) {
            // Apply Weakness if it's night
            player.addPotionEffect(WEAKNESS_EFFECT);
        } else {
            // Remove Weakness if it's day and they have it from this ability
            if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                // Check if the existing effect matches what this ability applies
                PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.WEAKNESS);
                if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER) {
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Ensure effect is removed on quit if applied by this ability
        Player player = event.getPlayer();
        if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.WEAKNESS);
            if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER) {
                player.removePotionEffect(PotionEffectType.WEAKNESS);
            }
        }
    }

    // No onAbilityRemove as it's not overridable

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}