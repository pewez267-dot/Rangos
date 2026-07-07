/*
 * Decompiled with CFR 0.152.
 */
package com.claimblocks;

import com.claimblocks.command.ClaimAdminCommands;
import com.claimblocks.command.ClaimCommands;
import com.claimblocks.data.Claim;
import com.claimblocks.data.ClaimManager;
import com.claimblocks.data.GlobalFlags;
import com.claimblocks.event.BlockProtectionEvents;
import com.claimblocks.event.EntityProtectionEvents;
import com.claimblocks.event.PassiveEffectsManager;
import com.claimblocks.event.PlayerTracker;
import com.claimblocks.gui.ClaimMenuHandler;
import com.claimblocks.item.ClaimItems;
import com.claimblocks.net.ClaimBordersPacket;
import com.claimblocks.net.ClaimNetwork;
import com.claimblocks.render.ParticleBorder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

@Mod(value="claimblocks")
public class ClaimBlocksMod {
    public static final String MOD_ID = "claimblocks";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static int particleCounter = 0;

    public ClaimBlocksMod() {
        LOGGER.info("[ClaimBlocks] Inicializando v7.6.0 (Forge 1.20.1)...");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ClaimItems.register(modBus);
        ClaimNetwork.init();
        MinecraftForge.EVENT_BUS.register((Object)this);
        MinecraftForge.EVENT_BUS.register((Object)new BlockProtectionEvents());
        MinecraftForge.EVENT_BUS.register((Object)new EntityProtectionEvents());
        MinecraftForge.EVENT_BUS.register((Object)new PlayerTracker());
        LOGGER.info("[ClaimBlocks] Eventos, items y red registrados.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ClaimCommands.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
        ClaimAdminCommands.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
        ClaimBlocksMod.registerMergeCommand(event.getDispatcher());
    }

    // /claimmerge accept|reject <code> | /claimmerge leave  (para los botones de invitacion)
    private static void registerMergeCommand(CommandDispatcher<CommandSourceStack> d) {
        d.register(net.minecraft.commands.Commands.literal("claimmerge")
            .then(net.minecraft.commands.Commands.literal("accept")
                .then(net.minecraft.commands.Commands.argument("code", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> {
                        ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                        ClaimMenuHandler.acceptMerge(p, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "code"));
                        return 1;
                    })))
            .then(net.minecraft.commands.Commands.literal("reject")
                .then(net.minecraft.commands.Commands.argument("code", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> {
                        ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                        ClaimMenuHandler.rejectMerge(p, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "code"));
                        return 1;
                    })))
            .then(net.minecraft.commands.Commands.literal("leave")
                .executes(ctx -> {
                    ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                    ClaimMenuHandler.leaveMerge(p);
                    return 1;
                })));
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ClaimManager.getInstance().load(event.getServer());
        GlobalFlags.getInstance().load(event.getServer());
        LOGGER.info("[ClaimBlocks] Datos cargados.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ClaimManager.getInstance().save();
        GlobalFlags.getInstance().save(event.getServer());
        LOGGER.info("[ClaimBlocks] Datos guardados al apagar.");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            ClaimManager.getInstance().flushPendingTo(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerTracker.onDisconnect(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server;
        if (event.phase == TickEvent.Phase.END && (server = ServerLifecycleHooks.getCurrentServer()) != null) {
            PlayerTracker.tick(server);
            BlockProtectionEvents.tickFireSweep(server);
            PassiveEffectsManager.tick(server);
            if (++particleCounter % 4 == 0) {
                ClaimBlocksMod.renderClaimParticles(server);
            }
            if (particleCounter % 20 == 0) {
                ClaimBlocksMod.sendBorderPackets(server);
            }
        }
    }

    private static void sendBorderPackets(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            String dim = level.dimension().location().toString();
            for (ServerPlayer player : level.players()) {
                ArrayList<double[]> boxes = new ArrayList<double[]>();
                HashSet<UUID> doneClaims = new HashSet<UUID>();
                HashSet<UUID> doneGroups = new HashSet<UUID>();
                Claim here = ClaimManager.getInstance().getClaimAt((Level)level, player.blockPosition());
                if (here != null && here.getFlags().showBorder && here.canModify((Player)player)) {
                    ClaimBlocksMod.addBorder(boxes, here, player, dim, doneClaims, doneGroups);
                }
                for (Claim owned : ClaimManager.getInstance().getClaimsOf(player.getUUID())) {
                    if (!owned.getWorld().equals(dim) || !owned.getFlags().showBorder || !ParticleBorder.withinRenderRange(player, owned)) continue;
                    ClaimBlocksMod.addBorder(boxes, owned, player, dim, doneClaims, doneGroups);
                }
                ClaimNetwork.sendTo(player, new ClaimBordersPacket(boxes));
            }
        }
    }

    // Contorno de una claim: si esta agrupada, dibuja el contorno UNIFICADO del grupo
    // (una sola vez por grupo); si no, la caja normal.
    private static void addBorder(ArrayList<double[]> boxes, Claim claim, ServerPlayer player, String dim, HashSet<UUID> doneClaims, HashSet<UUID> doneGroups) {
        if (claim.getGroupId() != null) {
            UUID gid = claim.getGroupId();
            if (doneGroups.contains(gid)) {
                return;
            }
            doneGroups.add(gid);
            ClaimBlocksMod.addGroupOutline(boxes, gid, player, dim);
        } else {
            if (doneClaims.contains(claim.getClaimId())) {
                return;
            }
            doneClaims.add(claim.getClaimId());
            boxes.add(ClaimBlocksMod.boxOf(claim));
        }
    }

    // Perimetro de la UNION del grupo, como paredes verticales (cajas planas).
    // Recorre las celdas cercanas al jugador y emite una pared en cada frontera
    // cubierto/descubierto, fusionando tramos colineales. Un solo contorno irregular.
    private static void addGroupOutline(ArrayList<double[]> boxes, UUID gid, ServerPlayer player, String dim) {
        ClaimManager mgr = ClaimManager.getInstance();
        Claim mother = mgr.getMotherClaim(gid);
        if (mother == null) {
            return;
        }
        java.util.List<Claim> gc = new ArrayList<Claim>();
        for (Claim c : mgr.getGroupClaims(gid)) {
            if (c.getWorld().equals(dim)) {
                gc.add(c);
            }
        }
        if (gc.isEmpty()) {
            return;
        }
        double minY = mother.getY() - mother.getOwnHeight();
        double maxY = mother.getY() + mother.getOwnHeight() + 1;
        float cr = 1.0f, cg = 1.0f, cb = 1.0f;
        if (mother.getTier() != null) {
            cr = mother.getTier().r;
            cg = mother.getTier().g;
            cb = mother.getTier().b;
        }
        int px = player.blockPosition().getX();
        int pz = player.blockPosition().getZ();
        int R = 28;
        int x0 = px - R, x1 = px + R, z0 = pz - R, z1 = pz + R;
        // Paredes verticales (planos X): frontera entre columnas x-1 y x.
        for (int x = x0; x <= x1 + 1; ++x) {
            int runStart = Integer.MIN_VALUE;
            for (int z = z0; z <= z1; ++z) {
                boolean edge = ClaimBlocksMod.covered(gc, x - 1, z) != ClaimBlocksMod.covered(gc, x, z);
                if (edge && runStart == Integer.MIN_VALUE) {
                    runStart = z;
                } else if (!edge && runStart != Integer.MIN_VALUE) {
                    boxes.add(new double[]{x, minY, runStart, x, maxY, z, cr, cg, cb});
                    runStart = Integer.MIN_VALUE;
                }
            }
            if (runStart != Integer.MIN_VALUE) {
                boxes.add(new double[]{x, minY, runStart, x, maxY, z1 + 1, cr, cg, cb});
            }
        }
        // Paredes horizontales (planos Z): frontera entre filas z-1 y z.
        for (int z = z0; z <= z1 + 1; ++z) {
            int runStart = Integer.MIN_VALUE;
            for (int x = x0; x <= x1; ++x) {
                boolean edge = ClaimBlocksMod.covered(gc, x, z - 1) != ClaimBlocksMod.covered(gc, x, z);
                if (edge && runStart == Integer.MIN_VALUE) {
                    runStart = x;
                } else if (!edge && runStart != Integer.MIN_VALUE) {
                    boxes.add(new double[]{runStart, minY, z, x, maxY, z, cr, cg, cb});
                    runStart = Integer.MIN_VALUE;
                }
            }
            if (runStart != Integer.MIN_VALUE) {
                boxes.add(new double[]{runStart, minY, z, x1 + 1, maxY, z, cr, cg, cb});
            }
        }
    }

    private static boolean covered(java.util.List<Claim> gc, int x, int z) {
        for (Claim c : gc) {
            if (Math.abs(x - c.getX()) <= c.getRadius() && Math.abs(z - c.getZ()) <= c.getRadius()) {
                return true;
            }
        }
        return false;
    }

    private static double[] boxOf(Claim claim) {
        int r = claim.getRadius();
        int h = claim.getHeight();
        float cr = 1.0f;
        float cg = 1.0f;
        float cb = 1.0f;
        if (claim.getTier() != null) {
            cr = claim.getTier().r;
            cg = claim.getTier().g;
            cb = claim.getTier().b;
        }
        return new double[]{claim.getX() - r, claim.getY() - h, claim.getZ() - r, claim.getX() + r + 1, claim.getY() + h + 1, claim.getZ() + r + 1, cr, cg, cb};
    }

    private static void renderClaimParticles(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            String dim = level.dimension().location().toString();
            for (ServerPlayer player : level.players()) {
                HashSet<UUID> rendered = new HashSet<UUID>();
                Claim here = ClaimManager.getInstance().getClaimAt((Level)level, player.blockPosition());
                if (here != null && here.getFlags().showParticles && here.canModify((Player)player)) {
                    ParticleBorder.fillClaim(level, player, here);
                    rendered.add(here.getClaimId());
                }
                for (Claim owned : ClaimManager.getInstance().getClaimsOf(player.getUUID())) {
                    if (rendered.contains(owned.getClaimId()) || !owned.getFlags().showParticles || !owned.getWorld().equals(dim) || !ParticleBorder.withinRenderRange(player, owned)) continue;
                    ParticleBorder.fillClaim(level, player, owned);
                    rendered.add(owned.getClaimId());
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ClaimMenuHandler.handleChat(event);
    }

    @SubscribeEvent
    public void onCommandEvent(CommandEvent var1) {
    }
}

