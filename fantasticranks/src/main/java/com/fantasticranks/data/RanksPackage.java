package com.fantasticranks.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved rank ladder. Holds an ordered list of {@link RankDefinition}s (index order is
 * the progression order). Ships with a suggested 15-rank default that is fully editable
 * from the admin GUI — nothing here is hardcoded into gameplay.
 */
public final class RanksPackage {

    private String id;
    private String name;
    private final List<RankDefinition> ranks = new ArrayList<>();

    public RanksPackage(String id, String name) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public List<RankDefinition> getRanks() {
        return ranks;
    }

    public int size() {
        return ranks.size();
    }

    public RankDefinition get(int index) {
        if (index < 0 || index >= ranks.size()) {
            return null;
        }
        return ranks.get(index);
    }

    public void addRank(RankDefinition rank) {
        if (rank != null) {
            ranks.add(rank);
            renumber();
        }
    }

    public void removeRank(int index) {
        if (index >= 0 && index < ranks.size()) {
            ranks.remove(index);
            renumber();
        }
    }

    /** Moves the rank at {@code index} one position up; returns the new index. */
    public int moveUp(int index) {
        if (index > 0 && index < ranks.size()) {
            RankDefinition r = ranks.remove(index);
            ranks.add(index - 1, r);
            renumber();
            return index - 1;
        }
        return index;
    }

    /** Moves the rank at {@code index} one position down; returns the new index. */
    public int moveDown(int index) {
        if (index >= 0 && index < ranks.size() - 1) {
            RankDefinition r = ranks.remove(index);
            ranks.add(index + 1, r);
            renumber();
            return index + 1;
        }
        return index;
    }

    /** Reassigns 1-based {@code rankNumber} values to match list order. */
    public void renumber() {
        for (int i = 0; i < ranks.size(); i++) {
            ranks.get(i).setRankNumber(i + 1);
        }
    }

    /** Finds a rank by (case-insensitive) display name. */
    public RankDefinition findByName(String rankName) {
        if (rankName == null) {
            return null;
        }
        for (RankDefinition rank : ranks) {
            if (rank.getRankName().equalsIgnoreCase(rankName)) {
                return rank;
            }
        }
        return null;
    }

    public RanksPackage copy() {
        RanksPackage copy = new RanksPackage(id, name);
        for (RankDefinition rank : ranks) {
            copy.ranks.add(rank.copy());
        }
        return copy;
    }

    // ---- Default suggested ladder (editable seed, not hardcoded gameplay) ----

    public static RanksPackage createDefault(String id, String name) {
        RanksPackage pkg = new RanksPackage(id, name);
        pkg.addSeed("Novato", 0.0D, 0xAAAAAA);
        pkg.addSeed("Aprendiz", 5.0D, 0xFFFFFF);
        pkg.addSeed("Iniciado", 10.0D, 0xCFEFD0);
        pkg.addSeed("Explorador", 15.0D, 0x55FF55);
        pkg.addSeed("Rastreador", 22.0D, 0x88FF88);
        pkg.addSeed("Aventurero", 30.0D, 0x33CCAA);
        pkg.addSeed("Veterano", 60.0D, 0x5555FF);
        pkg.addSeed("Estratega", 80.0D, 0x3377FF);
        pkg.addSeed("Experto", 100.0D, 0x2233AA);
        pkg.addSeed("Maestro", 160.0D, 0xAA55FF);
        pkg.addSeed("Gran Maestro", 240.0D, 0x8800CC);
        pkg.addSeed("Campeon", 350.0D, 0xFFAA00);
        pkg.addSeed("Titan", 420.0D, 0xFF5522);
        pkg.addSeed("Mitico", 480.0D, 0xCC3322);
        pkg.addSeed("Leyenda", 500.0D, 0xFFD700);
        pkg.renumber();
        return pkg;
    }

    private void addSeed(String rankName, double hours, int color) {
        NametagStyle style = new NametagStyle();
        style.setColor(color);
        ranks.add(new RankDefinition(ranks.size() + 1, rankName, hours, style));
    }

    // ---- NBT ----

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("name", name);
        ListTag list = new ListTag();
        for (RankDefinition rank : ranks) {
            list.add(rank.toNbt());
        }
        tag.put("ranks", list);
        return tag;
    }

    public static RanksPackage fromNbt(CompoundTag tag) {
        RanksPackage pkg = new RanksPackage(tag.getString("id"), tag.getString("name"));
        ListTag list = tag.getList("ranks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            pkg.ranks.add(RankDefinition.fromNbt(list.getCompound(i)));
        }
        return pkg;
    }

    // ---- Network ----

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeUtf(name);
        buf.writeVarInt(ranks.size());
        for (RankDefinition rank : ranks) {
            rank.toBuf(buf);
        }
    }

    public static RanksPackage fromBuf(FriendlyByteBuf buf) {
        RanksPackage pkg = new RanksPackage(buf.readUtf(), buf.readUtf());
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            pkg.ranks.add(RankDefinition.fromBuf(buf));
        }
        return pkg;
    }
}
