#!/usr/bin/env bash
# Compila el nucleo del emulador + AudioProbe y ejecuta el analisis headless de audio.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
PROJ="$(cd "$HERE/.." && pwd)"
SRC="$PROJ/src/main/java"
BUILD="$HERE/.build-audio"

rm -rf "$BUILD"
mkdir -p "$BUILD/src/com/gbaminecraft" "$BUILD/out"
cp -r "$SRC/com/gbaminecraft/emulator" "$BUILD/src/com/gbaminecraft/"

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
javac -cp "$BUILD/out" -d "$BUILD/out" "$HERE/AudioProbe.java"
( cd "$PROJ" && java -cp "$BUILD/out" AudioProbe )
