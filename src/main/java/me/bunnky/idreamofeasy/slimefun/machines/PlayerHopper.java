package me.bunnky.idreamofeasy.slimefun.machines;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.VanillaInventoryDropHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.bunnky.idreamofeasy.utils.IDOEUtility;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
/*
A convenient drop-off point for players to store items easily.
 */
public class PlayerHopper extends SimpleSlimefunItem<BlockTicker> implements EnergyNetComponent {

    private final ItemSetting<Boolean> silent = new ItemSetting<>(this, "silent", false);
    private final ItemSetting<Boolean> toggleable = new ItemSetting<>(this, "toggleable-with-redstone", false);

    private final int cap;
    private final int ecost;

    public PlayerHopper(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int ecost, int cap) {
        super(itemGroup, item, recipeType, recipe);
        IDOEUtility.setGlow(item);

        this.cap = cap;
        this.ecost = ecost;

        addItemSetting(silent, toggleable);

        addItemHandler(new VanillaInventoryDropHandler<>(org.bukkit.block.Hopper.class));
    }

    @Override
    public @NotNull BlockTicker getItemHandler() {
        return new BlockTicker() {
            @Override
            public void tick(Block b, SlimefunItem sfItem, Config config) {
                if (b.getType() != Material.HOPPER) {
                    BlockStorage.clearBlockInfo(b);
                    return;
                }

                if (toggleable.getValue()) {
                    Hopper hopper = (Hopper) b.getBlockData();

                    if (!hopper.isEnabled()) {
                        return;
                    }
                }

                Location loc = b.getLocation();
                boolean playSound = false;
                double xMin = -1;
                double xMax = 1;
                double yMin = 0;
                double yMax = 2.0;
                double zMin = -1;
                double zMax = 1;

                List<Player> players = (List<Player>) loc.getNearbyPlayers(xMax - xMin, yMax - yMin, zMax - zMin);

                for (Player p : players) {
                    if (!(Slimefun.getProtectionManager().hasPermission(p, b, Interaction.INTERACT_BLOCK))) {
                        return;
                    }

                    Location ploc = p.getLocation();

                    double blockCenterX = loc.getX() + 0.5;
                    double blockCenterY = loc.getY() + 0.5;
                    double blockCenterZ = loc.getZ() + 0.5;

                    if (ploc.getX() >= blockCenterX + xMin && ploc.getX() <= blockCenterX + xMax &&
                        ploc.getY() >= blockCenterY + yMin && ploc.getY() <= blockCenterY + yMax &&
                        ploc.getZ() >= blockCenterZ + zMin && ploc.getZ() <= blockCenterZ + zMax) {

                        org.bukkit.block.Hopper h = (org.bukkit.block.Hopper) b.getState();
                        ItemStack[] items = p.getInventory().getContents();
                        ItemStack[] hItems = h.getInventory().getContents();
                        int emptySlots = (int) Arrays.stream(hItems).filter(Objects::isNull).count();

                        if (!(h.getInventory().getViewers().isEmpty())){
                            return;
                        }

                        for (ItemStack item : items) {
                            if (item != null && getCharge(loc) >= ecost) {
                                ItemStack offhandItem = p.getInventory().getItemInOffHand();
                                ItemStack[] armorContents = p.getInventory().getArmorContents();

                                boolean isArmorOrOffhand = false;

                                for (ItemStack armorItem : armorContents) {
                                    if (armorItem != null && item.isSimilar(armorItem)) {
                                        isArmorOrOffhand = true;
                                        break;
                                    }
                                }

                                if (!isArmorOrOffhand && !item.isSimilar(offhandItem) && emptySlots > 0) {
                                    HashMap<Integer, ItemStack> leftover = h.getInventory().addItem(item);

                                    if (leftover.isEmpty()) {
                                        p.getInventory().removeItem(item);
                                        removeCharge(loc, ecost);
                                        emptySlots--;
                                        b.getWorld().spawnParticle(Particle.CRIT, b.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
                                        playSound = true;
                                    } else {
                                        for (ItemStack remaining : leftover.values()) {
                                            p.getInventory().addItem(remaining);
                                        }
                                    }

                                    if (playSound && !silent.getValue()) {
                                        SoundEffect.INFUSED_HOPPER_TELEPORT_SOUND.playAt(b);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public boolean isSynchronized() {
                return true;
            }
        };
    }

    @Override
    public @NotNull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return this.cap;
    }

}
