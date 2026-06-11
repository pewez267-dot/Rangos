package com.fscrates.config;

import net.minecraft.nbt.CompoundTag;

/**
 * A fully editable particle "layer". A crate can have any number of these. Each
 * layer says WHICH particle, in WHICH phase of the opening, in WHAT shape, and
 * with how much count/speed/spread/colour. This is what powers the in-game
 * particle editor — the renderer/blockentity just plays every layer.
 */
public class ParticleLayer {

    /** When the layer emits during the crate's life/opening. */
    public enum Phase {
        IDLE("Reposo"), ANTICIPATION("Tensi\u00f3n"), OPEN("Apertura"),
        REVEAL("Revelaci\u00f3n"), FINALE("Final");
        public final String label;
        Phase(String l) { this.label = l; }
        public Phase next() { Phase[] v = values(); return v[(ordinal() + 1) % v.length]; }
    }

    /** The emission shape/motion of the layer. */
    public enum Shape {
        HALO("Halo"),
        RING("Anillo"),
        BURST("Estallido"),
        COLUMN("Columna"),
        SPIRAL("Espiral"),
        FOUNTAIN("Fuente"),
        VORTEX("V\u00f3rtice"),
        RAIN("Lluvia"),
        POINT("Punto");
        public final String label;
        Shape(String l) { this.label = l; }
        public Shape next() { Shape[] v = values(); return v[(ordinal() + 1) % v.length]; }
    }

    public String particleId = "minecraft:enchant";
    public Phase phase = Phase.IDLE;
    public Shape shape = Shape.HALO;
    public int count = 4;
    public double speed = 0.04;
    public double spread = 0.4;
    public double radius = 0.85;
    public double yOffset = 1.10;
    /** For coloured particles (dust): use the crate tier colour or a custom hex. */
    public boolean useRarityColor = true;
    public String colorHex = "#FFFFFF";
    /** Emit every N game-ticks (used by IDLE; pulse phases emit each tick). */
    public int interval = 4;

    public ParticleLayer() {
        applyShapeDefaults();
    }

    public ParticleLayer(String id, Phase phase, Shape shape) {
        this.particleId = id;
        this.phase = phase;
        this.shape = shape;
        applyShapeDefaults();
    }

    /**
     * Sets radius/yOffset/spread/count to reasonable values for the current
     * shape so a brand-new layer (or one that just changed shape) produces a
     * particle pattern <b>outside</b> the chest, not inside it.
     */
    public void applyShapeDefaults() {
        switch (shape) {
            case HALO -> {        radius = 0.85; yOffset = 1.10; spread = 0.20; count = 6; speed = 0.02; }
            case RING -> {        radius = 0.95; yOffset = 0.45; spread = 0.05; count = 24; speed = 0.02; }
            case BURST -> {       radius = 0.10; yOffset = 0.85; spread = 0.55; count = 14; speed = 0.30; }
            case COLUMN -> {      radius = 0.10; yOffset = 0.85; spread = 0.20; count = 6;  speed = 0.10; }
            case SPIRAL -> {      radius = 0.75; yOffset = 0.40; spread = 0.10; count = 12; speed = 0.04; }
            case FOUNTAIN -> {    radius = 0.05; yOffset = 0.85; spread = 0.45; count = 10; speed = 0.30; }
            case VORTEX -> {      radius = 0.85; yOffset = 0.95; spread = 0.05; count = 16; speed = 0.05; }
            case RAIN -> {        radius = 0.00; yOffset = 1.80; spread = 1.10; count = 6;  speed = 0.30; }
            case POINT -> {       radius = 0.00; yOffset = 0.90; spread = 0.05; count = 1;  speed = 0.04; }
        }
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("p", particleId);
        t.putString("phase", phase.name());
        t.putString("shape", shape.name());
        t.putInt("count", count);
        t.putDouble("speed", speed);
        t.putDouble("spread", spread);
        t.putDouble("radius", radius);
        t.putDouble("y", yOffset);
        t.putBoolean("tier", useRarityColor);
        t.putString("hex", colorHex);
        t.putInt("interval", interval);
        return t;
    }

    public static ParticleLayer load(CompoundTag t) {
        ParticleLayer l = new ParticleLayer();
        l.particleId = t.contains("p") ? t.getString("p") : "minecraft:enchant";
        l.phase = enumOr(Phase.class, t.getString("phase"), Phase.IDLE);
        l.shape = enumOr(Shape.class, t.getString("shape"), Shape.HALO);
        l.count = t.contains("count") ? t.getInt("count") : 4;
        l.speed = t.contains("speed") ? t.getDouble("speed") : 0.04;
        l.spread = t.contains("spread") ? t.getDouble("spread") : 0.4;
        l.radius = t.contains("radius") ? t.getDouble("radius") : 0.85;
        l.yOffset = t.contains("y") ? t.getDouble("y") : 1.10;
        l.useRarityColor = !t.contains("tier") || t.getBoolean("tier");
        l.colorHex = t.contains("hex") ? t.getString("hex") : "#FFFFFF";
        l.interval = t.contains("interval") ? Math.max(1, t.getInt("interval")) : 4;
        return l;
    }

    public ParticleLayer copy() {
        return load(this.save());
    }

    public String shortLabel() {
        String pid = particleId.contains(":") ? particleId.substring(particleId.indexOf(':') + 1) : particleId;
        return ParticleNames.spanish(pid) + " \u00A78[" + phase.label + "/" + shape.label + "]";
    }

    private static <E extends Enum<E>> E enumOr(Class<E> type, String name, E def) {
        try {
            return Enum.valueOf(type, name);
        } catch (Exception e) {
            return def;
        }
    }

    /** A pleasant default set of layers for a brand-new crate. */
    public static java.util.List<ParticleLayer> defaults() {
        java.util.List<ParticleLayer> list = new java.util.ArrayList<>();
        ParticleLayer halo = new ParticleLayer("minecraft:enchant", Phase.IDLE, Shape.HALO);
        halo.count = 3; halo.interval = 4;
        list.add(halo);
        ParticleLayer ring = new ParticleLayer("minecraft:dust", Phase.IDLE, Shape.RING);
        ring.count = 12; ring.interval = 8;
        list.add(ring);
        ParticleLayer open = new ParticleLayer("minecraft:end_rod", Phase.OPEN, Shape.FOUNTAIN);
        open.count = 12; open.speed = 0.30; open.spread = 0.35;
        list.add(open);
        ParticleLayer reveal = new ParticleLayer("minecraft:dust", Phase.REVEAL, Shape.VORTEX);
        reveal.count = 18; reveal.radius = 0.95; reveal.yOffset = 1.15;
        list.add(reveal);
        ParticleLayer finale = new ParticleLayer("minecraft:firework", Phase.FINALE, Shape.BURST);
        finale.count = 28; finale.speed = 0.45; finale.spread = 0.7; finale.yOffset = 1.0;
        list.add(finale);
        return list;
    }
}
