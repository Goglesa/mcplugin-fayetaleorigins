package com.example.fayetaleorigin.golem;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;
import java.util.Set;

public class PickaxeWeaknessAbility implements VisibleAbility, Listener {

    private static final double DAMAGE_MULTIPLIER = 1.5; 
    private static final Set<Material> PICKAXE_MATERIALS = Set.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    );

    public PickaxeWeaknessAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "pickaxe_weakness");
    }

    @Override
    public String description() {
        return "Your stone form is vulnerable to pickaxes.";
    }

    @Override
    public String title() {
        return "Pickaxe Vulnerability";
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        runForAbility(victim, p -> {
            ItemStack weapon = damager.getInventory().getItemInMainHand();
            if (weapon != null && PICKAXE_MATERIALS.contains(weapon.getType())) {
                double originalDamage = event.getDamage();
                event.setDamage(originalDamage * DAMAGE_MULTIPLIER);
            }
        });
    }
}