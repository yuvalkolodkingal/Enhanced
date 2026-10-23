package com.kolodkingal.enhanced.platform.neoforge;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.attach.Attach;
import com.kolodkingal.enhanced.platform.attach.AttachKey;
import com.kolodkingal.enhanced.platform.attach.AttachService;
import com.kolodkingal.enhanced.platform.attach.SyncTo;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Attachments on NeoForge: each {@link Attach.Spec} becomes an {@link AttachmentType} with the same id, built with
 * the same builder calls the mod made before the port, in one {@code DeferredRegister} handed to the mod bus by
 * {@link NeoSetup}.
 */
public final class NeoAttach implements AttachService {
    final DeferredRegister<AttachmentType<?>> register =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Enhanced.MODID);

    @Override
    public <T> AttachKey.Handle<T> register(Attach.Spec<T> spec) {
        return new Handle<>(register.register(spec.id().getPath(), () -> type(spec)));
    }

    @Override
    public AttachKey.Handle<CompoundTag> persistentData(Attach.Spec<CompoundTag> spec) {
        Supplier<AttachmentType<CompoundTag>> type = register.register(spec.id().getPath(), () ->
                AttachmentType.builder(spec.defaultValue())
                        .serialize(spec.codec(), spec.shouldSerialize())
                        .copyHandler(NeoPersistentData::copyOnClone)
                        .build());
        return new NeoPersistentData(new Handle<>(type));
    }

    private static <T> AttachmentType<T> type(Attach.Spec<T> spec) {
        AttachmentType.Builder<T> builder = AttachmentType.builder(spec.defaultValue());
        if (spec.codec() != null) {
            if (spec.shouldSerialize() == null) {
                builder.serialize(spec.codec());
            } else {
                builder.serialize(spec.codec(), spec.shouldSerialize());
            }
        }
        if (spec.copyOnDeath()) {
            builder.copyOnDeath();
        }
        if (spec.syncCodec() != null) {
            if (spec.syncTo() == SyncTo.SELF) {
                builder.sync((holder, to) -> holder == to, spec.syncCodec());
            } else {
                builder.sync(spec.syncCodec());
            }
        }
        return builder.build();
    }

    /** A key's NeoForge attachment type. */
    static final class Handle<T> implements AttachKey.Handle<T> {
        private final Supplier<AttachmentType<T>> type;

        Handle(Supplier<AttachmentType<T>> type) {
            this.type = type;
        }

        /** The attachment type (registered). */
        AttachmentType<T> type() {
            return type.get();
        }

        private static IAttachmentHolder holder(Object holder) {
            return (IAttachmentHolder) holder;
        }

        @Override
        public T get(Object holder) {
            return holder(holder).getData(type.get());
        }

        @Override
        public @Nullable T set(Object holder, T value) {
            return holder(holder).setData(type.get(), value);
        }

        @Override
        public boolean has(Object holder) {
            return holder(holder).hasData(type.get());
        }

        @Override
        public @Nullable T remove(Object holder) {
            return holder(holder).removeData(type.get());
        }

        @Override
        public Optional<T> getExisting(Object holder) {
            return holder(holder).getExistingData(type.get());
        }

        @Override
        public void sync(Object holder) {
            holder(holder).syncData(type.get());
        }
    }

    /** The NeoForge attachment type behind a key (for the loader's own glue and its GameTest hooks). */
    public static AttachmentType<?> typeOf(AttachKey<?> key) {
        AttachKey.Handle<?> handle = key.handle();
        if (handle instanceof NeoPersistentData data) {
            handle = data.attachment();
        }
        return ((Handle<?>) handle).type();
    }
}
