package com.fantasticchest.network;

import com.fantasticchest.config.ChestConfig;
import com.fantasticchest.data.ChestDefinition;
import com.fantasticchest.data.ChestRegistry;
import com.fantasticchest.inventory.CompressedInventory;
import com.fantasticchest.item.ChestItem;
import com.fantasticchest.security.PermissionValidator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** C-&gt;S: OP confirms chest creation. Server validates OP + unique id, then builds and gives the item. */
public final class CreateChestPacket {

    private final String id;
    private final String name;
    private final boolean doBulk;
    private final long bulkValue;
    private final Map<String, Long> overrides;
    private final List<String> permitted;

    public CreateChestPacket(final String id, final String name, final boolean doBulk, final long bulkValue,
                             final Map<String, Long> overrides, final List<String> permitted) {
        this.id = id;
        this.name = name;
        this.doBulk = doBulk;
        this.bulkValue = bulkValue;
        this.overrides = overrides;
        this.permitted = permitted;
    }

    public static void encode(final CreateChestPacket m, final FriendlyByteBuf buf) {
        buf.writeUtf(m.id == null ? "" : m.id);
        buf.writeUtf(m.name == null ? "" : m.name);
        buf.writeBoolean(m.doBulk);
        buf.writeLong(m.bulkValue);
        PacketHandler.writeLongMap(buf, m.overrides);
        PacketHandler.writeStringList(buf, m.permitted);
    }

    public static CreateChestPacket decode(final FriendlyByteBuf buf) {
        return new CreateChestPacket(buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readLong(),
                PacketHandler.readLongMap(buf), PacketHandler.readStringList(buf));
    }

    /** Shared inventory builder for create/edit: bulk fill (one registry pass) then overrides. */
    public static CompressedInventory buildInventory(final boolean doBulk, final long bulkValue, final Map<String, Long> overrides) {
        final CompressedInventory inv = new CompressedInventory();
        if (doBulk) {
            final long q = bulkValue > 0L ? bulkValue : ChestConfig.defaultQuantity();
            for (final Item item : ForgeRegistries.ITEMS.getValues()) {
                inv.set(item, q);
            }
        }
        if (overrides != null) {
            for (final Map.Entry<String, Long> e : overrides.entrySet()) {
                final Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.getKey()));
                if (item != null) {
                    inv.set(item, e.getValue() == null ? 0L : e.getValue());
                }
            }
        }
        return inv;
    }

    public static void handle(final CreateChestPacket m, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null || !PermissionValidator.isOp(player)) {
                return;
            }
            final String id = ChestRegistry.normalizeId(m.id);
            if (id.isBlank() || !id.matches("[a-z0-9_\\-]+")) {
                player.sendSystemMessage(Component.literal("§cID invalido: solo letras minusculas, numeros, '_' y '-'."));
                return;
            }
            if (ChestRegistry.get().exists(id)) {
                player.sendSystemMessage(Component.literal("§cYa existe un cofre con el ID §e" + id + "§c."));
                return;
            }

            final CompressedInventory inventory = buildInventory(m.doBulk, m.bulkValue, m.overrides);
            final CompressedInventory original = inventory.copy();

            final Set<UUID> perm = new HashSet<>();
            for (final String s : m.permitted) {
                final UUID uuid = PermissionValidator.resolveUuid(player.getServer(), s);
                if (uuid != null) {
                    perm.add(uuid);
                }
            }
            final UUID owner = player.getUUID();

            final ChestDefinition def = new ChestDefinition();
            def.id = id;
            def.name = m.name == null ? "" : m.name;
            def.ownerUuid = owner.toString();
            for (final UUID uuid : perm) {
                def.permitted.add(uuid.toString());
            }
            def.placed = false;
            def.inventory = inventory.toIdMap();
            def.originalStock = original.toIdMap();
            def.createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            def.createdBy = player.getGameProfile().getName();
            ChestRegistry.get().put(def);

            final ItemStack stack = ChestItem.buildStack(id, def.name, owner, perm, inventory, original);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            player.sendSystemMessage(Component.literal("§aFantastic Chest §e" + id + " §acreado y entregado. Coloca el bloque para activarlo."));
        });
        context.setPacketHandled(true);
    }
}
