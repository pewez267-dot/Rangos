package com.fscrates.network;

import com.fscrates.block.CrateBlockEntity;
import com.fscrates.config.CrateConfig;
import com.fscrates.crate.CrateRegistry;
import com.fscrates.item.CrateItems;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

public class SaveCratePacket {
    private final CompoundTag configNbt;
    private final BlockPos pos;

    public SaveCratePacket(CompoundTag configNbt) {
        this(configNbt, null);
    }

    public SaveCratePacket(CompoundTag configNbt, BlockPos pos) {
        this.configNbt = configNbt;
        this.pos = pos;
    }

    public static void encode(SaveCratePacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.configNbt);
        boolean hasPos = msg.pos != null;
        buf.writeBoolean(hasPos);
        if (hasPos) {
            buf.writeBlockPos(msg.pos);
        }
    }

    public static SaveCratePacket decode(FriendlyByteBuf buf) {
        CompoundTag nbt = buf.readNbt();
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new SaveCratePacket(nbt, pos);
    }

    public static void handle(SaveCratePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(4) && msg.configNbt != null) {
                CrateConfig crate = CrateConfig.load(msg.configNbt);
                if (crate.id == null || crate.id.isBlank()) {
                    crate.id = "crate_" + System.currentTimeMillis();
                }
                CrateRegistry.get(player.serverLevel()).put(crate);
                if (msg.pos != null) {
                    ServerLevel level = player.serverLevel();
                    BlockEntity patt2064$temp = level.getBlockEntity(msg.pos);
                    if (patt2064$temp instanceof CrateBlockEntity) {
                        CrateBlockEntity crateBe = (CrateBlockEntity)patt2064$temp;
                        crateBe.setConfig(crate);
                        player.sendSystemMessage((Component)Component.literal((String)("\u00a7aCofre '" + crate.id + "' actualizado en el sitio.")));
                    } else {
                        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eEl cofre ya no esta ahi; se guardo '" + crate.id + "' en el registro.")));
                    }
                    // Al EDITAR y guardar una caja con llave unica, se entrega SIEMPRE la llave
                    // (nueva) al inventario, para no tener que recrear la caja desde cero.
                    if (crate.uniqueKeyEnabled) {
                        ItemStack uniqueKey = CrateItems.buildUniqueKey(crate);
                        if (!player.getInventory().add(uniqueKey)) {
                            player.drop(uniqueKey, false);
                        }
                        player.sendSystemMessage((Component)Component.literal((String)("\u00a7b\u2726 Llave \u00fanica \u2726\u00a7a de '" + crate.id + "' entregada a tu inventario.")));
                    }
                } else {
                    ItemStack crateItem = CrateItems.buildCrate(crate);
                    if (!player.getInventory().add(crateItem)) {
                        player.drop(crateItem, false);
                    }
                    if (crate.uniqueKeyEnabled) {
                        // Entrega la LLAVE UNICA enlazada junto con la caja (creacion de la caja).
                        ItemStack uniqueKey = CrateItems.buildUniqueKey(crate);
                        if (!player.getInventory().add(uniqueKey)) {
                            player.drop(uniqueKey, false);
                        }
                        player.sendSystemMessage((Component)Component.literal((String)("\u00a7aCrate '" + crate.id + "' guardada y entregada \u00a77(+ su \u00a7b\u2726 llave \u00fanica \u2726\u00a77).")));
                    } else {
                        player.sendSystemMessage((Component)Component.literal((String)("\u00a7aCrate '" + crate.id + "' guardada y entregada. Usa \u00a7e/fscrate key give\u00a7a para dar llaves.")));
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}

