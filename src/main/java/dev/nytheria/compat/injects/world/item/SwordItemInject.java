/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;

@Mixin(SwordItem.class)
public abstract class SwordItemInject extends TieredItem {
    @Shadow
    public static ItemAttributeModifiers createAttributes(Tier tier, int attackDamage, float attackSpeed) {
        throw new AssertionError();
    }

    public SwordItemInject(Tier tier, Properties properties) {
        super(tier, properties);
    }

    /**
     * Neo: Allow modded Swords to set exactly what Tool data component to use for their sword.
     */
    @CreateInitializer
    public SwordItemInject(Tier tier, Item.Properties properties, Tool toolComponentData) {
        super(tier, properties.component(DataComponents.TOOL, toolComponentData));
    }

    @Unique private static float nytheriacompat$damage = Float.MIN_VALUE;

    @CreateStatic
    private static ItemAttributeModifiers createAttributes(Tier tier, float damage, float speed) {
        nytheriacompat$damage = damage;
        var modifiers = createAttributes(tier, (int) damage, speed);
        nytheriacompat$damage = Float.MIN_VALUE;

        return modifiers;
    }

    @Definition(id = "AttributeModifier", type = AttributeModifier.class)
    @Definition(id = "BASE_ATTACK_DAMAGE_ID", field = "Lnet/minecraft/world/item/SwordItem;BASE_ATTACK_DAMAGE_ID:Lnet/minecraft/resources/ResourceLocation;")
    @Expression("new AttributeModifier(BASE_ATTACK_DAMAGE_ID, @(?), ?)")
    @ModifyExpressionValue(method = "createAttributes", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static double nytheriacompat$useFloatAttackDamage(double original, @Local(argsOnly = true) Tier tier) {
        if (nytheriacompat$damage != Float.MIN_VALUE) {
            return nytheriacompat$damage + tier.getAttackDamageBonus();
        }

        return original;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility toolAction) {
        return ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(toolAction);
    }
}
