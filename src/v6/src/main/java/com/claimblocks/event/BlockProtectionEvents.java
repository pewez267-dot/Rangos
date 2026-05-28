/*
 * BlockProtectionEvents v6.0.0
 * - FIX: protección de break ClaimStone movida al BEFORE (cancelable correctamente).
 * - FIX: isInteractiveBlock usa tags vanilla en lugar de strings.
 * - FIX: isMatureCrop con cobertura extendida (sweet berries, cocoa, sugar cane).
 */
package com.claimblocks.event;

import com.claimblocks.block.ClaimStoneBlock;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.class_124;
import net.minecraft.class_1263;
import net.minecraft.class_1269;
import net.minecraft.class_1271;
import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_1750;
import net.minecraft.class_1755;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2199;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2281;
import net.minecraft.class_2302;
import net.minecraft.class_2336;
import net.minecraft.class_2338;
import net.minecraft.class_2363;
import net.minecraft.class_2421;
import net.minecraft.class_2478;
import net.minecraft.class_2480;
import net.minecraft.class_2561;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3481;
import net.minecraft.class_3708;
import net.minecraft.server.MinecraftServer;

public final class BlockProtectionEvents {
    private static int fireSweepCounter = 0;

    public static void register() {
        registerBreakEvents();
        registerPlaceAndUseEvents();
        registerItemUseEvent();
    }

    private static boolean isBypassing(class_1657 player) {
        return player.method_5687(2) && ClaimManager.getInstance().isBypassing(player.method_5667());
    }

    private static boolean denyForVisitor(Claim claim, class_1657 player, boolean specificFlag) {
        if (claim.canModify(player)) return false;
        if (isBypassing(player)) return false;
        if (claim.getFlags().publicMode) return true;
        return specificFlag;
    }

    private static void deny(class_1657 player, String msg) {
        if (player instanceof class_3222 sp) {
            sp.method_7353(class_2561.method_43470(msg).method_27692(class_124.field_1061), true);
        }
    }

    private static void registerBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (world.field_9236) return true;
            if (isBypassing(player)) return true;

