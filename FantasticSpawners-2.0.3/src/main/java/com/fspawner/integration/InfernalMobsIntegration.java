// 
// Decompiled by Procyon v0.6.0
// 

package com.fspawner.integration;

import com.fspawner.FSpawner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;

public final class InfernalMobsIntegration
{
    private static final String MOD_ID = "infernalmobs";
    private static final String CORE_CLASS = "atomicstryker.infernalmobs.common.InfernalMobsCore";
    private static Boolean available;
    private static Object coreInstance;
    private static Method instanceMethod;
    private static Method addModifiersMethod;
    private static boolean reflectionFailed;
    
    private InfernalMobsIntegration() {
    }
    
    public static boolean isLoaded() {
        if (InfernalMobsIntegration.available == null) {
            InfernalMobsIntegration.available = (ModList.get() != null && ModList.get().isLoaded("infernalmobs"));
        }
        return InfernalMobsIntegration.available;
    }
    
    private static boolean ensureReflection() {
        if (!isLoaded() || InfernalMobsIntegration.reflectionFailed) {
            return false;
        }
        if (InfernalMobsIntegration.addModifiersMethod != null && InfernalMobsIntegration.coreInstance != null) {
            return true;
        }
        try {
            final Class<?> coreClass = Class.forName("atomicstryker.infernalmobs.common.InfernalMobsCore");
            InfernalMobsIntegration.instanceMethod = coreClass.getMethod("instance", (Class<?>[])new Class[0]);
            InfernalMobsIntegration.coreInstance = InfernalMobsIntegration.instanceMethod.invoke(null, new Object[0]);
            InfernalMobsIntegration.addModifiersMethod = coreClass.getMethod("addEntityModifiersByString", LivingEntity.class, String.class);
            return InfernalMobsIntegration.coreInstance != null;
        }
        catch (final Throwable t) {
            InfernalMobsIntegration.reflectionFailed = true;
            FSpawner.LOGGER.warn("[FSpawner] Failed to bind Infernal Mobs reflection: {}", t.toString());
            return false;
        }
    }
    
    public static void applyModifiers(final LivingEntity entity, final String modifierString) {
        if (entity == null || modifierString == null || modifierString.isBlank()) {
            return;
        }
        if (!ensureReflection()) {
            return;
        }
        try {
            InfernalMobsIntegration.addModifiersMethod.invoke(InfernalMobsIntegration.coreInstance, entity, modifierString.trim());
        }
        catch (final Throwable t) {
            FSpawner.LOGGER.warn("[FSpawner] Could not apply Infernal modifiers '{}': {}", modifierString, t.toString());
        }
    }
    
    static {
        InfernalMobsIntegration.reflectionFailed = false;
    }
}
