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
        LOGGER.info("[ClaimBlocks] Inicializando v7.6.2 (Forge 1.20.1)...");
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

    // Las zonas AGRUPADAS dibujan su contorno con PARTICULAS (polvillo blanco), no lineas.
    // Solo las claims sueltas usan la caja de lineas normal.
    private static void addBorder(ArrayList<double[]> boxes, Claim claim, ServerPlayer player, String dim, HashSet<UUID> doneClaims, HashSet<UUID> doneGroups) {
        if (claim.getGroupId() != null) {
            return;
        }
        if (doneClaims.contains(claim.getClaimId())) {
            return;
        }
        doneClaims.add(claim.getClaimId());
        boxes.add(ClaimBlocksMod.boxOf(claim));
    }

    // Contorno de la UNION del grupo dibujado con POLVILLO magico blanco (end_rod).
    // Escanea las celdas cercanas al jugador y suelta particulas en cada frontera
    // cubierto/descubierto, a una altura aleatoria de la banda de la nodriza.
    private static void spawnGroupDust(ServerLevel level, ServerPlayer player, UUID gid) {
        ClaimManager mgr = ClaimManager.getInstance();
        Claim mother = mgr.getMotherClaim(gid);
        if (mother == null) {
            return;
        }
        String dim = level.dimension().location().toString();
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
        net.minecraft.util.RandomSource rnd = level.getRandom();
        int px = player.blockPosition().getX();
        int pz = player.blockPosition().getZ();
        int R = 22;
        int budget = 70;
        for (int x = px - R; x <= px + R + 1 && budget > 0; ++x) {
            for (int z = pz - R; z <= pz + R + 1 && budget > 0; ++z) {
                boolean cxz = ClaimBlocksMod.covered(gc, x, z);
                if (ClaimBlocksMod.covered(gc, x - 1, z) != cxz && rnd.nextFloat() < 0.12f) {
                    double y = minY + rnd.nextDouble() * (maxY - minY);
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.END_ROD, true, (double) x, y, (double) z + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                    --budget;
                }
                if (budget > 0 && ClaimBlocksMod.covered(gc, x, z - 1) != cxz && rnd.nextFloat() < 0.12f) {
                    double y = minY + rnd.nextDouble() * (maxY - minY);
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.END_ROD, true, (double) x + 0.5, y, (double) z, 1, 0.0, 0.0, 0.0, 0.0);
                    --budget;
                }
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
                HashSet<UUID> dustedGroups = new HashSet<UUID>();
                Claim here = ClaimManager.getInstance().getClaimAt((Level)level, player.blockPosition());
                if (here != null && here.canModify((Player)player)) {
                    if (here.getFlags().showParticles && rendered.add(here.getClaimId())) {
                        ParticleBorder.fillClaim(level, player, here);
                    }
                    if (here.getGroupId() != null && here.getFlags().showBorder && dustedGroups.add(here.getGroupId())) {
                        ClaimBlocksMod.spawnGroupDust(level, player, here.getGroupId());
                    }
                }
                for (Claim owned : ClaimManager.getInstance().getClaimsOf(player.getUUID())) {
                    if (!owned.getWorld().equals(dim) || !ParticleBorder.withinRenderRange(player, owned)) continue;
                    if (owned.getFlags().showParticles && !rendered.contains(owned.getClaimId())) {
                        rendered.add(owned.getClaimId());
                        ParticleBorder.fillClaim(level, player, owned);
                    }
                    if (owned.getGroupId() != null && owned.getFlags().showBorder && dustedGroups.add(owned.getGroupId())) {
                        ClaimBlocksMod.spawnGroupDust(level, player, owned.getGroupId());
                    }
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

