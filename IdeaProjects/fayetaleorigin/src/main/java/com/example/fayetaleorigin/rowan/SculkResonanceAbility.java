package com.example.fayetaleorigin.rowan;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.CooldownAbility;
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.cooldowns.Cooldowns;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.HashMap;
import java.util.List;

public class SculkResonanceAbility implements VisibleAbility, Listener, CooldownAbility {

    private static final int BUFF_DURATION = 20 * 8;
    private static final int BUFF_AMPLIFIER = 1;
    private static final int COOLDOWN_TICKS = 20 * 20;
    private static final NamespacedKey RESONANCE_ITEM_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "sculk_resonance_item"); // Changed key name for clarity

    public SculkResonanceAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "sculk_resonance");
    }

    @Override
    public String description() {
        return "Right-click your Resonating Chronometer to gain a burst of Speed II and Haste II for 8 seconds (20s Cooldown). Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "Sculk Resonance";
    }

    @Override
    public Cooldowns.CooldownInfo getCooldownInfo() {
        return new Cooldowns.CooldownInfo(COOLDOWN_TICKS, "sculk_resonance");
    }

    private boolean isResonanceItem(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta()) return false; // Changed Material.COMPASS to Material.CLOCK
        return item.getItemMeta().getPersistentDataContainer().has(RESONANCE_ITEM_KEY, PersistentDataType.BYTE);
    }

    private ItemStack createResonanceItem() {
        ItemStack item = new ItemStack(Material.CLOCK); // Changed Material.COMPASS to Material.CLOCK
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Resonating Chronometer").color(NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)); // Changed item name
            meta.lore(List.of(Component.text("Right-click to resonate with the sculk.").color(NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(RESONANCE_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveItemIfNotPresent(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isResonanceItem(item)) return;
        }
        ItemStack resonanceItem = createResonanceItem();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(resonanceItem);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), resonanceItem);
            player.sendMessage(Component.text("Your Resonating Chronometer couldn't fit and was dropped!").color(NamedTextColor.YELLOW));
        }
    }

    @EventHandler public void onPlayerJoin(PlayerJoinEvent event) { runForAbility(event.getPlayer(), this::giveItemIfNotPresent); }
    @EventHandler public void onPlayerRespawn(PlayerRespawnEvent event) { Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> runForAbility(event.getPlayer(), this::giveItemIfNotPresent), 1L); }
    @EventHandler(priority = EventPriority.HIGH) public void onDropItem(PlayerDropItemEvent event) { if (isResonanceItem(event.getItemDrop().getItemStack())) { runForAbility(event.getPlayer(), p -> event.setCancelled(true)); } }
    @EventHandler(priority = EventPriority.HIGH) public void onPlayerDeath(PlayerDeathEvent event) { runForAbility(event.getEntity(), p -> event.getDrops().removeIf(this::isResonanceItem)); }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();

        if (isResonanceItem(heldItem)) {
            event.setCancelled(true);
            runForAbility(player, p -> {
                if (this.hasCooldown(p)) return;
                this.setCooldown(p);
                performResonance(p);
            });
        }
    }

    private void performResonance(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION, BUFF_AMPLIFIER));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, BUFF_DURATION, BUFF_AMPLIFIER));

        world.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 1.3f);
        world.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}