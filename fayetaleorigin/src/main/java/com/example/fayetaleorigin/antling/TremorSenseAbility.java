package com.example.fayetaleorigin.antling;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.Collection;

public class TremorSenseAbility implements VisibleAbility, Listener {

    private static final double SENSE_RADIUS = 20.0; 
    private static final int GLOW_DURATION = 40;



    @Override
    public @NotNull Key getKey() {

        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "tremor_sense"); 
    }

    @Override
    public String description() {
        return "While sneaking, you can briefly sense nearby creatures through the earth.";
    }

    @Override
    public String title() {
        return "Tremor Sense";
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking()) {

            runForAbility(player, p -> {

                Collection<Entity> nearbyEntities = p.getWorld().getNearbyEntities(
                        p.getLocation(),
                        SENSE_RADIUS, SENSE_RADIUS, SENSE_RADIUS,
                        entity -> entity instanceof LivingEntity && !(entity instanceof Player) && !entity.getUniqueId().equals(p.getUniqueId())
                );

                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.addPotionEffect(new PotionEffect(
                                PotionEffectType.GLOWING,
                                GLOW_DURATION,
                                0, 
                                true,
                                false, 
                                false  
                        ));
                    }
                }
            });
        }
    }


}