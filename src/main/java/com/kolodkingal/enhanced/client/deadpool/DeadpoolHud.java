package com.kolodkingal.enhanced.client.deadpool;

import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.client.render.DeadpoolActing;
import com.kolodkingal.enhanced.deadpool.DeadpoolState;
import com.kolodkingal.enhanced.deadpool.DogpoolEntity;
import com.kolodkingal.enhanced.deadpool.Pistols;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Deadpool's HUD lines (3.7, after each key's name): R the Parry's time while he holds it; G the magazine ("16/16",
 * gold when low) or "Reloading…"; B the chimichangas in his bag; N Dogpool's time and health while he is out; the
 * cooldowns as every hero's.
 */
public final class DeadpoolHud {
    /** The Parry's longest hold as the HUD counts it (the server's default). */
    static final int PARRY_TICKS = 80;

    /** The status after key {@code ability}'s line on his HUD. */
    @Nullable
    public static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        Component cooldown = PowerHud.cooldown(data, ability, now);
        return switch (ability) {
            case 1 -> {
                if (data.is(Live.DEADPOOL_PARRYING)) {
                    float spun = DeadpoolActing.parrySpin(player, 0.0F) / 47.0F;
                    float left = Math.max(0.0F, (PARRY_TICKS - spun) / 20.0F);
                    yield Component.translatable("hud.enhanced.deadpool.parrying",
                            String.format(Locale.ROOT, "%.1f", left)).withStyle(ChatFormatting.YELLOW);
                }
                yield cooldown;
            }
            case 2 -> {
                DeadpoolState state = ModAttachments.DEADPOOL.get(player);
                MutableComponent line;
                if (state.reloadAt() > now) {
                    line = Component.translatable("hud.enhanced.deadpool.reloading").withStyle(ChatFormatting.AQUA);
                } else {
                    int ammo = Math.min(state.ammo(), Pistols.magazine());
                    line = Component.literal(ammo + "/" + Pistols.magazine()).withStyle(ammo > 4
                            ? ChatFormatting.WHITE : ChatFormatting.GOLD);
                }
                yield cooldown == null ? line : line.append(" ").append(cooldown);
            }
            case 4 -> {
                int snacks = player.getInventory().countItem(ModItems.CHIMICHANGA.get());
                MutableComponent line = snacks > 0
                        ? Component.translatable("hud.enhanced.deadpool.snacks", snacks).withStyle(ChatFormatting.GOLD)
                        : Component.translatable("hud.enhanced.deadpool.no_snacks").withStyle(ChatFormatting.DARK_GRAY);
                yield cooldown == null ? line : cooldown.copy().append(" ").append(line);
            }
            case 5 -> {
                DeadpoolState state = ModAttachments.DEADPOOL.get(player);
                if (state.dogpool().isPresent() && state.dogpoolUntil() > now) {
                    long left = (state.dogpoolUntil() - now) / 20;
                    String time = String.format(Locale.ROOT, "%d:%02d", left / 60, left % 60);
                    DogpoolEntity dog = dogpool(player);
                    String health = dog == null ? "?" : String.valueOf(Math.round(dog.getHealth()));
                    yield Component.translatable("hud.enhanced.deadpool.dogpool", time, health)
                            .withStyle(ChatFormatting.RED);
                }
                yield cooldown;
            }
            default -> cooldown;
        };
    }

    /** His Dogpool as his client sees him (near), or null. */
    @Nullable
    static DogpoolEntity dogpool(LocalPlayer player) {
        for (DogpoolEntity dog : player.level().getEntitiesOfClass(DogpoolEntity.class,
                player.getBoundingBox().inflate(64.0))) {
            if (player.getUUID().equals(dog.getOwnerUUID())) {
                return dog;
            }
        }
        return null;
    }

    private DeadpoolHud() {
    }
}
