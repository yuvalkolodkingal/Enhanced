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

1.21.11 (data format 94.1, resource format 75; read from the client jar's version.json): the 1.21.4 overlays still
apply (their formats did not change again: recipes, item model definitions, the loader-free models), with a 1.21.11
overlay on top of them for what changed again:
* data: biomes with their colours, music and sounds as environment attributes (1.21.11); dimension types with their
  sky, beds, anchors and piglins as attributes, the mod's own skies as NeoForge's custom sky, clouds and weather
  (compat.DimensionSpecialEffects), and a fixed time as a timeline of its own (data/enhanced/timeline/fixed_<t>):
  the vanilla day's tracks held at that time;
* assets: the post effects in 1.21.6's format (a fragment shader of the mod's own, shaders/post/slow_motion.fsh,
  for 1.21.1's colour convolution: 1.21.11's has no saturation or colour scale).
1.21.11 checks the overlay entries' formats strictly: every entry has "formats", and the 1.21.11 ones also
"min_format" and "max_format" (the same major version).
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
DATA_11 = "overlay_1_21_11_data"
ASSETS_11 = "overlay_1_21_11_assets"
# 1.21.11's pack formats (version.json of the 1.21.11 client jar: pack_version data_major 94.1, resource_major 75)
DATA_FORMAT_11 = 94
ASSETS_FORMAT_11 = 75
# 1.21.11's vanilla day (data/minecraft/timeline/day.json of the 1.21.11 jar), its tracks a fixed time holds: tools/
DAY_11 = os.path.join(ROOT, "tools", "day_1_21_11.json")
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


# ---------------------------------------------------------------------------------------------------------------- 1.21.11

def hex_rgb(v):
    return "#%06x" % (v & 0xFFFFFF)


def biome_11(obj):
    """1.21.1's biome as 1.21.11's: colours, music and sounds are environment attributes; the carvers a list."""
    obj = json.loads(json.dumps(obj))
    effects = obj.get("effects", {})
    attributes = dict(obj.get("attributes", {}))
    out_effects = {}
    for key in ("water_color", "grass_color", "foliage_color"):
        if key in effects:
            out_effects[key] = hex_rgb(effects[key]) if isinstance(effects[key], int) else effects[key]
    if "grass_color_modifier" in effects:
        out_effects["grass_color_modifier"] = effects["grass_color_modifier"]
    for key, attr in (("fog_color", "visual/fog_color"), ("sky_color", "visual/sky_color"),
                      ("water_fog_color", "visual/water_fog_color")):
        if key in effects:
            attributes["minecraft:" + attr] = hex_rgb(effects[key])
    music = effects.get("music")
    if isinstance(music, list):
        music = music[0]["data"] if music else None
    if music:
        attributes["minecraft:audio/background_music"] = {"default": {k: v for k, v in music.items()}}
    sounds = {}
    if "ambient_sound" in effects:
        sounds["loop"] = effects["ambient_sound"]
    if "mood_sound" in effects:
        sounds["mood"] = effects["mood_sound"]
    if "additions_sound" in effects:
        sounds["additions"] = effects["additions_sound"]
    if sounds:
        attributes["minecraft:audio/ambient_sounds"] = sounds
    particle = effects.get("particle")
    if particle:
        attributes["minecraft:visual/ambient_particles"] = [{"particle": particle["options"],
                                                             "probability": particle["probability"]}]
    obj["effects"] = out_effects
    if attributes:
        obj["attributes"] = attributes
    carvers = obj.get("carvers")
    if isinstance(carvers, dict):
        obj["carvers"] = [c for step in carvers.values() for c in (step if isinstance(step, list) else [step])]
    return obj


def colour(v):
    """A timeline value as (a, r, g, b) and how to write it back."""
    if isinstance(v, str):
        h = v.lstrip("#")
        if len(h) == 6:
            return (255, int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)), "rgb"
        return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), int(h[6:8], 16)), "argb"
    u = v & 0xFFFFFFFF
    return ((u >> 24) & 255, (u >> 16) & 255, (u >> 8) & 255, u & 255), "int"


