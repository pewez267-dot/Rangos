package com.fantasticterraform.schematics.litematica;

/**
 * Array de bits empaquetados tal como lo usa Litematica (empaquetado SIN relleno
 * entre longs, a diferencia del formato de chunk de 1.16+). Cada entrada ocupa
 * exactamente {@code bits} bits y puede cruzar la frontera entre dos longs.
 */
public final class LitematicaBitArray {

    private final long[] longArray;
    private final int bitsPerEntry;
    private final long maxEntryValue;
    private final long arraySize;

    public LitematicaBitArray(int bitsPerEntry, long arraySize) {
        this(bitsPerEntry, arraySize, new long[(int) (((arraySize * bitsPerEntry) + 63L) / 64L)]);
    }

    public LitematicaBitArray(int bitsPerEntry, long arraySize, long[] longArray) {
        this.bitsPerEntry = bitsPerEntry;
        this.arraySize = arraySize;
        this.maxEntryValue = (1L << bitsPerEntry) - 1L;
        this.longArray = longArray;
    }

    public void setAt(long index, int value) {
        long startOffset = index * (long) bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6);
        int endArrIndex = (int) (((index + 1L) * (long) bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3FL);

        longArray[startArrIndex] = longArray[startArrIndex] & ~(maxEntryValue << startBitOffset)
                | ((long) value & maxEntryValue) << startBitOffset;

        if (startArrIndex != endArrIndex) {
            int endOffset = 64 - startBitOffset;
            int j1 = bitsPerEntry - endOffset;
            longArray[endArrIndex] = longArray[endArrIndex] >>> j1 << j1
                    | ((long) value & maxEntryValue) >> endOffset;
        }
    }

    public int getAt(long index) {
        long startOffset = index * (long) bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6);
        int endArrIndex = (int) (((index + 1L) * (long) bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3FL);

        if (startArrIndex == endArrIndex) {
            return (int) (longArray[startArrIndex] >>> startBitOffset & maxEntryValue);
        }
        int endOffset = 64 - startBitOffset;
        return (int) ((longArray[startArrIndex] >>> startBitOffset | longArray[endArrIndex] << endOffset) & maxEntryValue);
    }

    public long[] getBackingArray() {
        return longArray;
    }

    public long size() {
        return arraySize;
    }

    public static int bitsForPaletteSize(int paletteSize) {
        int ceilLog2 = paletteSize <= 1 ? 0 : (32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        return Math.max(2, ceilLog2);
    }
}
