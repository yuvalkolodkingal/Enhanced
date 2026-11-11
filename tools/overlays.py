#!/usr/bin/env python3
"""The resources of the Minecraft versions after 1.21.1 (docs/MULTIVERSION.md), as pack overlays: folders that
pack.mcmeta switches on by pack format, on top of the 1.21.1 resources the jar holds. Run from the repository root;
it rewrites src/main/resources/pack.mcmeta and the overlay folders.

1.21.4 (data format 57-61, resource format 42-46):
* data: recipes with ingredients written as ids ("minecraft:stick", "#tag"); biomes with carvers as a list and their
  music as a weighted list;
* assets: an item model definition (assets/enhanced/items/<item>.json) for every item model: the 1.21.1 overrides as
  range dispatches on the mod's item properties (the "enhanced:legacy" property, compat.LegacyItems), vanilla's
  "blocking" as "using_item", the separate GUI model as a display-context select, the spawn eggs' colours and the
  Stone Locator's socket colour as tints, the items drawn by code ("builtin/entity") as special models
  ("enhanced:legacy"); copies of the "builtin/entity" models without that parent (1.21.4 has none); the post effects
  in their 1.21.4 format (assets/enhanced/post_effect).
"""
import json
import os
import re
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
NS = "enhanced"
DATA_4 = "overlay_1_21_4_data"
ASSETS_4 = "overlay_1_21_4_assets"
MODELS = os.path.join(RES, "assets", NS, "models", "item")

# items drawn by code in 1.21.1 (IClientItemExtensions' BEWLR): ClientSetup.registerItemRenderers
SPECIAL = {"vibranium_shield"}
# the items' tints in 1.21.1 (ClientRegistrar.itemColor): item -> the tint indices the mod colours
TINTED = {"stone_locator": [1]}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def ingredient(value):
    """1.21.1's {"item": x} / {"tag": t} (or a list of them) as 1.21.2's "x" / "#t"; NeoForge's custom ingredients
    name their type with "neoforge:ingredient_type"."""
    if isinstance(value, list):
        return [ingredient(v) for v in value]
    if isinstance(value, dict):
        if str(value.get("type", "")).startswith("neoforge:"):
            out = dict(value)
            out["neoforge:ingredient_type"] = out.pop("type")
            return out
        if "item" in value:
            return value["item"]
        if "tag" in value:
            return "#" + value["tag"]
    return value


def recipe(obj):
    obj = dict(obj)
    if "key" in obj:
        obj["key"] = {k: ingredient(v) for k, v in obj["key"].items()}
    for field in ("ingredient", "ingredients", "base", "addition", "template"):
        if field in obj:
            obj[field] = ingredient(obj[field])
    return obj


def conditions(obj):
    """NeoForge 21.4's load conditions: item_exists is "registered" (with "value")."""
    if isinstance(obj, list):
        return [conditions(v) for v in obj]
    if isinstance(obj, dict):
        if obj.get("type") == "neoforge:item_exists":
            return {"type": "neoforge:registered", "value": obj["item"]}
        return {k: conditions(v) for k, v in obj.items()}
    return obj


def eggs():
    """The spawn eggs' two colours, from ModItems.java: register("x_spawn_egg", () -> Registrars.spawnEgg(T, a, b, ..."""
    src = open(os.path.join(ROOT, "src/main/java/com/kolodkingal/enhanced/registry/ModItems.java"), encoding="utf-8").read()
    out = {}
    for name, a, b in re.findall(r'"(\w+_spawn_egg)",\s*\(\) -> Registrars\.spawnEgg\([\w.]+, (0x[0-9A-Fa-f]+), (0x[0-9A-Fa-f]+)', src):
        out[name] = (int(a, 16), int(b, 16))
    return out


def signed(rgb):
    v = 0xFF000000 | rgb
    return v - (1 << 32)


def ref(model):
    return {"type": "minecraft:model", "model": model if ":" in model else "minecraft:" + model}