def uncolour(c, kind):
    a, r, g, b = (int(round(x)) for x in c)
    if kind == "rgb":
        return "#%02x%02x%02x" % (r, g, b)
    if kind == "argb":
        return "#%02x%02x%02x%02x" % (a, r, g, b)
    u = (a << 24) | (r << 16) | (g << 8) | b
    return u - (1 << 32) if u >= (1 << 31) else u


def held(track, t, period):
    """A day track's value at day time {@code t} (linear between its keyframes, round the day)."""
    frames = sorted(track["keyframes"], key=lambda f: f["ticks"])
    values = [f["value"] for f in frames]
    if len(frames) == 2 and frames[0]["ticks"] == frames[1]["ticks"] and isinstance(values[0], (int, float)):
        # an angle that turns once a day from that time (the sun's, the stars', the moon's)
        start, a, b = frames[0]["ticks"], values[1], values[0]
        return a + (b - a) * (((t - start) % period) / period)
    before = [f for f in frames if f["ticks"] <= t]
    after = [f for f in frames if f["ticks"] > t]
    f0 = before[-1] if before else dict(frames[-1], ticks=frames[-1]["ticks"] - period)
    f1 = after[0] if after else dict(frames[0], ticks=frames[0]["ticks"] + period)
    v0, v1 = f0["value"], f1["value"]
    if isinstance(v0, bool) or not isinstance(v0, (int, float, str)):
        return v0
    k = 0.0 if f1["ticks"] == f0["ticks"] else (t - f0["ticks"]) / (f1["ticks"] - f0["ticks"])
    if isinstance(v0, str) or (isinstance(v0, int) and "color" in track.get("_name", "")):
        c0, kind = colour(v0)
        c1, _ = colour(v1)
        return uncolour([x + (y - x) * k for x, y in zip(c0, c1)], kind)
    return v0 + (v1 - v0) * k


