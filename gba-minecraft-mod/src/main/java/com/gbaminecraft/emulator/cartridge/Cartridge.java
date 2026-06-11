package com.gbaminecraft.emulator.cartridge;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

/**
 * GBA Cartridge loader.
 * Reads .gba ROM files and exposes header info and ROM data.
 * Also manages SRAM/Flash/EEPROM save data.
 */
public class Cartridge {

    // Header offsets
    private static final int HEADER_ENTRY_POINT = 0x000; // 4 bytes - ARM branch
    private static final int HEADER_LOGO        = 0x004; // 156 bytes - Nintendo logo
    private static final int HEADER_TITLE       = 0x0A0; // 12 bytes - game title
    private static final int HEADER_GAMECODE    = 0x0AC; // 4 bytes - game code
    private static final int HEADER_MAKERCODE   = 0x0B0; // 2 bytes - maker code
    private static final int HEADER_FIXED_96    = 0x0B2; // 0x96
    private static final int HEADER_UNIT_CODE   = 0x0B3; // 0x00
    private static final int HEADER_VERSION     = 0x0BC; // ROM version
    private static final int HEADER_CHECKSUM    = 0x0BD; // header checksum
    private static final int HEADER_SIZE        = 0x0C0; // end of header

    // Save types
    public enum SaveType { NONE, SRAM, EEPROM_4K, EEPROM_64K, FLASH_512K, FLASH_1M }

    private byte[] romData;
    private byte[] saveData;
    private SaveType saveType = SaveType.NONE;

    private String title      = "";
    private String gameCode   = "";
    private String makerCode  = "";
    private int    version    = 0;
    private boolean valid     = false;

    public Cartridge() {}

    // ── Load from byte array ───────────────────────────────────────────────
    public boolean loadROM(byte[] data) {
        if (data == null || data.length < HEADER_SIZE) {
            return false;
        }
        this.romData = data;
        parseHeader();
        detectSaveType();
        initSaveData();
        valid = true;
        return true;
    }

    // ── Load from file ─────────────────────────────────────────────────────
    public boolean loadROM(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            return loadROM(data);
        } catch (IOException e) {
            return false;
        }
    }

    // ── Header parsing ─────────────────────────────────────────────────────
    private void parseHeader() {
        if (romData.length < HEADER_SIZE) return;

        // Game title (12 bytes, null-padded ASCII)
        byte[] titleBytes = Arrays.copyOfRange(romData, HEADER_TITLE, HEADER_TITLE + 12);
        title = new String(titleBytes).trim().replace("\0", "");

        // Game code (4 bytes)
        gameCode = new String(Arrays.copyOfRange(romData, HEADER_GAMECODE, HEADER_GAMECODE + 4)).trim();

        // Maker code (2 bytes)
        makerCode = new String(Arrays.copyOfRange(romData, HEADER_MAKERCODE, HEADER_MAKERCODE + 2)).trim();

        // Version
        version = romData[HEADER_VERSION] & 0xFF;
    }

    // ── Save type detection ────────────────────────────────────────────────
    private void detectSaveType() {
        saveType = SaveType.NONE;
        if (romData == null) return;

        // Scan ROM for save type strings
        String romStr = new String(romData, java.nio.charset.StandardCharsets.ISO_8859_1);

        if (romStr.contains("EEPROM_V")) {
            // Distinguish 4K vs 64K by ROM size
            saveType = romData.length > 16 * 1024 * 1024 ? SaveType.EEPROM_64K : SaveType.EEPROM_4K;
        } else if (romStr.contains("SRAM_V") || romStr.contains("SRAM_F_V")) {
            saveType = SaveType.SRAM;
        } else if (romStr.contains("FLASH_V") || romStr.contains("FLASH512_V")) {
            saveType = SaveType.FLASH_512K;
        } else if (romStr.contains("FLASH1M_V")) {
            saveType = SaveType.FLASH_1M;
        } else {
            // Default — try SRAM for unknown
            saveType = SaveType.SRAM;
        }
    }

    private void initSaveData() {
        switch (saveType) {
            case SRAM:       saveData = new byte[32 * 1024]; break;
            case EEPROM_4K:  saveData = new byte[512]; break;
            case EEPROM_64K: saveData = new byte[8192]; break;
            case FLASH_512K: saveData = new byte[64 * 1024]; break;
            case FLASH_1M:   saveData = new byte[128 * 1024]; break;
            default:         saveData = new byte[0]; break;
        }
        Arrays.fill(saveData, (byte) 0xFF);
    }

    // ── Save persistence ───────────────────────────────────────────────────
    public boolean loadSave(File saveFile) {
        try {
            byte[] data = Files.readAllBytes(saveFile.toPath());
            int len = Math.min(data.length, saveData.length);
            System.arraycopy(data, 0, saveData, 0, len);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean writeSave(File saveFile) {
        try {
            Files.write(saveFile.toPath(), saveData);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ── ROM read ───────────────────────────────────────────────────────────
    public byte[] getROMData()   { return romData; }
    public byte[] getSaveData()  { return saveData; }
    public SaveType getSaveType(){ return saveType; }
    public String getTitle()     { return title; }
    public String getGameCode()  { return gameCode; }
    public String getMakerCode() { return makerCode; }
    public int    getVersion()   { return version; }
    public boolean isValid()     { return valid; }
    public int    getSize()      { return romData != null ? romData.length : 0; }

    // ── SRAM/Flash read/write ─────────────────────────────────────────────
    public int readSRAM(int offset) {
        offset &= (saveData.length - 1);
        return saveData[offset] & 0xFF;
    }

    public void writeSRAM(int offset, int val) {
        offset &= (saveData.length - 1);
        saveData[offset] = (byte)(val & 0xFF);
    }

    // ── Checksum validation ───────────────────────────────────────────────
    public boolean validateChecksum() {
        if (romData == null || romData.length < 0xBE) return false;
        int checksum = 0;
        for (int i = 0xA0; i <= 0xBC; i++) {
            checksum -= romData[i] & 0xFF;
        }
        checksum = (checksum - 0x19) & 0xFF;
        return checksum == (romData[0xBD] & 0xFF);
    }

    public void reset() {
        valid = false;
        romData = null;
        if (saveData != null) Arrays.fill(saveData, (byte) 0xFF);
    }

    @Override
    public String toString() {
        return String.format("GBA Cartridge [%s] code=%s maker=%s ver=%d size=%dKB save=%s valid=%b",
            title, gameCode, makerCode, version, getSize() / 1024, saveType, valid);
    }
}
