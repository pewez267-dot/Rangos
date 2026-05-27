/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.DefaultConstructorMarker
 *  kotlin.jvm.internal.Intrinsics
 *  net.minecraft.class_2338
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.f0cus.protectionstones;

import com.f0cus.protectionstones.flags.Flag;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.class_2338;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010$\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010#\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0017\n\u0002\u0010\b\n\u0002\b\u0014\b\u0086\b\u0018\u00002\u00020\u0001B\u0081\u0001\u0012\u0006\u0010\u0003\u001a\u00020\u0002\u0012\u0006\u0010\u0005\u001a\u00020\u0004\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\u0006\u0010\b\u001a\u00020\u0002\u0012\u0006\u0010\t\u001a\u00020\u0006\u0012\u0006\u0010\n\u001a\u00020\u0006\u0012\u0016\u0010\r\u001a\u0012\u0012\b\u0012\u0006\u0012\u0002\b\u00030\f\u0012\u0004\u0012\u00020\u00010\u000b\u0012\u000e\b\u0002\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00040\u000e\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0002\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0002\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0002\u00a2\u0006\u0004\b\u0013\u0010\u0014J!\u0010\u0017\u001a\u00028\u0000\"\u0004\b\u0000\u0010\u00152\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00028\u00000\f\u00a2\u0006\u0004\b\u0017\u0010\u0018J\u001d\u0010\u001c\u001a\u00020\u001b2\u0006\u0010\u0019\u001a\u00020\u00062\u0006\u0010\u001a\u001a\u00020\u0002\u00a2\u0006\u0004\b\u001c\u0010\u001dJ\u0010\u0010\u001e\u001a\u00020\u0002H\u00c6\u0003\u00a2\u0006\u0004\b\u001e\u0010\u001fJ\u0010\u0010 \u001a\u00020\u0004H\u00c6\u0003\u00a2\u0006\u0004\b \u0010!J\u0010\u0010\"\u001a\u00020\u0006H\u00c6\u0003\u00a2\u0006\u0004\b\"\u0010#J\u0010\u0010$\u001a\u00020\u0002H\u00c6\u0003\u00a2\u0006\u0004\b$\u0010\u001fJ\u0010\u0010%\u001a\u00020\u0006H\u00c6\u0003\u00a2\u0006\u0004\b%\u0010#J\u0010\u0010&\u001a\u00020\u0006H\u00c6\u0003\u00a2\u0006\u0004\b&\u0010#J \u0010'\u001a\u0012\u0012\b\u0012\u0006\u0012\u0002\b\u00030\f\u0012\u0004\u0012\u00020\u00010\u000bH\u00c6\u0003\u00a2\u0006\u0004\b'\u0010(J\u0016\u0010)\u001a\b\u0012\u0004\u0012\u00020\u00040\u000eH\u00c6\u0003\u00a2\u0006\u0004\b)\u0010*J\u0012\u0010+\u001a\u0004\u0018\u00010\u0002H\u00c6\u0003\u00a2\u0006\u0004\b+\u0010\u001fJ\u0012\u0010,\u001a\u0004\u0018\u00010\u0002H\u00c6\u0003\u00a2\u0006\u0004\b,\u0010\u001fJ\u0010\u0010-\u001a\u00020\u0002H\u00c6\u0003\u00a2\u0006\u0004\b-\u0010\u001fJ\u0098\u0001\u0010.\u001a\u00020\u00002\b\b\u0002\u0010\u0003\u001a\u00020\u00022\b\b\u0002\u0010\u0005\u001a\u00020\u00042\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\u00022\b\b\u0002\u0010\t\u001a\u00020\u00062\b\b\u0002\u0010\n\u001a\u00020\u00062\u0018\b\u0002\u0010\r\u001a\u0012\u0012\b\u0012\u0006\u0012\u0002\b\u00030\f\u0012\u0004\u0012\u00020\u00010\u000b2\u000e\b\u0002\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00040\u000e2\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00022\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00022\b\b\u0002\u0010\u0012\u001a\u00020\u0002H\u00c6\u0001\u00a2\u0006\u0004\b.\u0010/J\u001a\u00101\u001a\u00020\u001b2\b\u00100\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003\u00a2\u0006\u0004\b1\u00102J\u0010\u00104\u001a\u000203H\u00d6\u0001\u00a2\u0006\u0004\b4\u00105J\u0010\u00106\u001a\u00020\u0002H\u00d6\u0001\u00a2\u0006\u0004\b6\u0010\u001fR\u0017\u0010\u0003\u001a\u00020\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0003\u00107\u001a\u0004\b8\u0010\u001fR\u0017\u0010\u0005\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u0005\u00109\u001a\u0004\b:\u0010!R\u0017\u0010\u0007\u001a\u00020\u00068\u0006\u00a2\u0006\f\n\u0004\b\u0007\u0010;\u001a\u0004\b<\u0010#R\u0017\u0010\b\u001a\u00020\u00028\u0006\u00a2\u0006\f\n\u0004\b\b\u00107\u001a\u0004\b=\u0010\u001fR\u0017\u0010\t\u001a\u00020\u00068\u0006\u00a2\u0006\f\n\u0004\b\t\u0010;\u001a\u0004\b>\u0010#R\u0017\u0010\n\u001a\u00020\u00068\u0006\u00a2\u0006\f\n\u0004\b\n\u0010;\u001a\u0004\b?\u0010#R'\u0010\r\u001a\u0012\u0012\b\u0012\u0006\u0012\u0002\b\u00030\f\u0012\u0004\u0012\u00020\u00010\u000b8\u0006\u00a2\u0006\f\n\u0004\b\r\u0010@\u001a\u0004\bA\u0010(R\u001d\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00040\u000e8\u0006\u00a2\u0006\f\n\u0004\b\u000f\u0010B\u001a\u0004\bC\u0010*R\u0019\u0010\u0010\u001a\u0004\u0018\u00010\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0010\u00107\u001a\u0004\bD\u0010\u001fR\u0019\u0010\u0011\u001a\u0004\u0018\u00010\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0011\u00107\u001a\u0004\bE\u0010\u001fR\u0017\u0010\u0012\u001a\u00020\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0012\u00107\u001a\u0004\bF\u0010\u001f\u00a8\u0006G"}, d2={"Lcom/f0cus/protectionstones/CBRegion;", "", "", "name", "Ljava/util/UUID;", "owner", "Lnet/minecraft/class_2338;", "centerBlock", "stoneType", "pos1", "pos2", "", "Lcom/f0cus/protectionstones/flags/Flag;", "flags", "", "members", "enterTitle", "enterSubtitle", "world", "<init>", "(Ljava/lang/String;Ljava/util/UUID;Lnet/minecraft/class_2338;Ljava/lang/String;Lnet/minecraft/class_2338;Lnet/minecraft/class_2338;Ljava/util/Map;Ljava/util/Set;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "T", "flag", "getFlag", "(Lcom/f0cus/protectionstones/flags/Flag;)Ljava/lang/Object;", "pos", "checkWorld", "", "isInside", "(Lnet/minecraft/class_2338;Ljava/lang/String;)Z", "component1", "()Ljava/lang/String;", "component2", "()Ljava/util/UUID;", "component3", "()Lnet/minecraft/class_2338;", "component4", "component5", "component6", "component7", "()Ljava/util/Map;", "component8", "()Ljava/util/Set;", "component9", "component10", "component11", "copy", "(Ljava/lang/String;Ljava/util/UUID;Lnet/minecraft/class_2338;Ljava/lang/String;Lnet/minecraft/class_2338;Lnet/minecraft/class_2338;Ljava/util/Map;Ljava/util/Set;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/f0cus/protectionstones/CBRegion;", "other", "equals", "(Ljava/lang/Object;)Z", "", "hashCode", "()I", "toString", "Ljava/lang/String;", "getName", "Ljava/util/UUID;", "getOwner", "Lnet/minecraft/class_2338;", "getCenterBlock", "getStoneType", "getPos1", "getPos2", "Ljava/util/Map;", "getFlags", "Ljava/util/Set;", "getMembers", "getEnterTitle", "getEnterSubtitle", "getWorld", "ClaimBlocks"})
public final class CBRegion {
    @NotNull
    private final String name;
    @NotNull
    private final UUID owner;
    @NotNull
    private final class_2338 centerBlock;
    @NotNull
    private final String stoneType;
    @NotNull
    private final class_2338 pos1;
    @NotNull
    private final class_2338 pos2;
    @NotNull
    private final Map<Flag<?>, Object> flags;
    @NotNull
    private final Set<UUID> members;
    @Nullable
    private final String enterTitle;
    @Nullable
    private final String enterSubtitle;
    @NotNull
    private final String world;

