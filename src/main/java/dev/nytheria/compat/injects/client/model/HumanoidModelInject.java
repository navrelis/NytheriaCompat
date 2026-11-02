// TRACKED HASH: decab18fc678570fcc5f2c2b899561ef7d4dcd7a
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.client.model.HumanoidModelArmPoseInjection;

@Mixin(HumanoidModel.class)
public class HumanoidModelInject {
    // TODO: oh boy

    @Mixin(HumanoidModel.ArmPose.class)
    public static class ArmPoseInject implements HumanoidModelArmPoseInjection, IExtensibleEnum {

        @ReservedConstructor
        private ArmPoseInject(String name, int ordinal, final boolean twoHanded) {}

        @CreateInitializer
        private ArmPoseInject(String name, int ordinal, final boolean twoHanded, IArmPoseTransformer forgeArmPose) {
            this(name, ordinal, twoHanded);
            this.forgeArmPose = forgeArmPose;
        }

        private IArmPoseTransformer forgeArmPose;

        @CreateStatic
        private static ExtensionInfo getExtensionInfo() {
            return ExtensionInfo.nonExtended(HumanoidModel.ArmPose.class);
        }
    }
}