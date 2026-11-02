// TRACKED HASH: 297d980725ef2ec87d9bfaf04e6eab59c792fccd
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item.crafting;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntList;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.item.crafting.IngredientInjection;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

@Implements(@Interface(iface = IngredientInjection.class, prefix = "nytheriacompat$i$"))
@Mixin(Ingredient.class)
public abstract class IngredientInject implements IngredientInjection {
    // Nytheria Compat: StreamCodec handling in CustomIngredientPacketCodecMixin

    @Shadow public abstract ItemStack[] getItems();
    @Shadow @Final private Ingredient.Value[] values;
    @Shadow @Nullable private ItemStack[] itemStacks;
    @Shadow @Nullable private IntList stackingIds;

    @Shadow @Final @Mutable public static Codec<Ingredient> CODEC;
    @Shadow @Final @Mutable public static Codec<Ingredient> CODEC_NONEMPTY;
    @Shadow @Final @Mutable public static StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC;

    @CreateStatic
    private static final MapCodec<Ingredient> MAP_CODEC_NONEMPTY = IngredientInjection.MAP_CODEC_NONEMPTY;

    @CreateStatic
    private static final Codec<List<Ingredient>> LIST_CODEC = IngredientInjection.LIST_CODEC;

    @CreateStatic
    private static final Codec<List<Ingredient>> LIST_CODEC_NONEMPTY = IngredientInjection.LIST_CODEC_NONEMPTY;

    public IngredientInject(Ingredient.Value[] values) {}

    @Unique @Nullable
    private ICustomIngredient customIngredient;

    @CreateInitializer
    public IngredientInject(ICustomIngredient customIngredient) {
        this(new Ingredient.Value[0]);
        this.customIngredient = customIngredient;
    }

