/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.world.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.nytheria.compat.helpers.mixin.CreateInitializer;
import dev.nytheria.compat.helpers.mixin.CreateStatic;
import dev.nytheria.compat.injections.world.item.CreativeModeTabInjection;
import dev.nytheria.compat.mixin.CreativeModeTabAccessor;
import dev.nytheria.compat.mixin.world.item.CreativeModeTabBuilderAccessor;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabInject implements CreativeModeTabInjection {
    @Shadow
    public abstract boolean canScroll();

    @Unique private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    @Unique private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    @Unique private ResourceLocation scrollerSpriteLocation;
    @Unique private boolean hasSearchBar;
    @Unique private int searchBarWidth;
    @Unique private ResourceLocation tabImage;
    @Unique private int labelColor = Integer.MIN_VALUE;
    @Unique private int slotColor = Integer.MIN_VALUE;
    @Unique private List<ResourceLocation> tabsBefore = new ArrayList<>();
    @Unique private List<ResourceLocation> tabsAfter = new ArrayList<>();

    CreativeModeTabInject(CreativeModeTab.Row row, int column, CreativeModeTab.Type type, Component displayName, Supplier<ItemStack> iconGenerator, CreativeModeTab.DisplayItemsGenerator displayItemGenerator) {
    }

    @CreateInitializer
    protected CreativeModeTabInject(CreativeModeTab.Builder builder) {
        this(((CreativeModeTabBuilderAccessor) builder).getRow(), ((CreativeModeTabBuilderAccessor) builder).getColumn(), ((CreativeModeTabBuilderAccessor) builder).getType(), ((CreativeModeTabBuilderAccessor) builder).getDisplayName(), ((CreativeModeTabBuilderAccessor) builder).getIconGenerator(), ((CreativeModeTabBuilderAccessor) builder).getDisplayItemsGenerator());

        nytheriacompat$assignValues(builder);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nytheriacompat$initCustomFields(CreativeModeTab.Row row, int column, CreativeModeTab.Type type, Component displayName, Supplier iconGenerator, CreativeModeTab.DisplayItemsGenerator displayItemGenerator, CallbackInfo ci) {
        this.tabsBefore = new ArrayList<>();
        this.tabsAfter = new ArrayList<>();
    }

    @Override
    public void nytheriacompat$assignValues(CreativeModeTab.Builder builder) {
        var b = (CreativeModeTabInjection.BuilderInjection) builder;
        this.scrollerSpriteLocation = b.nytheriacompat$scrollerSpriteLocation();
        this.hasSearchBar = b.nytheriacompat$hasSearchBar();
        this.searchBarWidth = b.nytheriacompat$searchBarWidth();
        this.tabImage = b.nytheriacompat$getTabsImage();
        this.labelColor = b.nytheriacompat$labelColor();
        this.slotColor = b.nytheriacompat$slotColor();
        this.tabsBefore = b.nytheriacompat$getTabsBefore();
        this.tabsAfter = b.nytheriacompat$getTabsAfter();
    }

    @CreateStatic
    private static CreativeModeTab.Builder builder() {
        return new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0);
    }

    @WrapOperation(method = "buildContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab$DisplayItemsGenerator;accept(Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayParameters;Lnet/minecraft/world/item/CreativeModeTab$Output;)V"))
    private void nytheriacompat$buildContentsWithNeoForge(CreativeModeTab.DisplayItemsGenerator instance, CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output, Operation<Void> original, @Local ResourceKey<CreativeModeTab> resourceKey) {
        EventHooks.onCreativeModeTabBuildContents((CreativeModeTab) (Object) this, resourceKey, (params, out) -> original.call(instance, params, out), parameters, output);
    }

    @Override
    public void nytheriacompat$setScrollerSprite(ResourceLocation location) {
        this.scrollerSpriteLocation = location;
    }

    @Override
    public ResourceLocation getScrollerSprite() {
        if (this.scrollerSpriteLocation == null)
            return this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;

        return scrollerSpriteLocation;
    }

    @Override
    public boolean hasSearchBar() {
        return hasSearchBar;
    }

    @Override
    public int getSearchBarWidth() {
        return searchBarWidth;
    }

    @Override
    public ResourceLocation getTabsImage() {
        return tabImage;
    }

    @Override
    public int getLabelColor() {
        return labelColor;
    }

    @Override
    public int getSlotColor() {
        return slotColor;
    }

    @Override
    public List<ResourceLocation> nytheriacompat$getTabsBefore() {
        return this.tabsBefore;
    }

    @Override
    public List<ResourceLocation> nytheriacompat$getTabsAfter() {
        return this.tabsAfter;
    }

    @Mixin(CreativeModeTab.Builder.class)
    public static abstract class BuilderInject implements CreativeModeTabInjection.BuilderInjection {
        @Shadow
        private ResourceLocation backgroundTexture;

        @Shadow
        public abstract CreativeModeTab.Builder backgroundTexture(ResourceLocation resourceLocation);

        @Shadow
        public abstract CreativeModeTab.Builder displayItems(CreativeModeTab.DisplayItemsGenerator displayItemsGenerator);

        @Unique private static final ResourceLocation CREATIVE_INVENTORY_TABS_IMAGE = ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tabs.png");
        @Unique private static final ResourceLocation CREATIVE_ITEM_SEARCH_BACKGROUND = CreativeModeTab.createTextureLocation("item_search");

        @Unique @Nullable private ResourceLocation spriteScrollerLocation;
        @Unique private boolean hasSearchBar = false;
        @Unique private int searchBarWidth = 89;
        @Unique private ResourceLocation tabsImage = CREATIVE_INVENTORY_TABS_IMAGE;
        @Unique private int labelColor = 4210752;
        @Unique private int slotColor = -2130706433;
        @Unique private Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory = CreativeModeTabInjection::create;
        @Unique private final List<ResourceLocation> tabsBefore = new ArrayList<>();
        @Unique private final List<ResourceLocation> tabsAfter = new ArrayList<>();

        @Unique
        private CreativeModeTab.Builder self() {
            return (CreativeModeTab.Builder) (Object) this;
        }

        @Override
        public CreativeModeTab.Builder withScrollBarSpriteLocation(ResourceLocation background) {
            this.spriteScrollerLocation = background;
            return self();
        }

        @Override
        public CreativeModeTab.Builder withSearchBar() {
            this.hasSearchBar = true;
            if (this.backgroundTexture == CreativeModeTabAccessor.getDefaultBackground())
                return this.backgroundTexture(CREATIVE_ITEM_SEARCH_BACKGROUND);

            return self();
        }

        @Override
        public CreativeModeTab.Builder withSearchBar(int searchBarWidth) {
            this.searchBarWidth = searchBarWidth;
            return withSearchBar();
        }

        @Override
        public CreativeModeTab.Builder withTabsImage(ResourceLocation tabsImage) {
            this.tabsImage = tabsImage;
            return self();
        }

        @Override
        public CreativeModeTab.Builder withLabelColor(int labelColor) {
            this.labelColor = labelColor;
            return self();
        }

        @Override
        public CreativeModeTab.Builder withSlotColor(int slotColor) {
            this.slotColor = slotColor;
            return self();
        }

        @Override
        public CreativeModeTab.Builder withTabFactory(Function<CreativeModeTab.Builder, CreativeModeTab> factory) {
            this.tabFactory = factory;
            return self();
        }

        @Override
        public CreativeModeTab.Builder withTabsBefore(ResourceKey<CreativeModeTab>... tabs) {
            Stream.of(tabs).map(ResourceKey::location).forEach(this.tabsBefore::add);
            return self();
        }

        @Override
        public CreativeModeTab.Builder withTabsAfter(ResourceKey<CreativeModeTab>... tabs) {
            Stream.of(tabs).map(ResourceKey::location).forEach(this.tabsAfter::add);
            return self();
        }

        @Override
        public CreativeModeTab.Builder displayItems(Collection<? extends Holder<? extends ItemLike>> collection) {
            return this.displayItems((p, o) -> collection.stream()
                .map(Holder::value)
                .map(ItemLike::asItem)
                .filter(i -> i != Items.AIR)
                .filter(i -> i.isEnabled(p.enabledFeatures()))
                .forEach(o::accept)
            );
        }

        @Override
        public CreativeModeTab.Builder withTabsBefore(ResourceLocation... tabs) {
            this.tabsBefore.addAll(List.of(tabs));
            return self();
        }

        @Override
        public CreativeModeTab.Builder withTabsAfter(ResourceLocation... tabs) {
            this.tabsAfter.addAll(List.of(tabs));
            return self();
        }

        @Override
        public boolean nytheriacompat$hasSearchBar() {
            return hasSearchBar;
        }

        @Override
        public int nytheriacompat$searchBarWidth() {
            return searchBarWidth;
        }

        @Override
        public ResourceLocation nytheriacompat$getTabsImage() {
            return tabsImage;
        }

        @Override
        public int nytheriacompat$labelColor() {
            return labelColor;
        }

        @Override
        public int nytheriacompat$slotColor() {
            return slotColor;
        }

        @Override
        public Function<CreativeModeTab.Builder, CreativeModeTab> nytheriacompat$getTabFactory() {
            return tabFactory;
        }

        @Override
        public List<ResourceLocation> nytheriacompat$getTabsBefore() {
            return tabsBefore;
        }

        @Override
        public List<ResourceLocation> nytheriacompat$getTabsAfter() {
            return tabsAfter;
        }

        @Override
        public ResourceLocation nytheriacompat$scrollerSpriteLocation() {
            return this.spriteScrollerLocation;
        }
    }
}
