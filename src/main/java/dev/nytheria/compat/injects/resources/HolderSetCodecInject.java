// TRACKED HASH: 400a9b89f8ca95162963fb4fdbbc9877f5ae0add
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.resources;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.ICustomHolderSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

@Mixin(HolderSetCodec.class)
public abstract class HolderSetCodecInject<E> {
    @Shadow @Final private Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;
    @Unique private Codec<ICustomHolderSet<E>> forgeDispatchCodec;
    @Unique private Codec<Either<ICustomHolderSet<E>, Either<TagKey<E>, List<Holder<E>>>>> combinedCodec;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$initHolderSetCodecs(ResourceKey<? extends Registry<E>> registryKey, Codec<Holder<E>> elementCodec, boolean disallowInline, CallbackInfo ci) {
        this.forgeDispatchCodec = NeoForgeRegistries.HOLDER_SET_TYPES.byNameCodec()
            .dispatch(ICustomHolderSet::type, type -> type.makeCodec(registryKey, elementCodec, disallowInline));

        this.combinedCodec = Codec.either(this.forgeDispatchCodec, this.registryAwareCodec);
    }

    @ModifyReceiver(method = "decode", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;decode(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
    private <T> Codec nytheriacompat$useCombinedCodec(Codec<E> instance, DynamicOps<T> dynamicOps, T o) {
        return Codec.either(this.forgeDispatchCodec, instance);
    }

    @WrapOperation(method = "method_40386", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Either;map(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private static <T, E> E nytheriacompat$mapEitherCalls(Either<ICustomHolderSet<T>, Either<TagKey<T>, List<Holder<T>>>> instance, Function<TagKey<T>, HolderSet.Named<T>> function, Function<List<Holder<T>>, HolderSet.Direct<T>> function2, Operation<E> original) {
        return (E) instance.map(DataResult::success, tagOrList -> tagOrList.map(function, function2));
    }
}