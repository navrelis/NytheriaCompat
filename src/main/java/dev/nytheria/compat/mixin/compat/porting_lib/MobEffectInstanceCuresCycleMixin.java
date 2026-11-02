package dev.nytheria.compat.mixin.compat.porting_lib;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nytheria Compat: guard against a Porting Lib bug that made saving a player overflow the stack ("Saving entity NBT",
 * endless {@code MobEffectInstance.getCures} recursion).
 * <p>
 * Since Porting Lib 3.1.0-beta.91 (bundled by Twilight Forest, and it wins over Nytheria Compat's own copy),
 * {@code porting_lib_entity}'s {@code MobEffectInstanceMixin} keeps the effect cures in a lazy memoized supplier, and
 * its {@code setDetailsFrom} TAIL handler replaces it with {@code Suppliers.memoize(() -> new HashSet<>(other.getCures()))},
 * a link to the other <i>instance</i>, not to its set. Vanilla's hidden effects build a cycle out of that:
 * {@code update()} creates {@code hiddenEffect = new MobEffectInstance(this)} (hidden links to this), and when the
 * stronger effect expires {@code tick()} calls {@code this.setDetailsFrom(this.hiddenEffect)} (this links to hidden).
 * If nothing read the cures in between, the next {@code asDetails()} (every save) recurses forever.
 * <p>
 * Resolving the source's cures before Porting Lib builds the link means every link points at an instance whose value
 * is already fixed, so no cycle can form, whatever the mixin order or Porting Lib version (older versions copy eagerly
 * and this call changes nothing). Porting Lib made the cures lazy to avoid touching unbound effect holders too early,
 * so an unbound holder is left alone.
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceCuresCycleMixin {
    @Inject(method = "setDetailsFrom", at = @At("HEAD"))
    private void nytheriacompat$resolvePortingLibCures(MobEffectInstance source, CallbackInfo ci) {
        if (source != (Object) this && source.getEffect().isBound()) {
            source.getCures();
        }
    }
}
