// TRACKED HASH: 2bffb0fe9cfd4490897033dbf7f02f15990726ba
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.commands.synchronization;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.spongepowered.asm.mixin.Mixin;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.commands.synchronization.ArgumentTypeInfosInjection;

@Mixin(ArgumentTypeInfos.class)
public class ArgumentTypeInfosInject implements ArgumentTypeInfosInjection {
    @CreateStatic
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerByClass(Class<A> infoClass, I argumentTypeInfo) {
        return ArgumentTypeInfosInjection.registerByClass(infoClass, argumentTypeInfo);
    }
}