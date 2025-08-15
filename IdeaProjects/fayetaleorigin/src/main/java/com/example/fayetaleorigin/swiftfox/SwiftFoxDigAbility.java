package com.example.fayetaleorigin.swiftfox;

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

public class SwiftFoxDigAbility implements VisibleAbility, Listener {

    private static final int EFFECT_AMPLIFIER = 1; // Haste II (amplifier is level - 1)
    private static final int EFFECT_DURATION = 60; // Apply for 3 seconds, refresh

    private static final PotionEffect HASTE_EFFECT = new PotionEffect(
            PotionEffectType.HASTE, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);

    public SwiftFoxDigAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "swift_fox_dig");
    }

    @Override
    public String description() {
        return "You dig much faster (Haste II).";
    }

    @Override
    public String title() {
        return "Quick Paws";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        if (event.getTickNumber() % 40 == 0) { // Refresh roughly every 2 seconds
            for (Player player : Bukkit.getOnlinePlayers()) {
                runForAbility(player, p -> p.addPotionEffect(HASTE_EFFECT));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.HASTE);
            if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER) {
                player.removePotionEffect(PotionEffectType.HASTE);
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}