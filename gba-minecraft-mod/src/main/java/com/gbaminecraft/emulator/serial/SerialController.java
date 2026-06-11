package com.gbaminecraft.emulator.serial;

import com.gbaminecraft.emulator.memory.MemoryBus;

/**
 * GBA Serial I/O (SIO) controller.
 *
 * Modela el comportamiento de la comunicación serie del GBA cuando NO hay un
 * segundo cartucho conectado (que es el caso real al emular un solo juego).
 *
 * ¿Por qué importa para arrancar Pokémon Emerald?
 *   Emerald (y muchos juegos) arrancan inicializando el cable link: escriben
 *   SIOCNT con el bit de "start/active" (bit 7) y, a menudo, el bit de IRQ
 *   (bit 14). Luego hacen *polling* del bit 7 esperando que el hardware lo
 *   limpie cuando la transferencia termina, o esperan la IRQ de Serial (IF bit
 *   7). En hardware real, aunque no haya nadie al otro lado, la transferencia
 *   "se completa" sola: el bit de start se limpia y se dispara la IRQ. Sin
 *   modelar esto, el juego se queda en un bucle infinito antes del menú.
 *
 * Registros (offsets relativos a 0x04000000):
 *   0x120 SIODATA32 / SIOMULTI0   (32/16 bits)
 *   0x122 SIOMULTI1
 *   0x124 SIOMULTI2
 *   0x126 SIOMULTI3 / SIODATA8 alto
 *   0x128 SIOCNT   (registro de control)
 *   0x12A SIOMLT_SEND / SIODATA8
 *   0x134 RCNT     (modo general / GPIO / modo serie)
 *
 * Modo de SIOCNT viene de RCNT[15:14] + SIOCNT:
 *   - Si RCNT bit 15 = 0: modo Normal/Multi según SIOCNT bits [13:12].
 *   - SIOCNT[13:12]: 00=Normal-8bit, 01=Normal-32bit, 10=Multiplayer, 11=UART.
 */
public final class SerialController {

    // Offsets de I/O (relativos a 0x04000000), tal como los pasa MemoryBus.
    public static final int SIODATA32_L = 0x120; // SIOMULTI0
    public static final int SIODATA32_H = 0x122; // SIOMULTI1
    public static final int SIOMULTI2   = 0x124;
    public static final int SIOMULTI3   = 0x126;
    public static final int SIOCNT      = 0x128;
    public static final int SIOMLT_SEND = 0x12A; // SIODATA8
    public static final int RCNT        = 0x134;

    // SIOCNT bits
    private static final int SIOCNT_SHIFT_CLOCK   = 1 << 0;  // 0=external,1=internal(master)
    private static final int SIOCNT_START         = 1 << 7;  // start/busy (normal modes)
    private static final int SIOCNT_MULTI_BUSY    = 1 << 7;  // busy (multiplayer)
    private static final int SIOCNT_IRQ_ENABLE    = 1 << 14;

    // IF bit para la IRQ de Serial
    private static final int IRQ_SERIAL = 1 << 7;

    private final MemoryBus bus;

    // Registros (16 bits cada uno) — espejo local que también vive en io[].
    private int siocnt   = 0;
    private int rcnt     = 0;
    private int data32Lo = 0xFFFF;
    private int data32Hi = 0xFFFF;
    private int multi0   = 0xFFFF;
    private int multi1   = 0xFFFF;
    private int multi2   = 0xFFFF;
    private int multi3   = 0xFFFF;
    private int mltSend  = 0xFFFF;

    // Transferencia en curso: cuenta atrás de ciclos hasta completarse.
    private boolean transferActive = false;
    private int     transferCyclesLeft = 0;

    public SerialController(MemoryBus bus) {
        this.bus = bus;
    }

    // ── Lectura de registros (8 bits, como el resto del bus) ───────────────
    public int readRegister(int offset) {
        switch (offset) {
            case SIODATA32_L:     return data32Lo & 0xFF;
            case SIODATA32_L + 1: return (data32Lo >>> 8) & 0xFF;
            case SIODATA32_H:     return data32Hi & 0xFF;
            case SIODATA32_H + 1: return (data32Hi >>> 8) & 0xFF;
            case SIOMULTI2:       return multi2 & 0xFF;
            case SIOMULTI2 + 1:   return (multi2 >>> 8) & 0xFF;
            case SIOMULTI3:       return multi3 & 0xFF;
            case SIOMULTI3 + 1:   return (multi3 >>> 8) & 0xFF;
            case SIOCNT:          return siocnt & 0xFF;
            case SIOCNT + 1:      return (siocnt >>> 8) & 0xFF;
            case SIOMLT_SEND:     return mltSend & 0xFF;
            case SIOMLT_SEND + 1: return (mltSend >>> 8) & 0xFF;
            case RCNT:            return rcnt & 0xFF;
            case RCNT + 1:        return (rcnt >>> 8) & 0xFF;
            default:              return 0;
        }
    }