def load(name):
    path = os.path.join(MODELS, name + ".json")
    return json.load(open(path, encoding="utf-8")) if os.path.exists(path) else None


def plain(item, model_id, tints=None):
    """The model of a model id: drawn by code when its model is (or its base is) builtin/entity."""
    name = model_id.split("/", 1)[1] if model_id.startswith(NS + ":item/") else None
    model = load(name) if name else None
    if item in SPECIAL and model is not None and model.get("parent") == "builtin/entity":
        return {"type": "minecraft:special", "base": model_id, "model": {"type": "enhanced:legacy", "id": f"{NS}:{item}"}}
    out = ref(model_id)
    if tints:
        out["tints"] = tints
    return out


def overridden(item, base_model, overrides, tints):
    """1.21.1's overrides on a base model as 1.21.4's dispatch: one property each (as the mod's models have)."""
    fallback = plain(item, base_model, tints)
    if not overrides:
        return fallback
    keys = {k for o in overrides for k in o["predicate"]}
    if len(keys) != 1:
        raise SystemExit(f"{item}: overrides on more than one property: {keys}")
    key = keys.pop()
    if key in ("blocking", "minecraft:blocking"):
        on = [o for o in overrides if o["predicate"][key] >= 1]
        return {"type": "minecraft:condition", "property": "minecraft:using_item",
                "on_true": plain(item, on[-1]["model"], tints), "on_false": fallback}
    prop = key if ":" in key else "minecraft:" + key
    entries = [{"threshold": o["predicate"][key], "model": plain(item, o["model"], tints)} for o in overrides]
    return {"type": "minecraft:range_dispatch", "property": "enhanced:legacy", "name": prop,
            "entries": entries, "fallback": fallback}


SUB_MODELS = {}


def sub_model(item, ctx, m):
    """A separate_transforms part: its parent if that is all it is, else a model file of its own (1.21.4 has no
    inline models)."""
    if set(m) == {"parent"}:
        return m["parent"]
    name = f"{item}_{ctx}"
    SUB_MODELS[name] = m
    return f"{NS}:item/{name}"


def replacement(model):
    """1.21.4's version of a model file that uses a NeoForge 21.1 loader (or None if it needs none)."""
    loader = model.get("loader")
    if loader == "neoforge:item_layers":
        # (21.4 has no per-layer light for generated items: the layers are drawn plainly)
        return {k: v for k, v in model.items() if k not in ("loader", "neoforge_data")}
    if loader == "neoforge:separate_transforms":
        base = model["base"]
        return dict(base) if set(base) != {"parent"} else {"parent": base["parent"]}
    return None


def item_model(item, model, egg):
    if egg:
        return {"type": "minecraft:model", "model": "minecraft:item/template_spawn_egg",
                "tints": [{"type": "minecraft:constant", "value": signed(egg[0])},
                          {"type": "minecraft:constant", "value": signed(egg[1])}]}
    tints = None
    if item in TINTED:
        top = max(TINTED[item])
        tints = [{"type": "enhanced:legacy", "item": f"{NS}:{item}", "index": i} if i in TINTED[item]
                 else {"type": "minecraft:constant", "value": -1} for i in range(top + 1)]
    overrides = model.get("overrides", [])
    if model.get("loader") == "neoforge:separate_transforms":
        base = sub_model(item, "base", model["base"])
        inner = overridden(item, base, overrides, tints)
        cases = [{"when": ctx, "model": plain(item, sub_model(item, ctx, m), tints)}
                 for ctx, m in model.get("perspectives", {}).items()]
        return {"type": "minecraft:select", "property": "minecraft:display_context", "cases": cases, "fallback": inner}
    return overridden(item, f"{NS}:item/{item}", overrides, tints)


