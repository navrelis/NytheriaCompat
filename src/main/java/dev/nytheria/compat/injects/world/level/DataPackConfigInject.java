// TRACKED HASH: cb5056cedb17924cc92cd94ea36fa5c1b11500e2
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level;

import com.google.common.collect.Lists;
import net.minecraft.world.level.DataPackConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.level.DataPackConfigInjection;

import java.util.List;

@Mixin(DataPackConfig.class)
public class DataPackConfigInject implements DataPackConfigInjection {
    @Final @Shadow @Mutable
    private List<String> enabled;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void nytheriacompat$makeEnabledPacksMutable(List<String> list, List<String> list2, CallbackInfo ci) {
        this.enabled = Lists.newArrayList(list);
    }

    @Override
    public void addModPacks(List<String> modPacks) {
        this.enabled.addAll(modPacks.stream().filter(p -> !this.enabled.contains(p)).toList());
    }
}