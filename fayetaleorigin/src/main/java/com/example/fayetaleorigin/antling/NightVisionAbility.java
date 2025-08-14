package com.example.fayetaleorigin.antling;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.events.PlayerSwapOriginEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class NightVisionAbility implements VisibleAbility, Listener {


    private static final int NIGHT_VISION_DURATION = 10 * 60 * 20;

    public NightVisionAbility() {}

    @Override
    public @NotNull Key getKey() {

        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "nightvisionability");
    }

    @Override
    public String description() {
        return "Your eyes have adapted to the darkness, granting you permanent Night Vision.";
    }

    @Override
    public String title() {
        return "Night Sight";
    }

    @EventHandler
    public void onOriginSwap(PlayerSwapOriginEvent event) {

        if (event.getOldOrigin() != null && event.getOldOrigin().hasAbility(getKey())) {
            event.getPlayer().removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);


        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {

                    runForAbility(player, p -> {

                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.NIGHT_VISION,
                                NIGHT_VISION_DURATION,
                                0,
                                true,
                                false,
                                true  
                        ));
                    });
                }
            }
        }.runTaskTimer(plugin, 0L, 200L); 
    }
}