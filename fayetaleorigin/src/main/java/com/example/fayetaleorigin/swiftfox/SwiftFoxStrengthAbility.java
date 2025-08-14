package com.example.fayetaleorigin.swiftfox;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent; 
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; 
import org.bukkit.event.Listener;  
import org.bukkit.event.player.PlayerQuitEvent; 
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect; 
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;


public class SwiftFoxStrengthAbility implements VisibleAbility, Listener {

    private static final int EFFECT_AMPLIFIER = 0; 
    private static final int EFFECT_DURATION = 60;

    private static final PotionEffect STRENGTH_EFFECT = new PotionEffect(
            PotionEffectType.STRENGTH, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);

    public SwiftFoxStrengthAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "swift_fox_strength");
    }

    @Override
    public String description() {
        return "You are naturally stronger, dealing more damage with your attacks.";
    }

    @Override
    public String title() {
        return "Sharp Bite";
    }

 

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
 
        if (event.getTickNumber() % 40 == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
              
                runForAbility(player, p -> p.addPotionEffect(STRENGTH_EFFECT));
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
            PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.STRENGTH);
            if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER && existingEffect.getDuration() <= EFFECT_DURATION) {
                player.removePotionEffect(PotionEffectType.STRENGTH);
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}