    // Nytheria Compat: an ingredient keeps the items it resolved on the first call for good. Before the first data pack load has
    //  bound the tags, a tag has no items yet, so an ingredient asked that early (other mods do it while the recipes are
    //  being loaded) kept "no items" for the rest of the game run. For a plain tag ingredient the mod that asked usually
    //  cleans up itself, but not for the ingredients inside a NeoForge custom ingredient such as neoforge:difference,
    //  which it cannot see: the trapped chest, chest minecart and chest boat recipes then had an empty chest slot and
    //  matched nothing until the next reload. So an ingredient that came out empty before the tags were bound is
    //  looked up again once they are. This only runs where the items are computed, never on a cached call.
    @Inject(method = "getItems", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/crafting/Ingredient;itemStacks:[Lnet/minecraft/world/item/ItemStack;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void nytheriacompat$rememberItemsResolvedBeforeTags(CallbackInfoReturnable<ItemStack[]> cir) {
        if (this.itemStacks.length == 0 && (this.values.length != 0 || this.customIngredient != null)) {
            IngredientInjection.EMPTY_BEFORE_TAGS.resolvedEmpty((Ingredient) (Object) this);
        }
    }

    @Override
    public void nytheriacompat$forgetItems() {
        this.itemStacks = null;
        this.stackingIds = null;
    }

    @ModifyExpressionValue(method = "getItems", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;flatMap(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    private <R> Stream<R> nytheriacompat$useNeoCustomIngredientItems(Stream<R> original) {
        if (this.customIngredient != null) {
            return (Stream<R>) this.customIngredient.getItems();
        }

        return original;
    }

    @ModifyReceiver(method = "getItems", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;"))
    private <A extends ItemStack> Stream<A> nytheriacompat$properlyDistinctifyStacks(Stream<A> instance, IntFunction<A[]> intFunction) {
        return (Stream<A>) instance.collect(Collectors.toCollection(ItemStackLinkedSet::createTypeAndComponentsSet)).stream();
    }

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void nytheriacompat$testNeoCustomIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && this.customIngredient != null)
            cir.setReturnValue(this.customIngredient.test(stack));
    }

    @ModifyReturnValue(method = "isEmpty", at = @At("RETURN"))
    private boolean nytheriacompat$checkIsCustom(boolean original) {
        return original && !isCustom();
    }

    @Override
    public boolean hasNoItems() {
        ItemStack[] items = this.getItems();
        if (items.length == 0)
            return true;

        if (items.length == 1) {
            ItemStack item = items[0];
            // Nytheria Compat: what? why is it actually a barrier in the list???
            return item.getItem() == Items.BARRIER
                && item.getHoverName() instanceof MutableComponent hoverName
                && hoverName.getString().startsWith("Empty Tag: ");
        }

        return false;
    }

    @WrapOperation(method = "equals", at = @At(value = "INVOKE", target = "Ljava/util/Arrays;equals([Ljava/lang/Object;[Ljava/lang/Object;)Z"))
    private boolean nytheriacompat$checkIsCustomIngredientEqual(Object[] a, Object[] a2, Operation<Boolean> original, @Local Ingredient ingredient) {
        return Objects.equals(this.customIngredient, ingredient.neoforge$getCustomIngredient()) && original.call(a, a2);
    }

    // TODO: make this more mod-compatible
    @Override
    public int hashCode() {
        if (this.customIngredient != null)
            return this.customIngredient.hashCode();

        return Arrays.hashCode(this.values);
    }

    // FIXME: this probably breaks with an accessor, watch over this!
    @Override
    public Ingredient.Value[] getValues() {
        if (this.isCustom()) {
            throw new IllegalStateException("Cannot retrieve values from custom ingredient!");
        }

        return this.values;
    }

    @Override
    public boolean isSimple() {
        return this.customIngredient == null || this.customIngredient.isSimple();
    }

    @Override
    public ICustomIngredient neoforge$getCustomIngredient() {
        if (NytheriaCompatHelper.INSTANCE.hasMethodOverrideWithReturnType(this.getClass(), Ingredient.class, "getCustomIngredient", ICustomIngredient.class)) {
            return this.getCustomIngredient();
        }

        return this.customIngredient;
    }

    // Nytheria Compat: this becomes Ingredient#getCustomIngredient. It returns the field, like NeoForge's own method. It used to return neoforge$getCustomIngredient(), which
    //  calls getCustomIngredient() again when a subclass overrides it, so an override that calls super.getCustomIngredient() recursed forever. neoforge$getCustomIngredient()
    //  is what dispatches to a real override; nothing in Nytheria Compat calls this method itself (a call on a subclass reaches its override directly).
    public ICustomIngredient nytheriacompat$i$getCustomIngredient() {
        return this.customIngredient;
    }

    @Override
    public void nytheriacompat$setCustomIngredient(ICustomIngredient customIngredient) {
        this.customIngredient = customIngredient;
    }

    @Override
    public boolean isCustom() {
        return this.customIngredient != null;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void nytheriacompat$initNeoIngredientCodecs(CallbackInfo ci) {
        CODEC = Codec.withAlternative(CODEC, CraftingHelper.nytheriacompat$makeIngredientCodec(CODEC.listOf()));
        CODEC_NONEMPTY = Codec.withAlternative(CODEC_NONEMPTY, CraftingHelper.nytheriacompat$makeIngredientCodec(CODEC_NONEMPTY.listOf()));

        StreamCodec<RegistryFriendlyByteBuf, Ingredient> original = CONTENTS_STREAM_CODEC;
        StreamCodec<RegistryFriendlyByteBuf, ICustomIngredient> customIngredientCodec = ByteBufCodecs.registry(NeoForgeRegistries.Keys.INGREDIENT_TYPES)
            .dispatch(ICustomIngredient::getType, IngredientType::streamCodec);

        CONTENTS_STREAM_CODEC = StreamCodec.of((buf, value) -> {
            if (value.isSimple()) {
                original.encode(buf, value);
            } else {
                buf.writeVarInt(-1);
                customIngredientCodec.encode(buf, value.neoforge$getCustomIngredient());
            }
        }, (buf) -> {
            buf.markReaderIndex();
            var size = buf.readVarInt();
            if (size == -1) {
                try {
                    return IngredientInjection.create(customIngredientCodec.decode(buf));
                } catch (Throwable ignored) {
                }
            }

            buf.resetReaderIndex();
            return original.decode(buf);
        });
    }

    @Mixin(Ingredient.ItemValue.class)
    public abstract static class ItemValueInject {
        @Shadow
        public abstract ItemStack item();

        @Override
        public int hashCode() {
            return 31 * this.item().getItem().hashCode() + this.item().getCount();
        }
    }

    @Mixin(Ingredient.TagValue.class)
    public abstract static class TagValueInject {

    }

    @Mixin(Ingredient.Value.class)
    public interface ValueInject {

    }
}
