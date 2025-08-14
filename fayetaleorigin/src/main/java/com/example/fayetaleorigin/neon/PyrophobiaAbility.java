package com.example.fayetaleorigin.neon;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit; 

import java.util.EnumSet;
import java.util.Set;

public class PyrophobiaAbility implements VisibleAbility, Listener {

    private static final double DAMAGE_MULTIPLIER = 1.5; 
    private static final Set<DamageCause> FIRE_DAMAGE_CAUSES = EnumSet.of(
            DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA, DamageCause.HOT_FLOOR 
    );

    public PyrophobiaAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "pyrophobia");
    }

    @Override
    public String description() {
        return "Fire and heat affect you more severely.";
    }

    @Override
    public String title() {
        return "Pyrophobia";
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (FIRE_DAMAGE_CAUSES.contains(event.getCause())) {
            runForAbility(player, p -> {
                double originalDamage = event.getDamage();
                event.setDamage(originalDamage * DAMAGE_MULTIPLIER);
            });
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}