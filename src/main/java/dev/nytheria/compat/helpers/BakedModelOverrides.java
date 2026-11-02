package dev.nytheria.compat.helpers;

import java.util.HashSet;
import java.util.Set;

import dev.nytheria.compat.util.NytheriaCompatHelper;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Nytheria Compat: "does this model need NeoForge's answer?" for the block render type and ambient occlusion hooks (vanilla
 * section compiler, BlockRenderDispatcher#renderSingleBlock and Sodium's FabricModelAccess/FabricBlockAccess).
 * <p>
 * A plain {@link NytheriaCompatHelper.MethodOverrideCheck} against {@link IBakedModelExtension} is true for practically every
 * vanilla block model: {@code SimpleBakedModelInject} merges {@code getRenderTypes} into {@link SimpleBakedModel}, and
 * {@code MultiPartBakedModelInject}, {@code WeightedBakedModelInject} and {@code ForwardingBakedModelMixin} merge
 * delegating {@code getRenderTypes} / {@code useAmbientOcclusion} into {@link MultiPartBakedModel},
 * {@link WeightedBakedModel} and Fabric API's {@link ForwardingBakedModel}. So these classes are answered here instead:
 * <ul>
 *     <li>a subclass that overrides the NeoForge method itself counts (against the vanilla class that holds the merged
 *     method),</li>
 *     <li>{@link SimpleBakedModel} counts only when it was baked with a non-empty RenderTypeGroup ({@code render_type}
 *     in its model json, or a NeoForge geometry that set one),</li>
 *     <li>the delegating ones count when a model they delegate to counts (for MultiPart and Weighted cached per model,
 *     their children never change after baking).</li>
 * </ul>
 * Any other class counts when it overrides the NeoForge method (a NeoForge model, BakedModelWrapper, ...). Every
 * check is a ClassValue lookup plus at most a field read, nothing is allocated per call.
 * <p>
 * When a model doesn't count, NeoForge's own answer is the vanilla one: {@code IBakedModelExtension#getRenderTypes}
 * returns {@code ItemBlockRenderTypes.getRenderLayers(state)}, which is {@code ItemBlockRenderTypes.getChunkRenderType(state)}
 * as a set, unless a NeoForge mod gave the block several layers with {@code ItemBlockRenderTypes.setRenderLayer}
 * (see {@link #hasMultipleRenderLayers}); and {@code IBakedModelExtension#useAmbientOcclusion} returns DEFAULT/FALSE from
 * {@code useAmbientOcclusion()}.
 */
public final class BakedModelOverrides {
    /** Implemented by {@code SimpleBakedModelInject}. */
    public interface RenderTypeGroupHolder {
        /** True when the model was baked with a non-empty RenderTypeGroup. */
        boolean nytheriacompat$hasBlockRenderTypes();
    }

    /** Implemented by {@code MultiPartBakedModelInject} and {@code WeightedBakedModelInject}. */
    public interface Composite {
        /** True when any child model has NeoForge block render types ({@link #hasNeoBlockRenderTypes}); cached per model. */
        boolean nytheriacompat$hasNeoBlockRenderTypes();

        /** The model whose {@code useAmbientOcclusion} the composite returns, in vanilla and in Nytheria Compat's NeoForge override. */
        BakedModel nytheriacompat$getAmbientOcclusionDelegate();
    }

    /** Implemented by {@code ForwardingBakedModelMixin}. */
    public interface Forwarding {
        BakedModel nytheriacompat$getWrapped();
    }

    private static final Class<?>[] GET_RENDER_TYPES_ARGS = { BlockState.class, RandomSource.class, ModelData.class };
    private static final Class<?>[] USE_AMBIENT_OCCLUSION_ARGS = { BlockState.class, ModelData.class, RenderType.class };

    private static final NytheriaCompatHelper.MethodOverrideCheck GET_RENDER_TYPES = NytheriaCompatHelper.MethodOverrideCheck.of(IBakedModelExtension.class, "getRenderTypes", GET_RENDER_TYPES_ARGS);
    private static final NytheriaCompatHelper.MethodOverrideCheck SIMPLE_GET_RENDER_TYPES = NytheriaCompatHelper.MethodOverrideCheck.of(SimpleBakedModel.class, "getRenderTypes", GET_RENDER_TYPES_ARGS);
    private static final NytheriaCompatHelper.MethodOverrideCheck MULTI_PART_GET_RENDER_TYPES = NytheriaCompatHelper.MethodOverrideCheck.of(MultiPartBakedModel.class, "getRenderTypes", GET_RENDER_TYPES_ARGS);
    private static final NytheriaCompatHelper.MethodOverrideCheck WEIGHTED_GET_RENDER_TYPES = NytheriaCompatHelper.MethodOverrideCheck.of(WeightedBakedModel.class, "getRenderTypes", GET_RENDER_TYPES_ARGS);
    private static final NytheriaCompatHelper.MethodOverrideCheck FORWARDING_GET_RENDER_TYPES = NytheriaCompatHelper.MethodOverrideCheck.of(ForwardingBakedModel.class, "getRenderTypes", GET_RENDER_TYPES_ARGS);

    private static final NytheriaCompatHelper.MethodOverrideCheck USE_AMBIENT_OCCLUSION = NytheriaCompatHelper.MethodOverrideCheck.of(IBakedModelExtension.class, "useAmbientOcclusion", USE_AMBIENT_OCCLUSION_ARGS);
    private static final NytheriaCompatHelper.MethodOverrideCheck MULTI_PART_USE_AMBIENT_OCCLUSION = NytheriaCompatHelper.MethodOverrideCheck.of(MultiPartBakedModel.class, "useAmbientOcclusion", USE_AMBIENT_OCCLUSION_ARGS);
    private static final NytheriaCompatHelper.MethodOverrideCheck WEIGHTED_USE_AMBIENT_OCCLUSION = NytheriaCompatHelper.MethodOverrideCheck.of(WeightedBakedModel.class, "useAmbientOcclusion", USE_AMBIENT_OCCLUSION_ARGS);
    private static final NytheriaCompatHelper.MethodOverrideCheck FORWARDING_USE_AMBIENT_OCCLUSION = NytheriaCompatHelper.MethodOverrideCheck.of(ForwardingBakedModel.class, "useAmbientOcclusion", USE_AMBIENT_OCCLUSION_ARGS);

    /**
     * Blocks a NeoForge mod gave more than one render layer through {@code ItemBlockRenderTypes.setRenderLayer}. Only
     * written while the client loads, so it is replaced as a whole (copy on write) and read without a lock.
     */
    private static volatile Set<Block> multipleRenderLayerBlocks = Set.of();

    private BakedModelOverrides() {}

    /**
     * True when the block render types of this model in this state have to come from NeoForge's
     * {@code getRenderTypes}; false when they are the single vanilla chunk layer {@code ItemBlockRenderTypes.getChunkRenderType(state)}.
     */
    public static boolean usesNeoBlockRenderTypes(BakedModel model, BlockState state) {
        return hasNeoBlockRenderTypes(model) || hasMultipleRenderLayers(state.getBlock());
    }

    /**
     * True when the quads and model data of this model in this state have to come from NeoForge's overloads with a ModelData and
     * a RenderType. For the models vanilla bakes (and their Nytheria Compat merges) that is only so when NeoForge block render types are
     * involved, because their NeoForge overloads answer what the plain ones do for one layer. Any other class that overrides them
     * decides for itself.
     */
    public static boolean usesNeoQuads(BakedModel model, BlockState state) {
        Class<?> modelClass = model.getClass();

        if (modelClass == SimpleBakedModel.class || modelClass == MultiPartBakedModel.class || modelClass == WeightedBakedModel.class)
            return usesNeoBlockRenderTypes(model, state);

        return true;
    }

    /** True when the model (or a model it delegates to) has its own NeoForge block render types. */
    public static boolean hasNeoBlockRenderTypes(BakedModel model) {
        Class<?> modelClass = model.getClass();

        if (model instanceof SimpleBakedModel) {
            return SIMPLE_GET_RENDER_TYPES.test(modelClass) || ((RenderTypeGroupHolder) model).nytheriacompat$hasBlockRenderTypes();
        }

        if (model instanceof MultiPartBakedModel) {
            return MULTI_PART_GET_RENDER_TYPES.test(modelClass) || ((Composite) model).nytheriacompat$hasNeoBlockRenderTypes();
        }

        if (model instanceof WeightedBakedModel) {
            return WEIGHTED_GET_RENDER_TYPES.test(modelClass) || ((Composite) model).nytheriacompat$hasNeoBlockRenderTypes();
        }

        if (model instanceof ForwardingBakedModel) {
            if (FORWARDING_GET_RENDER_TYPES.test(modelClass))
                return true;

            BakedModel wrapped = ((Forwarding) model).nytheriacompat$getWrapped();
            return wrapped != null && wrapped != model && hasNeoBlockRenderTypes(wrapped);
        }

        return GET_RENDER_TYPES.test(modelClass);
    }

    /**
     * True when {@code useAmbientOcclusion(BlockState, ModelData, RenderType)} of this model (or of the model it
     * delegates to) is a real NeoForge override, i.e. can answer something else than {@code useAmbientOcclusion()}.
     */
    public static boolean overridesAmbientOcclusion(BakedModel model) {
        Class<?> modelClass = model.getClass();

        if (model instanceof MultiPartBakedModel) {
            return MULTI_PART_USE_AMBIENT_OCCLUSION.test(modelClass) || overridesAmbientOcclusionOfDelegate(model, ((Composite) model).nytheriacompat$getAmbientOcclusionDelegate());
        }

        if (model instanceof WeightedBakedModel) {
            return WEIGHTED_USE_AMBIENT_OCCLUSION.test(modelClass) || overridesAmbientOcclusionOfDelegate(model, ((Composite) model).nytheriacompat$getAmbientOcclusionDelegate());
        }

        if (model instanceof ForwardingBakedModel) {
            return FORWARDING_USE_AMBIENT_OCCLUSION.test(modelClass) || overridesAmbientOcclusionOfDelegate(model, ((Forwarding) model).nytheriacompat$getWrapped());
        }

        return USE_AMBIENT_OCCLUSION.test(modelClass);
    }

    private static boolean overridesAmbientOcclusionOfDelegate(BakedModel model, BakedModel delegate) {
        return delegate != null && delegate != model && overridesAmbientOcclusion(delegate);
    }

    /** True when a NeoForge mod gave this block more than one render layer. */
    public static boolean hasMultipleRenderLayers(Block block) {
        Set<Block> blocks = multipleRenderLayerBlocks;
        return !blocks.isEmpty() && blocks.contains(block);
    }

    /** Called by {@code ItemBlockRenderTypesInjection.setRenderLayer(Block, ChunkRenderTypeSet)}. */
    public static synchronized void onRenderLayersSet(Block block, ChunkRenderTypeSet layers) {
        boolean multiple = layers.nytheriacompat$size() > 1;

        if (multiple == multipleRenderLayerBlocks.contains(block))
            return;

        Set<Block> blocks = new HashSet<>(multipleRenderLayerBlocks);

        if (multiple)
            blocks.add(block);
        else
            blocks.remove(block);

        multipleRenderLayerBlocks = Set.copyOf(blocks);
    }
}
