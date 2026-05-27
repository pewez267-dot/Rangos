/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 *  kotlin.Metadata
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.io.CloseableKt
 *  kotlin.io.FilesKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.text.Charsets
 *  kotlin.text.StringsKt
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_2338
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.f0cus.protectionstones;

import com.f0cus.protectionstones.CBRegion;
import com.f0cus.protectionstones.flags.Flag;
import com.f0cus.protectionstones.flags.Flags;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.CloseableKt;
import kotlin.io.FilesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2338;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000V\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0010%\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001&B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\r\u0010\u0005\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0003J\r\u0010\u0006\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0006\u0010\u0003J)\u0010\f\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u00072\n\u0010\n\u001a\u0006\u0012\u0002\b\u00030\t2\u0006\u0010\u000b\u001a\u00020\u0001\u00a2\u0006\u0004\b\f\u0010\rJ\u0015\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u000e\u001a\u00020\u0007\u00a2\u0006\u0004\b\u0010\u0010\u0011J#\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00070\u00162\u0006\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0015\u001a\u00020\u0014\u00a2\u0006\u0004\b\u0017\u0010\u0018J\u001f\u0010\u0019\u001a\u0004\u0018\u00010\u00072\u0006\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0015\u001a\u00020\u0014\u00a2\u0006\u0004\b\u0019\u0010\u001aR#\u0010\u001c\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00070\u001b8\u0006\u00a2\u0006\f\n\u0004\b\u001c\u0010\u001d\u001a\u0004\b\u001e\u0010\u001fR\u0018\u0010!\u001a\u0004\u0018\u00010 8\u0002@\u0002X\u0082\u000e\u00a2\u0006\u0006\n\u0004\b!\u0010\"R\u0014\u0010$\u001a\u00020#8\u0002X\u0082\u0004\u00a2\u0006\u0006\n\u0004\b$\u0010%\u00a8\u0006'"}, d2={"Lcom/f0cus/protectionstones/CBManager;", "", "<init>", "()V", "", "load", "save", "Lcom/f0cus/protectionstones/CBRegion;", "region", "Lcom/f0cus/protectionstones/flags/Flag;", "flag", "newValue", "updateRegionFlag", "(Lcom/f0cus/protectionstones/CBRegion;Lcom/f0cus/protectionstones/flags/Flag;Ljava/lang/Object;)V", "newRegion", "", "isOverlapping", "(Lcom/f0cus/protectionstones/CBRegion;)Z", "Lnet/minecraft/class_2338;", "pos", "", "world", "", "getAreasAt", "(Lnet/minecraft/class_2338;Ljava/lang/String;)Ljava/util/List;", "getAreaAtCenter", "(Lnet/minecraft/class_2338;Ljava/lang/String;)Lcom/f0cus/protectionstones/CBRegion;", "", "regions", "Ljava/util/Map;", "getRegions", "()Ljava/util/Map;", "Ljava/io/File;", "regionFile", "Ljava/io/File;", "Lcom/google/gson/Gson;", "GSON", "Lcom/google/gson/Gson;", "RegionData", "ClaimBlocks"})
@SourceDebugExtension(value={"SMAP\nCBManager.kt\nKotlin\n*S Kotlin\n*F\n+ 1 CBManager.kt\ncom/f0cus/protectionstones/CBManager\n+ 2 _Maps.kt\nkotlin/collections/MapsKt___MapsKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,201:1\n216#2,2:202\n1563#3:204\n1634#3,3:205\n1761#3,3:208\n774#3:211\n865#3,2:212\n295#3,2:214\n*S KotlinDebug\n*F\n+ 1 CBManager.kt\ncom/f0cus/protectionstones/CBManager\n*L\n70#1:202,2\n112#1:204\n112#1:205,3\n160#1:208,3\n192#1:211\n192#1:212,2\n196#1:214,2\n*E\n"})
public final class CBManager {
    @NotNull
    public static final CBManager INSTANCE = new CBManager();
    @NotNull
    private static final Map<String, CBRegion> regions = new ConcurrentHashMap();
    @Nullable
    private static File regionFile;
    @NotNull
    private static final Gson GSON;

    private CBManager() {
    }

