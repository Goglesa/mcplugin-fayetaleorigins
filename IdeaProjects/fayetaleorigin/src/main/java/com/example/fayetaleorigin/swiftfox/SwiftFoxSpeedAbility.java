package com.example.fayetaleorigin.swiftfox;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SwiftFoxSpeedAbility implements VisibleAbility, Listener {

    private static final double CHECK_RANGE = 10.0;
    private static final int REQUIRED_PLAYERS = 1;
    private static final int EFFECT_AMPLIFIER = 8; // Speed IX for approx. 12m/s
    private static final int EFFECT_DURATION = 60; // Apply for 3 seconds, refresh frequently

    private final Map<UUID, Boolean> hasSpeedBuff = new HashMap<>();

    private static final PotionEffect SPEED_BUFF = new PotionEffect(
            PotionEffectType.SPEED, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);

    public SwiftFoxSpeedAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "swift_fox_speed");
    }

    @Override
    public String description() {
        return "You run significantly faster (approx. 12m/s) when other players are nearby.";
    }

    @Override
    public String title() {
        return "Swift Pursuit";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        if (event.getTickNumber() % 20 == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                runForAbility(player, this::updateSpeedBuff);
            }
        }
    }

    private void updateSpeedBuff(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentlyBuffed = hasSpeedBuff.getOrDefault(playerId, false);

        List<Entity> nearbyEntities = player.getNearbyEntities(CHECK_RANGE, CHECK_RANGE, CHECK_RANGE);
        long nearbyPlayerCount = nearbyEntities.stream()
                .filter(entity -> entity instanceof Player && !entity.getUniqueId().equals(playerId))
                .count();

        boolean shouldBeBuffed = (nearbyPlayerCount >= REQUIRED_PLAYERS);

        if (shouldBeBuffed) {
            player.addPotionEffect(SPEED_BUFF);
            if (!currentlyBuffed) {
                hasSpeedBuff.put(playerId, true);
            }
        } else {
            if (currentlyBuffed) {
                if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                    PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.SPEED);
                    if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER) {
                        player.removePotionEffect(PotionEffectType.SPEED);
                    }
                }
                hasSpeedBuff.put(playerId, false);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (hasSpeedBuff.getOrDefault(playerId, false)) {
            PotionEffect existingEffect = event.getPlayer().getPotionEffect(PotionEffectType.SPEED);
            if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER) {
                event.getPlayer().removePotionEffect(PotionEffectType.SPEED);
            }
        }
        hasSpeedBuff.remove(playerId);
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}