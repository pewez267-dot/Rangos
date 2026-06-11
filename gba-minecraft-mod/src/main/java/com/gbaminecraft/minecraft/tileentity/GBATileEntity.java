package com.gbaminecraft.minecraft.tileentity;

import com.gbaminecraft.GBAMod;
import com.gbaminecraft.emulator.GBAEmulator;
import com.gbaminecraft.minecraft.gui.GBAMenu;
import com.gbaminecraft.minecraft.item.GBACartridgeItem;
import com.gbaminecraft.minecraft.registry.ModTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Tile entity for the GBA Console block.
 * Owns the GBAEmulator instance and manages its lifecycle.
 * One emulator per placed block — each console is independent.
 */
public class GBATileEntity extends BlockEntity implements MenuProvider {

    private final GBAEmulator emulator;
    private String loadedRomName = "";
    private boolean cartridgeInserted = false;
    private ItemStack cartridgeStack = ItemStack.EMPTY;

    // Tick counter for sync
    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 20; // every second

    public GBATileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntities.GBA_CONSOLE.get(), pos, state);
        this.emulator = new GBAEmulator();
    }

    // ── Menu provider ──────────────────────────────────────────────────────
    @Override
    public Component getDisplayName() {
        return Component.literal(cartridgeInserted ? "GBA: " + loadedRomName : "GBA Console");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new GBAMenu(id, inv, this);
    }

    // ── Tick ───────────────────────────────────────────────────────────────
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        tickCounter++;
        if (tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            // Could sync state to clients here if needed
        }
    }

    // ── ROM loading ────────────────────────────────────────────────────────
    /**
     * Insert a GBA cartridge. Called when player right-clicks the console with a cartridge item,
     * or from the GUI slot.
     */
    public boolean insertCartridge(ItemStack cartridge) {
        if (!GBACartridgeItem.hasROM(cartridge)) return false;

        byte[] rom = GBACartridgeItem.getROM(cartridge);
        if (rom == null) return false;

        String name = GBACartridgeItem.getRomName(cartridge);
        cartridgeStack = cartridge.copy();
        cartridgeInserted = true;
        loadedRomName = name;

        emulator.loadROM(rom, name);
        emulator.start();

        setChanged();
        GBAMod.LOGGER.info("GBA Console at {} loaded ROM: {}", worldPosition, name);
        return true;
    }

    public void ejectCartridge() {
        emulator.stop();
        cartridgeInserted = false;
        cartridgeStack = ItemStack.EMPTY;
        loadedRomName = "";
        setChanged();
    }

    public void stopEmulator() {
        emulator.stop();
    }

    // ── Input forwarding ───────────────────────────────────────────────────
    public void pressKey(int key)   { emulator.pressKey(key); }
    public void releaseKey(int key) { emulator.releaseKey(key); }

    // ── Persistence ────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("LoadedRomName", loadedRomName);
        tag.putBoolean("CartridgeInserted", cartridgeInserted);
        if (!cartridgeStack.isEmpty()) {
            CompoundTag cartTag = new CompoundTag();
            cartridgeStack.save(cartTag);
            tag.put("CartridgeStack", cartTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadedRomName = tag.getString("LoadedRomName");
        cartridgeInserted = tag.getBoolean("CartridgeInserted");
        if (tag.contains("CartridgeStack")) {
            cartridgeStack = ItemStack.of(tag.getCompound("CartridgeStack"));
            // Re-load ROM on world load
            if (cartridgeInserted && GBACartridgeItem.hasROM(cartridgeStack)) {
                byte[] rom = GBACartridgeItem.getROM(cartridgeStack);
                if (rom != null) {
                    emulator.loadROM(rom, loadedRomName);
                    emulator.start();
                }
            }
        }
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public GBAEmulator getEmulator()       { return emulator; }
    public boolean isCartridgeInserted()   { return cartridgeInserted; }
    public String  getLoadedRomName()      { return loadedRomName; }
    public ItemStack getCartridgeStack()   { return cartridgeStack; }

    @Override
    public void setRemoved() {
        super.setRemoved();
        emulator.stop();
    }
}
