// 
// Decompiled by Procyon v0.6.0
// 

package com.fscrates.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

public class ParticleLayer
{
    public String particleId;
    public Phase phase;
    public Shape shape;
    public int count;
    public double speed;
    public double spread;
    public double radius;
    public double yOffset;
    public boolean useRarityColor;
    public String colorHex;
    public int interval;
    
    public ParticleLayer() {
        this.particleId = "minecraft:enchant";
        this.phase = Phase.IDLE;
        this.shape = Shape.HALO;
        this.count = 4;
        this.speed = 0.04;
        this.spread = 0.4;
        this.radius = 0.85;
        this.yOffset = 1.1;
        this.useRarityColor = true;
        this.colorHex = "#FFFFFF";
        this.interval = 4;
        this.applyShapeDefaults();
    }
    
    public ParticleLayer(final String id, final Phase phase, final Shape shape) {
        this.particleId = "minecraft:enchant";
        this.phase = Phase.IDLE;
        this.shape = Shape.HALO;
        this.count = 4;
        this.speed = 0.04;
        this.spread = 0.4;
        this.radius = 0.85;
        this.yOffset = 1.1;
        this.useRarityColor = true;
        this.colorHex = "#FFFFFF";
        this.interval = 4;
        this.particleId = id;
        this.phase = phase;
        this.shape = shape;
        this.applyShapeDefaults();
    }
    
    public void applyShapeDefaults() {
        switch (this.shape) {
            case HALO: {
                this.radius = 0.85;
                this.yOffset = 1.1;
                this.spread = 0.2;
                this.count = 6;
                this.speed = 0.02;
                break;
            }
            case RING: {
                this.radius = 0.95;
                this.yOffset = 0.45;
                this.spread = 0.05;
                this.count = 24;
                this.speed = 0.02;
                break;
            }
            case BURST: {
                this.radius = 0.1;
                this.yOffset = 0.85;
                this.spread = 0.55;
                this.count = 14;
                this.speed = 0.3;
                break;
            }
            case COLUMN: {
                this.radius = 0.1;
                this.yOffset = 0.85;
                this.spread = 0.2;
                this.count = 6;
                this.speed = 0.1;
                break;
            }
            case SPIRAL: {
                this.radius = 0.75;
                this.yOffset = 0.4;
                this.spread = 0.1;
                this.count = 12;
                this.speed = 0.04;
                break;
            }
            case FOUNTAIN: {
                this.radius = 0.05;
                this.yOffset = 0.85;
                this.spread = 0.45;
                this.count = 10;
                this.speed = 0.3;
                break;
            }
            case VORTEX: {
                this.radius = 0.85;
                this.yOffset = 0.95;
                this.spread = 0.05;
                this.count = 16;
                this.speed = 0.05;
                break;
            }
            case RAIN: {
                this.radius = 0.0;
                this.yOffset = 1.8;
                this.spread = 1.1;
                this.count = 6;
                this.speed = 0.3;
                break;
            }
            case POINT: {
                this.radius = 0.0;
                this.yOffset = 0.9;
                this.spread = 0.05;
                this.count = 1;
                this.speed = 0.04;
                break;
            }
        }
    }
    
    public CompoundTag save() {
        final CompoundTag t = new CompoundTag();
        t.m_128359_("p", this.particleId);
        t.m_128359_("phase", this.phase.name());
        t.m_128359_("shape", this.shape.name());
        t.m_128405_("count", this.count);
        t.m_128347_("speed", this.speed);
        t.m_128347_("spread", this.spread);
        t.m_128347_("radius", this.radius);
        t.m_128347_("y", this.yOffset);
        t.m_128379_("tier", this.useRarityColor);
        t.m_128359_("hex", this.colorHex);
        t.m_128405_("interval", this.interval);
        return t;
    }
    
    public static ParticleLayer load(final CompoundTag t) {
        final ParticleLayer l = new ParticleLayer();
        l.particleId = (t.m_128441_("p") ? t.m_128461_("p") : "minecraft:enchant");
        l.phase = enumOr(Phase.class, t.m_128461_("phase"), Phase.IDLE);
        l.shape = enumOr(Shape.class, t.m_128461_("shape"), Shape.HALO);
        l.count = (t.m_128441_("count") ? t.m_128451_("count") : 4);
        l.speed = (t.m_128441_("speed") ? t.m_128459_("speed") : 0.04);
        l.spread = (t.m_128441_("spread") ? t.m_128459_("spread") : 0.4);
        l.radius = (t.m_128441_("radius") ? t.m_128459_("radius") : 0.85);
        l.yOffset = (t.m_128441_("y") ? t.m_128459_("y") : 1.1);
        l.useRarityColor = (!t.m_128441_("tier") || t.m_128471_("tier"));
        l.colorHex = (t.m_128441_("hex") ? t.m_128461_("hex") : "#FFFFFF");
        l.interval = (t.m_128441_("interval") ? Math.max(1, t.m_128451_("interval")) : 4);
        return l;
    }
    
    public ParticleLayer copy() {
        return load(this.save());
    }
    
    public String shortLabel() {
        final String pid = this.particleId.contains(":") ? this.particleId.substring(this.particleId.indexOf(58) + 1) : this.particleId;
        return ParticleNames.spanish(pid) + " §8[" + this.phase.label + "/" + this.shape.label;
    }
    
    private static <E extends Enum<E>> E enumOr(final Class<E> type, final String name, final E def) {
        try {
            return Enum.valueOf(type, name);
        }
        catch (final Exception e) {
            return def;
        }
    }
    
    public static List<ParticleLayer> defaults() {
        final List<ParticleLayer> list = new ArrayList<ParticleLayer>();
        final ParticleLayer halo = new ParticleLayer("minecraft:enchant", Phase.IDLE, Shape.HALO);
        halo.count = 3;
        halo.interval = 4;
        list.add(halo);
        final ParticleLayer ring = new ParticleLayer("minecraft:dust", Phase.IDLE, Shape.RING);
        ring.count = 12;
        ring.interval = 8;
        list.add(ring);
        final ParticleLayer open = new ParticleLayer("minecraft:end_rod", Phase.OPEN, Shape.FOUNTAIN);
        open.count = 12;
        open.speed = 0.3;
        open.spread = 0.35;
        list.add(open);
        final ParticleLayer reveal = new ParticleLayer("minecraft:dust", Phase.REVEAL, Shape.VORTEX);
        reveal.count = 18;
        reveal.radius = 0.95;
        reveal.yOffset = 1.15;
        list.add(reveal);
        final ParticleLayer finale = new ParticleLayer("minecraft:firework", Phase.FINALE, Shape.BURST);
        finale.count = 28;
        finale.speed = 0.45;
        finale.spread = 0.7;
        finale.yOffset = 1.0;
        list.add(finale);
        return list;
    }
    
    public enum Phase
    {
        IDLE("Reposo"), 
        ANTICIPATION("Tensi\u00f3n"), 
        OPEN("Apertura"), 
        REVEAL("Revelaci\u00f3n"), 
        FINALE("Final");
        
        public final String label;
        
        private Phase(final String l) {
            this.label = l;
        }
        
        public Phase next() {
            final Phase[] v = values();
            return v[(this.ordinal() + 1) % v.length];
        }
    }
    
    public enum Shape
    {
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
        
        private Shape(final String l) {
            this.label = l;
        }
        
        public Shape next() {
            final Shape[] v = values();
            return v[(this.ordinal() + 1) % v.length];
        }
    }
}
