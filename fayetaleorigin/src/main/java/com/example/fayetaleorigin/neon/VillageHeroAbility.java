package com.example.fayetaleorigin.neon;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

public class VillageHeroAbility implements VisibleAbility, Listener {

    private static final int EFFECT_AMPLIFIER = 4; 
    private static final int EFFECT_DURATION = 60;

    public VillageHeroAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "village_hero");
    }

    @Override
    public String description() {
        return "Villagers see you as a hero, granting you favorable trades.";
    }

    @Override
    public String title() {
        return "Village Hero";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {

        if (event.getTickNumber() % 40 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                runForAbility(p, player -> {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.HERO_OF_THE_VILLAGE,
                            EFFECT_DURATION,
                            EFFECT_AMPLIFIER,
                            true, 
                            false, 
                            true  
                    ));
                });
            }
        }
    }



    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}