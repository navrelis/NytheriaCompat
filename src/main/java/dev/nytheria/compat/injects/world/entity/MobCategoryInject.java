// TRACKED HASH: 0770f54a29d8a50372a2a26c9e0c7652bc5fa502
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.entity;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

@NamedEnum
@Mixin(MobCategory.class)
public abstract class MobCategoryInject implements IExtensibleEnum {
    @Shadow @Final @Mutable
    public static Codec<MobCategory> CODEC;

    @Shadow public abstract String getName();

    @CreateStatic
    private static ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.nonExtended(MobCategory.class);
    }
}