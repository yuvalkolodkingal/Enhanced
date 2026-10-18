package com.kolodkingal.enhanced.custom.modules;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.ModuleLogic;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The custom powers of the mind (3.3): Super Senses, Telepathy, Telekinesis (the shared one), Mind Control, Animal
 * Control, Duplication (Loki's illusions, a fighting kind added), Technopathy and Time Manipulation. The creatures a hero
 * controls are kept in {@link #CONTROLS}: allies fight for him, calmed ones never pick him as a target.
 */
public final class MindPowers {
    public static void register(Map<PowerModule, ModuleLogic> logic) {
        logic.put(PowerModule.SUPER_SENSES, new Senses());
        logic.put(PowerModule.TELEPATHY, new Telepathy());
        logic.put(PowerModule.TELEKINESIS, new Kinesis());
        logic.put(PowerModule.MIND_CONTROL, new MindControl());
        logic.put(PowerModule.ANIMAL_CONTROL, new AnimalControl());
        logic.put(PowerModule.DUPLICATION, new Duplication());
        logic.put(PowerModule.TECHNOPATHY, new Technopathy());
        logic.put(PowerModule.TIME, new Time());
    }

    // ---- creatures under a hero's control ----

    /** How a creature is controlled. */
    public enum Kind {
        /** It fights for him (Mind Control "ally", Technopathy "command"). */
        ALLY,
        /** An animal of his pack: it goes for {@link Control#victim}, or his foes, and follows him. */
        PACK,
        /** It never picks him as a target (Mind Control and Animal Control "calm"). */
        CALM
    }

    /** A creature controlled by a hero until a game time. */
    public record Control(UUID owner, Kind kind, long until, @Nullable UUID victim, int color) {
    }

    private static final Map<UUID, Control> CONTROLS = new HashMap<>();

    /** The control on this creature, or null. */
    @Nullable
    public static Control control(Entity e) {
        return CONTROLS.get(e.getUUID());
    }

    /** Whether {@code mob} may take {@code target} as its target, as far as the custom heroes' controls go. */
    public static boolean mayTarget(Mob mob, LivingEntity target) {
        Control control = CONTROLS.get(mob.getUUID());
        if (control != null) {
            if (target.getUUID().equals(control.owner())) {
                return false; // never the one who controls it
            }
            Control other = CONTROLS.get(target.getUUID());
            if (control.kind() != Kind.CALM && other != null && other.owner().equals(control.owner()) && other.kind() != Kind.CALM) {
                return false; // nor the others on his side
            }
        }
        return true;
    }

    static void control(Mob mob, ServerPlayer owner, Kind kind, int ticks, @Nullable LivingEntity victim, int color) {
        CONTROLS.put(mob.getUUID(), new Control(owner.getUUID(), kind, owner.serverLevel().getGameTime() + ticks,
                victim == null ? null : victim.getUUID(), color));
        if (mob.getTarget() == owner) {
            mob.setTarget(null);
        }
        ServerLevel level = owner.serverLevel();
        level.sendParticles(new EnergyParticleOptions(color, 1.3F), mob.getX(), mob.getEyeY() + 0.4, mob.getZ(), 10, 0.3, 0.2, 0.3, 0.02);
    }

    /** Whether a hero may take this creature over: no player, no boss of a fight. */
    static boolean controllable(@Nullable LivingEntity e) {
        return e instanceof Mob && !(e instanceof LokiIllusionEntity) && !e.getType().is(ModTags.BOSSES) && !e.getType().is(ModTags.BOSS_MINIONS)
                && !BossFights.isFightEntity(e);
    }

    /** Runs the controls of one hero (from his tick, every 5 ticks). */
    static void tickControls(ServerPlayer owner) {
        if (owner.tickCount % 5 != 0) {
            return;
        }
        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Control>> it = CONTROLS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Control> entry = it.next();
            Control control = entry.getValue();
            if (!control.owner().equals(owner.getUUID())) {
                continue;
            }
            Entity e = level.getEntity(entry.getKey());
            if (!(e instanceof Mob mob) || !mob.isAlive() || now > control.until() || mob.distanceToSqr(owner) > 64 * 64) {
                if (e instanceof Mob mob && control.kind() != Kind.CALM) {
                    mob.setTarget(null);
                }
                it.remove();
                continue;
            }
            if (owner.tickCount % 20 == 0) {
                level.sendParticles(new EnergyParticleOptions(control.color(), 0.9F), mob.getX(), mob.getEyeY() + 0.5, mob.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            }
            if (control.kind() == Kind.CALM) {
                if (mob.getTarget() == owner) {
                    mob.setTarget(null);
                }
                continue;
            }
            LivingEntity foe = foe(owner, mob, control, level);
            boolean fights = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null && !(mob instanceof Animal);
            if (foe != null) {
                if (fights) {
                    mob.setTarget(foe);
                } else {
                    // An animal with no attack of its own runs at the foe and butts it.
                    mob.getNavigation().moveTo(foe, 1.4);
                    if (mob.distanceToSqr(foe) < Math.pow(mob.getBbWidth() * 0.5 + foe.getBbWidth() * 0.5 + 1.2, 2) && owner.tickCount % 20 == 0) {
                        Shots.hurt(owner, foe, 2.0F * (float) ModuleLogic.scale(com.kolodkingal.enhanced.config.CustomHeroConfig.DAMAGE_SCALE, 1.0));
                        Shots.push(foe, mob.position(), 0.4, 0.2);
                    }
                }
            } else {
                if (fights && mob.getTarget() != null && !mayTarget(mob, mob.getTarget())) {
                    mob.setTarget(null);
                }
                if (mob.distanceToSqr(owner) > 36) {
                    mob.getNavigation().moveTo(owner, 1.2);
                }
            }
        }
    }

    /** What a controlled creature goes for: its victim, whoever hurt its master or whom he hurt, else the nearest monster. */
    @Nullable
    private static LivingEntity foe(ServerPlayer owner, Mob mob, Control control, ServerLevel level) {
        if (control.victim() != null && level.getEntity(control.victim()) instanceof LivingEntity victim && victim.isAlive()) {
            return victim;
        }
        for (LivingEntity candidate : new LivingEntity[]{owner.getLastHurtByMob(), owner.getLastHurtMob(), mob.getTarget()}) {
            if (candidate != null && candidate.isAlive() && candidate != mob && candidate.distanceToSqr(mob) < 24 * 24
                    && mayTarget(mob, candidate) && AreaTargets.mayHurt(owner, candidate, false)) {
                return candidate;
            }
        }
        LivingEntity best = null;
        double bestDistance = 12 * 12;
        for (Mob other : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(12), m -> m instanceof Enemy && m.isAlive() && m != mob)) {
            if (mob.distanceToSqr(other) < bestDistance && mayTarget(mob, other) && AreaTargets.mayHurt(owner, other, false)) {
                bestDistance = mob.distanceToSqr(other);
                best = other;
            }
        }
        return best;
    }

    /** Lets go of every creature this hero controls. */
    static void releaseAll(UUID owner, @Nullable ServerLevel level) {
        Iterator<Map.Entry<UUID, Control>> it = CONTROLS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Control> entry = it.next();
            if (entry.getValue().owner().equals(owner)) {
                if (level != null && level.getEntity(entry.getKey()) instanceof Mob mob && entry.getValue().kind() != Kind.CALM) {
                    mob.setTarget(null);
                }
                it.remove();
            }
        }
    }

    static int controlled(UUID owner, Kind kind) {
        int n = 0;
        for (Control control : CONTROLS.values()) {
            if (control.owner().equals(owner) && control.kind() == kind) {
                n++;
            }
        }
        return n;
    }

    /** How many creatures a hero controls now (for the tests and the HUD). */
    public static int controlled(UUID owner) {
        int n = 0;
        for (Control control : CONTROLS.values()) {
            if (control.owner().equals(owner)) {
                n++;
            }
        }
        return n;
    }

    // ---- Super Senses: night vision, the sense of danger, or creatures seen through walls (a switch: the client) ----

    static final class Senses implements ModuleLogic {
        @Override
        public void tick(Ctx c) {
            if (c.option("night") && c.player.tickCount % 20 == 0) {
                MobEffectInstance effect = c.player.getEffect(MobEffects.NIGHT_VISION);
                if (effect == null || effect.getDuration() < 260) {
                    c.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
                }
            }
        }

        @Override
        public void key(Ctx c, boolean down) {
            if (down && c.option("xray")) {
                boolean on = !c.state.isOn(PowerModule.SUPER_SENSES);
                c.state.set(PowerModule.SUPER_SENSES, on);
                c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, on ? 1.5F : 0.8F);
                c.say(on ? "message.enhanced.custom.senses.on" : "message.enhanced.custom.senses.off");
            }
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.SUPER_SENSES, false);
            MobEffectInstance effect = c.player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.isAmbient()) {
                c.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    private static final Map<UUID, Long> LAST_WARNING = new HashMap<>();

    /**
     * A creature has just picked a hero with the sense of danger as its target: he is warned where it is (once every three
     * seconds at most).
     */
    public static void danger(ServerPlayer hero, Mob mob) {
        ModuleLogic.Ctx c = com.kolodkingal.enhanced.custom.CustomHeroServer.ctx(hero, PowerModule.SUPER_SENSES);
        if (c == null || !c.option("danger") || mob.distanceTo(hero) > c.v(ModuleStats.DANGER_RANGE)) {
            return;
        }
        long now = hero.serverLevel().getGameTime();
        Long last = LAST_WARNING.get(hero.getUUID());
        if (last != null && now - last < 60 && now >= last) {
            return;
        }
        LAST_WARNING.put(hero.getUUID(), now);
        Vec3 to = mob.position().subtract(hero.position());
        double angle = Math.toDegrees(Math.atan2(-to.x, to.z)) - hero.getYRot();
        angle = ((angle % 360) + 540) % 360 - 180;
        String side = Math.abs(angle) <= 45 ? "front" : Math.abs(angle) >= 135 ? "behind" : angle > 0 ? "right" : "left";
        c.say("message.enhanced.custom.senses.danger", mob.getType().getDescription(),
                Component.translatable("message.enhanced.custom.senses." + side));
        c.sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.8F);
    }

    // ---- Telepathy: minds glow through walls (a switch: the client), or the thoughts of a creature are read ----

    static final class Telepathy implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            if (c.option("sense")) {
                boolean on = !c.state.isOn(PowerModule.TELEPATHY);
                c.state.set(PowerModule.TELEPATHY, on);
                c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, on ? 1.4F : 0.7F);
                c.say(on ? "message.enhanced.custom.telepathy.on" : "message.enhanced.custom.telepathy.off");
                return;
            }
            if (!c.ready()) {
                return;
            }
            Shots.Aim aim = Shots.aim(c.player, c.v(ModuleStats.READ_RANGE));
            LivingEntity target = aim.target();
            if (target == null) {
                c.refuse("message.enhanced.custom.no_target");
                return;
            }
            c.player.displayClientMessage(Component.translatable("message.enhanced.custom.telepathy.thought", target.getDisplayName(),
                    thought(c.player, target)).withStyle(s -> s.withColor(c.color())), true);
            c.level().sendParticles(new EnergyParticleOptions(c.color(), 1.0F), target.getX(), target.getEyeY() + 0.4, target.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            c.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
            c.cooldown(ModuleStats.READ_COOLDOWN);
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.TELEPATHY, false);
        }

        /** What a creature is thinking, as far as a telepath can tell. */
        static Component thought(ServerPlayer reader, LivingEntity target) {
            String key;
            Object arg = "";
            if (target instanceof Player player) {
                Power power = Powers.power(player);
                if (power.hasPowers()) {
                    key = "hero";
                    arg = power == Power.CUSTOM ? CustomHeroes.name(player) : power.displayName();
                } else {
                    key = player.getMainHandItem().isEmpty() ? "player" : "item";
                    arg = player.getMainHandItem().getHoverName();
                }
            } else if (target instanceof Mob mob && mob.getTarget() == reader) {
                key = "you";
            } else if (target instanceof Mob mob && mob.getTarget() != null) {
                key = "hunting";
                arg = mob.getTarget().getDisplayName();
            } else if (target instanceof Enemy) {
                key = "trouble";
            } else if (target instanceof AbstractVillager) {
                key = "trade";
            } else if (target instanceof Animal) {
                key = "food";
            } else {
                key = "calm";
            }
            return Component.translatable("message.enhanced.custom.telepathy." + key, arg);
        }
    }

    // ---- Telekinesis: the shared telekinesis, with his reach and his throw ----

    static final class Kinesis implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (down) {
                Telekinesis.press(c.player, c.option("everything") ? Telekinesis.Mode.CUSTOM_BLOCKS : Telekinesis.Mode.CUSTOM);
            }
        }

        @Override
        public void end(Ctx c) {
            if (Telekinesis.isHolding(c.player)) {
                Telekinesis.release(c.player, false);
            }
        }
    }

    // ---- Mind Control: a creature fights for him ("ally"), or the monsters round him forget him ("calm") ----

    static final class MindControl implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            int ticks = c.ticks(ModuleStats.MIND_TIME);
            if (c.option("calm")) {
                int n = 0;
                for (Mob mob : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(c.v(ModuleStats.MIND_RANGE)),
                        m -> m instanceof Enemy && m.isAlive())) {
                    if (controllable(mob) && Shots.reaches(p, mob)) {
                        control(mob, p, Kind.CALM, ticks, null, c.color());
                        n++;
                    }
                }
                c.say("message.enhanced.custom.mind.calm", n);
            } else {
                Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.MIND_RANGE));
                if (!controllable(aim.target())) {
                    c.refuse(aim.target() == null ? "message.enhanced.custom.no_target" : "message.enhanced.custom.mind.refused");
                    return;
                }
                Mob mob = (Mob) aim.target();
                control(mob, p, Kind.ALLY, ticks, null, c.color());
                Shots.line(c.level(), p.getEyePosition(), mob.getEyePosition(), c.color(), c.glow());
                c.say("message.enhanced.custom.mind.ally", mob.getDisplayName(), ticks / 20);
            }
            c.sound(ModSounds.CUSTOM_SPELL.get(), 1.0F, 1.2F);
            c.cooldown(ModuleStats.MIND_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            tickControls(c.player);
        }

        @Override
        public void end(Ctx c) {
            releaseAll(c.player.getUUID(), c.level());
        }

        @Override
        public void forget(ServerPlayer player) {
            releaseAll(player.getUUID(), player.serverLevel());
        }

        @Override
        public void clear() {
            CONTROLS.clear();
            LAST_WARNING.clear();
        }
    }

    // ---- Animal Control: the animals round him go for what he looks at ("pack"), or are calm ----

    static final class AnimalControl implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            int ticks = c.ticks(ModuleStats.ANIMAL_TIME);
            LivingEntity victim = null;
            if (c.option("pack")) {
                Shots.Aim aim = Shots.aim(p, 24);
                victim = aim.target() != null && AreaTargets.mayHurt(p, aim.target(), true) && !(aim.target() instanceof Animal) ? aim.target() : null;
            }
            int n = 0;
            for (Animal animal : p.serverLevel().getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(c.v(ModuleStats.ANIMAL_RADIUS)),
                    a -> a.isAlive() && Shots.reaches(p, a))) {
                if (!controllable(animal)) {
                    continue;
                }
                control(animal, p, c.option("pack") ? Kind.PACK : Kind.CALM, ticks, victim, c.color());
                n++;
            }
            if (n == 0) {
                c.refuse("message.enhanced.custom.animals.none");
                return;
            }
            c.say(c.option("pack") ? "message.enhanced.custom.animals.pack" : "message.enhanced.custom.animals.calm", n);
            c.sound(SoundEvents.WOLF_HOWL, 0.8F, 1.2F);
            c.cooldown(ModuleStats.ANIMAL_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            if (CustomHeroes.pick(c.player, PowerModule.MIND_CONTROL) == null) {
                tickControls(c.player); // (Mind Control runs them when the hero has it too)
            }
        }

        @Override
        public void end(Ctx c) {
            releaseAll(c.player.getUUID(), c.level());
        }
    }

    // ---- Duplication: copies of him that fight ("clones") or that walk off and draw the monsters ("decoys") ----

    static final class Duplication implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            for (LokiIllusionEntity old : LokiIllusionEntity.ownedBy(level, p.getUUID())) {
                old.dissolve(null);
            }
            boolean clones = c.option("clones");
            int count = Math.round(c.v(clones ? ModuleStats.CLONES : ModuleStats.DECOYS));
            int ticks = c.ticks(clones ? ModuleStats.CLONE_TIME : ModuleStats.DECOY_TIME);
            List<LokiIllusionEntity> copies = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                double angle = p.getYRot() * Math.PI / 180 + Math.PI * 2 * (i + 0.5) / count;
                Vec3 dir = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
                Vec3 at = p.position().add(dir.scale(1.5));
                if (!level.noCollision(p, p.getBoundingBox().move(at.subtract(p.position()))) || Shots.hexCut(level, p.position().add(0, 1, 0), at.add(0, 1, 0)) != null) {
                    at = p.position();
                }
                LokiIllusionEntity copy = LokiIllusionEntity.conjure(p, at, clones ? null : p.position().add(dir.scale(12)),
                        clones ? LokiIllusionEntity.Mode.FIGHTER : LokiIllusionEntity.Mode.DECOY, ticks);
                if (clones && copy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null) {
                    copy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                            .setBaseValue(2.0 + c.strength() * 0.5 * ModuleLogic.scale(com.kolodkingal.enhanced.config.CustomHeroConfig.DAMAGE_SCALE, 1.0));
                }
                copies.add(copy);
                level.sendParticles(new EnergyParticleOptions(c.color(), 1.2F), at.x, at.y + 1.0, at.z, 12, 0.25, 0.6, 0.25, 0.02);
            }
            if (!clones && !copies.isEmpty()) {
                for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), m -> m.getTarget() == p)) {
                    LokiIllusionEntity decoy = copies.get(mob.getRandom().nextInt(copies.size()));
                    mob.setTarget(decoy);
                }
            }
            c.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
            c.say(clones ? "message.enhanced.custom.duplicate.clones" : "message.enhanced.custom.duplicate.decoys", count);
            c.cooldown(clones ? ModuleStats.CLONE_COOLDOWN : ModuleStats.DECOY_COOLDOWN);
        }

        @Override
        public void end(Ctx c) {
            for (LokiIllusionEntity copy : LokiIllusionEntity.ownedBy(c.level(), c.player.getUUID())) {
                copy.dissolve(null);
            }
        }
    }

    // ---- Technopathy: doors, trapdoors, gates and levers obey him ("hack"); golems fight for him ("command") ----

    static final class Technopathy implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            if (c.option("command")) {
                int n = 0;
                for (AbstractGolem golem : level.getEntitiesOfClass(AbstractGolem.class, p.getBoundingBox().inflate(c.v(ModuleStats.COMMAND_RADIUS)),
                        g -> g.isAlive() && controllable(g) && Shots.reaches(p, g))) {
                    control(golem, p, Kind.ALLY, c.ticks(ModuleStats.COMMAND_TIME), null, c.color());
                    n++;
                }
                if (n == 0) {
                    c.refuse("message.enhanced.custom.tech.no_machines");
                    return;
                }
                c.say("message.enhanced.custom.tech.command", n);
                c.sound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.2F);
                c.cooldown(ModuleStats.COMMAND_COOLDOWN);
                return;
            }
            int radius = Math.round(c.v(ModuleStats.HACK_RADIUS));
            int n = 0;
            for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-radius, -radius, -radius), p.blockPosition().offset(radius, radius, radius))) {
                if (n >= 32 || pos.distSqr(p.blockPosition()) > radius * radius) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof FenceGateBlock
                        || state.getBlock() instanceof LeverBlock)) {
                    continue;
                }
                if (BossFights.protects(level, pos) || !level.mayInteract(p, pos) || Shots.hexCut(level, p.getEyePosition(), Vec3.atCenterOf(pos)) != null) {
                    continue;
                }
                if (toggle(level, pos.immutable(), state, p)) {
                    n++;
                    level.sendParticles(new EnergyParticleOptions(c.color(), 0.9F), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.0);
                }
            }
            if (n == 0) {
                c.refuse("message.enhanced.custom.tech.no_machines");
                return;
            }
            c.say("message.enhanced.custom.tech.hack", n);
            c.sound(SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.8F);
            c.cooldown(ModuleStats.HACK_COOLDOWN);
        }

        /** Opens or closes a door, a trapdoor or a gate, or flips a lever, as a hand would. */
        private static boolean toggle(Level level, BlockPos pos, BlockState state, ServerPlayer p) {
            if (state.getBlock() instanceof DoorBlock door) {
                if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER || !door.type().canOpenByHand()) {
                    return false;
                }
                door.setOpen(p, level, state, pos, !state.getValue(BlockStateProperties.OPEN));
                return true;
            }
            if (state.getBlock() instanceof LeverBlock lever) {
                lever.pull(state, level, pos, p);
                return true;
            }
            if (state.hasProperty(BlockStateProperties.OPEN)) {
                boolean open = !state.getValue(BlockStateProperties.OPEN);
                level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 10);
                level.playSound(null, pos, open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                return true;
            }
            return false;
        }

        @Override
        public void tick(Ctx c) {
            if (CustomHeroes.pick(c.player, PowerModule.MIND_CONTROL) == null && CustomHeroes.pick(c.player, PowerModule.ANIMAL_CONTROL) == null) {
                tickControls(c.player);
            }
        }

        @Override
        public void end(Ctx c) {
            releaseAll(c.player.getUUID(), c.level());
        }
    }

    // ---- Time Manipulation: the world slows round him (a switch), a creature stands still, or he goes back in time ----

    private record Moment(ResourceKey<Level> dimension, Vec3 at, float health, long time) {
    }

    private static final Map<UUID, Deque<Moment>> HISTORY = new HashMap<>();

    static final class Time implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("slow")) {
                boolean on = !c.state.isOn(PowerModule.TIME);
                if (on && !c.fed()) {
                    c.refuse("message.enhanced.custom.hungry");
                    return;
                }
                c.state.set(PowerModule.TIME, on);
                c.sound(SoundEvents.BEACON_ACTIVATE, 0.8F, on ? 0.6F : 1.4F);
                c.say(on ? "message.enhanced.custom.time.slow_on" : "message.enhanced.custom.time.slow_off");
                return;
            }
            if (!c.ready()) {
                return;
            }
            if (c.option("stop")) {
                Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.STOP_RANGE));
                LivingEntity target = aim.target();
                if (target == null || !AreaTargets.mayHurt(p, target, true) || target.getType().is(ModTags.BOSSES)) {
                    c.refuse("message.enhanced.custom.no_target");
                    return;
                }
                int ticks = c.ticks(ModuleStats.STOP_TIME);
                target.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, ticks, 0), p);
                target.setDeltaMovement(Vec3.ZERO);
                target.hurtMarked = true;
                Shots.line(c.level(), Shots.hand(p), aim.end(), c.color(), c.glow());
                c.level().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getEyeY(), target.getZ(), 12, 0.4, 0.4, 0.4, 0.01);
                c.say("message.enhanced.custom.time.stopped", target.getDisplayName(), ticks / 20);
                c.sound(SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.6F);
                c.cooldown(ModuleStats.STOP_COOLDOWN);
                return;
            }
            Deque<Moment> history = HISTORY.get(p.getUUID());
            long target = c.now() - c.ticks(ModuleStats.REWIND_TIME);
            Moment back = null;
            if (history != null) {
                for (Moment m : history) {
                    if (m.time() <= target && m.dimension() == p.level().dimension()) {
                        back = m;
                    }
                }
                if (back == null) {
                    back = history.peekFirst();
                }
            }
            if (back == null || back.dimension() != p.level().dimension() || !HexManager.onTeleport(c.level(), p, back.at())) {
                c.refuse("message.enhanced.custom.time.no_past");
                return;
            }
            c.level().sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            p.teleportTo(back.at().x, back.at().y, back.at().z);
            p.resetFallDistance();
            p.clearFire();
            if (back.health() > p.getHealth()) {
                p.setHealth(Math.min(p.getMaxHealth(), back.health()));
            }
            history.clear();
            c.sparks(back.at().add(0, 1, 0), 30, 0.5, 1.3F);
            c.sound(ModSounds.CUSTOM_TELEPORT.get(), 1.0F, 0.6F);
            c.cooldown(ModuleStats.REWIND_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            ServerPlayer p = c.player;
            if (c.option("rewind") && p.tickCount % 5 == 0) {
                Deque<Moment> history = HISTORY.computeIfAbsent(p.getUUID(), u -> new ArrayDeque<>());
                history.addLast(new Moment(p.level().dimension(), p.position(), p.getHealth(), c.now()));
                while (history.size() > 50) {
                    history.removeFirst();
                }
            }
            if (!c.option("slow") || !c.state.isOn(PowerModule.TIME)) {
                return;
            }
            double radius = c.v(ModuleStats.SLOW_RADIUS);
            if (p.tickCount % 10 == 0) {
                for (LivingEntity e : Shots.area(p, p.position(), radius)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, true, false));
                    e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 2, true, false));
                }
                Shots.ring(c.level(), p.position().add(0, 0.2, 0), radius, c.color());
            }
            if (p.tickCount % 20 == 0) {
                c.exhaust(c.v(ModuleStats.SLOW_HUNGER));
                if (!c.fed()) {
                    c.state.set(PowerModule.TIME, false);
                    return;
                }
            }
            for (Projectile shot : c.level().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(radius),
                    e -> e.getOwner() != p && e.getDeltaMovement().lengthSqr() > 0.04)) {
                shot.setDeltaMovement(shot.getDeltaMovement().scale(0.7));
                shot.hurtMarked = true;
            }
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.TIME, false);
            HISTORY.remove(c.player.getUUID());
        }

        @Override
        public void forget(ServerPlayer player) {
            HISTORY.remove(player.getUUID());
        }

        @Override
        public void clear() {
            HISTORY.clear();
        }
    }

    /** Whether this hero has a past to go back to (for the tests). */
    public static int historySize(UUID player) {
        Deque<Moment> history = HISTORY.get(player);
        return history == null ? 0 : history.size();
    }

    private MindPowers() {
    }
}
