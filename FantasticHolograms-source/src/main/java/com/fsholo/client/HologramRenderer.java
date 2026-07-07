/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.Font$DisplayMode
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RenderLevelStageEvent
 *  net.minecraftforge.client.event.RenderLevelStageEvent$Stage
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 */
package com.fsholo.client;

import com.fsholo.client.ClientHolograms;
import com.fsholo.data.HoloLine;
import com.fsholo.data.Hologram;
import com.fsholo.util.HoloColors;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid="fsholo", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class HologramRenderer {
    private static int frame;

    private HologramRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || ClientHolograms.all().isEmpty()) {
            return;
        }
        String dim = mc.level.dimension().location().toString();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Quaternionf rot = camera.rotation();
        org.joml.Vector3f leftv = camera.getLeftVector();
        double rightX = -leftv.x();
        double rightZ = -leftv.z();
        double rlen = Math.sqrt(rightX * rightX + rightZ * rightZ);
        if (rlen > 1.0E-4) {
            rightX /= rlen;
            rightZ /= rlen;
        } else {
            rightX = 1.0;
            rightZ = 0.0;
        }
        PoseStack pose = event.getPoseStack();
        Font font = mc.font;
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        ++frame;
        float animTime = (float)(System.currentTimeMillis() % 100000L) / 1000.0f;
        float[] anim = new float[3];
        for (Hologram h : ClientHolograms.all()) {
            if (!dim.equals(h.dimension)) continue;
            int n = h.lines.size();
            int bg = (int)(Math.max(0.0f, Math.min(1.0f, h.background)) * 255.0f) << 24;
            for (int i = 0; i < n; ++i) {
                double ly = h.y + h.yOffset + (double)(n - 1 - i) * h.lineSpacing;
                HoloLine ln = h.lines.get(i);
                com.fsholo.util.HoloAnimations.compute(h.animation, animTime, h.animSpeed, h.animIntensity, i, n, anim);
                double ax = h.x + rightX * (double)anim[0];
                double az = h.z + rightZ * (double)anim[0];
                double ay = ly + (double)anim[1];
                float ascale = h.scale * anim[2];
                HologramRenderer.renderLine(pose, buffer, font, ln, ax, ay, az, cam, rot, ascale, bg);
                if (ln.particles && frame % com.fsholo.util.HoloParticles.rateFrames(ln.particleRate) == 0) {
                    double dx = ax - cam.x;
                    double dy = ay - cam.y;
                    double dz = az - cam.z;
                    if (dx * dx + dy * dy + dz * dz < 2304.0) {
                        float ps = 0.025f * Math.max(0.1f, ascale);
                        String plain = HoloColors.strip(ln.text);
                        double halfW = (double)font.width(plain) * (double)ps / 2.0;
                        double halfH = (double)font.lineHeight * (double)ps / 2.0;
                        com.fsholo.util.HoloParticles.spawn(mc.level, ax, ay + halfH, az, rightX, rightZ, halfW, halfH, ln, mc.player.getRandom());
                    }
                }
            }
        }
        buffer.endBatch();
    }

    private static void renderLine(PoseStack pose, MultiBufferSource.BufferSource buffer, Font font, HoloLine line, double wx, double wy, double wz, Vec3 cam, Quaternionf rot, float scale, int bgColor) {
        pose.pushPose();
        pose.translate(wx - cam.x, wy - cam.y, wz - cam.z);
        pose.mulPose(rot);
        float s = 0.025f * Math.max(0.1f, scale);
        pose.scale(-s, -s, s);
        Matrix4f matrix = pose.last().pose();
        int light = 0xF000F0;
        Style base = Style.EMPTY.withBold(Boolean.valueOf(line.bold)).withItalic(Boolean.valueOf(line.italic)).withUnderlined(Boolean.valueOf(line.underline)).withStrikethrough(Boolean.valueOf(line.strikethrough)).withObfuscated(Boolean.valueOf(line.obfuscated));
        if (line.gradient || line.rainbow) {
            String txt = HoloColors.strip(line.text);
            if (!txt.isEmpty()) {
                float total = 0.0f;
                for (int i = 0; i < txt.length(); ++i) {
                    total += (float)font.width((FormattedText)Component.literal((String)String.valueOf(txt.charAt(i))).withStyle(base));
                }
                int len = txt.length();
                int from = HoloColors.parse(line.gradFrom, 0xFF5555);
                int to = HoloColors.parse(line.gradTo, 0x55AAFF);
                float time = (float)(System.currentTimeMillis() % 3000L) / 3000.0f;
                float x = -total / 2.0f;
                for (int i = 0; i < len; ++i) {
                    int color = line.rainbow ? HoloColors.rainbowColor(line.rainbowStyle, (float)i / (float)Math.max(1, len), time) : HoloColors.lerp(from, to, len <= 1 ? 0.0f : (float)i / (float)(len - 1));
                    MutableComponent ch = Component.literal((String)String.valueOf(txt.charAt(i))).withStyle(base.withColor(TextColor.fromRgb((int)color)));
                    font.drawInBatch((Component)ch, x, 0.0f, 0xFF000000 | color, line.shadow, matrix, (MultiBufferSource)buffer, Font.DisplayMode.NORMAL, bgColor, light);
                    x += (float)font.width((FormattedText)ch);
                }
            }
        } else {
            int color = HoloColors.parse(line.color, 0xFFFFFF);
            MutableComponent comp = Component.literal((String)HoloColors.amp(line.text)).withStyle(base.withColor(TextColor.fromRgb((int)color)));
            float w = font.width((FormattedText)comp);
            font.drawInBatch((Component)comp, -w / 2.0f, 0.0f, 0xFF000000 | color, line.shadow, matrix, (MultiBufferSource)buffer, Font.DisplayMode.NORMAL, bgColor, light);
        }
        pose.popPose();
    }
}