def fixed_timeline(t):
    """A timeline that holds the vanilla day's tracks at day time {@code t} (1.21.1's fixed_time)."""
    day = json.load(open(DAY_11, encoding="utf-8"))
    period = day.get("period_ticks", 24000)
    tracks = {}
    for name, track in day["tracks"].items():
        track = dict(track, _name=name)
        value = held(track, t, period)
        out = {"keyframes": [{"ticks": 0, "value": value}, {"ticks": period // 2, "value": value}]}
        if "modifier" in track:
            out["modifier"] = track["modifier"]
        tracks[name] = out
    return {"period_ticks": period, "tracks": tracks}


DIMENSION_KEEP = ("ambient_light", "coordinate_scale", "has_ceiling", "has_skylight", "height", "infiniburn",
                  "logical_height", "min_y", "monster_spawn_block_light_limit", "monster_spawn_light_level")


def dimension_type_11(obj, timelines):
    """1.21.1's dimension type as 1.21.11's (see the module's doc)."""
    out = {k: obj[k] for k in DIMENSION_KEEP if k in obj}
    attributes = {}
    effects = obj.get("effects", "minecraft:overworld")
    if effects == "minecraft:the_nether":
        out["skybox"] = "none"
        out["cardinal_light"] = "nether"
    elif effects == "minecraft:the_end":
        out["skybox"] = "end"
    elif effects != "minecraft:overworld":
        # the mod's own sky: NeoForge's custom renderers, registered under this id (ClientSetup, compat.DimensionSpecialEffects);
        # the skybox is not "none", for 1.21.11 adds the sky pass (where NeoForge calls the custom renderer) only for a skybox
        out["skybox"] = "overworld"
        for kind in ("custom_skybox", "custom_clouds", "custom_weather_effects"):
            attributes["neoforge:" + kind] = effects
    if obj.get("bed_works", True):
        attributes["minecraft:gameplay/bed_rule"] = {"can_set_spawn": "always", "can_sleep": "when_dark",
                                                     "error_message": {"translate": "block.minecraft.bed.no_sleep"}}
    else:
        attributes["minecraft:gameplay/bed_rule"] = {"can_set_spawn": "never", "can_sleep": "never", "explodes": True}
    attributes["minecraft:gameplay/respawn_anchor_works"] = bool(obj.get("respawn_anchor_works", False))
    if obj.get("piglin_safe", False):
        attributes["minecraft:gameplay/piglins_zombify"] = False
    if not obj.get("has_raids", True):
        attributes["minecraft:gameplay/can_start_raid"] = False
    if obj.get("ultrawarm", False):
        attributes["minecraft:gameplay/water_evaporates"] = True
        attributes["minecraft:gameplay/fast_lava"] = True
    if "fixed_time" in obj:
        t = int(obj["fixed_time"])
        out["has_fixed_time"] = True
        name = f"fixed_{t}"
        timelines[name] = fixed_timeline(t)
        out["timelines"] = [f"{NS}:{name}"]
    else:
        out["timelines"] = "#minecraft:in_overworld"
    out["attributes"] = attributes
    return out


def post_effect_11(name, obj):
    """1.21.1's post chain as 1.21.6's post effect: colour convolutions with the mod's shader (enhanced:post/slow_motion,
    1.21.1's colour convolution), blits with vanilla's."""
    targets = {}
    for t in obj.get("targets", []):
        if isinstance(t, str):
            targets[t] = {}
        else:
            targets[t["name"]] = {k: v for k, v in t.items() if k != "name"}
    passes = []
    for p in obj["passes"]:
        values = {u["name"]: u["values"] for u in p.get("uniforms", [])}
        out = {"vertex_shader": "minecraft:core/screenquad",
               "inputs": [{"sampler_name": "In", "target": p["intarget"]}],
               "output": p["outtarget"]}
        if p["name"] == "color_convolve":
            out["fragment_shader"] = f"{NS}:post/slow_motion"
            out["uniforms"] = {"SlowMotionConfig": [
                {"name": "Saturation", "type": "float", "value": values.get("Saturation", [1.8])[0]},
                {"name": "ColorScale", "type": "vec3", "value": values.get("ColorScale", [1.0, 1.0, 1.0])},
            ]}
        elif p["name"] == "blit":
            out["fragment_shader"] = "minecraft:post/blit"
            out["uniforms"] = {"BlitConfig": [{"name": "ColorModulate", "type": "vec4",
                                               "value": values.get("ColorModulate", [1.0, 1.0, 1.0, 1.0])}]}
        else:
            raise SystemExit(f"{name}: no 1.21.11 shader for the pass {p['name']}")
        passes.append(out)
    return {"targets": targets, "passes": passes}


# 1.21.1's post/color_convolve (with its saturation and colour scale), for 1.21.6's post pipeline
SLOW_MOTION_FSH = """#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// (Enhanced: 1.21.1's colour convolution, as the slow motion effect used it; 1.21.11's has neither uniform)
layout(std140) uniform SlowMotionConfig {
    float Saturation;
    vec3 ColorScale;
};

const vec3 Gray = vec3(0.3, 0.59, 0.11);

out vec4 fragColor;

void main() {
    vec4 InTexel = texture(InSampler, texCoord);
    vec3 OutColor = InTexel.rgb * ColorScale;
    float Luma = dot(OutColor, Gray);
    vec3 Chroma = OutColor - Luma;
    OutColor = (Chroma * Saturation) + Luma;
    fragColor = vec4(OutColor, 1.0);
}
"""


# ids that changed from 1.21.9 (the copper age: the chain is the iron chain)
IDS_11 = {"minecraft:chain": "minecraft:iron_chain"}


def own_item_check(condition):
    """A load condition that only asks whether one of the mod's own items exists (they always do)."""
    return (isinstance(condition, dict) and condition.get("type") in ("neoforge:item_exists", "neoforge:registered")
            and str(condition.get("item", condition.get("value", ""))).startswith(NS + ":")
            and condition.get("registry", "minecraft:item") == "minecraft:item")


def data_11(obj):
    """What changed again in any data file: ids, the dyed colour component (a plain colour from 1.21.5), and the load
    conditions on the mod's own items (1.21.11's registered condition does not see the items when loot tables load:
    the items always exist, so the condition goes)."""
    if isinstance(obj, list):
        return [data_11(v) for v in obj]
    if isinstance(obj, str):
        return IDS_11.get(obj, ("#" + IDS_11[obj[1:]]) if obj.startswith("#") and obj[1:] in IDS_11 else obj)
    if not isinstance(obj, dict):
        return obj
    out = {}
    for k, v in obj.items():
        if k == "minecraft:dyed_color" and isinstance(v, dict) and "rgb" in v:
            out[k] = v["rgb"]
        elif k == "neoforge:conditions" and isinstance(v, list) and all(own_item_check(c) for c in v):
            continue
        else:
            out[k] = data_11(v)
    return out


def main_11():
    for d in (DATA_11, ASSETS_11):
        shutil.rmtree(os.path.join(RES, d), ignore_errors=True)
    # every data file as 1.21.11 sees it (1.21.4's overlay over the 1.21.1 file) that changed again
    for base, _, files in os.walk(os.path.join(RES, "data")):
        for name in files:
            if not name.endswith(".json"):
                continue
            rel = os.path.relpath(os.path.join(base, name), RES)
            seen = os.path.join(RES, DATA_4, rel)
            path = seen if os.path.exists(seen) else os.path.join(RES, rel)
            obj = json.load(open(path, encoding="utf-8"))
            new = data_11(obj)
            if new != obj:
                write(os.path.join(RES, DATA_11, rel), new)
    data = os.path.join(RES, "data", NS)
    for name in sorted(os.listdir(os.path.join(data, "worldgen", "biome"))):
        obj = json.load(open(os.path.join(data, "worldgen", "biome", name), encoding="utf-8"))
        write(os.path.join(RES, DATA_11, "data", NS, "worldgen", "biome", name), biome_11(obj))
    timelines = {}
    for name in sorted(os.listdir(os.path.join(data, "dimension_type"))):
        obj = json.load(open(os.path.join(data, "dimension_type", name), encoding="utf-8"))
        write(os.path.join(RES, DATA_11, "data", NS, "dimension_type", name), dimension_type_11(obj, timelines))
    for name, obj in timelines.items():
        write(os.path.join(RES, DATA_11, "data", NS, "timeline", name + ".json"), obj)

    assets = os.path.join(RES, ASSETS_11, "assets", NS)
    post = os.path.join(RES, "assets", NS, "post_chain")
    for name in sorted(os.listdir(post)) if os.path.isdir(post) else []:
        obj = json.load(open(os.path.join(post, name), encoding="utf-8"))
        write(os.path.join(assets, "post_effect", name), post_effect_11(name, obj))
    os.makedirs(os.path.join(assets, "shaders", "post"), exist_ok=True)
    with open(os.path.join(assets, "shaders", "post", "slow_motion.fsh"), "w", encoding="utf-8") as f:
        f.write(SLOW_MOTION_FSH)


def entries_11(directories, fmt):
    return [{"formats": {"min_inclusive": fmt, "max_inclusive": fmt}, "min_format": fmt, "max_format": fmt,
             "directory": d} for d in directories]


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

    main_11()

    # (a later entry wins over an earlier one: 1.21.11's overlays are on top of 1.21.4's)
    write(os.path.join(RES, "pack.mcmeta"), {
        "overlays": {"entries": [
            {"formats": {"min_inclusive": 57, "max_inclusive": 61}, "directory": DATA_4},
            {"formats": {"min_inclusive": 42, "max_inclusive": 46}, "directory": ASSETS_4},
        ] + entries_11([DATA_4, DATA_11], DATA_FORMAT_11) + entries_11([ASSETS_4, ASSETS_11], ASSETS_FORMAT_11)}
    })
    print("overlays written; spawn eggs:", sorted(colours))


if __name__ == "__main__":
    main()
