/*
 * BlockProtectionEvents v6.0.0 - 100% server-side, sin bloques custom.
 *
 * Detecta items de claim por NBT (no por instanceof). Al hacer click derecho:
 *   - Si la posición clicada es el centro de un claim ⇒ abrir menú GUI.
 *   - Si el item en mano tiene NBT de claim ⇒ validar overlap, colocar concreto
 *     vanilla y registrar el claim.
 * Al romper:
 *   - Si la posición es un centro de claim ⇒ validar dueño, devolver item con
 *     NBT y eliminar registro.
 */
package com.claimblocks.event;

import com.claimblocks.ClaimBlocks;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.ClaimTier;
import com.claimblocks.gui.ClaimMenuHandler;
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
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_3481;
import net.minecraft.class_3708;
import net.minecraft.server.MinecraftServer;

public final class BlockProtectionEvents {
    private static int fireSweepCounter = 0;

    public static void register() {
        registerBreakEvents();
        registerUseBlockEvent();
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

    // ============ BREAK ============
    private static void registerBreakEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (world.field_9236) return true;
            if (isBypassing(player)) return true;

            // ¿Es el bloque-centro de algún claim?
            Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (centerClaim != null) {
                ClaimTier tier = centerClaim.getTier();
                // Solo proteger si el bloque actual coincide con el tier esperado (es nuestra piedra de claim).
                if (tier != null && ClaimBlocks.isClaimConcreteForTier(state.method_26204(), tier)) {
                    if (centerClaim.isOwner(player) || player.method_5687(2)) {
                        // Permitir break: borrar el claim y devolver el item con NBT al jugador.
                        ClaimManager.getInstance().removeClaim(world, pos);
                        if (!player.method_31549().field_7477) {
                            class_1799 stack = ClaimBlocks.createTierItem(tier, 1);
                            if (!player.method_31548().method_7394(stack)) {
                                player.method_7328(stack, false);
                            }
                        }
                        // Sonido cristalino agradable al romper la piedra (distinto al "Pin" de colocar).
                        world.method_45445(null, pos, class_3417.field_26980, class_3419.field_15245, 1.0f, 1.0f);
                        deny(player, "");
                        if (player instanceof class_3222 sp) {
                            sp.method_7353(class_2561.method_43470("\u2714 Zona eliminada. Piedra devuelta a tu inventario.").method_27692(class_124.field_1060), false);
                        }
                        return true;
                    }
                    deny(player, "[!] Solo el dueño puede romper esta piedra.");
                    return false;
                }
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

            Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (centerClaim != null) {
                ClaimTier tier = centerClaim.getTier();
                class_2680 state = world.method_8320(pos);
                if (tier != null && ClaimBlocks.isClaimConcreteForTier(state.method_26204(), tier)) {
                    if (centerClaim.isOwner(player) || player.method_5687(2)) return class_1269.field_5811;
                    return class_1269.field_5814;
                }
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

    // ============ USE BLOCK (click derecho) ============
    private static void registerUseBlockEvent() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.field_9236) return class_1269.field_5811;
            class_2338 pos = hit.method_17777();
            class_1799 stack = player.method_5998(hand);

            // 1. ¿Click sobre el bloque-centro de algún claim?
            Claim centerClaim = ClaimManager.getInstance().getClaimByCenter(world, pos);
            if (centerClaim != null) {
                ClaimTier tier = centerClaim.getTier();
                class_2680 clickedState = world.method_8320(pos);
                // Solo abrir menú si el bloque coincide con el tier (no fue reemplazado)
                if (tier != null && ClaimBlocks.isClaimConcreteForTier(clickedState.method_26204(), tier)) {
                    if (player.method_5715()) {
                        // Sneak + click: dejar que la lógica vanilla siga (e.g. colocar bloque encima)
                    } else {
                        if (centerClaim.isOwner(player) || player.method_5687(2)) {
                            if (player instanceof class_3222 sp) {
                                ClaimMenuHandler.open(sp, centerClaim, 0);
                            }
                            return class_1269.field_21466;
                        }
                        deny(player, "[x] Solo el dueño puede administrar esta zona.");
                        return class_1269.field_21466;
                    }
                }
            }

            // 2. ¿Item con NBT de claim en la mano? Intentar colocar como nueva zona.
            ClaimTier itemTier = ClaimBlocks.readTier(stack);
            if (itemTier != null && !isBypassing(player)) {
                return tryPlaceClaim(player, world, hand, hit, stack, itemTier);
            }

            // 3. Lógica de protección normal
            return regularUseBlockChecks(player, world, hand, hit, stack);
        });
    }

    /** Coloca un claim a partir de un item con NBT marker. */
    private static class_1269 tryPlaceClaim(class_1657 player, class_1937 world, net.minecraft.class_1268 hand, net.minecraft.class_3965 hit, class_1799 stack, ClaimTier tier) {
        class_2338 clicked = hit.method_17777();
        class_2680 clickedState = world.method_8320(clicked);
        class_2338 placeAt;
        // Si el bloque clicado es reemplazable (e.g. hierba alta), colocar ahí; si no, en la cara.
        if (clickedState.method_45474()) {
            placeAt = clicked;
        } else {
            placeAt = clicked.method_10093(hit.method_17780());
        }

        // ¿La posición de placement está libre?
        class_2680 atState = world.method_8320(placeAt);
        if (!atState.method_26215() && !atState.method_45474()) {
            return class_1269.field_5811;
        }

        // Solo el dueño potencial (cualquiera) puede colocar. Si está en otro claim, no puede.
        Claim ownerOfPlace = ClaimManager.getInstance().getClaimAt(world, placeAt);
        if (ownerOfPlace != null && !ownerOfPlace.canModify(player) && !player.method_5687(2)) {
            deny(player, "[x] No puedes construir en esta zona.");
            return class_1269.field_21466;
        }

        ClaimManager mgr = ClaimManager.getInstance();
        // Validar overlap
        if (mgr.wouldOverlap(world, placeAt, tier.radius, tier.height)) {
            deny(player, "[x] Esta zona se solaparía con otra existente.");
            return class_1269.field_21466;
        }
        // Validar límite por jugador
        int max = ClaimManager.getMaxClaimsPerPlayer();
        if (max > 0 && !player.method_5687(2)) {
            int owned = mgr.getClaimsOf(player.method_5667()).size();
            if (owned >= max) {
                deny(player, "[x] Has alcanzado el límite de zonas (" + max + ").");
                return class_1269.field_21466;
            }
        }

        // Colocar el bloque vanilla y registrar claim
        class_2248 block = ClaimBlocks.blockForTier(tier);
        world.method_8501(placeAt, block.method_9564());
        // Sonido suave/cristalino al colocar (amethyst place)
        world.method_45445(null, placeAt, class_3417.field_26940, class_3419.field_15245, 0.8f, 1.2f);

        Claim created = mgr.createClaim(world, placeAt, player, tier);

        // Decrementar el stack y swing arm
        if (!player.method_31549().field_7477) {
            stack.method_7934(1);
        }
        player.method_6104(hand);

        if (player instanceof class_3222 sp) {
            sp.method_7353(class_2561.method_43470("\u2714 Zona creada: ")
                    .method_27695(new class_124[]{class_124.field_1060, class_124.field_1067})
                    .method_10852(class_2561.method_43470(tier.label()).method_27695(new class_124[]{class_124.field_1054, class_124.field_1067}))
                    .method_10852(class_2561.method_43470(" bloques | Altura: +/-" + tier.height).method_27692(class_124.field_1080)), false);
        }
        return class_1269.field_21466;
    }

    private static class_1269 regularUseBlockChecks(class_1657 player, class_1937 world, net.minecraft.class_1268 hand, net.minecraft.class_3965 hit, class_1799 stack) {
        if (isBypassing(player)) return class_1269.field_5811;
        class_2338 pos = hit.method_17777();
        class_2338 placeAt = pos.method_10093(hit.method_17780());
        class_2680 clickedState = world.method_8320(pos);
        class_2248 clickedBlock = clickedState.method_26204();

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
        if (stack.method_7909() instanceof class_1747) {
            class_2338 finalPos = clickedState.method_26166(new class_1750(player, hand, stack, hit)) ? pos : placeAt;
            Claim claim2 = ClaimManager.getInstance().getClaimAt(world, finalPos);
            if (claim2 != null && denyForVisitor(claim2, player, claim2.getFlags().blockBuilding)) {
                deny(player, "[!] No puedes construir aquí.");
                return class_1269.field_5814;
            }
        }
        if (isInteractiveBlock(clickedState)
                && (claim = ClaimManager.getInstance().getClaimAt(world, pos)) != null
                && denyForVisitor(claim, player, claim.getFlags().blockBuilding)) {
            deny(player, "[!] No puedes interactuar aquí.");
            return class_1269.field_5814;
        }
        return class_1269.field_5811;
    }

    private static void registerItemUseEvent() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            class_1799 stack = player.method_5998(hand);
            if (world.field_9236) return class_1271.method_22430(stack);
            if (isBypassing(player)) return class_1271.method_22430(stack);
            // Items de claim no se "usan en el aire"; ignorar.
            if (ClaimBlocks.readTierId(stack) != null) return class_1271.method_22430(stack);
            Claim claim = ClaimManager.getInstance().getClaimAt(world, player.method_24515());
            if (claim == null || claim.canModify(player)) return class_1271.method_22430(stack);
            if (claim.getFlags().publicMode || claim.getFlags().blockItemUse) {
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
            if (state.method_28498(class_2741.field_12550)) return state.method_11654(class_2741.field_12550) >= 7;
            if (state.method_28498(class_2741.field_12497)) return state.method_11654(class_2741.field_12497) >= 3;
        }
        if (b instanceof class_2421) return state.method_11654(class_2421.field_11306) >= 3;
        if (state.method_28498(class_2741.field_12550) && state.method_11654(class_2741.field_12550) >= 7) return true;
        if (state.method_28498(class_2741.field_12497) && state.method_11654(class_2741.field_12497) >= 3) return true;
        return false;
    }

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
