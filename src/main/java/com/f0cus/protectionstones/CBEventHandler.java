/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.properties.PropertyMap
 *  kotlin.Metadata
 *  kotlin.NoWhenBranchMatchedException
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.MapsKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.text.StringsKt
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
 *  net.fabricmc.fabric.api.event.player.UseBlockCallback
 *  net.fabricmc.fabric.api.message.v1.ServerMessageEvents
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.minecraft.class_124
 *  net.minecraft.class_1268
 *  net.minecraft.class_1269
 *  net.minecraft.class_1657
 *  net.minecraft.class_1747
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_1937
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2487
 *  net.minecraft.class_2556$class_7602
 *  net.minecraft.class_2561
 *  net.minecraft.class_2586
 *  net.minecraft.class_2631
 *  net.minecraft.class_2680
 *  net.minecraft.class_3222
 *  net.minecraft.class_3244
 *  net.minecraft.class_3965
 *  net.minecraft.class_7471
 *  net.minecraft.class_9279
 *  net.minecraft.class_9296
 *  net.minecraft.class_9334
 *  net.minecraft.server.MinecraftServer
 *  org.jetbrains.annotations.NotNull
 */
package com.f0cus.protectionstones;

import com.f0cus.protectionstones.CBConfig;
import com.f0cus.protectionstones.CBFlagHandlers;
import com.f0cus.protectionstones.CBItemManager;
import com.f0cus.protectionstones.CBLocationsManager;
import com.f0cus.protectionstones.CBManager;
import com.f0cus.protectionstones.CBRegion;
import com.f0cus.protectionstones.CBSelectionManager;
import com.f0cus.protectionstones.CBTexts;
import com.f0cus.protectionstones.CBTracker;
import com.f0cus.protectionstones.CBUtils;
import com.f0cus.protectionstones.CBVisualizationManager;
import com.f0cus.protectionstones.StoneConfig;
import com.f0cus.protectionstones.TitleEditSession;
import com.f0cus.protectionstones.TitleEditState;
import com.f0cus.protectionstones.flags.Flags;
import com.f0cus.protectionstones.gui.CBMenuProvider;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.class_124;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2556;
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_2631;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import net.minecraft.class_3244;
import net.minecraft.class_3965;
import net.minecraft.class_7471;
import net.minecraft.class_9279;
import net.minecraft.class_9296;
import net.minecraft.class_9334;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\r\u0010\u0005\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0003J'\u0010\u000b\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\bH\u0002\u00a2\u0006\u0004\b\u000b\u0010\fJ'\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\b2\u0006\u0010\u000f\u001a\u00020\u000eH\u0002\u00a2\u0006\u0004\b\u0010\u0010\u0011J'\u0010\u0014\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0012\u001a\u00020\b2\u0006\u0010\u0013\u001a\u00020\bH\u0002\u00a2\u0006\u0004\b\u0014\u0010\f\u00a8\u0006\u0015"}, d2={"Lcom/f0cus/protectionstones/CBEventHandler;", "", "<init>", "()V", "", "register", "Lnet/minecraft/class_3222;", "player", "", "newName", "oldAreaName", "handleRename", "(Lnet/minecraft/class_3222;Ljava/lang/String;Ljava/lang/String;)V", "text", "Lcom/f0cus/protectionstones/TitleEditSession;", "session", "handleTitleEdit", "(Lnet/minecraft/class_3222;Ljava/lang/String;Lcom/f0cus/protectionstones/TitleEditSession;)V", "memberName", "areaName", "handleAddMember", "ClaimBlocks"})
@SourceDebugExtension(value={"SMAP\nCBEventHandler.kt\nKotlin\n*S Kotlin\n*F\n+ 1 CBEventHandler.kt\ncom/f0cus/protectionstones/CBEventHandler\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,361:1\n295#2,2:362\n1869#2,2:364\n*S KotlinDebug\n*F\n+ 1 CBEventHandler.kt\ncom/f0cus/protectionstones/CBEventHandler\n*L\n116#1:362,2\n199#1:364,2\n*E\n"})
public final class CBEventHandler {
    @NotNull
    public static final CBEventHandler INSTANCE = new CBEventHandler();

    private CBEventHandler() {
    }

