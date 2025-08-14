package com.example.fayetaleorigin.witherfox;


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


public class StrengthInNumbersAbility implements VisibleAbility, Listener {

    
    private static final double DEFAULT_RANGE = 8.0;
    private static final int DEFAULT_REQUIRED_PLAYERS = 2; 
    private static final int EFFECT_AMPLIFIER = 0; 
    private static final int EFFECT_DURATION = 60; 

   
    private final Map<UUID, Boolean> hasStrengthBuff = new HashMap<>();

    private static final PotionEffect STRENGTH_BUFF = new PotionEffect(
            PotionEffectType.STRENGTH, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);


    public StrengthInNumbersAbility() {}

   

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
       
        if (event.getTickNumber() % 20 == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
            
                runForAbility(player, this::updateStrengthBuff);
            }
        }
    }

  
    private void updateStrengthBuff(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentlyBuffed = hasStrengthBuff.getOrDefault(playerId, false);

       
        List<Entity> entities = player.getNearbyEntities(DEFAULT_RANGE, DEFAULT_RANGE, DEFAULT_RANGE);
        long nearbyPlayerCount = entities.stream()
                .filter(entity -> entity instanceof Player && !entity.getUniqueId().equals(playerId))
                .count();

        boolean shouldBeBuffed = (nearbyPlayerCount >= DEFAULT_REQUIRED_PLAYERS);

        if (shouldBeBuffed) {
           
            player.addPotionEffect(STRENGTH_BUFF);
            if (!currentlyBuffed) {
                hasStrengthBuff.put(playerId, true); 
            }
        } else {

            if (currentlyBuffed) {

                if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
                    player.removePotionEffect(PotionEffectType.STRENGTH);
                }
                hasStrengthBuff.put(playerId, false); 
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (hasStrengthBuff.getOrDefault(playerId, false)) {
            event.getPlayer().removePotionEffect(PotionEffectType.STRENGTH);
        }
        hasStrengthBuff.remove(playerId);
    }



    @Override
    public String description() {
        return "You gain Strength I when near " + DEFAULT_REQUIRED_PLAYERS + " or more other players.";
    }

    @Override
    public String title() {
        return "Strength in Numbers";
    }

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "strength_in_numbers");
    }


    @Override
    public void initialize(JavaPlugin plugin) {

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
