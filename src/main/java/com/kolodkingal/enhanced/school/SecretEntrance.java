package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.block.ElevatorPadBlock;
import com.kolodkingal.enhanced.block.SecretBookshelfBlock;
import com.kolodkingal.enhanced.config.SchoolLabConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The lab's secret entrance in the Professor's study (3.7, the plan's 6.4): the four bookshelves of the west wall north
 * of the fireplace ({@link SchoolLabPlan#STUDY_SHELVES}) become secret bookshelves that swing open, the two planks
 * behind them ({@link SchoolLabPlan#STUDY_PADS}) the study's elevator pads, and the empty lectern ({@link
 * SchoolLabPlan#LECTERN}) gets the Professor's notes. Only where the school's own block still stands (the {@code
 * SchoolRepair.same} rule): a player's block there leaves the entrance out unless forced. Nothing else above ground
 * changes; no shaft is dug (the lift is a ride).
 */
public final class SecretEntrance {
    /** The heroes who may open the shelves when {@code [school_lab] openToAll} is off (operators may too). */
    public static final Set<Power> X_MEN = EnumSet.of(Power.PROFESSOR_X, Power.CYCLOPS, Power.WOLVERINE, Power.PHOENIX,
            Power.MAGNETO, Power.QUICKSILVER, Power.DEADPOOL);
    /** How long the shelves stay open (ticks) before they look whether they may close. */
    public static final int OPEN_TICKS = 100;

    /**
     * Puts the entrance in the study of the school at {@code site}: every shelf and pad, or none (a half entrance would
     * strand nobody, but would make no sense). Returns false when a player's block is in the way and not {@code force}.
     */
    public static boolean place(ServerLevel level, LabSite site, boolean force) {
        if (!force) {
            for (BlockPos p : SchoolLabPlan.STUDY_SHELVES) {
                BlockState there = level.getBlockState(site.school(p));
                if (!there.is(Blocks.BOOKSHELF) && !there.is(ModBlocks.SECRET_BOOKSHELF.get())) {
                    return false;
                }
            }
            for (BlockPos p : SchoolLabPlan.STUDY_PADS) {
                BlockState there = level.getBlockState(site.school(p));
                if (!there.is(Blocks.SPRUCE_PLANKS) && !there.is(ModBlocks.ELEVATOR_PAD.get())) {
                    return false;
                }
            }
        }
        for (BlockPos p : SchoolLabPlan.STUDY_SHELVES) {
            BlockPos at = site.school(p);
            if (!level.getBlockState(at).is(ModBlocks.SECRET_BOOKSHELF.get())) {
                level.setBlock(at, shelf(site, p), Block.UPDATE_ALL);
            }
        }
        for (BlockPos p : SchoolLabPlan.STUDY_PADS) {
            BlockPos at = site.school(p);
            if (!level.getBlockState(at).is(ModBlocks.ELEVATOR_PAD.get())) {
                level.setBlock(at, ModBlocks.ELEVATOR_PAD.get().defaultBlockState()
                        .setValue(ElevatorPadBlock.FACING, site.turn(Direction.EAST)), Block.UPDATE_ALL);
            }
        }
        if (lecternHint()) {
            BlockPos at = site.school(SchoolLabPlan.LECTERN);
            BlockState lectern = level.getBlockState(at);
            if (lectern.getBlock() instanceof LecternBlock && !lectern.getValue(LecternBlock.HAS_BOOK)) {
                LecternBlock.tryPlaceBook(null, level, at, lectern, notes());
            }
        }
        return true;
    }

    /**
     * The secret shelf the study's template position {@code p} gets: facing east into the study (turned), hinged at
     * its outer side (the north column's on the left of one in the nook, the south column's on the right).
     */
    static BlockState shelf(LabSite site, BlockPos p) {
        DoorHingeSide hinge = p.getZ() == SchoolLabPlan.STUDY_SHELVES.get(0).getZ() ? DoorHingeSide.LEFT
                : DoorHingeSide.RIGHT;
        return ModBlocks.SECRET_BOOKSHELF.get().defaultBlockState()
                .setValue(SecretBookshelfBlock.FACING, site.turn(Direction.EAST))
                .setValue(SecretBookshelfBlock.HINGE, hinge);
    }

    /** Opens the study's shelves of the school at {@code site} (a rider coming up), if they are there. */
    public static void openShelves(ServerLevel level, LabSite site) {
        BlockPos first = site.school(SchoolLabPlan.STUDY_SHELVES.get(0));
        if (level.getBlockState(first).is(ModBlocks.SECRET_BOOKSHELF.get())) {
            SecretBookshelfBlock.swing(level, first, true);
        }
    }

    /** Whether {@code player} may open a secret shelf ({@code openToAll}, else the X-Men and operators). */
    public static boolean mayOpen(Player player) {
        if (!EnhancedConfig.SPEC.isLoaded() || SchoolLabConfig.OPEN_TO_ALL.get() || player.hasPermissions(2)) {
            return true;
        }
        return X_MEN.contains(Powers.power(player));
    }

    static boolean lecternHint() {
        return !EnhancedConfig.SPEC.isLoaded() || SchoolLabConfig.LECTERN_HINT.get();
    }

    /** The Professor's notes: a written book whose name and page are translated (EN/HE). */
    public static ItemStack notes() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("The Professor's notes"), "C.X.", 0,
                List.of(Filterable.passThrough(Component.translatable("book.enhanced.professor_notes.page1")),
                        Filterable.passThrough(Component.translatable("book.enhanced.professor_notes.page2"))),
                true));
        book.set(DataComponents.CUSTOM_NAME, Component.translatable("book.enhanced.professor_notes.title"));
        return book;
    }

    private SecretEntrance() {
    }
}
