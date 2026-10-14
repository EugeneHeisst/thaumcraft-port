package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.*;

/**
 * BlockEssentiaReservoir / TileEssentiaReservoir: 256 essentia of any mix, one connectable face (placed against the
 * clicked block, turned by a wand: opposite of the clicked side, or the side itself while sneaking), pulls one essentia
 * every 5 ticks with suction 24, comparator output, and spills flux goo/gas with a small blast when broken full.
 */
public final class EssentiaReservoir {
    public static final DeferredHolder<Block,ReservoirBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("essentia_reservoir"));
    public static final DeferredHolder<Item,Item> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("essentia_reservoir"));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ReservoirEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("essentia_reservoir"));
    @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD) static final class Registration {
        @SubscribeEvent static void register(RegisterEvent e){
            e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new ReservoirBlock(Block.Properties.of().strength(2,17).sound(SoundType.METAL).noOcclusion())));
            /* BlockEssentiaReservoirItem.placeBlockAt: the new reservoir faces the block it was placed against */
            e.register(Registries.ITEM,h->h.register(ITEM.getId(),new BlockItem(BLOCK.get(),new Item.Properties()){@Override public net.minecraft.world.InteractionResult place(BlockPlaceContext c){var r=super.place(c);if(r.consumesAction()&&!c.getLevel().isClientSide&&c.getLevel().getBlockEntity(c.getClickedPos()) instanceof ReservoirEntity t){t.facing=c.getClickedFace().getOpposite();t.sync();}return r;}}));
            e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(ReservoirEntity::new,BLOCK.get()).build(null)));
        }
    }
    public static final class ReservoirBlock extends BaseEntityBlock {
        static final MapCodec<ReservoirBlock> CODEC=simpleCodec(ReservoirBlock::new);
        ReservoirBlock(Properties p){super(p);}
        @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ReservoirEntity(p,s);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==ENTITY.get()?(level,pos,state,be)->((ReservoirEntity)be).tick():null;}
        @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
            if(!WandData.isWand(stack))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if(!l.isClientSide&&l.getBlockEntity(p) instanceof ReservoirEntity r){r.facing=player.isShiftKeyDown()?hit.getDirection():hit.getDirection().getOpposite();r.sync();}
            player.swing(hand);return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){if(l.getBlockEntity(p) instanceof ReservoirEntity r){float f=r.essentia.visSize()/(float)ReservoirEntity.MAX;return net.minecraft.util.Mth.floor(f*14)+(r.essentia.visSize()>0?1:0);}return 0;}
        @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){
            if(!s.is(n.getBlock())&&l instanceof ServerLevel server&&l.getBlockEntity(p) instanceof ReservoirEntity r){int sz=r.essentia.visSize()/16;
                if(sz>0){server.explode(null,p.getX()+.5,p.getY()+.5,p.getZ()+.5,1,Level.ExplosionInteraction.NONE);int q=0;var rnd=l.random;
                    for(int a=0;a<50;a++){var at=p.offset(rnd.nextInt(5)-rnd.nextInt(5),rnd.nextInt(5)-rnd.nextInt(5),rnd.nextInt(5)-rnd.nextInt(5));if(!l.isEmptyBlock(at))continue;
                        var flux=(at.getY()<p.getY()?TC4Modern.FLUX_GOO:TC4Modern.FLUX_GAS).get().defaultBlockState();if(flux.hasProperty(FluxBlock.AMOUNT))flux=flux.setValue(FluxBlock.AMOUNT,8);l.setBlock(at,flux,3);if(q++>=sz)break;}}}
            super.onRemove(s,l,p,n,moving);
        }
    }
    public static final class ReservoirEntity extends BlockEntity implements IEssentiaTransport {
        static final int MAX=256;public AspectList essentia=new AspectList();public Direction facing=Direction.DOWN;int count;
        public float cr=1,cg=1,cb=1;float tri,tgi,tbi;public Aspect displayAspect;
        ReservoirEntity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}
        void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
        @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);essentia.writeToNBT(t);t.putByte("face",(byte)facing.get3DDataValue());}
        @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);essentia=new AspectList();essentia.readFromNBT(t);if(essentia.visSize()>MAX)essentia=new AspectList();facing=Direction.from3DDataValue(t.getByte("face"));}
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        void tick(){
            count++;
            if(!level.isClientSide){if(count%5==0&&essentia.visSize()<MAX)fill();return;}
            int vs=essentia.visSize();if(vs<=0){displayAspect=null;return;}
            if(level.random.nextInt(Math.max(1,500-vs))==0)level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,InfusionContent.MatrixEntity.sound("creak"),net.minecraft.sounds.SoundSource.BLOCKS,1,1.4f+level.random.nextFloat()*.2f,false);
            /* colour fades over 20 ticks to each stored aspect in turn */
            if(count%20==0&&essentia.size()>0){displayAspect=essentia.getAspects()[count/20%essentia.size()];int c=displayAspect.getColor();float tr=(c>>16&255)/255f,tg=(c>>8&255)/255f,tb=(c&255)/255f;tri=(cr-tr)/20;tgi=(cg-tg)/20;tbi=(cb-tb)/20;}
            if(displayAspect!=null){cr-=tri;cg-=tgi;cb-=tbi;}
        }
        void fill(){var ic=EssentiaTransport.connectable(level,worldPosition,facing);if(ic==null||!ic.canOutputTo(facing.getOpposite()))return;Aspect ta=null;
            if(ic.getEssentiaAmount(facing.getOpposite())>0&&ic.getSuctionAmount(facing.getOpposite())<getSuctionAmount(facing)&&getSuctionAmount(facing)>=ic.getMinimumSuction())ta=ic.getEssentiaType(facing.getOpposite());
            if(ta!=null&&ic.getSuctionAmount(facing.getOpposite())<getSuctionAmount(facing))addToContainer(ta,ic.takeEssentia(ta,1,facing.getOpposite()));}
        public AspectList getAspects(){return essentia;}
        public void setAspects(AspectList a){essentia=a.copy();}
        public boolean doesContainerAccept(Aspect a){return true;}
        public int addToContainer(Aspect a,int am){if(am==0)return am;int space=MAX-essentia.visSize();if(space>=am){essentia.add(a,am);am=0;}else{essentia.add(a,space);am-=space;}if(space>0)sync();return am;}
        public boolean takeFromContainer(Aspect a,int am){if(essentia.getAmount(a)>=am){essentia.remove(a,am);sync();return true;}return false;}
        public boolean takeFromContainer(AspectList ot){return false;}
        public boolean doesContainerContainAmount(Aspect a,int amt){return essentia.getAmount(a)>=amt;}
        public boolean doesContainerContain(AspectList ot){for(var a:ot.getAspects())if(essentia.getAmount(a)<ot.getAmount(a))return false;return true;}
        public int containerContains(Aspect a){return essentia.getAmount(a);}
        public boolean isConnectable(Direction f){return f==facing;}
        public boolean canInputFrom(Direction f){return f==facing;}
        public boolean canOutputTo(Direction f){return f==facing;}
        public void setSuction(Aspect a,int amount){}
        public boolean renderExtendedTube(){return false;}
        public int getMinimumSuction(){return 24;}
        public Aspect getSuctionType(Direction f){return null;}
        public int getSuctionAmount(Direction f){return essentia.visSize()<MAX?24:0;}
        /* the original only answers for ForgeDirection.UNKNOWN (null here) */
        public Aspect getEssentiaType(Direction f){return essentia.visSize()>0&&f==null?essentia.getAspects()[0]:null;}
        public int getEssentiaAmount(Direction f){return essentia.visSize();}
        public int takeEssentia(Aspect a,int amount,Direction f){return canOutputTo(f)&&takeFromContainer(a,amount)?amount:0;}
        public int addEssentia(Aspect a,int amount,Direction f){return canInputFrom(f)?amount-addToContainer(a,amount):0;}
    }
}
