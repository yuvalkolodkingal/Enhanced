package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.asgard.build.CityTemplates;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * The operators' commands of Asgard (3.5), under {@code /enhanced asgard} (the root asks permission level 2):
 * <ul>
 * <li>{@code tp [players]}: to the Observatory ({@link CityPlan#ARRIVAL}; an operator's teleport passes the gate);</li>
 * <li>{@code state}: the realm's saved state and the generation times so far;</li>
 * <li>{@code genbench}: generates a 9 x 9 block of chunks of the city round the palace, and one of the wild sea, in
 * fresh chunks that are thrown away (the world is not touched), and prints the milliseconds per chunk;</li>
 * <li>{@code mendhall} (3.7.2): mends the Hall of Feasts now ({@link FeastHall#mend}: a hall of 3.5 to 3.7.1 becomes
 * the new one, a block a player changed is left alone; it is done once by itself when the hall is first loaded).</li>
 * </ul>
 */
public final class AsgardCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("asgard")
                .then(Commands.literal("tp")
                        .executes(ctx -> tp(ctx, List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ctx -> tp(ctx, EntityArgument.getPlayers(ctx, "players")))))
                .then(Commands.literal("state").executes(AsgardCommands::state))
                .then(Commands.literal("genbench").executes(AsgardCommands::genbench))
                .then(Commands.literal("mendhall").executes(AsgardCommands::mendHall));
    }

    private static int tp(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        ServerLevel asgard = AsgardDimension.level(ctx.getSource().getServer());
        if (asgard == null) {
            ctx.getSource().sendFailure(Component.translatable("message.enhanced.bifrost.no_world"));
            return 0;
        }
        Vec3 at = CityPlan.ARRIVAL;
        for (ServerPlayer player : players) {
            player.teleportTo(asgard, at.x, at.y, at.z, CityPlan.ARRIVAL_YAW, 0.0F);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Asgard: " + players.size() + " to the Observatory"), true);
        return players.size();
    }

    private static int state(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        AsgardState s = AsgardState.get(server);
        long chunks = AsgardChunkGenerator.CHUNKS.get();
        String text = String.format(Locale.ROOT, "Asgard: level %s, plan v%d (code v%d), Odin %s, Hela beaten %d times, murals %s, "
                        + "%d residents, seal ready at %d; %d chunks generated, land %.2f ms and city %.2f ms a chunk, worst city chunk %.1f ms",
                AsgardDimension.level(server) == null ? "missing" : "present", s.planVersion(), CityPlan.VERSION,
                s.odinAwake() ? "awake" : "asleep", s.helaDefeats(), s.muralsConquest() ? "conquest" : "peace", s.population().size(),
                s.sealReadyAt(), chunks, chunks == 0 ? 0.0 : AsgardChunkGenerator.LAND_NANOS.get() / 1e6 / chunks,
                chunks == 0 ? 0.0 : AsgardChunkGenerator.CITY_NANOS.get() / 1e6 / chunks, AsgardChunkGenerator.WORST_NANOS.get() / 1e6);
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int mendHall(CommandContext<CommandSourceStack> ctx) {
        ServerLevel asgard = AsgardDimension.level(ctx.getSource().getServer());
        if (asgard == null || !FeastHall.loaded(asgard)) {
            ctx.getSource().sendFailure(Component.literal("Asgard: the Hall of Feasts is not loaded: go near it"));
            return 0;
        }
        FeastHall.Mend done = FeastHall.mend(asgard);
        AsgardState.get(ctx.getSource().getServer()).setFeastHallMended(true);
        String text = "Asgard: the Hall of Feasts: " + done.repaired() + " blocks set, " + done.alreadyRight()
                + " already right, " + done.skipped() + " left as a player made them";
        ctx.getSource().sendSuccess(() -> Component.literal(text), true);
        return done.repaired();
    }

    /** The measured times of a block of chunks: average and worst, in milliseconds. */
    public record Bench(int chunks, double averageMs, double worstMs) {
    }

    /** Generates the 9 x 9 chunks round chunk (cx, cz) into fresh chunks (land and city), timing each. */
    public static Bench bench(MinecraftServer server, int cx, int cz) {
        LevelHeightAccessor height = AsgardDimension.level(server) != null ? AsgardDimension.level(server) : server.overworld();
        var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
        CityTemplates.all(server.getStructureManager());
        double total = 0;
        double worst = 0;
        int n = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                ProtoChunk chunk = new ProtoChunk(new ChunkPos(cx + dx, cz + dz), UpgradeData.EMPTY, height, biomes, null);
                long start = System.nanoTime();
                AsgardChunkGenerator.fillLand(chunk);
                CityBuild.place(chunk, server.getStructureManager());
                double ms = (System.nanoTime() - start) / 1e6;
                total += ms;
                worst = Math.max(worst, ms);
                n++;
            }
        }
        return new Bench(n, total / n, worst);
    }

    private static int genbench(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        bench(server, 0, -6);
        Bench palace = bench(server, 0, -6);
        Bench houses = bench(server, -6, 7);
        Bench wild = bench(server, 18, -4);
        String text = String.format(Locale.ROOT, "Asgard genbench (81 chunks each): the palace %.2f ms a chunk (worst %.2f), the houses %.2f "
                + "(worst %.2f), the wild land and the sea %.2f (worst %.2f)", palace.averageMs(), palace.worstMs(), houses.averageMs(),
                houses.worstMs(), wild.averageMs(), wild.worstMs());
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private AsgardCommands() {
    }
}
