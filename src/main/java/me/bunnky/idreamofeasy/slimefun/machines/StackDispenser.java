package me.bunnky.idreamofeasy.slimefun.machines;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockDispenseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.VanillaInventoryDropHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.bunnky.idreamofeasy.IDreamOfEasy;
import me.bunnky.idreamofeasy.utils.IDOEUtility;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
/*
A dispenser that automatically releases items in stacks, providing a more efficient way to distribute items.
 */

public class StackDispenser extends SlimefunItem {
    public StackDispenser(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        IDOEUtility.setGlow(item);

        addItemHandler(onPlace(), onBlockDispense());
        addItemHandler(new VanillaInventoryDropHandler<>(Dispenser.class));
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(@NotNull BlockPlaceEvent e) {
                Player p = e.getPlayer();
                Block b = e.getBlock();
                BlockStorage.addBlockInfo(b, "owner", p.getUniqueId().toString());
            }
        };
    }

    @Nonnull
    private BlockDispenseHandler onBlockDispense() {
        return (e, dispenser, facedBlock, machine) -> {
            if (!hasPermission(dispenser, facedBlock)) {
                e.setCancelled(true);
                return;
            }

            Inventory inv = dispenser.getInventory();

            if (!(inv.getViewers().isEmpty())) {
                e.setCancelled(true);
                return;
            }

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(IDreamOfEasy.getInstance(), new Runnable() {
                public void run() {
                    for (ItemStack itemStack : inv.getContents()) {
                        if (itemStack != null && itemStack.getAmount() > 0) {
                            int maxStackSize = itemStack.getMaxStackSize();
                            int amountToDispense = Math.min(itemStack.getAmount(), maxStackSize);

                            ItemStack stackToDispense = itemStack.clone();
                            stackToDispense.setAmount(amountToDispense);

                            if (facedBlock.isEmpty()) {
                                dispenser.getWorld().dropItem(facedBlock.getLocation().add(0.5, 0.5, 0.5), stackToDispense);
                            }

                            itemStack.setAmount(itemStack.getAmount() - amountToDispense);
                            if (inv.containsAtLeast(itemStack, 1)) {
                                inv.removeItem(itemStack);
                            }
                            break;
                        }
                    }
                }
            }, 1);
        };
    }

    @ParametersAreNonnullByDefault
    private boolean hasPermission(Dispenser dispenser, Block target) {
        String owner = BlockStorage.getLocationInfo(dispenser.getLocation(), "owner");

        if (owner == null) {
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(owner));
        return Slimefun.getProtectionManager().hasPermission(player, target, Interaction.PLACE_BLOCK);
    }
}
