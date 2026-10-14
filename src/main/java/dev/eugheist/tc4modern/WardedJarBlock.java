package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** BlockJar metadata 0 (warded jar) with ItemJarFilled behaviour folded into its block item. */
public final class WardedJarBlock extends BaseEntityBlock {
    public static final MapCodec<WardedJarBlock> CODEC = simpleCodec(WardedJarBlock::new);
    static final SoundEvent JAR = SoundEvent.createVariableRangeEvent(AlchemyContent.id("jar"));
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);
    public WardedJarBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    /** User-requested DEVIATION: preserve filled jars when breaking them in creative mode. */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()
                && level.getBlockEntity(pos) instanceof WardedJarEntity jar && jar.state.amount > 0) {
            var stack = new ItemStack(this);
            writeContents(stack, jar);
            Block.popResource(level, pos, stack);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WardedJarEntity(pos, state); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EssentiaDevices.JAR_ENTITY.get(), WardedJarEntity::serverTick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof WardedJarEntity jar)) return;
        // ItemJarFilled.placeBlockAt: contents and label travel with the item.
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            var tag = data.copyTag();
            var aspects = new AspectList(); aspects.readFromNBT(tag);
            if (aspects.size() > 0) { jar.state.aspect = aspects.getAspects()[0]; jar.state.amount = aspects.getAmount(jar.state.aspect); }
            if (tag.contains("AspectFilter")) jar.state.aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
        }
        if (placer != null) jar.state.facing = AlembicBlock.facingFromYaw(placer);
        jar.changed();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WardedJarEntity jar) || !player.isShiftKeyDown()) return InteractionResult.PASS;
        var s = jar.state;
        if (s.aspectFilter != null && hit.getDirection().get3DDataValue() == s.facing) {
            s.aspectFilter = null;
            jar.changed();
            if (level.isClientSide) level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, AlembicBlock.PAGE, SoundSource.BLOCKS, 1f, 1f, false);
            else AlembicBlock.dropLabel(level, pos, hit);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        s.amount = 0;
        if (s.aspectFilter == null) s.aspect = null;
        jar.changed();
        if (level.isClientSide) {
            level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, JAR, SoundSource.BLOCKS, .4f, 1f, false);
            level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .5f, 1f + (level.random.nextFloat() - level.random.nextFloat()) * .3f, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!held.is(EssentiaDevices.LABEL.get()) || !(level.getBlockEntity(pos) instanceof WardedJarEntity jar) || jar.state.aspectFilter != null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var labelAspects = EssentiaItems.getAspects(held);
        if (jar.state.amount == 0 && labelAspects == null) return ItemInteractionResult.SUCCESS;
        if (jar.state.amount == 0) jar.state.aspect = labelAspects.getAspects()[0];
        held.shrink(1);
        jar.state.facing = AlembicBlock.facingFromYaw(player);
        jar.state.aspectFilter = jar.state.aspect;
        jar.changed();
        if (level.isClientSide) level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, JAR, SoundSource.BLOCKS, .4f, 1f, false);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    /** BlockJar.getDrops: plain jar when empty and unlabelled, otherwise contents/label on the item. */
    @Override protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof WardedJarEntity jar) writeContents(stack, jar);
        return java.util.List.of(stack);
    }
    static void writeContents(ItemStack stack, WardedJarEntity jar) {
        if (jar.state.amount <= 0 && jar.state.aspectFilter == null) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (jar.state.amount > 0) new AspectList().add(jar.state.aspect, jar.state.amount).writeToNBT(tag);
            if (jar.state.aspectFilter != null) tag.putString("AspectFilter", jar.state.aspectFilter.getTag());
        });
    }

    /** BlockJarItem/ItemJarFilled: tooltip lists contents for known aspects. */
    public static final class JarItem extends BlockItem {
        public JarItem(Block block, Item.Properties properties) { super(block, properties); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
            EssentiaItems.tooltip(stack, lines);
            var data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().contains("AspectFilter")) {
                var a = Aspect.getAspect(data.copyTag().getString("AspectFilter"));
                if (a != null) lines.add(Component.literal("§5" + a.getName()));
            }
        }
    }
}
