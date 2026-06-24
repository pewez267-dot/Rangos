package com.fspawner.integration;

import com.fspawner.FSpawner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Soft (reflection based) integration with Infernal Mobs. FSpawner never imports
 * Infernal Mobs classes directly, so the mod loads fine whether or not Infernal
 * Mobs is installed. When present we call
 * {@code InfernalMobsCore.instance().addEntityModifiersByString(entity, mods)}.
 */
public final class InfernalMobsIntegration {

    private InfernalMobsIntegration() {}

    private static final String MOD_ID = "infernalmobs";
    private static final String CORE_CLASS = "atomicstryker.infernalmobs.common.InfernalMobsCore";

    private static Boolean available;
    private static Object coreInstance;
    private static Method instanceMethod;
    private static Method addModifiersMethod;
    private static boolean reflectionFailed = false;

    /** Whether Infernal Mobs is loaded. Cached after first call. */
    public static boolean isLoaded() {
        if (available == null) {
            available = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
        }
        return available;
    }

    private static boolean ensureReflection() {
        if (!isLoaded() || reflectionFailed) {
            return false;
        }
        if (addModifiersMethod != null && coreInstance != null) {
            return true;
        }
        try {
            Class<?> coreClass = Class.forName(CORE_CLASS);
            instanceMethod = coreClass.getMethod("instance");
            coreInstance = instanceMethod.invoke(null);
            addModifiersMethod = coreClass.getMethod("addEntityModifiersByString",
                    LivingEntity.class, String.class);
            return coreInstance != null;
        } catch (Throwable t) {
            reflectionFailed = true;
            FSpawner.LOGGER.warn("[FSpawner] Failed to bind Infernal Mobs reflection: {}", t.toString());
            return false;
        }
    }

    /**
     * Applies the given space-separated modifier names (e.g. "Berserk Storm Ninja")
     * to the entity. Does nothing if the string is blank or Infernal Mobs is absent.
     */
    public static void applyModifiers(LivingEntity entity, String modifierString) {
        if (entity == null || modifierString == null || modifierString.isBlank()) {
            return;
        }
        if (!ensureReflection()) {
            return;
        }
        try {
            addModifiersMethod.invoke(coreInstance, entity, modifierString.trim());
        } catch (Throwable t) {
            FSpawner.LOGGER.warn("[FSpawner] Could not apply Infernal modifiers '{}': {}",
                    modifierString, t.toString());
        }
    }
}