def post_effect(obj):
    """1.21.1's post chain (shaders/post) as 1.21.4's post effect (post_effect)."""
    targets = {}
    for t in obj.get("targets", []):
        if isinstance(t, str):
            targets[t] = {}
        else:
            targets[t["name"]] = {k: v for k, v in t.items() if k != "name"}
    passes = []
    for p in obj["passes"]:
        out = {"program": "minecraft:post/" + p["name"],
               "inputs": [{"sampler_name": "In", "target": p["intarget"]}],
               "output": p["outtarget"]}
        if p.get("uniforms"):
            out["uniforms"] = [{"name": u["name"], "values": u["values"]} for u in p["uniforms"]]
        passes.append(out)
    return {"targets": targets, "passes": passes}


def main():
    for d in (DATA_4, ASSETS_4):
        shutil.rmtree(os.path.join(RES, d), ignore_errors=True)

    data = os.path.join(RES, "data", NS)
    for name in sorted(os.listdir(os.path.join(data, "recipe"))):
        obj = json.load(open(os.path.join(data, "recipe", name), encoding="utf-8"))
        write(os.path.join(RES, DATA_4, "data", NS, "recipe", name), conditions(recipe(obj)))
    for name in sorted(os.listdir(os.path.join(data, "worldgen", "biome"))):
        obj = json.load(open(os.path.join(data, "worldgen", "biome", name), encoding="utf-8"))
        carvers = obj.get("carvers")
        if isinstance(carvers, dict):
            obj["carvers"] = [c for step in carvers.values() for c in (step if isinstance(step, list) else [step])]
        music = obj.get("effects", {}).get("music")
        if isinstance(music, dict):
            obj["effects"]["music"] = [{"data": music, "weight": 1}]
        write(os.path.join(RES, DATA_4, "data", NS, "worldgen", "biome", name), obj)

    # every other data file whose NeoForge conditions changed
    for base, _, files in os.walk(os.path.join(RES, "data")):
        for name in files:
            path = os.path.join(base, name)
            rel = os.path.relpath(path, RES)
            if not name.endswith(".json") or os.path.exists(os.path.join(RES, DATA_4, rel)):
                continue
            obj = json.load(open(path, encoding="utf-8"))
            new = conditions(obj)
            if new != obj:
                write(os.path.join(RES, DATA_4, rel), new)

    colours = eggs()
    assets = os.path.join(RES, ASSETS_4, "assets", NS)
    for name in sorted(os.listdir(MODELS)):
        if not name.endswith(".json"):
            continue
        item = name[:-5]
        model = load(item)
        write(os.path.join(assets, "items", name), {"model": item_model(item, model, colours.get(item))})
        if model.get("parent") == "builtin/entity":
            copy = dict(model)
            del copy["parent"]
            write(os.path.join(assets, "models", "item", name), copy)
    # every model file (items' and their parts', in subfolders too) that uses a 21.1 loader
    for base, _, files in os.walk(os.path.join(RES, "assets", NS, "models")):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(base, name)
            new = replacement(json.load(open(path, encoding="utf-8")))
            if new is not None:
                write(os.path.join(RES, ASSETS_4, os.path.relpath(path, RES)), new)
    for name, m in SUB_MODELS.items():
        write(os.path.join(assets, "models", "item", name + ".json"), m)
    post = os.path.join(RES, "assets", NS, "post_chain")
    for name in sorted(os.listdir(post)) if os.path.isdir(post) else []:
        obj = json.load(open(os.path.join(post, name), encoding="utf-8"))
        write(os.path.join(assets, "post_effect", name), post_effect(obj))

    write(os.path.join(RES, "pack.mcmeta"), {
        "overlays": {"entries": [
            {"formats": {"min_inclusive": 57, "max_inclusive": 61}, "directory": DATA_4},
            {"formats": {"min_inclusive": 42, "max_inclusive": 46}, "directory": ASSETS_4},
        ]}
    })
    print("overlays written; spawn eggs:", sorted(colours))


if __name__ == "__main__":
    main()