    public CBRegion(@NotNull String name, @NotNull UUID owner, @NotNull class_2338 centerBlock, @NotNull String stoneType, @NotNull class_2338 pos1, @NotNull class_2338 pos2, @NotNull Map<Flag<?>, ? extends Object> flags, @NotNull Set<UUID> members, @Nullable String enterTitle, @Nullable String enterSubtitle, @NotNull String world) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)owner, (String)"owner");
        Intrinsics.checkNotNullParameter((Object)centerBlock, (String)"centerBlock");
        Intrinsics.checkNotNullParameter((Object)stoneType, (String)"stoneType");
        Intrinsics.checkNotNullParameter((Object)pos1, (String)"pos1");
        Intrinsics.checkNotNullParameter((Object)pos2, (String)"pos2");
        Intrinsics.checkNotNullParameter(flags, (String)"flags");
        Intrinsics.checkNotNullParameter(members, (String)"members");
        Intrinsics.checkNotNullParameter((Object)world, (String)"world");
        this.name = name;
        this.owner = owner;
        this.centerBlock = centerBlock;
        this.stoneType = stoneType;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.flags = flags;
        this.members = members;
        this.enterTitle = enterTitle;
        this.enterSubtitle = enterSubtitle;
        this.world = world;
    }

    public /* synthetic */ CBRegion(String string, UUID uUID, class_2338 class_23382, String string2, class_2338 class_23383, class_2338 class_23384, Map map, Set set, String string3, String string4, String string5, int n, DefaultConstructorMarker defaultConstructorMarker) {
        if ((n & 0x80) != 0) {
            set = new LinkedHashSet();
        }
        if ((n & 0x100) != 0) {
            string3 = null;
        }
        if ((n & 0x200) != 0) {
            string4 = null;
        }
        if ((n & 0x400) != 0) {
            string5 = "*";
        }
        this(string, uUID, class_23382, string2, class_23383, class_23384, map, set, string3, string4, string5);
    }

    @NotNull
    public final String getName() {
        return this.name;
    }

    @NotNull
    public final UUID getOwner() {
        return this.owner;
    }

    @NotNull
    public final class_2338 getCenterBlock() {
        return this.centerBlock;
    }

    @NotNull
    public final String getStoneType() {
        return this.stoneType;
    }

    @NotNull
    public final class_2338 getPos1() {
        return this.pos1;
    }

    @NotNull
    public final class_2338 getPos2() {
        return this.pos2;
    }

    @NotNull
    public final Map<Flag<?>, Object> getFlags() {
        return this.flags;
    }

    @NotNull
    public final Set<UUID> getMembers() {
        return this.members;
    }

    @Nullable
    public final String getEnterTitle() {
        return this.enterTitle;
    }

    @Nullable
    public final String getEnterSubtitle() {
        return this.enterSubtitle;
    }

    @NotNull
    public final String getWorld() {
        return this.world;
    }

    public final <T> T getFlag(@NotNull Flag<T> flag) {
        Intrinsics.checkNotNullParameter(flag, (String)"flag");
        return (T)this.flags.getOrDefault(flag, flag.getDefaultValue());
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public final boolean isInside(@NotNull class_2338 pos, @NotNull String checkWorld) {
        Intrinsics.checkNotNullParameter((Object)pos, (String)"pos");
        Intrinsics.checkNotNullParameter((Object)checkWorld, (String)"checkWorld");
        if (!Intrinsics.areEqual((Object)this.world, (Object)"*") && !Intrinsics.areEqual((Object)this.world, (Object)checkWorld)) {
            return false;
        }
        int minX = Math.min(this.pos1.method_10263(), this.pos2.method_10263());
        int minZ = Math.min(this.pos1.method_10260(), this.pos2.method_10260());
        int maxX = Math.max(this.pos1.method_10263(), this.pos2.method_10263());
        int maxZ = Math.max(this.pos1.method_10260(), this.pos2.method_10260());
        int n = pos.method_10263();
        if (minX > n) return false;
        if (n > maxX) return false;
        boolean bl = true;
        if (!bl) return false;
        n = pos.method_10260();
        if (minZ > n) return false;
        if (n > maxZ) return false;
        return true;
    }

    @NotNull
    public final String component1() {
        return this.name;
    }

    @NotNull
    public final UUID component2() {
        return this.owner;
    }

    @NotNull
    public final class_2338 component3() {
        return this.centerBlock;
    }

    @NotNull
    public final String component4() {
        return this.stoneType;
    }

    @NotNull
    public final class_2338 component5() {
        return this.pos1;
    }

    @NotNull
    public final class_2338 component6() {
        return this.pos2;
    }

    @NotNull
    public final Map<Flag<?>, Object> component7() {
        return this.flags;
    }

    @NotNull
    public final Set<UUID> component8() {
        return this.members;
    }

    @Nullable
    public final String component9() {
        return this.enterTitle;
    }

    @Nullable
    public final String component10() {
        return this.enterSubtitle;
    }

    @NotNull
    public final String component11() {
        return this.world;
    }

    @NotNull
    public final CBRegion copy(@NotNull String name, @NotNull UUID owner, @NotNull class_2338 centerBlock, @NotNull String stoneType, @NotNull class_2338 pos1, @NotNull class_2338 pos2, @NotNull Map<Flag<?>, ? extends Object> flags, @NotNull Set<UUID> members, @Nullable String enterTitle, @Nullable String enterSubtitle, @NotNull String world) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)owner, (String)"owner");
        Intrinsics.checkNotNullParameter((Object)centerBlock, (String)"centerBlock");
        Intrinsics.checkNotNullParameter((Object)stoneType, (String)"stoneType");
        Intrinsics.checkNotNullParameter((Object)pos1, (String)"pos1");
        Intrinsics.checkNotNullParameter((Object)pos2, (String)"pos2");
        Intrinsics.checkNotNullParameter(flags, (String)"flags");
        Intrinsics.checkNotNullParameter(members, (String)"members");
        Intrinsics.checkNotNullParameter((Object)world, (String)"world");
        return new CBRegion(name, owner, centerBlock, stoneType, pos1, pos2, flags, members, enterTitle, enterSubtitle, world);
    }

    public static /* synthetic */ CBRegion copy$default(CBRegion cBRegion, String string, UUID uUID, class_2338 class_23382, String string2, class_2338 class_23383, class_2338 class_23384, Map map, Set set, String string3, String string4, String string5, int n, Object object) {
        if ((n & 1) != 0) {
            string = cBRegion.name;
        }
        if ((n & 2) != 0) {
            uUID = cBRegion.owner;
        }
        if ((n & 4) != 0) {
            class_23382 = cBRegion.centerBlock;
        }
        if ((n & 8) != 0) {
            string2 = cBRegion.stoneType;
        }
        if ((n & 0x10) != 0) {
            class_23383 = cBRegion.pos1;
        }
        if ((n & 0x20) != 0) {
            class_23384 = cBRegion.pos2;
        }
        if ((n & 0x40) != 0) {
            map = cBRegion.flags;
        }
        if ((n & 0x80) != 0) {
            set = cBRegion.members;
        }
        if ((n & 0x100) != 0) {
            string3 = cBRegion.enterTitle;
        }
        if ((n & 0x200) != 0) {
            string4 = cBRegion.enterSubtitle;
        }
        if ((n & 0x400) != 0) {
            string5 = cBRegion.world;
        }
        return cBRegion.copy(string, uUID, class_23382, string2, class_23383, class_23384, map, set, string3, string4, string5);
    }

    @NotNull
    public String toString() {
        return "CBRegion(name=" + this.name + ", owner=" + this.owner + ", centerBlock=" + this.centerBlock + ", stoneType=" + this.stoneType + ", pos1=" + this.pos1 + ", pos2=" + this.pos2 + ", flags=" + this.flags + ", members=" + this.members + ", enterTitle=" + this.enterTitle + ", enterSubtitle=" + this.enterSubtitle + ", world=" + this.world + ")";
    }

    public int hashCode() {
        int result = this.name.hashCode();
        result = result * 31 + this.owner.hashCode();
        result = result * 31 + this.centerBlock.hashCode();
        result = result * 31 + this.stoneType.hashCode();
        result = result * 31 + this.pos1.hashCode();
        result = result * 31 + this.pos2.hashCode();
        result = result * 31 + ((Object)this.flags).hashCode();
        result = result * 31 + ((Object)this.members).hashCode();
        result = result * 31 + (this.enterTitle == null ? 0 : this.enterTitle.hashCode());
        result = result * 31 + (this.enterSubtitle == null ? 0 : this.enterSubtitle.hashCode());
        result = result * 31 + this.world.hashCode();
        return result;
    }

    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CBRegion)) {
            return false;
        }
        CBRegion cBRegion = (CBRegion)other;
        if (!Intrinsics.areEqual((Object)this.name, (Object)cBRegion.name)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.owner, (Object)cBRegion.owner)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.centerBlock, (Object)cBRegion.centerBlock)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.stoneType, (Object)cBRegion.stoneType)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.pos1, (Object)cBRegion.pos1)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.pos2, (Object)cBRegion.pos2)) {
            return false;
        }
        if (!Intrinsics.areEqual(this.flags, cBRegion.flags)) {
            return false;
        }
        if (!Intrinsics.areEqual(this.members, cBRegion.members)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.enterTitle, (Object)cBRegion.enterTitle)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.enterSubtitle, (Object)cBRegion.enterSubtitle)) {
            return false;
        }
        return Intrinsics.areEqual((Object)this.world, (Object)cBRegion.world);
    }
}
