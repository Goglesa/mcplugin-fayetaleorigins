package com.example.fayetaleorigin.rowan;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

public class SculkPresenceAbility implements VisibleAbility, Listener {

    private static final int PARTICLE_INTERVAL_TICKS = 10; 
    private static final int PARTICLE_COUNT = 1;
    private static final double PARTICLE_OFFSET = 0.5; 

    public SculkPresenceAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "sculk_presence");
    }

    @Override
    public String description() {
        return "Your body constantly sheds sculk particles.";
    }

    @Override
    public String title() {
        return "Sculk Presence";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {

        if (event.getTickNumber() % PARTICLE_INTERVAL_TICKS == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                runForAbility(player, p -> {
                    p.getWorld().spawnParticle(
                            Particle.SCULK_SOUL,
                            p.getLocation().add(0, 1, 0),
                            PARTICLE_COUNT, 
                            PARTICLE_OFFSET, PARTICLE_OFFSET, PARTICLE_OFFSET, 
                            0.01 
                    );
                });
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}