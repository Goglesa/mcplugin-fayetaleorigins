package com.example.fayetaleorigin.neon;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent; 
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

    private static final int EFFECT_AMPLIFIER = 1; 
    private static final int EFFECT_DURATION = 60; 

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

        if (event.getTickNumber() % 20 != 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            runForAbility(player, this::updateDebility);
        }
    }

    private void updateDebility(Player player) {
        boolean isNight = !player.getWorld().isDayTime(); 

        if (isNight) {

            player.addPotionEffect(WEAKNESS_EFFECT);
        } else {

            if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {

                PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.WEAKNESS);
                if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER) {
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.WEAKNESS);
            if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER) {
                player.removePotionEffect(PotionEffectType.WEAKNESS);
            }
        }
    }



    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}