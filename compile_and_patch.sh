#!/bin/bash
set -e

echo "=========================================="
echo "ClaimBlocks JAR Patcher"
echo "=========================================="

# Directorios
WORK_DIR="/projects/sandbox/Rangos"
BUILD_DIR="$WORK_DIR/build_temp"
DECOMPILED_SRC="$WORK_DIR/decompiled_source"
ORIGINAL_JAR="$WORK_DIR/ClaimBlocksREPARAR-1.0.jar"
OUTPUT_JAR="$WORK_DIR/ClaimBlocks-FIXED-1.0.jar"

# Limpiar y crear directorios
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/jar_extracted"

echo ""
echo "[1/6] Extrayendo JAR original..."
cd "$BUILD_DIR/jar_extracted"
unzip -q "$ORIGINAL_JAR"
echo "✓ JAR extraído"

echo ""
echo "[2/6] Compilando clases corregidas..."
cd "$WORK_DIR"

# Intentar compilar con las clases del JAR como classpath
javac -source 21 -target 21 \
      -cp "$BUILD_DIR/jar_extracted:." \
      -d "$BUILD_DIR/classes" \
      "$DECOMPILED_SRC/com/f0cus/protectionstones/CBEventHandler.java" \
      "$DECOMPILED_SRC/com/f0cus/protectionstones/CBManager.java" \
      2>&1 || {
    echo "⚠ Compilación con javac falló (esperado - faltan dependencias)"
    echo "   Procediendo con enfoque alternativo..."
}

# Si la compilación falló, intentamos un enfoque de reemplazo directo
if [ ! -f "$BUILD_DIR/classes/com/f0cus/protectionstones/CBEventHandler.class" ]; then
    echo ""
    echo "[3/6] Usando enfoque de bytecode patching..."
    
    # Copiar las clases originales primero
    cp -r "$BUILD_DIR/jar_extracted/com" "$BUILD_DIR/classes/"
    
    # Aquí normalmente usaríamos herramientas de bytecode manipulation
    # Como no tenemos javac funcional, vamos a documentar esto
    echo "⚠ No se puede compilar sin dependencias completas de Minecraft/Fabric"
    echo ""
    echo "SOLUCIÓN ALTERNATIVA:"
    echo "1. Las correcciones de código fuente están listas en: decompiled_source/"
    echo "2. Necesitas un entorno con Gradle + Fabric Loom para compilar"
    echo "3. O usar IntelliJ IDEA para importar y compilar el proyecto"
    echo ""
    
    exit 1
fi

echo "✓ Clases compiladas"

echo ""
echo "[4/6] Copiando clases compiladas al JAR..."
cp -v "$BUILD_DIR/classes/com/f0cus/protectionstones/CBEventHandler.class" \
      "$BUILD_DIR/jar_extracted/com/f0cus/protectionstones/"
cp -v "$BUILD_DIR/classes/com/f0cus/protectionstones/CBManager.class" \
      "$BUILD_DIR/jar_extracted/com/f0cus/protectionstones/"
echo "✓ Clases reemplazadas"

echo ""
echo "[5/6] Re-empaquetando JAR..."
cd "$BUILD_DIR/jar_extracted"
zip -r -q "$OUTPUT_JAR" .
echo "✓ JAR creado"

echo ""
echo "[6/6] Verificando..."
if [ -f "$OUTPUT_JAR" ]; then
    ls -lh "$OUTPUT_JAR"
    echo ""
    echo "=========================================="
    echo "✓ JAR CORREGIDO CREADO EXITOSAMENTE"
    echo "=========================================="
    echo ""
    echo "Ubicación: $OUTPUT_JAR"
    echo ""
else
    echo "✗ Error: No se pudo crear el JAR"
    exit 1
fi