    public final void register() {
        CBFlagHandlers.INSTANCE.register();
        UseBlockCallback.EVENT.register(CBEventHandler::register$lambda$0);
        ServerPlayConnectionEvents.DISCONNECT.register(CBEventHandler::register$lambda$1);
        ServerTickEvents.START_SERVER_TICK.register(CBEventHandler::register$lambda$2);
        ServerTickEvents.END_SERVER_TICK.register(CBEventHandler::register$lambda$3);
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(CBEventHandler::register$lambda$4);
    }

    private final void handleRename(class_3222 player, String newName, String oldAreaName) {
        if (StringsKt.equals((String)newName, (String)"cancel", (boolean)true)) {
            CBSelectionManager.INSTANCE.stopRenaming(player);
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getRenameCancelled()));
        } else if (CBManager.INSTANCE.getRegions().containsKey(newName)) {
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getNameAlreadyExists()));
        } else {
            CBRegion areaToRename = CBManager.INSTANCE.getRegions().remove(oldAreaName);
            if (areaToRename != null) {
                CBRegion updatedArea = CBRegion.copy$default(areaToRename, newName, null, null, null, null, null, null, null, null, null, null, 2046, null);
                CBManager.INSTANCE.getRegions().put(newName, updatedArea);
                CBManager.INSTANCE.save();
                String string = CBTexts.INSTANCE.getConfig().getMessages().getZoneRenamed();
                Object[] objectArray = new Object[]{oldAreaName, newName};
                String string2 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
                Intrinsics.checkNotNullExpressionValue((Object)string2, (String)"format(...)");
                String msg = string2;
                player.method_43496(CBUtils.INSTANCE.createSuccessMessage(msg));
            }
            CBSelectionManager.INSTANCE.stopRenaming(player);
        }
    }

    private final void handleTitleEdit(class_3222 player, String text, TitleEditSession session) {
        if (StringsKt.equals((String)text, (String)"cancel", (boolean)true)) {
            CBSelectionManager.INSTANCE.stopSettingTitle(player);
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getTitleEditCancelled()));
            return;
        }
        CBRegion area = CBManager.INSTANCE.getRegions().get(session.getAreaName());
        if (area == null) {
            CBSelectionManager.INSTANCE.stopSettingTitle(player);
            return;
        }
        switch (WhenMappings.$EnumSwitchMapping$0[session.getState().ordinal()]) {
            case 1: {
                String newTitle = StringsKt.equals((String)text, (String)"clear", (boolean)true) ? null : CBUtils.INSTANCE.translateColors(text);
                CBRegion updatedArea = CBRegion.copy$default(area, null, null, null, null, null, null, null, null, newTitle, null, null, 1791, null);
                CBManager.INSTANCE.getRegions().put(area.getName(), updatedArea);
                CBManager.INSTANCE.save();
                player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getTitleSaved()));
                CBSelectionManager.INSTANCE.startSettingTitle(player, area.getName(), TitleEditState.SUBTITLE);
                player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getEnterSubtitlePrompt()));
                break;
            }
            case 2: {
                String newSubtitle = StringsKt.equals((String)text, (String)"clear", (boolean)true) ? null : CBUtils.INSTANCE.translateColors(text);
                CBRegion updatedArea = CBRegion.copy$default(area, null, null, null, null, null, null, null, null, null, newSubtitle, null, 1535, null);
                CBManager.INSTANCE.getRegions().put(area.getName(), updatedArea);
                CBManager.INSTANCE.save();
                player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getSubtitleSaved()));
                CBSelectionManager.INSTANCE.stopSettingTitle(player);
                break;
            }
            default: {
                throw new NoWhenBranchMatchedException();
            }
        }
    }

    private final void handleAddMember(class_3222 player, String memberName, String areaName) {
        if (StringsKt.equals((String)memberName, (String)"cancel", (boolean)true)) {
            CBSelectionManager.INSTANCE.stopAddingMember(player);
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getAddMemberCancelled()));
            return;
        }
        CBRegion area = CBManager.INSTANCE.getRegions().get(areaName);
        if (area == null) {
            CBSelectionManager.INSTANCE.stopAddingMember(player);
            return;
        }
        if (StringsKt.equals((String)memberName, (String)player.method_5477().getString(), (boolean)true)) {
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(CBTexts.INSTANCE.getConfig().getMessages().getCannotAddSelf()));
            CBSelectionManager.INSTANCE.stopAddingMember(player);
            CBMenuProvider.INSTANCE.openMemberEditor(player, area);
            return;
        }
        class_3222 playerToAdd = player.field_13995.method_3760().method_14566(memberName);
        if (playerToAdd == null) {
            String string = CBTexts.INSTANCE.getConfig().getMessages().getPlayerNotFound();
            Object[] objectArray = new Object[]{memberName};
            String string2 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
            Intrinsics.checkNotNullExpressionValue((Object)string2, (String)"format(...)");
            String msg = string2;
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(msg));
            CBSelectionManager.INSTANCE.stopAddingMember(player);
            CBMenuProvider.INSTANCE.openMemberEditor(player, area);
            return;
        }
        Set<UUID> set = area.getMembers();
        UUID uUID = playerToAdd.method_5667();
        Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
        if (set.add(uUID)) {
            CBManager.INSTANCE.save();
            String string = CBTexts.INSTANCE.getConfig().getMessages().getMemberAdded();
            Object[] objectArray = new Object[]{memberName};
            String string3 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
            Intrinsics.checkNotNullExpressionValue((Object)string3, (String)"format(...)");
            String msg = string3;
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(msg));
        } else {
            String string = CBTexts.INSTANCE.getConfig().getMessages().getAlreadyMember();
            Object[] objectArray = new Object[]{memberName};
            String string4 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
            Intrinsics.checkNotNullExpressionValue((Object)string4, (String)"format(...)");
            String msg = string4;
            player.method_43496(CBUtils.INSTANCE.createSuccessMessage(msg));
        }
        CBSelectionManager.INSTANCE.stopAddingMember(player);
        CBMenuProvider.INSTANCE.openMemberEditor(player, area);
    }

    private static final class_1269 register$lambda$0(class_1657 player, class_1937 world, class_1268 hand, class_3965 hitResult) {
        if (!(player instanceof class_3222)) {
            return class_1269.field_5811;
        }
        class_1799 stackInHand = ((class_3222)player).method_5998(hand);
        Intrinsics.checkNotNull((Object)stackInHand);
        String string = CBItemManager.INSTANCE.getStoneType(stackInHand);
        if (string == null) {
            return class_1269.field_5811;
        }
        String stoneType = string;
        class_2338 placePos = hitResult.method_17777().method_10093(hitResult.method_17780());
        StoneConfig config = CBConfig.INSTANCE.getStones().get(stoneType);
        if (config == null) {
            ((class_3222)player).method_43496((class_2561)class_2561.method_43470((String)("Error: Configuration not found for '" + stoneType + "'.")).method_27692(class_124.field_1061));
            return class_1269.field_5812;
        }
        String string2 = world.method_27983().method_29177().toString();
        Intrinsics.checkNotNullExpressionValue((Object)string2, (String)"toString(...)");
        String worldKey = string2;
        Intrinsics.checkNotNull((Object)placePos);
        if (!((Collection)CBManager.INSTANCE.getAreasAt(placePos, worldKey)).isEmpty()) {
            ((class_3222)player).method_43496((class_2561)class_2561.method_43470((String)CBUtils.INSTANCE.translateColors(CBTexts.INSTANCE.getConfig().getMessages().getInsideAnotherZone())).method_27692(class_124.field_1061));
            return class_1269.field_5812;
        }
        if (world.method_8320(hitResult.method_17777()).method_26215()) {
            ((class_3222)player).method_43496((class_2561)class_2561.method_43470((String)CBUtils.INSTANCE.translateColors(CBTexts.INSTANCE.getConfig().getMessages().getAirPlacement())).method_27692(class_124.field_1061));
            return class_1269.field_5812;
        }
        if (!world.method_8320(placePos).method_26215() && !world.method_8320(placePos).method_45474()) {
            ((class_3222)player).method_43496((class_2561)class_2561.method_43470((String)CBUtils.INSTANCE.translateColors(CBTexts.INSTANCE.getConfig().getMessages().getSpaceBlocked())).method_27692(class_124.field_1061));
            return class_1269.field_5812;
        }
        int rX = config.getRadius_x();
        int rZ = config.getRadius_z();
        class_2338 pos1 = new class_2338(placePos.method_10263() - rX, -64, placePos.method_10260() - rZ);
        class_2338 pos2 = new class_2338(placePos.method_10263() + rX, 320, placePos.method_10260() + rZ);
        String string3 = ((class_3222)player).method_5477().getString();
        String string4 = UUID.randomUUID().toString();
        Intrinsics.checkNotNullExpressionValue((Object)string4, (String)"toString(...)");
        String areaName = string3 + "-" + stoneType + "-" + StringsKt.take((String)string4, (int)4);
        UUID owner = ((class_3222)player).method_5667();
        Intrinsics.checkNotNull((Object)owner);
        CBRegion newArea = new CBRegion(areaName, owner, placePos, stoneType, pos1, pos2, MapsKt.emptyMap(), null, null, null, worldKey, 896, null);
        if (CBManager.INSTANCE.isOverlapping(newArea)) {
            String string5;
            Object v4;
            block17: {
                Iterable $this$firstOrNull$iv = CBManager.INSTANCE.getRegions().values();
                boolean $i$f$firstOrNull = false;
                for (Object element$iv : $this$firstOrNull$iv) {
                    CBRegion it = (CBRegion)element$iv;
                    boolean bl = false;
                    if (!(!Intrinsics.areEqual((Object)it.getName(), (Object)newArea.getName()) && CBManager.INSTANCE.isOverlapping(newArea))) continue;
                    v4 = element$iv;
                    break block17;
                }
                v4 = null;
            }
            CBRegion overlappingArea = v4;
            if (overlappingArea != null) {
                String $i$f$firstOrNull = CBTexts.INSTANCE.getConfig().getMessages().getOverlapping();
                Object[] objectArray = new Object[3];
                Intrinsics.checkNotNullExpressionValue((Object)stoneType.toUpperCase(Locale.ROOT), (String)"toUpperCase(...)");
                objectArray[1] = rX;
                objectArray[2] = overlappingArea.getName();
                String string6 = String.format($i$f$firstOrNull, Arrays.copyOf(objectArray, objectArray.length));
                string5 = string6;
                Intrinsics.checkNotNullExpressionValue((Object)string6, (String)"format(...)");
            } else {
                string5 = CBTexts.INSTANCE.getConfig().getMessages().getOverlappingGeneric();
            }
            String msg = string5;
            ((class_3222)player).method_43496((class_2561)class_2561.method_43470((String)CBUtils.INSTANCE.translateColors(msg)).method_27692(class_124.field_1061));
            return class_1269.field_5812;
        }
        CBManager.INSTANCE.getRegions().put(newArea.getName(), newArea);
        CBManager.INSTANCE.save();
        CBLocationsManager.INSTANCE.addLocation(owner, newArea);
        class_1792 item = stackInHand.method_7909();
        if (item instanceof class_1747) {
            class_2338 blockPos = placePos;
            class_2680 blockState = ((class_1747)item).method_7711().method_9564();
            world.method_8652(blockPos, blockState, 2);
            class_2586 blockEntity = world.method_8321(blockPos);
            if (blockEntity instanceof class_2631) {
                class_9296 profile = (class_9296)stackInHand.method_57824(class_9334.field_49617);
                if (profile != null) {
                    ((class_2631)blockEntity).method_11333(profile);
                } else {
                    class_2487 tag;
                    class_9279 customData = (class_9279)stackInHand.method_57824(class_9334.field_49628);
                    if (customData != null && (tag = customData.method_57461()).method_10545("SkullOwner")) {
                        class_2487 skullOwner = tag.method_10562("SkullOwner");
                        UUID id = skullOwner.method_25928("Id") ? skullOwner.method_25926("Id") : UUID.randomUUID();
                        ((class_2631)blockEntity).method_11333(new class_9296(Optional.empty(), Optional.of(id), new PropertyMap()));
                    }
                }
            }
        } else {
            world.method_8652(placePos, class_2246.field_10340.method_9564(), 2);
        }
        // BUG 1 FIX: Synchronize item consumption with client
        if (!((class_3222)player).method_7337()) {  // if not creative
            stackInHand.method_7934(1);  // decrement count
            
            // Sync the stack back to the player
            if (stackInHand.method_7960()) {  // if stack is now empty (count == 0)
                ((class_3222)player).method_6030(hand, class_1799.field_8037);  // setStackInHand with ItemStack.EMPTY
            } else {
                ((class_3222)player).method_6030(hand, stackInHand);  // setStackInHand with updated stack
            }
        }
        String string7 = CBTexts.INSTANCE.getConfig().getMessages().getZoneCreated();
        Object[] objectArray = new Object[]{areaName};
        String string8 = String.format(string7, Arrays.copyOf(objectArray, objectArray.length));
        Intrinsics.checkNotNullExpressionValue((Object)string8, (String)"format(...)");
        String zoneCreatedMsg = string8;
        ((class_3222)player).method_43496(CBUtils.INSTANCE.createSuccessMessage(zoneCreatedMsg));
        return class_1269.field_5812;
    }

    private static final void register$lambda$1(class_3244 handler, MinecraftServer server) {
        UUID uUID = handler.field_14140.method_5667();
        Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
        CBTracker.INSTANCE.onPlayerDisconnect(uUID);
        UUID uUID2 = handler.field_14140.method_5667();
        Intrinsics.checkNotNullExpressionValue((Object)uUID2, (String)"getUUID(...)");
        CBVisualizationManager.INSTANCE.onPlayerDisconnect(uUID2);
    }

    private static final void register$lambda$2(MinecraftServer server) {
        Intrinsics.checkNotNull((Object)server);
        CBTracker.INSTANCE.tick(server);
        List list = server.method_3760().method_14571();
        Intrinsics.checkNotNullExpressionValue((Object)list, (String)"getPlayers(...)");
        Iterable $this$forEach$iv = list;
        boolean $i$f$forEach = false;
        for (Object element$iv : $this$forEach$iv) {
            String worldKey;
            class_3222 player = (class_3222)element$iv;
            boolean bl = false;
            Intrinsics.checkNotNullExpressionValue((Object)player.method_37908().method_27983().method_29177().toString(), (String)"toString(...)");
            class_2338 class_23382 = player.method_24515();
            Intrinsics.checkNotNullExpressionValue((Object)class_23382, (String)"blockPosition(...)");
            CBRegion area = (CBRegion)CollectionsKt.firstOrNull(CBManager.INSTANCE.getAreasAt(class_23382, worldKey));
            if (area == null || area.getFlag(Flags.INSTANCE.getHUNGER_DRAIN()).booleanValue() || player.method_7344().method_7586() >= 20) continue;
            player.method_7344().method_7580(20);
        }
    }

    private static final void register$lambda$3(MinecraftServer server) {
        Intrinsics.checkNotNull((Object)server);
        CBVisualizationManager.INSTANCE.tick(server);
    }

    private static final boolean register$lambda$4(class_7471 message, class_3222 player, class_2556.class_7602 typeKey) {
        String signedContent = message.method_44862();
        Intrinsics.checkNotNull((Object)player);
        String oldAreaName = CBSelectionManager.INSTANCE.getRenamingArea(player);
        TitleEditSession titleSession = CBSelectionManager.INSTANCE.getSettingTitleSession(player);
        String addingMemberArea = CBSelectionManager.INSTANCE.getAddingMemberArea(player);
        if (oldAreaName != null) {
            Intrinsics.checkNotNull((Object)signedContent);
            INSTANCE.handleRename(player, signedContent, oldAreaName);
            return false;
        }
        if (titleSession != null) {
            Intrinsics.checkNotNull((Object)signedContent);
            INSTANCE.handleTitleEdit(player, signedContent, titleSession);
            return false;
        }
        if (addingMemberArea != null) {
            Intrinsics.checkNotNull((Object)signedContent);
            INSTANCE.handleAddMember(player, signedContent, addingMemberArea);
            return false;
        }
        return true;
    }

    @Metadata(mv={2, 2, 0}, k=3, xi=48)
    public static final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] nArray = new int[TitleEditState.values().length];
            try {
                nArray[TitleEditState.TITLE.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[TitleEditState.SUBTITLE.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
        }
    }
}
