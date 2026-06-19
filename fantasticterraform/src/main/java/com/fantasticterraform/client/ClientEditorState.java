package com.fantasticterraform.client;

/**
 * Estado de editor del lado cliente: si el HUD esta activo y el progreso de la ultima
 * operacion masiva. Es puramente local; el servidor es la autoridad.
 */
public final class ClientEditorState {

    private static volatile boolean active;

    private static volatile String progressName = "";
    private static volatile int progressProcessed;
    private static volatile int progressTotal;
    private static volatile boolean progressDone = true;
    private static volatile long progressUpdatedAt;

    private ClientEditorState() {
    }

    public static void setActive(boolean value) {
        active = value;
        if (!value) {
            ClientSelectionState.clear();
            progressDone = true;
            ClientHudController.onEditorClosed();
        } else {
            ClientHudController.onEditorOpened();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static void updateProgress(String name, int processed, int total, boolean done) {
        progressName = name;
        progressProcessed = processed;
        progressTotal = total;
        progressDone = done;
        progressUpdatedAt = System.currentTimeMillis();
    }

    public static String progressName() {
        return progressName;
    }

    public static int progressProcessed() {
        return progressProcessed;
    }

    public static int progressTotal() {
        return progressTotal;
    }

    public static boolean progressDone() {
        return progressDone;
    }

    /** {@code true} si hay una barra de progreso reciente que vale la pena mostrar. */
    public static boolean hasRecentProgress() {
        if (progressDone) {
            return System.currentTimeMillis() - progressUpdatedAt < 1500L;
        }
        return true;
    }
}