    // ── Escritura de registros (8 bits) ────────────────────────────────────
    public void writeRegister(int offset, int val) {
        val &= 0xFF;
        switch (offset) {
            case SIODATA32_L:     data32Lo = (data32Lo & 0xFF00) | val;        break;
            case SIODATA32_L + 1: data32Lo = (data32Lo & 0x00FF) | (val << 8); break;
            case SIODATA32_H:     data32Hi = (data32Hi & 0xFF00) | val;        break;
            case SIODATA32_H + 1: data32Hi = (data32Hi & 0x00FF) | (val << 8); break;
            case SIOMULTI2:       multi2   = (multi2   & 0xFF00) | val;        break;
            case SIOMULTI2 + 1:   multi2   = (multi2   & 0x00FF) | (val << 8); break;
            case SIOMULTI3:       multi3   = (multi3   & 0xFF00) | val;        break;
            case SIOMULTI3 + 1:   multi3   = (multi3   & 0x00FF) | (val << 8); break;
            case SIOMLT_SEND:     mltSend  = (mltSend  & 0xFF00) | val;        break;
            case SIOMLT_SEND + 1: mltSend  = (mltSend  & 0x00FF) | (val << 8); break;
            case RCNT:            rcnt     = (rcnt     & 0xFF00) | val;        break;
            case RCNT + 1:        rcnt     = (rcnt     & 0x00FF) | (val << 8); break;
            case SIOCNT:          writeSiocnt((siocnt & 0xFF00) | val);        break;
            case SIOCNT + 1:      writeSiocnt((siocnt & 0x00FF) | (val << 8)); break;
            default: break;
        }
    }

    private void writeSiocnt(int newVal) {
        int mode = (newVal >>> 12) & 0x3;   // SIOCNT[13:12]
        boolean nowStart = (newVal & SIOCNT_START) != 0;

        // UART (modo 3) y modos sin start: solo almacenar.
        siocnt = newVal & 0xFFFF;

        // Una transferencia arranca cuando se pone el bit start/active estando
        // configurado como maestro (shift clock interno) en modos Normal, o el
        // bit busy en Multiplayer. Como no hay compañero, la completamos sola.
        if (nowStart && !transferActive) {
            // Sólo el maestro (clock interno) puede iniciar en modos Normal.
            // En Multiplayer, escribir bit7 inicia si somos el padre.
            boolean isMaster = (newVal & SIOCNT_SHIFT_CLOCK) != 0 || mode == 2;
            if (isMaster) {
                startTransfer(mode);
            } else {
                // Esclavo esperando clock externo: en hardware sin compañero
                // nunca llega. Para no colgar, tampoco dejamos el bit pegado
                // indefinidamente si el clock es externo y no hay nadie: lo
                // tratamos como completado inmediato con datos 0xFFFF.
                startTransfer(mode);
            }
        }
    }

    /**
     * Programa la finalización de la transferencia. La duración exacta depende
     * del modo y la velocidad; para arrancar juegos basta con una latencia
     * pequeña y determinista (~unos pocos miles de ciclos) tras la cual el bit
     * de start se limpia y, si procede, se dispara la IRQ de Serial.
     */
    private void startTransfer(int mode) {
        transferActive = true;
        // Latencia aproximada: 32 bits a 256 KHz ≈ varios miles de ciclos.
        // Mantener corto para que el polling salga rápido pero no instantáneo.
        switch (mode) {
            case 0:  transferCyclesLeft = 8 * 64;  break;   // Normal 8-bit
            case 1:  transferCyclesLeft = 32 * 64; break;   // Normal 32-bit
            case 2:  transferCyclesLeft = 32 * 64; break;   // Multiplayer
            default: transferCyclesLeft = 16 * 64; break;   // UART/otros
        }
    }

    /** Avanza el reloj de la SIO; completa transferencias pendientes. */
    public void tick(int cpuCycles) {
        if (!transferActive) return;
        transferCyclesLeft -= cpuCycles;
        if (transferCyclesLeft > 0) return;

        transferActive = false;
        int mode = (siocnt >>> 12) & 0x3;
        completeTransfer(mode);
    }

    private void completeTransfer(int mode) {
        // Sin compañero conectado, las líneas leen 1s: datos recibidos = 0xFFFF.
        if (mode == 2) {
            // Multiplayer: somos el único jugador (parent). SIOMULTI0 recibe lo
            // que enviamos; el resto de slots leen 0xFFFF (ausentes). El bit de
            // ID de jugador (SIOCNT[5:4]) queda en 0 (parent), error bit (6)=0.
            multi0 = mltSend & 0xFFFF;
            multi1 = 0xFFFF;
            multi2 = 0xFFFF;
            multi3 = 0xFFFF;
            siocnt &= ~SIOCNT_MULTI_BUSY;     // limpia busy (bit7)
        } else {
            // Normal 8/32: el dato recibido por ausencia de esclavo es todo 1s.
            data32Lo = 0xFFFF;
            data32Hi = 0xFFFF;
            siocnt &= ~SIOCNT_START;          // limpia start (bit7)
        }

        // Reflejar SIOCNT/data en el array io[] para que las lecturas directas
        // (read16 sobre 0x128) vean el bit ya limpio.
        syncToIo();

        // Disparar IRQ de Serial si está habilitada.
        if ((siocnt & SIOCNT_IRQ_ENABLE) != 0) {
            bus.requestInterrupt(IRQ_SERIAL);
        }
    }

    /** Copia el estado de SIOCNT al buffer io[] del bus (espejo de lectura). */
    private void syncToIo() {
        byte[] io = bus.getIO();
        io[SIOCNT]     = (byte) (siocnt & 0xFF);
        io[SIOCNT + 1] = (byte) ((siocnt >>> 8) & 0xFF);
    }

    public boolean isTransferActive() { return transferActive; }
    public int getSiocnt()            { return siocnt; }

    public void reset() {
        siocnt = 0; rcnt = 0;
        data32Lo = 0xFFFF; data32Hi = 0xFFFF;
        multi0 = 0xFFFF; multi1 = 0xFFFF; multi2 = 0xFFFF; multi3 = 0xFFFF;
        mltSend = 0xFFFF;
        transferActive = false; transferCyclesLeft = 0;
    }
}
