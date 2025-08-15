package com.example.fayetaleorigin.misc;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent; // Import Paper event
import org.bukkit.Bukkit; // Import Bukkit
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
// Removed JavaPlugin import as initialize is removed
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WeaknessAbility implements VisibleAbility, Listener {



    @Override
    public @NotNull Key getKey() {

        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "weakness");
    }

    @Override
    public String description() {
        return "You have constant weakness, making you deal less damage.";
    }

    @Override
    public String title() {
        return "Weakness";
    }


    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {

        if (event.getTickNumber() % 40 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {

                runForAbility(p, player -> {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.WEAKNESS,
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