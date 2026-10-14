package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class ArcaneWorkbenchBlock extends BaseEntityBlock {
    public static final MapCodec<ArcaneWorkbenchBlock> CODEC=simpleCodec(ArcaneWorkbenchBlock::new);
    public ArcaneWorkbenchBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ArcaneWorkbenchEntity(pos,state);}
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state){return net.minecraft.world.level.block.RenderShape.MODEL;}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneWorkbenchEntity table)net.minecraft.world.Containers.dropContents(level,pos,table.inventory);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(player.isShiftKeyDown())return net.minecraft.world.InteractionResult.PASS;
        if(player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof ArcaneWorkbenchEntity table){
            ThaumometerItem.sync(server,"");
            server.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,user)->new ArcaneWorkbenchMenu(id,inventory,net.minecraft.world.inventory.ContainerLevelAccess.create(level,pos),table.inventory),net.minecraft.network.chat.Component.translatable("block.tc4modern.arcane_workbench")));
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}
