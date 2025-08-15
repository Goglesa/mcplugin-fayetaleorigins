package com.example.fayetaleorigin.golem;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

public class StoneSkinAbility implements VisibleAbility, Listener {

    public StoneSkinAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "stone_skin");
    }

    @Override
    public String description() {
        return "Your stone body provides natural protection.";
    }

    @Override
    public String title() {
        return "Stone Skin";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        if (event.getTickNumber() % 40 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                runForAbility(p, player -> {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.RESISTANCE,
                            60, // Duration
                            0,  // Amplifier (Resistance I)
                            true, false, true
                    ));
                });
            }
        }
    }
}