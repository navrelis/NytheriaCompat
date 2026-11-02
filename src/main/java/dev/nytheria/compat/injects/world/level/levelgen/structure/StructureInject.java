// TRACKED HASH: f818e25808bed5f0e6c33a46353235bc2d7f4533
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.level.levelgen.structure;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.injections.world.level.levelgen.structure.StructureInjection;

import net.minecraft.world.level.levelgen.structure.Structure;

@Mixin(Structure.class)
public abstract class StructureInject implements StructureInjection {
    private ModifiableStructureInfo modifiableStructureInfo;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void nytheriacompat$loadModifiedStructureSettings(Structure.StructureSettings structureSettings, CallbackInfo ci) {
        modifiableStructureInfo = new ModifiableStructureInfo(new ModifiableStructureInfo.StructureInfo(structureSettings));
    }

    @ModifyExpressionValue(method = {"biomes", "spawnOverrides", "step", "terrainAdaptation"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/Structure;settings:Lnet/minecraft/world/level/levelgen/structure/Structure$StructureSettings;", opcode = Opcodes.GETFIELD))
    private Structure.StructureSettings nytheriacompat$readModifiedStructureSettings(Structure.StructureSettings original) {
        return getModifiedStructureSettings();
    }

    @Override
    public Structure.StructureSettings getModifiedStructureSettings() {
        return modifiableStructureInfo().get().structureSettings();
    }

    @Override
    public ModifiableStructureInfo modifiableStructureInfo() {
        return modifiableStructureInfo;
    }
}