    @NotNull
    public final Map<String, CBRegion> getRegions() {
        return regions;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public final void load() {
        block18: {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            File modDataDir = configDir.resolve("ClaimBlocks").toFile();
            if (!modDataDir.exists()) {
                modDataDir.mkdirs();
            }
            File file = regionFile = new File(modDataDir, "claims.json");
            boolean bl = file != null ? file.exists() : false;
            if (bl) {
                try {
                    File file2 = regionFile;
                    Intrinsics.checkNotNull((Object)file2);
                    String fileContent = ((Object)StringsKt.trim((CharSequence)FilesKt.readText$default((File)file2, null, (int)1, null))).toString();
                    if (((CharSequence)fileContent).length() == 0 || StringsKt.startsWith$default((CharSequence)fileContent, (char)'{', (boolean)false, (int)2, null)) {
                        String string = "[ClaimBlocks] ProtectionStones.json file was corrupt/malformed. Resetting to empty array [].";
                        System.out.println((Object)string);
                        File file3 = regionFile;
                        Intrinsics.checkNotNull((Object)file3);
                        FilesKt.writeText$default((File)file3, (String)"[]", null, (int)2, null);
                        return;
                    }
                    File file4 = regionFile;
                    if (file4 == null) break block18;
                    Object object = file4;
                    Object object2 = Charsets.UTF_8;
                    object = new InputStreamReader((InputStream)new FileInputStream((File)object), (Charset)object2);
                    object2 = null;
                    try {
                        InputStreamReader reader = (InputStreamReader)object;
                        boolean bl2 = false;
                        Type type2 = new TypeToken<List<? extends RegionData>>(){}.getType();
                        List dataList = (List)GSON.fromJson((Reader)reader, type2);
                        regions.clear();
                        if (dataList == null) {
                            System.out.println((Object)"[ClaimBlocks] Data list was null after read. Starting empty.");
                            return;
                        }
                        for (RegionData data : dataList) {
                            Map flagsMap = new LinkedHashMap();
                            Map<String, Object> $this$forEach$iv = data.getFlags();
                            boolean $i$f$forEach = false;
                            Iterator<Map.Entry<String, Object>> iterator = $this$forEach$iv.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<String, Object> element$iv;
                                Map.Entry<String, Object> entry = element$iv = iterator.next();
                                boolean bl3 = false;
                                String flagName = entry.getKey();
                                Object value = entry.getValue();
                                Flag<?> flag = Flags.INSTANCE.getFlagByName(flagName);
                                if (flag == null) continue;
                                Object correctedValue = value;
                                if (flag.getDefaultValue() instanceof Integer && value instanceof Double) {
                                    correctedValue = (int)((Number)value).doubleValue();
                                }
                                flagsMap.put(flag, correctedValue);
                            }
                            String string = data.getName();
                            UUID uUID = data.getOwner();
                            class_2338 class_23382 = data.getCenterBlock();
                            String string2 = data.getStoneType();
                            if (string2 == null) {
                                string2 = "coal";
                            }
                            class_2338 class_23383 = data.getPos1();
                            class_2338 class_23384 = data.getPos2();
                            Set set = data.getMembers();
                            if (set == null || (set = CollectionsKt.toMutableSet((Iterable)set)) == null) {
                                set = new LinkedHashSet();
                            }
                            String string3 = data.getEnterTitle();
                            String string4 = data.getEnterSubtitle();
                            String string5 = data.getWorld();
                            if (string5 == null) {
                                string5 = "*";
                            }
                            CBRegion region = new CBRegion(string, uUID, class_23382, string2, class_23383, class_23384, flagsMap, set, string3, string4, string5);
                            regions.put(region.getName(), region);
                        }
                        System.out.println((Object)("[ClaimBlocks] Loaded " + regions.size() + " protections."));
                        Unit unit = Unit.INSTANCE;
                    }
                    catch (Throwable throwable) {
                        object2 = throwable;
                        throw throwable;
                    }
                    finally {
                        CloseableKt.closeFinally((Closeable)object, (Throwable)object2);
                    }
                }
                catch (Exception e) {
                    System.out.println((Object)"[ClaimBlocks] Critical failure during data load. Deleting corrupt file.");
                    e.printStackTrace();
                    File file5 = regionFile;
                    if (file5 == null) break block18;
                    file5.delete();
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * WARNING - void declaration
     */
    public final void save() {
        block10: {
            if (regionFile == null) {
                return;
            }
            try {
                File file = regionFile;
                if (file == null) break block10;
                Object object = file;
                Object object2 = Charsets.UTF_8;
                object = new OutputStreamWriter((OutputStream)new FileOutputStream((File)object), (Charset)object2);
                object2 = null;
                try {
                    void $this$mapTo$iv$iv;
                    OutputStreamWriter writer = (OutputStreamWriter)object;
                    boolean bl = false;
                    Iterable $this$map$iv = regions.values();
                    boolean $i$f$map = false;
                    Iterable iterable = $this$map$iv;
                    Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault((Iterable)$this$map$iv, (int)10));
                    boolean $i$f$mapTo = false;
                    for (Object item$iv$iv : $this$mapTo$iv$iv) {
                        void region;
                        CBRegion cBRegion = (CBRegion)item$iv$iv;
                        Collection collection = destination$iv$iv;
                        boolean bl2 = false;
                        Map flagsByName = new LinkedHashMap();
                        for (Map.Entry<Flag<?>, Object> entry : region.getFlags().entrySet()) {
                            flagsByName.put(entry.getKey().getName(), entry.getValue());
                        }
                        collection.add(new RegionData(region.getName(), region.getOwner(), region.getCenterBlock(), region.getStoneType(), region.getPos1(), region.getPos2(), flagsByName, region.getMembers(), region.getEnterTitle(), region.getEnterSubtitle(), region.getWorld()));
                    }
                    List dataList = (List)destination$iv$iv;
                    GSON.toJson((Object)dataList, (Appendable)writer);
                    Unit unit = Unit.INSTANCE;
                }
                catch (Throwable throwable) {
                    object2 = throwable;
                    throw throwable;
                }
                finally {
                    CloseableKt.closeFinally((Closeable)object, (Throwable)object2);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public final void updateRegionFlag(@NotNull CBRegion region, @NotNull Flag<?> flag, @NotNull Object newValue) {
        Intrinsics.checkNotNullParameter((Object)region, (String)"region");
        Intrinsics.checkNotNullParameter(flag, (String)"flag");
        Intrinsics.checkNotNullParameter((Object)newValue, (String)"newValue");
        Map newFlags = new HashMap(region.getFlags());
        newFlags.put(flag, newValue);
        CBRegion updatedRegion = CBRegion.copy$default(region, null, null, null, null, null, null, newFlags, null, null, null, null, 1983, null);
        regions.put(region.getName(), updatedRegion);
        this.save();
    }

    public final boolean isOverlapping(@NotNull CBRegion newRegion) {
        boolean bl;
        block7: {
            Intrinsics.checkNotNullParameter((Object)newRegion, (String)"newRegion");
            int newMinX = Math.min(newRegion.getPos1().method_10263(), newRegion.getPos2().method_10263());
            int newMinZ = Math.min(newRegion.getPos1().method_10260(), newRegion.getPos2().method_10260());
            int newMaxX = Math.max(newRegion.getPos1().method_10263(), newRegion.getPos2().method_10263());
            int newMaxZ = Math.max(newRegion.getPos1().method_10260(), newRegion.getPos2().method_10260());
            Iterable $this$any$iv = regions.values();
            boolean $i$f$any = false;
            if ($this$any$iv instanceof Collection && ((Collection)$this$any$iv).isEmpty()) {
                bl = false;
            } else {
                for (Object element$iv : $this$any$iv) {
                    boolean bl2;
                    CBRegion existingRegion = (CBRegion)element$iv;
                    boolean bl3 = false;
                    if (Intrinsics.areEqual((Object)existingRegion.getName(), (Object)newRegion.getName())) {
                        bl2 = false;
                    } else if (Intrinsics.areEqual((Object)existingRegion.getOwner(), (Object)newRegion.getOwner())) {
                        // BUG 3 FIX: Skip overlap check if same owner
                        bl2 = false;
                    } else if (!(Intrinsics.areEqual((Object)existingRegion.getWorld(), (Object)"*") || Intrinsics.areEqual((Object)newRegion.getWorld(), (Object)"*") || Intrinsics.areEqual((Object)existingRegion.getWorld(), (Object)newRegion.getWorld()))) {
                        bl2 = false;
                    } else {
                        int existingMinX = Math.min(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
                        int existingMinZ = Math.min(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
                        int existingMaxX = Math.max(existingRegion.getPos1().method_10263(), existingRegion.getPos2().method_10263());
                        int existingMaxZ = Math.max(existingRegion.getPos1().method_10260(), existingRegion.getPos2().method_10260());
                        bl2 = newMinX <= existingMaxX && newMaxX >= existingMinX && newMinZ <= existingMaxZ && newMaxZ >= existingMinZ;
                    }
                    if (!bl2) continue;
                    bl = true;
                    break block7;
                }
                bl = false;
            }
        }
        return bl;
    }

    /*
     * WARNING - void declaration
     */
    @NotNull
    public final List<CBRegion> getAreasAt(@NotNull class_2338 pos, @NotNull String world) {
        void $this$filterTo$iv$iv;
        Intrinsics.checkNotNullParameter((Object)pos, (String)"pos");
        Intrinsics.checkNotNullParameter((Object)world, (String)"world");
        Iterable $this$filter$iv = regions.values();
        boolean $i$f$filter = false;
        Iterable iterable = $this$filter$iv;
        Collection destination$iv$iv = new ArrayList();
        boolean $i$f$filterTo = false;
        for (Object element$iv$iv : $this$filterTo$iv$iv) {
            CBRegion it = (CBRegion)element$iv$iv;
            boolean bl = false;
            if (!it.isInside(pos, world)) continue;
            destination$iv$iv.add(element$iv$iv);
        }
        return (List)destination$iv$iv;
    }

    @Nullable
    public final CBRegion getAreaAtCenter(@NotNull class_2338 pos, @NotNull String world) {
        Object v0;
        block1: {
            Intrinsics.checkNotNullParameter((Object)pos, (String)"pos");
            Intrinsics.checkNotNullParameter((Object)world, (String)"world");
            Iterable $this$firstOrNull$iv = regions.values();
            boolean $i$f$firstOrNull = false;
            for (Object element$iv : $this$firstOrNull$iv) {
                CBRegion it = (CBRegion)element$iv;
                boolean bl = false;
                if (!(Intrinsics.areEqual((Object)it.getCenterBlock(), (Object)pos) && (Intrinsics.areEqual((Object)it.getWorld(), (Object)"*") || Intrinsics.areEqual((Object)it.getWorld(), (Object)world)))) continue;
                v0 = element$iv;
                break block1;
            }
            v0 = null;
        }
        return v0;
    }

    static {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Intrinsics.checkNotNullExpressionValue((Object)gson, (String)"create(...)");
        GSON = gson;
    }

    @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010$\n\u0000\n\u0002\u0010\"\n\u0002\b\u0019\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0014\b\u0082\b\u0018\u00002\u00020\u0001B{\u0012\u0006\u0010\u0003\u001a\u00020\u0002\u0012\u0006\u0010\u0005\u001a\u00020\u0004\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0002\u0012\u0006\u0010\t\u001a\u00020\u0006\u0012\u0006\u0010\n\u001a\u00020\u0006\u0012\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00010\u000b\u0012\u000e\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\r\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0002\u0012\b\u0010\u0010\u001a\u0004\u0018\u00010\u0002\u0012\b\u0010\u0011\u001a\u0004\u0018\u00010\u0002\u00a2\u0006\u0004\b\u0012\u0010\u0013J\u0010\u0010\u0014\u001a\u00020\u0002H\u00c6\u0003\u00a2\u0006\u0004\b\u0014\u0010\u0015J\u0010\u0010\u0016\u001a\u00020\u0004H\u00c6\u0003\u00a2\u0006\u0004\b\u0016\u0010\u0017J\u0010\u0010\u0018\u001a\u00020\u0006H\u00c6\u0003\u00a2\u0006\u0004\b\u0018\u0010\u0019J\u0012\u0010\u001a\u001a\u0004\u0018\u00010\u0002H\u00c6\u0003\u00a2\u0006\u0004\b\u001a\u0010\u0015J\u0010\u0010\u001b\u001a\u00020\u0006H\u00c6\u0003\u00a2\u0006\u0004\b\u001b\u0010\u0019J\u0010\u0010\u001c\u001a\u00020\u0006H\u00c6\u0003\u00a2\u0006\u0004\b\u001c\u0010\u0019J\u001c\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00010\u000bH\u00c6\u0003\u00a2\u0006\u0004\b\u001d\u0010\u001eJ\u0018\u0010\u001f\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\rH\u00c6\u0003\u00a2\u0006\u0004\b\u001f\u0010 J\u0012\u0010!\u001a\u0004\u0018\u00010\u0002H\u00c6\u0003\u00a2\u0006\u0004\b!\u0010\u0015J\u0012\u0010\"\u001a\u0004\u0018\u00010\u0002H\u00c6\u0003\u00a2\u0006\u0004\b\"\u0010\u0015J\u0012\u0010#\u001a\u0004\u0018\u00010\u0002H\u00c6\u0003\u00a2\u0006\u0004\b#\u0010\u0015J\u009a\u0001\u0010$\u001a\u00020\u00002\b\b\u0002\u0010\u0003\u001a\u00020\u00022\b\b\u0002\u0010\u0005\u001a\u00020\u00042\b\b\u0002\u0010\u0007\u001a\u00020\u00062\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00022\b\b\u0002\u0010\t\u001a\u00020\u00062\b\b\u0002\u0010\n\u001a\u00020\u00062\u0014\b\u0002\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00010\u000b2\u0010\b\u0002\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\r2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00022\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00022\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0002H\u00c6\u0001\u00a2\u0006\u0004\b$\u0010%J\u001a\u0010(\u001a\u00020'2\b\u0010&\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003\u00a2\u0006\u0004\b(\u0010)J\u0010\u0010+\u001a\u00020*H\u00d6\u0001\u00a2\u0006\u0004\b+\u0010,J\u0010\u0010-\u001a\u00020\u0002H\u00d6\u0001\u00a2\u0006\u0004\b-\u0010\u0015R\u0017\u0010\u0003\u001a\u00020\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0003\u0010.\u001a\u0004\b/\u0010\u0015R\u0017\u0010\u0005\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u0005\u00100\u001a\u0004\b1\u0010\u0017R\u0017\u0010\u0007\u001a\u00020\u00068\u0006\u00a2\u0006\f\n\u0004\b\u0007\u00102\u001a\u0004\b3\u0010\u0019R\u0019\u0010\b\u001a\u0004\u0018\u00010\u00028\u0006\u00a2\u0006\f\n\u0004\b\b\u0010.\u001a\u0004\b4\u0010\u0015R\u0017\u0010\t\u001a\u00020\u00068\u0006\u00a2\u0006\f\n\u0004\b\t\u00102\u001a\u0004\b5\u0010\u0019R\u0017\u0010\n\u001a\u00020\u00068\u0006\u00a2\u0006\f\n\u0004\b\n\u00102\u001a\u0004\b6\u0010\u0019R#\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00010\u000b8\u0006\u00a2\u0006\f\n\u0004\b\f\u00107\u001a\u0004\b8\u0010\u001eR\u001f\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\r8\u0006\u00a2\u0006\f\n\u0004\b\u000e\u00109\u001a\u0004\b:\u0010 R\u0019\u0010\u000f\u001a\u0004\u0018\u00010\u00028\u0006\u00a2\u0006\f\n\u0004\b\u000f\u0010.\u001a\u0004\b;\u0010\u0015R\u0019\u0010\u0010\u001a\u0004\u0018\u00010\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0010\u0010.\u001a\u0004\b<\u0010\u0015R\u0019\u0010\u0011\u001a\u0004\u0018\u00010\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0011\u0010.\u001a\u0004\b=\u0010\u0015\u00a8\u0006>"}, d2={"Lcom/f0cus/protectionstones/CBManager$RegionData;", "", "", "name", "Ljava/util/UUID;", "owner", "Lnet/minecraft/class_2338;", "centerBlock", "stoneType", "pos1", "pos2", "", "flags", "", "members", "enterTitle", "enterSubtitle", "world", "<init>", "(Ljava/lang/String;Ljava/util/UUID;Lnet/minecraft/class_2338;Ljava/lang/String;Lnet/minecraft/class_2338;Lnet/minecraft/class_2338;Ljava/util/Map;Ljava/util/Set;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "component1", "()Ljava/lang/String;", "component2", "()Ljava/util/UUID;", "component3", "()Lnet/minecraft/class_2338;", "component4", "component5", "component6", "component7", "()Ljava/util/Map;", "component8", "()Ljava/util/Set;", "component9", "component10", "component11", "copy", "(Ljava/lang/String;Ljava/util/UUID;Lnet/minecraft/class_2338;Ljava/lang/String;Lnet/minecraft/class_2338;Lnet/minecraft/class_2338;Ljava/util/Map;Ljava/util/Set;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/f0cus/protectionstones/CBManager$RegionData;", "other", "", "equals", "(Ljava/lang/Object;)Z", "", "hashCode", "()I", "toString", "Ljava/lang/String;", "getName", "Ljava/util/UUID;", "getOwner", "Lnet/minecraft/class_2338;", "getCenterBlock", "getStoneType", "getPos1", "getPos2", "Ljava/util/Map;", "getFlags", "Ljava/util/Set;", "getMembers", "getEnterTitle", "getEnterSubtitle", "getWorld", "ClaimBlocks"})
    private static final class RegionData {
        @NotNull
        private final String name;
        @NotNull
        private final UUID owner;
        @NotNull
        private final class_2338 centerBlock;
        @Nullable
        private final String stoneType;
        @NotNull
        private final class_2338 pos1;
        @NotNull
        private final class_2338 pos2;
        @NotNull
        private final Map<String, Object> flags;
        @Nullable
        private final Set<UUID> members;
        @Nullable
        private final String enterTitle;
        @Nullable
        private final String enterSubtitle;
        @Nullable
        private final String world;

        public RegionData(@NotNull String name, @NotNull UUID owner, @NotNull class_2338 centerBlock, @Nullable String stoneType, @NotNull class_2338 pos1, @NotNull class_2338 pos2, @NotNull Map<String, ? extends Object> flags, @Nullable Set<UUID> members, @Nullable String enterTitle, @Nullable String enterSubtitle, @Nullable String world) {
            Intrinsics.checkNotNullParameter((Object)name, (String)"name");
            Intrinsics.checkNotNullParameter((Object)owner, (String)"owner");
            Intrinsics.checkNotNullParameter((Object)centerBlock, (String)"centerBlock");
            Intrinsics.checkNotNullParameter((Object)pos1, (String)"pos1");
            Intrinsics.checkNotNullParameter((Object)pos2, (String)"pos2");
            Intrinsics.checkNotNullParameter(flags, (String)"flags");
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

        @Nullable
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
        public final Map<String, Object> getFlags() {
            return this.flags;
        }

        @Nullable
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

        @Nullable
        public final String getWorld() {
            return this.world;
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

        @Nullable
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
        public final Map<String, Object> component7() {
            return this.flags;
        }

        @Nullable
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

        @Nullable
        public final String component11() {
            return this.world;
        }

        @NotNull
        public final RegionData copy(@NotNull String name, @NotNull UUID owner, @NotNull class_2338 centerBlock, @Nullable String stoneType, @NotNull class_2338 pos1, @NotNull class_2338 pos2, @NotNull Map<String, ? extends Object> flags, @Nullable Set<UUID> members, @Nullable String enterTitle, @Nullable String enterSubtitle, @Nullable String world) {
            Intrinsics.checkNotNullParameter((Object)name, (String)"name");
            Intrinsics.checkNotNullParameter((Object)owner, (String)"owner");
            Intrinsics.checkNotNullParameter((Object)centerBlock, (String)"centerBlock");
            Intrinsics.checkNotNullParameter((Object)pos1, (String)"pos1");
            Intrinsics.checkNotNullParameter((Object)pos2, (String)"pos2");
            Intrinsics.checkNotNullParameter(flags, (String)"flags");
            return new RegionData(name, owner, centerBlock, stoneType, pos1, pos2, flags, members, enterTitle, enterSubtitle, world);
        }

        public static /* synthetic */ RegionData copy$default(RegionData regionData, String string, UUID uUID, class_2338 class_23382, String string2, class_2338 class_23383, class_2338 class_23384, Map map, Set set, String string3, String string4, String string5, int n, Object object) {
            if ((n & 1) != 0) {
                string = regionData.name;
            }
            if ((n & 2) != 0) {
                uUID = regionData.owner;
            }
            if ((n & 4) != 0) {
                class_23382 = regionData.centerBlock;
            }
            if ((n & 8) != 0) {
                string2 = regionData.stoneType;
            }
            if ((n & 0x10) != 0) {
                class_23383 = regionData.pos1;
            }
            if ((n & 0x20) != 0) {
                class_23384 = regionData.pos2;
            }
            if ((n & 0x40) != 0) {
                map = regionData.flags;
            }
            if ((n & 0x80) != 0) {
                set = regionData.members;
            }
            if ((n & 0x100) != 0) {
                string3 = regionData.enterTitle;
            }
            if ((n & 0x200) != 0) {
                string4 = regionData.enterSubtitle;
            }
            if ((n & 0x400) != 0) {
                string5 = regionData.world;
            }
            return regionData.copy(string, uUID, class_23382, string2, class_23383, class_23384, map, set, string3, string4, string5);
        }

        @NotNull
        public String toString() {
            return "RegionData(name=" + this.name + ", owner=" + this.owner + ", centerBlock=" + this.centerBlock + ", stoneType=" + this.stoneType + ", pos1=" + this.pos1 + ", pos2=" + this.pos2 + ", flags=" + this.flags + ", members=" + this.members + ", enterTitle=" + this.enterTitle + ", enterSubtitle=" + this.enterSubtitle + ", world=" + this.world + ")";
        }

        public int hashCode() {
            int result = this.name.hashCode();
            result = result * 31 + this.owner.hashCode();
            result = result * 31 + this.centerBlock.hashCode();
            result = result * 31 + (this.stoneType == null ? 0 : this.stoneType.hashCode());
            result = result * 31 + this.pos1.hashCode();
            result = result * 31 + this.pos2.hashCode();
            result = result * 31 + ((Object)this.flags).hashCode();
            result = result * 31 + (this.members == null ? 0 : ((Object)this.members).hashCode());
            result = result * 31 + (this.enterTitle == null ? 0 : this.enterTitle.hashCode());
            result = result * 31 + (this.enterSubtitle == null ? 0 : this.enterSubtitle.hashCode());
            result = result * 31 + (this.world == null ? 0 : this.world.hashCode());
            return result;
        }

        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RegionData)) {
                return false;
            }
            RegionData regionData = (RegionData)other;
            if (!Intrinsics.areEqual((Object)this.name, (Object)regionData.name)) {
                return false;
            }
            if (!Intrinsics.areEqual((Object)this.owner, (Object)regionData.owner)) {
                return false;
            }
            if (!Intrinsics.areEqual((Object)this.centerBlock, (Object)regionData.centerBlock)) {
                return false;
            }
            if (!Intrinsics.areEqual((Object)this.stoneType, (Object)regionData.stoneType)) {
                return false;
            }
            if (!Intrinsics.areEqual((Object)this.pos1, (Object)regionData.pos1)) {
                return false;
            }
            if (!Intrinsics.areEqual((Object)this.pos2, (Object)regionData.pos2)) {
                return false;
            }
            if (!Intrinsics.areEqual(this.flags, regionData.flags)) {
                return false;
            }
            if (!Intrinsics.areEqual(this.members, regionData.members)) {
                return false;
            }
            if (!Intrinsics.areEqual((Object)this.enterTitle, (Object)regionData.enterTitle)) {
                return false;
            }
            if (!Intrinsics.areEqual((Object)this.enterSubtitle, (Object)regionData.enterSubtitle)) {
                return false;
            }
            return Intrinsics.areEqual((Object)this.world, (Object)regionData.world);
        }
    }
}
