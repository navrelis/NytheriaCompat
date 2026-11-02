/*
 * Changed by the Nytheria Compat project on 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injections.world.item;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import dev.nytheria.compat.mixin.CreativeModeTabAccessor;
import dev.nytheria.compat.mixin.world.item.CreativeModeTabBuilderAccessor;
import dev.nytheria.compat.util.NytheriaCompatHelper;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

public interface CreativeModeTabInjection {
    static CreativeModeTab create(CreativeModeTab.Builder builder) {
        var tab = CreativeModeTabAccessor.createCreativeModeTab(((CreativeModeTabBuilderAccessor) builder).getRow(), ((CreativeModeTabBuilderAccessor) builder).getColumn(), ((CreativeModeTabBuilderAccessor) builder).getType(), ((CreativeModeTabBuilderAccessor) builder).getDisplayName(), ((CreativeModeTabBuilderAccessor) builder).getIconGenerator(), ((CreativeModeTabBuilderAccessor) builder).getDisplayItemsGenerator());
        tab.nytheriacompat$assignValues(builder);

        return tab;
    }

    static CreativeModeTab.Builder builder() {
        return new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0);
    }

    // Helper method for both create() here and <init> in the mixin
    default void nytheriacompat$assignValues(CreativeModeTab.Builder builder) {}
    default void nytheriacompat$setScrollerSprite(ResourceLocation location) {}

    default ResourceLocation getScrollerSprite() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "getScrollerSprite");
    }

    default boolean hasSearchBar() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "hasSearchBar");
    }

    default int getSearchBarWidth() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "getSearchBarWidth");
    }

    default ResourceLocation getTabsImage() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "getTabsImage");
    }

    default int getLabelColor() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "getLabelColor");
    }

    default int getSlotColor() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "");
    }

    default List<ResourceLocation> nytheriacompat$getTabsBefore() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "nytheriacompat$getTabsBefore");
    }

    default List<ResourceLocation> nytheriacompat$getTabsAfter() {
        throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.class, "nytheriacompat$getTabsAfter");
    }

    interface BuilderInjection {
        default CreativeModeTab.Builder withScrollBarSpriteLocation(ResourceLocation location) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withScrollBarSpriteLocation");
        }

        default CreativeModeTab.Builder withSearchBar() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withSearchBar");
        }

        default CreativeModeTab.Builder withSearchBar(int searchBarWidth) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withSearchBar");
        }

        default CreativeModeTab.Builder withTabsImage(ResourceLocation tabsImage) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withTabsImage");
        }

        default CreativeModeTab.Builder withLabelColor(int labelColor) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withLabelColor");
        }

        default CreativeModeTab.Builder withSlotColor(int slotColor) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withSlotColor");
        }

        default CreativeModeTab.Builder withTabFactory(Function<CreativeModeTab.Builder, CreativeModeTab> factory) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withTabFactory");
        }

        default CreativeModeTab.Builder withTabsBefore(ResourceKey<CreativeModeTab>... tabs) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withTabsBefore");
        }

        default CreativeModeTab.Builder withTabsAfter(ResourceKey<CreativeModeTab>... tabs) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withTabsAfter");
        }

        default CreativeModeTab.Builder withTabsBefore(ResourceLocation... tabs) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withTabsBefore");
        }

        default CreativeModeTab.Builder withTabsAfter(ResourceLocation... tabs) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "withTabsAfter");
        }

        default CreativeModeTab.Builder displayItems(Collection<? extends Holder<? extends ItemLike>> collection) {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "displayItems");
        }

        default boolean nytheriacompat$hasSearchBar() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$hasSearchBar");
        }

        default int nytheriacompat$searchBarWidth() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$searchBarWidth");
        }

        default ResourceLocation nytheriacompat$getTabsImage() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$getTabsImage");
        }

        default int nytheriacompat$labelColor() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$labelColor");
        }

        default int nytheriacompat$slotColor() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$slotColor");
        }

        default Function<CreativeModeTab.Builder, CreativeModeTab> nytheriacompat$getTabFactory() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$getTabFactory");
        }

        default List<ResourceLocation> nytheriacompat$getTabsBefore() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$getTabsBefore");
        }

        default List<ResourceLocation> nytheriacompat$getTabsAfter() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$getTabsAfter");
        }

        default ResourceLocation nytheriacompat$scrollerSpriteLocation() {
            throw NytheriaCompatHelper.createMixinException(CreativeModeTabInjection.BuilderInjection.class, "nytheriacompat$scrollerSpriteLocation");
        }
    }
}
