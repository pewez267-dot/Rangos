#!/usr/bin/env python3
"""Remapea tokens SRG (m_XXX_/f_XXX_) a nombres oficiales usando el tsrg de ForgeGradle.
Tambien corrige el artefacto de CFR de labels de enum cualificados en switch de flechas.
Uso: remap.py <archivo1.java> [archivo2.java ...]
"""
import re
import sys

TSRG = "/root/.gradle/caches/forge_gradle/minecraft_user_repo/de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412/srg_to_official_1.20.1.tsrg"

def load_dict():
    d = {}
    with open(TSRG, "r", encoding="utf-8") as f:
        for line in f:
            if not line.startswith("\t"):
                continue
            parts = line.strip().split(" ")
            srg = parts[0]
            if srg.startswith("m_") and len(parts) >= 3:
                d[srg] = parts[2]
            elif srg.startswith("f_") and len(parts) >= 2:
                d[srg] = parts[1]
    return d

TOKEN = re.compile(r"\b([mf]_\d+_)\b")

def main():
    d = load_dict()
    missing = set()
    for path in sys.argv[1:]:
        with open(path, "r", encoding="utf-8") as f:
            src = f.read()
        def repl(m):
            tok = m.group(1)
            if tok in d:
                return d[tok]
            missing.add(tok)
            return tok
        out = TOKEN.sub(repl, src)
        # Fix CFR: labels de enum cualificados en switch de flechas -> sin cualificar
        out = out.replace("case ClaimFlags.FlagId.", "case ")
        with open(path, "w", encoding="utf-8") as f:
            f.write(out)
        print(f"remapeado: {path}")
    if missing:
        print(f"[WARN] {len(missing)} tokens SRG sin mapping (revisar): {sorted(list(missing))[:30]}")
    else:
        print("[OK] todos los tokens SRG mapeados")

if __name__ == "__main__":
    main()
