package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * BlockWoodenDevice metadata 8 with TileBanner, all sixteen colours and colour -1 for the Crimson Cult. Standing banners face one of 16
 * directions; banners placed on a wall hang flat against it. No collision, selection box two blocks tall.
 */
public final class CrimsonBanner extends BaseEntityBlock {
    public static final MapCodec<CrimsonBanner> CODEC = simpleCodec(CrimsonBanner::new);
    private static final VoxelShape STANDING = Block.box(5.28, 0, 5.28, 10.56, 32, 10.56);

    public static net.minecraft.world.item.ItemStack colored(int color){var stack=new net.minecraft.world.item.ItemStack(CrimsonContent.BANNER.get());var tag=new CompoundTag();tag.putByte("color",(byte)color);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));return stack;}
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder builder){var stack=new net.minecraft.world.item.ItemStack(this);if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof Entity banner&&(banner.color>=0||banner.aspect!=null)){var tag=new CompoundTag();tag.putByte("color",banner.color);if(banner.aspect!=null)tag.putString("aspect",banner.aspect.getTag());stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));}return java.util.List.of(stack);}
    private net.minecraft.world.InteractionResult interact(net.minecraft.world.item.ItemStack held,Level level,BlockPos pos,net.minecraft.world.entity.player.Player p){if(!(level.getBlockEntity(pos) instanceof Entity banner)||banner.color<0)return net.minecraft.world.InteractionResult.SUCCESS;if(p.isShiftKeyDown()||held.is(LiquidDeath.CRYSTAL.get())){if(!level.isClientSide){if(p.isShiftKeyDown())banner.aspect=null;else {var aspects=EssentiaItems.getAspects(held);if(aspects!=null&&aspects.size()>0){banner.aspect=aspects.getAspects()[0];held.shrink(1);}}banner.setFacing(banner.facing);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.WOOL_STEP,net.minecraft.sounds.SoundSource.BLOCKS,1,1);}return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);}return net.minecraft.world.InteractionResult.SUCCESS;}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,BlockState s,Level l,BlockPos pos,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){interact(held,l,pos,p);return net.minecraft.world.ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,net.minecraft.world.entity.player.Player p,net.minecraft.world.phys.BlockHitResult hit){return interact(net.minecraft.world.item.ItemStack.EMPTY,l,pos,p);}
    public CrimsonBanner(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Entity(pos, state); }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof Entity banner) || !banner.wall) return STANDING;
        return switch (banner.facing) {
            case 0 -> Block.box(0, 0, 0, 16, 32, 4);
            case 8 -> Block.box(0, 0, 12, 16, 32, 16);
            case 12 -> Block.box(0, 0, 0, 4, 32, 16);
            case 4 -> Block.box(12, 0, 0, 16, 32, 16);
            default -> STANDING;
        };
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    /** BlockWoodenDeviceItem.placeBlockAt: top/bottom click faces the player's yaw in 16ths, side click hangs on the wall. */
    public static final class Item extends net.minecraft.world.item.BlockItem {
        public Item(Block block, net.minecraft.world.item.Item.Properties properties) { super(block, properties); }
        @Override public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack stack) {
            var data=NoteData.tag(stack);int color=data.contains("color")?data.getByte("color"):-1;
            return color>=0&&color<16?net.minecraft.network.chat.Component.translatable("block.tc4modern.crimson_banner.color."+color):super.getName(stack);
        }
        @Override public net.minecraft.world.InteractionResult place(BlockPlaceContext context) {
            var data=NoteData.tag(context.getItemInHand()).copy();
            var result = super.place(context);
            if (result.consumesAction() && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Entity banner) {
                banner.color=data.contains("color")?data.getByte("color"):-1;
                banner.aspect=thaumcraft.api.aspects.Aspect.getAspect(data.getString("aspect"));
                var face = context.getClickedFace();
                if (face.getAxis().isVertical()) banner.setFacing((byte) (Mth.floor((context.getRotation() + 180) * 16 / 360 + .5) & 15));
                else {
                    banner.wall = true;
                    banner.setFacing((byte) switch (face) { case NORTH -> 8; case WEST -> 4; case EAST -> 12; default -> 0; });
                }
            }
            return result;
        }
    }

    /** TileBanner: facing 0-15, colour -1 (cult), no aspect. */
    public static final class Entity extends BlockEntity {
        public byte facing,color=-1;
        public thaumcraft.api.aspects.Aspect aspect;
        public boolean wall;
        public Entity(BlockPos pos, BlockState state) { super(CrimsonContent.BANNER_ENTITY.get(), pos, state); }
        public void setFacing(byte facing) {
            this.facing = facing;
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putByte("facing", facing);
            tag.putByte("color", color);
            tag.putString("aspect", aspect==null?"":aspect.getTag());
            tag.putBoolean("wall", wall);
        }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            facing = tag.getByte("facing");
            color=tag.contains("color")?tag.getByte("color"):-1;
            aspect=thaumcraft.api.aspects.Aspect.getAspect(tag.getString("aspect"));
            wall = tag.getBoolean("wall");
        }
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
            return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
        }
        public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0, 1, 0); }
    }

}
