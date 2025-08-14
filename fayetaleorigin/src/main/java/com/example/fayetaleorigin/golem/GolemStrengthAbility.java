package com.example.fayetaleorigin.golem;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.Bukkit; 
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin; 
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

public class GolemStrengthAbility implements VisibleAbility, Listener {

    public GolemStrengthAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "golem_strength");
    }

    @Override
    public String description() {
        return "Your punches hit harder when your hands are empty.";
    }

    @Override
    public String title() {
        return "Heavy Hands";
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        runForAbility(damager, player -> {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand == null || mainHand.getType() == Material.AIR) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20,
                        0, 
                        true, false, false 
                ));
            }
        });
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}