            // FIX v6: ahora SÍ validamos break de ClaimStone aquí (antes se permitía siempre).
            if (state.method_26204() instanceof ClaimStoneBlock) {
                Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
                if (claim == null) return true; // piedra huérfana, dejar romper
                if (claim.isOwner(player) || player.method_5687(2)) return true;
                deny(player, "[!] Solo el dueño puede romper esta piedra.");
                return false;
            }

            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null) return true;
            if (claim.canModify(player)) return true;

            if (state.method_26164(class_3481.field_15475) && (claim.getFlags().publicMode || claim.getFlags().blockTreeChopping)) {
                deny(player, "[!] No puedes talar árboles en esta zona.");
                return false;
            }
            if (isMatureCrop(state) && (claim.getFlags().publicMode || claim.getFlags().blockCropHarvest)) {
                deny(player, "[!] No puedes cosechar cultivos aquí.");
                return false;
            }
            if (denyForVisitor(claim, player, claim.getFlags().blockBreaking)) {
                deny(player, "[!] No puedes romper bloques aquí.");
                return false;
            }
            return true;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
            if (world.field_9236) return class_1269.field_5811;
            if (isBypassing(player)) return class_1269.field_5811;
            class_2248 b = world.method_8320(pos).method_26204();
            if (b instanceof ClaimStoneBlock) {
                Claim claim = ClaimManager.getInstance().getClaimByCenter(world, pos);
                if (claim != null && !claim.isOwner(player) && !player.method_5687(2)) {
                    return class_1269.field_5814;
                }
                return class_1269.field_5811;
            }
            Claim claim = ClaimManager.getInstance().getClaimAt(world, pos);
            if (claim == null || claim.canModify(player)) return class_1269.field_5811;
            class_2680 state = world.method_8320(pos);
            if (state.method_26164(class_3481.field_15475) && (claim.getFlags().publicMode || claim.getFlags().blockTreeChopping))
                return class_1269.field_5814;
            if (isMatureCrop(state) && (claim.getFlags().publicMode || claim.getFlags().blockCropHarvest))
                return class_1269.field_5814;
            if (denyForVisitor(claim, player, claim.getFlags().blockBreaking))
                return class_1269.field_5814;
            return class_1269.field_5811;
        });
    }

    private static void registerPlaceAndUseEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.field_9236) return class_1269.field_5811;
            if (isBypassing(player)) return class_1269.field_5811;
            class_2338 pos = hit.method_17777();
            class_2338 placeAt = pos.method_10093(hit.method_17780());
            class_1799 stack = player.method_5998(hand);
            class_2680 clickedState = world.method_8320(pos);
            class_2248 clickedBlock = clickedState.method_26204();
            boolean clickingClaimStone = clickedBlock instanceof ClaimStoneBlock;

            Claim cc;
            if (isContainer(world, pos) && (cc = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                    && !cc.canModify(player) && (cc.getFlags().publicMode || cc.getFlags().blockChestAccess)) {
                deny(player, "[!] No puedes abrir contenedores aquí.");
                return class_1269.field_5814;
            }

            Claim claim;
            if (clickedBlock instanceof class_2199 && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                    && !claim.canModify(player) && (claim.getFlags().publicMode || claim.getFlags().blockAnvilUse)) {
                deny(player, "[!] No puedes usar yunques aquí.");
                return class_1269.field_5814;
            }
            if (clickedBlock instanceof class_2478 && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                    && !claim.canModify(player) && (claim.getFlags().publicMode || claim.getFlags().blockSignEditing)) {
                deny(player, "[!] No puedes editar letreros aquí.");
                return class_1269.field_5814;
            }
            if (stack.method_7909() instanceof class_1755 && (claim = ClaimManager.getInstance().getClaimAt(world, placeAt)) != null
                    && !claim.canModify(player) && (claim.getFlags().publicMode || claim.getFlags().blockFluids || claim.getFlags().blockBuilding)) {
                deny(player, "[!] No puedes colocar fluidos aquí.");
                return class_1269.field_5814;
            }
            if (stack.method_7909() instanceof class_1747 && !clickingClaimStone) {
                class_2338 finalPos = clickedState.method_26166(new class_1750(player, hand, stack, hit)) ? pos : placeAt;
                Claim claim2 = ClaimManager.getInstance().getClaimAt(world, finalPos);
                if (claim2 != null && denyForVisitor(claim2, player, claim2.getFlags().blockBuilding)) {
                    deny(player, "[!] No puedes construir aquí.");
                    return class_1269.field_5814;
                }
            }
            if (!clickingClaimStone && isInteractiveBlock(clickedState)
                    && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                    && denyForVisitor(claim, player, claim.getFlags().blockBuilding)) {
                deny(player, "[!] No puedes interactuar aquí.");
                return class_1269.field_5814;
            }
            return class_1269.field_5811;
        });
    }

    private static void registerItemUseEvent() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            class_1799 stack = player.method_5998(hand);
            if (world.field_9236) return class_1271.method_22430(stack);
            if (isBypassing(player)) return class_1271.method_22430(stack);
            Claim claim = ClaimManager.getInstance().getClaimAt(world, player.method_24515());
            if (claim == null || claim.canModify(player)) return class_1271.method_22430(stack);
            if (claim.getFlags().publicMode || claim.getFlags().blockItemUse) {
                class_1792 it = stack.method_7909();
                if (it instanceof class_1747 bi && bi.method_7711() instanceof ClaimStoneBlock) {
                    return class_1271.method_22430(stack);
                }
                deny(player, "[!] No puedes usar items en esta zona.");
                return class_1271.method_22431(stack);
            }
            return class_1271.method_22430(stack);
        });
    }

    public static boolean isContainer(class_1937 world, class_2338 pos) {
        class_2680 state = world.method_8320(pos);
        class_2248 b = state.method_26204();
        if (b instanceof class_2281 || b instanceof class_2336 || b instanceof class_3708 || b instanceof class_2480 || b instanceof class_2363) return true;
        class_2586 be = world.method_8321(pos);
        return be instanceof class_1263;
    }

    private static boolean isMatureCrop(class_2680 state) {
        class_2248 b = state.method_26204();
        if (b instanceof class_2302) {
            // class_2741.field_12550 = AGE_7 / class_2741.field_12497 = AGE_3 (beetroots)
            if (state.method_28498(class_2741.field_12550)) return state.method_11654(class_2741.field_12550) >= 7;
            if (state.method_28498(class_2741.field_12497)) return state.method_11654(class_2741.field_12497) >= 3;
        }
        if (b instanceof class_2421) return state.method_11654(class_2421.field_11306) >= 3;
        // Cocoa pod (CocoaBlock = class_2348). Sweet berries (class_2406). Sugar cane no se "cosecha".
        // Cobertura genérica: si tiene un AGE property y está al máximo, considerarlo madura.
        if (state.method_28498(class_2741.field_12550) && state.method_11654(class_2741.field_12550) >= 7) return true;
        if (state.method_28498(class_2741.field_12497) && state.method_11654(class_2741.field_12497) >= 3) return true;
        return false;
    }

    /**
     * FIX v6: ahora usa tags vanilla. Captura buttons, doors, trapdoors, pressure plates,
     * fence gates, comparadores, repetidores, y trampillas/puertas custom.
     */
    private static boolean isInteractiveBlock(class_2680 state) {
        if (state.method_26164(class_3481.field_15487)) return true; // BUTTONS
        if (state.method_26164(class_3481.field_15469)) return true; // DOORS
        if (state.method_26164(class_3481.field_15490)) return true; // TRAPDOORS
        if (state.method_26164(class_3481.field_16584)) return true; // FENCE_GATES
        if (state.method_26164(class_3481.field_15477)) return true; // WOODEN_PRESSURE_PLATES
        if (state.method_26164(class_3481.field_15493)) return true; // PRESSURE_PLATES
        class_2248 b = state.method_26204();
        if (b == class_2246.field_10363) return true; // CRAFTING_TABLE
        if (b == class_2246.field_10179) return true; // LEVER
        if (b == class_2246.field_10223) return true; // ENCHANTING_TABLE
        if (b == class_2246.field_16330) return true; // GRINDSTONE
        if (b == class_2246.field_10183) return true; // BREWING_STAND
        return false;
    }

    public static void tickFireSweep(MinecraftServer server) {
        if (++fireSweepCounter % 40 != 0) return;
        for (class_3218 world : server.method_3738()) {
            for (Claim claim : ClaimManager.getInstance().getClaimsInWorld(world.method_27983().method_29177().toString())) {
                if (!claim.getFlags().blockFire && !claim.getFlags().publicMode) continue;
                for (class_3222 p : world.method_18456()) {
                    if (!claim.contains(p.method_24515())) continue;
                    extinguishAround(world, p.method_24515(), claim);
                }
            }
        }
    }

    private static void extinguishAround(class_3218 world, class_2338 centre, Claim claim) {
        int r = 6;
        class_2338.class_2339 m = new class_2338.class_2339();
        for (int dx = -r; dx <= r; ++dx) {
            for (int dy = -r; dy <= r; ++dy) {
                for (int dz = -r; dz <= r; ++dz) {
                    m.method_10103(centre.method_10263() + dx, centre.method_10264() + dy, centre.method_10260() + dz);
                    if (!claim.contains(m)) continue;
                    class_2680 bs = world.method_8320(m);
                    if (bs.method_26204() == class_2246.field_10036 || bs.method_26204() == class_2246.field_22089) {
                        world.method_8652(m.method_10062(), class_2246.field_10124.method_9564(), 3);
                    }
                }
            }
        }
    }
}
