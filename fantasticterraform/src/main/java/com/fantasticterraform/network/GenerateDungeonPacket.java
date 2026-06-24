package com.fantasticterraform.network;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.intelligent.dungeon.DungeonConfig;
import com.fantasticterraform.intelligent.dungeon.DungeonMaterializer;
import com.fantasticterraform.intelligent.dungeon.DungeonSizeRequirement;
import com.fantasticterraform.intelligent.dungeon.DungeonSizeValidator;
import com.fantasticterraform.intelligent.dungeon.DungeonTier;
import com.fantasticterraform.intelligent.dungeon.themes.CustomTheme;
import com.fantasticterraform.intelligent.dungeon.themes.DungeonTheme;
import com.fantasticterraform.schematics.BlockStateCodec;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** C->S: genera una dungeon completa con todos los parametros configurados en el HUD. */
public final class GenerateDungeonPacket {

    private final String themeId;
    private final int tier;
    private final boolean multiLevel;
    private final int levels;
    private final int trapDensityIndex;
    private final boolean[] trapTypes;
    private final boolean bossEnabled;
    private final String bossEntity;
    private final int bossCount;
    private final String treasureLoot;
    private final String bossLoot;
    private final String normalLoot;
    private final long seed;
    private final int loopDensity;
    private final String[] customPalette; // wall, floor, ceiling, pillar, light, accent
    private final String customMob;

    public GenerateDungeonPacket(String themeId, int tier, boolean multiLevel, int levels, int trapDensityIndex,
                                 boolean[] trapTypes, boolean bossEnabled, String bossEntity, int bossCount,
                                 String treasureLoot, String bossLoot, String normalLoot, long seed, int loopDensity,
                                 String[] customPalette, String customMob) {
        this.themeId = themeId;
        this.tier = tier;
        this.multiLevel = multiLevel;
        this.levels = levels;
        this.trapDensityIndex = trapDensityIndex;
        this.trapTypes = trapTypes;
        this.bossEnabled = bossEnabled;
        this.bossEntity = bossEntity;
        this.bossCount = bossCount;
        this.treasureLoot = treasureLoot;
        this.bossLoot = bossLoot;
        this.normalLoot = normalLoot;
        this.seed = seed;
        this.loopDensity = loopDensity;
        this.customPalette = customPalette;
        this.customMob = customMob;
    }

    public static void encode(GenerateDungeonPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.themeId);
        buf.writeInt(m.tier);
        buf.writeBoolean(m.multiLevel);
        buf.writeInt(m.levels);
        buf.writeInt(m.trapDensityIndex);
        buf.writeInt(m.trapTypes.length);
        for (boolean b : m.trapTypes) {
            buf.writeBoolean(b);
        }
        buf.writeBoolean(m.bossEnabled);
        buf.writeUtf(m.bossEntity);
        buf.writeInt(m.bossCount);
        buf.writeUtf(m.treasureLoot);
        buf.writeUtf(m.bossLoot);
        buf.writeUtf(m.normalLoot);
        buf.writeLong(m.seed);
        buf.writeInt(m.loopDensity);
        buf.writeInt(m.customPalette.length);
        for (String s : m.customPalette) {
            buf.writeUtf(s);
        }
        buf.writeUtf(m.customMob);
    }

    public static GenerateDungeonPacket decode(FriendlyByteBuf buf) {
        String theme = buf.readUtf();
        int tier = buf.readInt();
        boolean multi = buf.readBoolean();
        int levels = buf.readInt();
        int trapDensity = buf.readInt();
        int tn = buf.readInt();
        boolean[] traps = new boolean[tn];
        for (int i = 0; i < tn; i++) {
            traps[i] = buf.readBoolean();
        }
        boolean boss = buf.readBoolean();
        String bossEntity = buf.readUtf();
        int bossCount = buf.readInt();
        String treasure = buf.readUtf();
        String bossLoot = buf.readUtf();
        String normal = buf.readUtf();
        long seed = buf.readLong();
        int loop = buf.readInt();
        int pn = buf.readInt();
        String[] palette = new String[pn];
        for (int i = 0; i < pn; i++) {
            palette[i] = buf.readUtf();
        }
        String mob = buf.readUtf();
        return new GenerateDungeonPacket(theme, tier, multi, levels, trapDensity, traps, boss, bossEntity, bossCount,
                treasure, bossLoot, normal, seed, loop, palette, mob);
    }

    public static void handle(GenerateDungeonPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = PacketHandler.requireOp(c);
            if (player == null) {
                return;
            }
            SelectionShape sel = SelectionManager.get(player).getShape();
            DungeonTier[] tiers = DungeonTier.values();
            DungeonTier tier = m.tier >= 0 && m.tier < tiers.length ? tiers[m.tier] : DungeonTier.SMALL;

            DungeonSizeValidator.Result result = DungeonSizeValidator.validate(sel, DungeonSizeRequirement.forTier(tier));
            if (!result.ok) {
                player.sendSystemMessage(Component.literal("\u00a7c" + result.message));
                return;
            }

            HolderLookup<Block> lookup = player.server.registryAccess().lookupOrThrow(Registries.BLOCK);
            DungeonConfig cfg = new DungeonConfig();
            cfg.tier = tier;
            cfg.theme = buildTheme(m, lookup);
            cfg.multiLevel = m.multiLevel;
            cfg.levels = m.levels;
            cfg.trapDensity = trapDensityValue(m.trapDensityIndex);
            cfg.trapTypes = m.trapTypes.length == 5 ? m.trapTypes : new boolean[] {true, true, true, true, true};
            cfg.bossEnabled = m.bossEnabled;
            cfg.bossEntityId = m.bossEntity;
            cfg.bossCount = m.bossCount;
            cfg.treasureLootTable = m.treasureLoot;
            cfg.bossLootTable = m.bossLoot;
            cfg.normalLootTable = m.normalLoot;
            cfg.seed = IntelligentSeeds.resolve(m.seed, TerraformConfig.GENERAL.dungeonSeed.get(), player);
            cfg.loopDensityPercent = m.loopDensity;

            DungeonMaterializer.generate(player, (ServerLevel) player.level(), sel, cfg);
        });
        c.setPacketHandled(true);
    }

    private static double trapDensityValue(int index) {
        TerraformConfig.General g = TerraformConfig.GENERAL;
        switch (index) {
            case 1:
                return g.trapDensityLow.get();
            case 2:
                return g.trapDensityMedium.get();
            case 3:
                return g.trapDensityHigh.get();
            default:
                return 0.0D;
        }
    }

    private static DungeonTheme buildTheme(GenerateDungeonPacket m, HolderLookup<Block> lookup) {
        if (!"custom".equals(m.themeId) || m.customPalette.length < 6) {
            return DungeonTheme.byId(m.themeId);
        }
        List<EntityType<?>> mobs = new ArrayList<>();
        ResourceLocation mobId = ResourceLocation.tryParse(m.customMob);
        EntityType<?> mob = mobId == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(mobId);
        mobs.add(mob == null ? EntityType.ZOMBIE : mob);
        return new CustomTheme(
                BlockStateCodec.parse(lookup, m.customPalette[0]),
                BlockStateCodec.parse(lookup, m.customPalette[1]),
                BlockStateCodec.parse(lookup, m.customPalette[2]),
                BlockStateCodec.parse(lookup, m.customPalette[3]),
                BlockStateCodec.parse(lookup, m.customPalette[4]),
                BlockStateCodec.parse(lookup, m.customPalette[5]),
                mobs);
    }
}
