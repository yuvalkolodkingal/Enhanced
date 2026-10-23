package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** An event about a player (NeoForge's PlayerEvent). */
public abstract class PlayerEvent extends LivingEvent {
    protected PlayerEvent(Player entity) {
        super(entity);
    }

    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    /** A player joined the server. */
    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player entity) {
            super(entity);
        }
    }

    /** A player left the server. */
    public static class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player entity) {
            super(entity);
        }
    }

    /** A player arrived in another dimension. */
    public static class PlayerChangedDimensionEvent extends PlayerEvent {
        private final ResourceKey<Level> from;
        private final ResourceKey<Level> to;

        public PlayerChangedDimensionEvent(Player entity, ResourceKey<Level> from, ResourceKey<Level> to) {
            super(entity);
            this.from = from;
            this.to = to;
        }

        public ResourceKey<Level> getFrom() {
            return from;
        }

        public ResourceKey<Level> getTo() {
            return to;
        }
    }

    /** A player respawned (or came back from the End). */
    public static class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;

        public PlayerRespawnEvent(Player entity, boolean endConquered) {
            super(entity);
            this.endConquered = endConquered;
        }

        public boolean isEndConquered() {
            return endConquered;
        }
    }

    /** A player starts to see (track) an entity: the moment to send it what the client must know. */
    public static class StartTracking extends PlayerEvent {
        private final Entity target;

        public StartTracking(Player entity, Entity target) {
            super(entity);
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    /** A new player object replaces the old one (respawn or return from the End): copy what must survive. */
    public static class Clone extends PlayerEvent {
        private final Player original;
        private final boolean wasDeath;

        public Clone(Player entity, Player original, boolean wasDeath) {
            super(entity);
            this.original = original;
            this.wasDeath = wasDeath;
        }

        public Player getOriginal() {
            return original;
        }

        public boolean isWasDeath() {
            return wasDeath;
        }
    }

    /** How fast a player breaks a block. */
    public static class BreakSpeed extends PlayerEvent implements Cancellable {
        private final BlockState state;
        private final float originalSpeed;
        private float newSpeed;
        private final @Nullable BlockPos pos;
        private boolean canceled;

        public BreakSpeed(Player entity, BlockState state, float originalSpeed, @Nullable BlockPos pos) {
            super(entity);
            this.state = state;
            this.originalSpeed = originalSpeed;
            this.newSpeed = originalSpeed;
            this.pos = pos;
        }

        public BlockState getState() {
            return state;
        }

        public float getOriginalSpeed() {
            return originalSpeed;
        }

        public float getNewSpeed() {
            return newSpeed;
        }

        public void setNewSpeed(float newSpeed) {
            this.newSpeed = newSpeed;
        }

        @Nullable
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        public Optional<BlockPos> getPosition() {
            return Optional.ofNullable(getPos());
        }
    }

    /** A player took a crafted item. */
    public static class ItemCraftedEvent extends PlayerEvent {
        private final ItemStack crafting;
        private final Container inventory;

        public ItemCraftedEvent(Player entity, ItemStack crafting, Container inventory) {
            super(entity);
            this.crafting = crafting;
            this.inventory = inventory;
        }

        public ItemStack getCrafting() {
            return crafting;
        }

        public Container getInventory() {
            return inventory;
        }
    }

    /** A player's game mode is about to change. */
    public static class PlayerChangeGameModeEvent extends PlayerEvent implements Cancellable {
        private final GameType currentGameMode;
        private GameType newGameMode;
        private boolean canceled;

        public PlayerChangeGameModeEvent(Player entity, GameType currentGameMode, GameType newGameMode) {
            super(entity);
            this.currentGameMode = currentGameMode;
            this.newGameMode = newGameMode;
        }

        public GameType getCurrentGameMode() {
            return currentGameMode;
        }

        public GameType getNewGameMode() {
            return newGameMode;
        }

        public void setNewGameMode(GameType newGameMode) {
            this.newGameMode = newGameMode;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }
}
