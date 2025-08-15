package com.example.fayetaleorigin.golem;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class OreEaterAbility implements VisibleAbility, Listener {

    private final Map<Player, Integer> lastInteractedTicks = new HashMap<>();

    private static final Set<Material> EDIBLE_ORES = Set.of(
            Material.COAL, Material.RAW_COPPER, Material.RAW_IRON, Material.RAW_GOLD,
            Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND
    );

    private static final List<PotionEffectType> POSITIVE_EFFECTS = List.of(
            PotionEffectType.SPEED, PotionEffectType.REGENERATION, PotionEffectType.HASTE,
            PotionEffectType.JUMP_BOOST, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.WATER_BREATHING,
            PotionEffectType.ABSORPTION, PotionEffectType.SATURATION
    );

    public OreEaterAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "ore_eater");
    }

    @Override
    public String description() {
        return "You can only consume raw ores and minerals for sustenance. Diamonds grant temporary buffs.";
    }

    @Override
    public String title() {
        return "Mineral Diet";
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !event.getAction().isRightClick()) return;

        Material type = item.getType();

        runForAbility(player, p -> {
            if (lastInteractedTicks.getOrDefault(p, -1) == Bukkit.getCurrentTick()) return;

            if (EDIBLE_ORES.contains(type)) {
                handleOreConsumption(p, item, type, event);
            } else if (type.isEdible() &&
                    type != Material.POTION &&
                    type != Material.MILK_BUCKET &&
                    type != Material.GOLDEN_APPLE && // <-- Allow Golden Apple
                    type != Material.ENCHANTED_GOLDEN_APPLE) { // <-- Allow Enchanted Golden Apple
                // It's other vanilla food, prevent eating
                // p.sendActionBar(Component.text("Your body cannot process this food.").color(NamedTextColor.GRAY));
                event.setCancelled(true);
                lastInteractedTicks.put(p, Bukkit.getCurrentTick());
            }
            // Let Golden Apples, Enchanted Golden Apples, Potions, Milk proceed normally
        });
    }

    private void handleOreConsumption(Player player, ItemStack item, Material type, PlayerInteractEvent event) {
        int food = 0;
        float saturation = 0f;
        PotionEffect randomEffect = null;

        switch (type) {
            case COAL:           food = 1; saturation = 0.2f; break;
            case RAW_COPPER:     food = 1; saturation = 0.3f; break;
            case RAW_IRON:       food = 2; saturation = 0.4f; break;
            case RAW_GOLD:       food = 2; saturation = 1.0f; break;
            case COPPER_INGOT:   food = 2; saturation = 0.5f; break;
            case IRON_INGOT:     food = 4; saturation = 0.6f; break;
            case GOLD_INGOT:     food = 4; saturation = 1.2f; break;
            case DIAMOND:
                food = 6;
                saturation = 1.2f;
                if (!POSITIVE_EFFECTS.isEmpty()) {
                    PotionEffectType randomEffectType = POSITIVE_EFFECTS.get(ThreadLocalRandom.current().nextInt(POSITIVE_EFFECTS.size()));
                    int amplifier = 0;
                    int effectDuration = (randomEffectType.equals(PotionEffectType.SATURATION)) ? 1 : (20 * 180);
                    randomEffect = new PotionEffect(randomEffectType, effectDuration, amplifier, true, true, true);
                }
                break;
            default:
                return;
        }

        if (player.getFoodLevel() >= 20) {
            event.setCancelled(true);
            return;
        }

        lastInteractedTicks.put(player, Bukkit.getCurrentTick());
        player.swingMainHand();
        item.setAmount(item.getAmount() - 1);

        if (randomEffect != null) {
            player.addPotionEffect(randomEffect);
        }

        player.setFoodLevel(Math.min(player.getFoodLevel() + food, 20));
        player.setSaturation(Math.min(player.getSaturation() + saturation, (float) player.getFoodLevel()));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        event.setCancelled(true);
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}