package com.fsrecipes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Reload listener que se anade DESPUES del RecipeManager (via AddReloadListenerEvent). Cuando el
 * juego recarga datapacks (arranque del servidor o /reload), el RecipeManager vuelve a cargar TODAS
 * las recetas; este listener corre justo despues en la fase de "apply" y reaplica los baneos
 * guardados, de modo que las recetas prohibidas se vuelven a quitar antes de sincronizar a los
 * clientes.
 */
public final class BanReloadListener implements PreparableReloadListener {

    private final RecipeManager recipeManager;
    private final RegistryAccess registryAccess;

    public BanReloadListener(RecipeManager recipeManager, RegistryAccess registryAccess) {
        this.recipeManager = recipeManager;
        this.registryAccess = registryAccess;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier prepBarrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return prepBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            // Fase apply (hilo principal): el manager ya trae TODAS las recetas frescas.
            RecipeBans.loadFromDisk();
            RecipeBans.applyToManager(this.recipeManager, this.registryAccess, true);
        }, gameExecutor);
    }
}
