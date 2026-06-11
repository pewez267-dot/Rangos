#!/usr/bin/env bash
# Headless emulator test runner — verifica el núcleo (CPU/PPU) SIN Minecraft.
# Requiere solo un JDK 17+ en el PATH (javac/java).
#
# Uso:  cd emulator-tests && ./run-tests.sh
set -e

HERE="$(cd "$(dirname "$0")" && pwd)"
PROJ="$(cd "$HERE/.." && pwd)"
SRC="$PROJ/src/main/java"
BUILD="$HERE/.build"

echo "==> Compilando el núcleo del emulador (paquete com.gbaminecraft.emulator)"
rm -rf "$BUILD"
mkdir -p "$BUILD/src/com/gbaminecraft" "$BUILD/out"
cp -r "$SRC/com/gbaminecraft/emulator" "$BUILD/src/com/gbaminecraft/"

# Stub mínimo de GBAMod.LOGGER (la clase real depende de Forge; aquí no la necesitamos)
cat > "$BUILD/src/com/gbaminecraft/GBAMod.java" <<'EOF'
package com.gbaminecraft;
public class GBAMod {
  public static final Log LOGGER = new Log();
  public static class Log {
    public void info(String m){}  public void info(String m, Object a){}
    public void warn(String m){}  public void warn(String m, Object a){}
    public void error(String m){} public void error(String m, Object a){}
    public void error(String m, Throwable t){}
  }
}
EOF

( cd "$BUILD/src" && javac -d "$BUILD/out" $(find . -name '*.java') )

echo "==> Compilando los tests"
javac -cp "$BUILD/out" -d "$BUILD/out" "$HERE/CpuTest.java" "$HERE/PpuTest.java" "$HERE/IntegrationTest.java" "$HERE/BiosFlashTest.java" "$HERE/PpuFxTest.java" "$HERE/EepromTest.java" "$HERE/IrqTest.java" "$HERE/StateTest.java" "$HERE/StackTest.java" "$HERE/SerialTest.java" "$HERE/IntrWaitTest.java" "$HERE/VramMirrorTest.java"

echo
echo "########################  CPU  ########################"
java -cp "$BUILD/out" CpuTest
echo
echo "########################  PPU  ########################"
java -cp "$BUILD/out" PpuTest
echo
echo "##############  INTEGRACION CPU+MEM+PPU  ##############"
java -cp "$BUILD/out" IntegrationTest
echo
echo "##############  HLE BIOS + FLASH (Pokémon)  ##########"
java -cp "$BUILD/out" BiosFlashTest
echo
echo "##############  PPU FX (ventanas/blending/fade)  #####"
java -cp "$BUILD/out" PpuFxTest
echo
echo "##############  EEPROM (save serial)  ################"
java -cp "$BUILD/out" EepromTest
echo
echo "##############  IRQ HLE (handlers de juego)  ########"
java -cp "$BUILD/out" IrqTest
echo
echo "##############  SAVE-STATE (snapshot)  ##############"
java -cp "$BUILD/out" StateTest
echo
echo "##############  STACK LDM/STM (push/pop)  ###########"
java -cp "$BUILD/out" StackTest
echo
echo "##############  SERIAL (link-cable boot)  ###########"
java -cp "$BUILD/out" SerialTest
echo
echo "##############  INTRWAIT + RTC  #####################"
java -cp "$BUILD/out" IntrWaitTest
echo
echo "##############  VRAM MIRROR  ########################"
java -cp "$BUILD/out" VramMirrorTest

echo
echo "==> Listo. Si ves 'X PASARON, 0 FALLARON' en las tres suites, el núcleo está sano."
