package com.example.fayetaleorigin.antling;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent; // Requires Paper server
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class QuickAbility implements VisibleAbility, Listener {

    public QuickAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "quick");
    }

    @Override
    public String description() {
        return "Your body allows you to move faster.";
    }

    @Override
    public String title() {
        return "Quickened";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        if (event.getTickNumber() % 40 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                runForAbility(p, player -> {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.SPEED,
                            60,
                            0,
                            true,
                            false,
                            true
                    ));
                });
            }
        }
    }


}