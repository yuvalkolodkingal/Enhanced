package com.kolodkingal.enhanced.tower;

/**
 * The Avengers Tower's armoury as a change to the 3.6.1 tower (3.8, PLAN 9.6): what {@link TowerRefit} sets
 * down in a tower generated before 3.8. Written by the tower's generator from its 3.6.1 and 3.8.1 models
 * ({@code SCRATCH/v38/art/l6/tower381.py --refit}); not edited by hand. Points are in the generator's model
 * frame ({@code TowerPlan}: x east, z south, y the tower's layer).
 */
final class TowerRefitTable {
    /**
     * x y z | the 3.6.1 block | the 3.8.1 block | its block entity (SNBT, or a sign's text: sign:<colour>:<its four
     * lines' lang keys>; empty: none). Blocks without a namespace are the game's.
     */
    static final String[] BLOCKS = {
            "12 6 8|dark_oak_wall_sign[facing=south]|dark_oak_wall_sign[facing=south]|sign:white:,sign.enhanced.ave"
            + "ngers_tower,sign.enhanced.tower.workshop_hint,sign.enhanced.tower.workshop_hint_2",
            "12 56 12|air|birch_wall_sign[facing=south]|sign:light_blue:,sign.enhanced.tower.project_rebirth,,",
            "12 57 11|air|enhanced:tech_case[facing=south,generated=true]|{id:\"enhanced:tech_case\",shown:{id:\"en"
            + "hanced:special_soldier_serum\",count:1},rule:\"anyone\"}",
            "6 62 8|air|enhanced:suit_display[facing=south,generated=true,half=lower]|{id:\"enhanced:suit_display\""
            + ",suit:\"mark_1\"}",
            "8 62 8|air|enhanced:suit_display[facing=south,generated=true,half=lower]|{id:\"enhanced:suit_display\""
            + ",suit:\"mark_3\"}",
            "10 62 8|air|enhanced:suit_display[facing=south,generated=true,half=lower]|{id:\"enhanced:suit_display"
            + "\",suit:\"mark_5\"}",
            "12 62 8|air|enhanced:suit_display[facing=south,generated=true,half=lower]|{id:\"enhanced:suit_display"
            + "\",suit:\"mark_50\"}",
            "14 62 8|air|enhanced:suit_display[facing=south,generated=true,half=lower]|{id:\"enhanced:suit_display"
            + "\",suit:\"mark_85\"}",
            "13 62 13|air|dark_oak_wall_sign[facing=south]|sign:light_blue:,sign.enhanced.tower.reactors,,",
            "12 62 17|air|enhanced:suit_display[facing=south,generated=true,half=lower]|{id:\"enhanced:suit_display"
            + "\",suit:\"hulkbuster\"}",
            "12 62 19|air|dark_oak_sign[rotation=0]|sign:red:,sign.enhanced.tower.hulkbuster,,",
            "6 63 8|air|enhanced:suit_display[facing=south,generated=true,half=upper]|",
            "8 63 8|air|enhanced:suit_display[facing=south,generated=true,half=upper]|",
            "10 63 8|air|enhanced:suit_display[facing=south,generated=true,half=upper]|",
            "12 63 8|air|enhanced:suit_display[facing=south,generated=true,half=upper]|",
            "14 63 8|air|enhanced:suit_display[facing=south,generated=true,half=upper]|",
            "12 63 12|beacon|enhanced:tech_case[facing=south,generated=true]|{id:\"enhanced:tech_case\",shown:{id:"
            + "\"enhanced:arc_reactor\",count:1},rule:\"anyone\"}",
            "13 63 12|black_stained_glass_pane[east=true,north=false,south=false,west=true]|enhanced:tech_case[faci"
            + "ng=south,generated=true]|{id:\"enhanced:tech_case\",shown:{id:\"enhanced:nano_arc_reactor\",count:1}"
            + ",rule:\"anyone\"}",
            "12 63 17|air|enhanced:suit_display[facing=south,generated=true,half=upper]|",
            "6 64 8|air|dark_oak_wall_sign[facing=south]|sign:red:,sign.enhanced.tower.mark_1,,",
            "8 64 8|air|dark_oak_wall_sign[facing=south]|sign:red:,sign.enhanced.tower.mark_3,,",
            "10 64 8|air|dark_oak_wall_sign[facing=south]|sign:red:,sign.enhanced.tower.mark_5,,",
            "12 64 8|air|dark_oak_wall_sign[facing=south]|sign:red:,sign.enhanced.tower.mark_50,,",
            "14 64 8|air|dark_oak_wall_sign[facing=south]|sign:red:,sign.enhanced.tower.mark_85,,",
            "7 65 8|air|iron_bars[east=false,north=false,south=false,west=false]|",
            "9 65 8|air|iron_bars[east=false,north=false,south=false,west=false]|",
            "7 66 8|air|iron_bars[east=true,north=false,south=false,west=false]|",
            "8 66 8|air|iron_bars[east=true,north=false,south=false,west=true]|",
            "9 66 8|air|iron_bars[east=false,north=false,south=false,west=true]|",
            "8 67 8|air|chain[axis=y]|",
            "8 68 8|air|chain[axis=y]|",
            "8 69 8|air|chain[axis=y]|",
            "8 70 8|air|chain[axis=y]|",
            "8 71 8|air|chain[axis=y]|",
            "8 72 8|air|chain[axis=y]|",
            "7 83 9|air|enhanced:tech_case[facing=south,generated=true]|{id:\"enhanced:tech_case\",shown:{id:\"enha"
            + "nced:vibranium_shield\",count:1},rule:\"captain_only\"}",
            "13 90 16|air|end_rod[facing=up]|",
            "14 90 16|cartography_table|enhanced:tech_case[facing=south,generated=true]|{id:\"enhanced:tech_case\","
            + "shown:{id:\"enhanced:falcon_exo_wings\",count:1},rule:\"anyone\"}",
            "15 90 16|air|end_rod[facing=up]|",
            "13 90 18|air|cartography_table|",
            "14 90 18|air|birch_sign[rotation=0]|sign:light_blue:,sign.enhanced.tower.falcon_wings,sign.enhanced.to"
            + "wer.falcon_wings_2,",
    };

