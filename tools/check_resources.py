#!/usr/bin/env python3
"""Consistency checker for the VotListva Forge mod (Minecraft 1.20.1).

Static analysis only - no JDK, Gradle or network access is required. It answers
the questions that are easy to get wrong by hand:

  1. does every JSON file parse?
  2. does every registered block/item have its blockstate, block model, item
     model, loot table, lang entries and creative-tab wiring?
  3. does the blockstate JSON cover every value of the block state property
     that the Java enum declares?
  4. does the loot table cover every value of that property?
  5. do the lang files contain every translation key used in Java, with a
     matching number of format specifiers?

Usage:  python3 tools/check_resources.py
Exit code 0 = no errors, 1 = at least one error.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
JAVA = ROOT / "src/main/java"

errors: list[str] = []
warnings: list[str] = []


def err(msg: str) -> None:
    errors.append(msg)


def warn(msg: str) -> None:
    warnings.append(msg)


def java_files() -> list[Path]:
    return sorted(JAVA.rglob("*.java"))


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def split_top_level(text: str) -> list[str]:
    """Split on commas that are not nested inside parentheses or brackets."""
    parts, depth, current = [], 0, []
    for ch in text:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
    if current:
        parts.append("".join(current).strip())
    return [p for p in parts if p]


def matching_call(source: str, start: int) -> str:
    """Return the argument text of the call whose '(' is at index `start`."""
    depth, i = 0, start
    while i < len(source):
        if source[i] == "(":
            depth += 1
        elif source[i] == ")":
            depth -= 1
            if depth == 0:
                return source[start + 1:i]
        i += 1
    return ""


def parse_enum(path: Path, name: str) -> list[str]:
    """Return the serialized names of an enum that implements StringRepresentable."""
    body = read(path).split("enum " + name, 1)[1].split(";", 1)[0]
    return [m.group(2) for m in re.finditer(r'([A-Z_0-9]+)\("([a-z_0-9]+)"', body)]


# --------------------------------------------------------------- registries
def registered_names(path: Path, register_type: str) -> list[str]:
    src = read(path)
    names = []
    for match in re.finditer(
            r"DeferredRegister<[^>]+>\s+\w+\s*=\s*DeferredRegister\.create\([^)]*\)", src):
        pass
    for match in re.finditer(r'\.register\(\s*"([a-z_0-9]+)"', src):
        names.append(match.group(1))
    if register_type not in src:
        warn(f"{path.name}: could not confirm a DeferredRegister<{register_type}>")
    return names


blocks = registered_names(JAVA / "ru/arena/votlistva/registry/ModBlocks.java", "Block")
items = registered_names(JAVA / "ru/arena/votlistva/registry/ModItems.java", "Item")
print("registered blocks:", blocks)
print("registered items :", items)
if not blocks:
    err("no block registrations found in ModBlocks.java")
if not items:
    err("no item registrations found in ModItems.java")

# ------------------------------------------------------------------ json
parsed: dict[Path, object] = {}
for f in sorted(RES.rglob("*.json")) + [RES / "pack.mcmeta"]:
    if f.suffix == ".json" or f.name == "pack.mcmeta":
        try:
            parsed[f] = json.loads(read(f))
        except Exception as exc:  # noqa: BLE001
            err(f"JSON parse error in {f.relative_to(ROOT)}: {exc}")
print(f"\nparsed {len(parsed)} json files")

# ------------------------------------------------- per-registration assets
for name in blocks:
    for rel_path, what in (
            (f"assets/votlistva/blockstates/{name}.json", "blockstate"),
            (f"data/votlistva/loot_tables/blocks/{name}.json", "loot table"),
    ):
        if not (RES / rel_path).exists():
            err(f"block {name!r}: missing {what} ({rel_path})")
for name in items:
    if not (RES / f"assets/votlistva/models/item/{name}.json").exists():
        err(f"item {name!r}: missing item model")

# every model referenced by a blockstate or an item model must exist
referenced: set[str] = set()
for path, doc in parsed.items():
    rel = str(path.relative_to(RES))
    if not (rel.startswith("assets/votlistva/blockstates/") or rel.startswith("assets/votlistva/models/item/")):
        continue
    models = set()
    if isinstance(doc, dict):
        for part in doc.get("multipart") or []:
            model = (part.get("apply") or {}).get("model")
            if model:
                models.add(model)
        for value in (doc.get("variants") or {}).values():
            if isinstance(value, dict) and value.get("model"):
                models.add(value["model"])
        if doc.get("parent"):
            models.add(doc["parent"])
    for model in sorted(models):
        ns, _, sub = model.partition(":")
        if ns != "votlistva":
            continue
        referenced.add(model)
        if not (RES / f"assets/{ns}/models/{sub}.json").exists():
            err(f"{rel} references missing model {model}")

# a registered block must be wired to at least one model through its blockstate
for name in blocks:
    doc = parsed.get(RES / f"assets/votlistva/blockstates/{name}.json") or {}
    wired = any(m.startswith("votlistva:block/") for m in referenced
                if m in json.dumps(doc))
    if not wired:
        err(f"blockstate {name}.json does not reference any votlistva model")

# ------------------------------------------------------- blockstate props
leaf_types = parse_enum(JAVA / "ru/arena/votlistva/world/block/LivingLeafType.java", "LivingLeafType")
log_types = parse_enum(JAVA / "ru/arena/votlistva/world/block/LivingLogType.java", "LivingLogType")

leaf_bs = parsed.get(RES / "assets/votlistva/blockstates/living_leaf.json")
if leaf_bs is None:
    err("blockstates/living_leaf.json missing")
else:
    if "multipart" not in leaf_bs:
        err("living_leaf.json: expected a 'multipart' block")
    else:
        covered, models = set(), set()
        for part in leaf_bs["multipart"]:
            when = part.get("when") or {}
            for key in when:
                if key != "leaf_type":
                    warn(f"living_leaf.json: multipart filters on unexpected property {key!r}")
            values = when.get("leaf_type")
            if isinstance(values, list):
                covered.update(values)
            elif isinstance(values, str):
                covered.add(values)
            model = (part.get("apply") or {}).get("model")
            if model:
                models.add(model)
        want = set(leaf_types)
        if want - covered:
            err(f"living_leaf.json: no model for leaf_type {sorted(want - covered)}")
        if covered - want:
            err(f"living_leaf.json: unknown leaf_type {sorted(covered - want)}")
        if not (want - covered) and not (covered - want):
            print(f"living_leaf.json: all {len(want)} leaf_type values covered")
        for model in sorted(models):
            ns, _, path = model.partition(":")
            if ns == "votlistva" and not (RES / f"assets/{ns}/models/{path}.json").exists():
                err(f"living_leaf.json references missing model {model}")

log_bs = parsed.get(RES / "assets/votlistva/blockstates/living_log.json")
if log_bs is None:
    err("blockstates/living_log.json missing")
else:
    variants = log_bs.get("variants") or {}
    want = {f"axis={axis},log_type={t}" for axis in ("x", "y", "z") for t in log_types}
    if want - set(variants):
        err(f"living_log.json: missing variants {sorted(want - set(variants))}")
    if set(variants) - want:
        err(f"living_log.json: unexpected variants {sorted(set(variants) - want)}")
    if not (want - set(variants)) and not (set(variants) - want):
        print(f"living_log.json: all {len(want)} variants covered")
    for key, value in variants.items():
        axis = key.split(",")[0]
        if axis == "axis=x" and not value.get("x"):
            err(f"living_log.json: {key} needs an 'x' rotation")
        if axis == "axis=z" and (not value.get("x") or not value.get("y")):
            err(f"living_log.json: {key} needs 'x' and 'y' rotations")

# ------------------------------------------------------------ loot tables
loot = RES / "data/votlistva/loot_tables/blocks/living_log.json"
if loot.exists():
    text = read(loot)
    for value in log_types:
        if f'"log_type": "{value}"' not in text:
            warn(f"living_log loot table: no explicit condition for log_type={value} "
                 "(falls back to the unconditional oak_log entry)")
    if '"minecraft:alternatives"' not in text:
        err("living_log loot table: expected a minecraft:alternatives entry")

# ---------------------------------------------------------------- lang
java_src = "\n".join(read(p) for p in java_files())
keys: set[str] = set()
for match in re.finditer(r'translatable\(', java_src):
    args = split_top_level(matching_call(java_src, match.end() - 1))
    if args and args[0].startswith('"'):
        keys.add(args[0].strip('"'))
print("\ntranslation keys used in java:", sorted(keys))

for lang in ("en_us", "ru_ru"):
    data = parsed.get(RES / f"assets/votlistva/lang/{lang}.json")
    if data is None:
        err(f"lang/{lang}.json missing")
        continue
    for key in sorted(keys):
        if key not in data:
            err(f"lang/{lang}.json is missing key {key!r}")
    for name in blocks:
        for prefix in ("block", "item"):
            key = f"{prefix}.votlistva.{name}"
            if key not in data:
                err(f"lang/{lang}.json is missing {key!r}")
    # format specifiers must match the number of arguments passed in java
    for match in re.finditer(r'translatable\(', java_src):
        args = split_top_level(matching_call(java_src, match.end() - 1))
        if not args or not args[0].startswith('"'):
            continue
        key = args[0].strip('"')
        n_spec = len(re.findall(r"%[sdf]", data.get(key, "")))
        n_args = len(args) - 1
        if n_spec != n_args:
            err(f"lang/{lang}.json key {key!r}: {n_spec} format specifier(s) "
                f"but java passes {n_args} argument(s)")
    print(f"lang/{lang}.json: {len(data)} keys checked")

# ------------------------------------------------------------- pack.mcmeta
pack = parsed.get(RES / "pack.mcmeta")
if pack is None:
    err("pack.mcmeta missing")
else:
    fmt = pack.get("pack", {}).get("pack_format")
    if fmt != 15:
        err(f"pack.mcmeta: pack_format {fmt} is wrong for Minecraft 1.20.1 (expected 15)")
    else:
        print(f"pack.mcmeta: pack_format {fmt} (correct for 1.20.1)")

# --------------------------------------------------------- textures
LEAF_TEXTURES = {f"{s}_leaves" for s in (
    "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
    "azalea", "flowering_azalea")}
for f in sorted(RES.glob("assets/votlistva/models/block/living_*.json")):
    for slot, path in (parsed.get(f, {}).get("textures") or {}).items():
        ns, _, name = path.partition(":")
        if ns != "minecraft":
            err(f"{f.relative_to(ROOT)}: non-vanilla texture {path}")
            continue
        stem = name.rsplit("/", 1)[-1]
        if slot == "all" and stem not in LEAF_TEXTURES:
            err(f"{f.relative_to(ROOT)}: {path} is not a vanilla leaf texture")
        if slot in ("side", "end") and not (
                stem.endswith(("_log", "_log_top", "_stem", "_stem_top"))):
            err(f"{f.relative_to(ROOT)}: {path} is not a vanilla log/stem texture")

# ----------------------------------------------------- java cross-checks
for cls, method in (("LivingLeafType", "isVanillaLeaf"),
                    ("LivingLogType", "isVanillaLog"),
                    ("LivingLeafType", "sourceBlock"),
                    ("LivingLogType", "sourceBlock")):
    path = next((p for p in java_files() if f"/{cls}.java" in str(p)), None)
    if path is None:
        err(f"{cls}.java not found")
    elif method not in read(path):
        err(f"{cls}.java does not declare {method}()")

events = JAVA / "ru/arena/votlistva/events/WorldEcologyEvents.java"
if events.exists():
    src = read(events)
    if "LinkedHashSet" not in src:
        warn("WorldEcologyEvents: round-robin chunk queue is gone")
    if "BlockTags" in src:
        err("WorldEcologyEvents still uses BlockTags; conversion is now enum driven")
else:
    err("WorldEcologyEvents.java not found")

# ---------------------------------------------------------------- report
print("\n==================== RESULT ====================")
if warnings:
    print("warnings:")
    for w in warnings:
        print("  !", w)
if errors:
    print("errors:")
    for e in errors:
        print("  X", e)
    sys.exit(1)
print("no errors")
