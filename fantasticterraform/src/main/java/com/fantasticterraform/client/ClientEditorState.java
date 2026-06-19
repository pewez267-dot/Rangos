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
    private static volatile long progressStartedAt;
    private static volatile int progressStartProcessed;
    private static volatile boolean progressWasDone = true;

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
        long now = System.currentTimeMillis();
        // Deteccion de nueva operacion: estaba terminada, cambio el nombre, o el contador reinicio.
        boolean newOp = progressWasDone || !name.equals(progressName) || processed < progressProcessed;
        if (newOp) {
            progressStartedAt = now;
            progressStartProcessed = processed;
        }
        progressName = name;
        progressProcessed = processed;
        progressTotal = total;
        progressDone = done;
        progressWasDone = done;
        progressUpdatedAt = now;
    }

    /** Velocidad estimada en bloques por segundo de la operacion en curso. */
    public static int progressRate() {
        long elapsed = System.currentTimeMillis() - progressStartedAt;
        if (elapsed < 200L) {
            return 0;
        }
        int delta = progressProcessed - progressStartProcessed;
        return delta <= 0 ? 0 : (int) (delta * 1000L / elapsed);
    }

    /** Segundos estimados restantes, o -1 si no se puede estimar. */
    public static int progressEtaSeconds() {
        int rate = progressRate();
        if (rate <= 0 || progressDone) {
            return -1;
        }
        int remaining = Math.max(0, progressTotal - progressProcessed);
        return remaining / rate;
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