    /** The 3.6.1 armour stands that go: x y z | their armour (feet, legs, chest, head). */
    static final String[] STANDS = {
            "6 62 8|golden_boots,leather_leggings,leather_chestplate,golden_helmet",
            "8 62 8|leather_boots,golden_leggings,leather_chestplate,golden_helmet",
            "10 62 8|iron_boots,iron_leggings,iron_chestplate,iron_helmet",
            "12 62 8|golden_boots,golden_leggings,leather_chestplate,leather_helmet",
            "14 62 8|netherite_boots,netherite_leggings,netherite_chestplate,netherite_helmet",
    };

    /** The 3.6.1 item frames that go: x y z | facing | the item. */
    static final String[] FRAMES = {
            "7 84 9|west|shield",
    };

    /** Blocks both towers have by the armoury (a 3.6.1 or 3.8 tower: most still stand): x y z | block. */
    static final String[] ANCHORS = {
            "10 62 12|smooth_quartz_slab[type=top]",
            "11 62 12|smooth_quartz_slab[type=top]",
            "14 62 12|smooth_quartz_slab[type=top]",
            "12 62 13|quartz_stairs[facing=south,half=bottom,shape=straight]",
            "7 62 8|light_blue_stained_glass_pane[east=false,north=false,south=false,west=false]",
            "13 62 8|light_blue_stained_glass_pane[east=false,north=false,south=false,west=false]",
            "10 56 10|iron_block",
            "13 56 11|iron_block",
            "11 57 11|brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]",
            "13 57 11|brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]",
            "6 83 9|blue_bed[facing=north,occupied=false,part=head]",
            "8 84 9|white_concrete",
            "16 90 16|lectern[facing=south,has_book=false,powered=false]",
            "12 6 7|iron_block",
    };

    private TowerRefitTable() {
    }
}
