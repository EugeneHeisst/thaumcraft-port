package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.*;

/**
 * Advanced alchemical furnace (ADVALCHEMYFURNACE): WandManager.createAdvancedAlchemicalFurnace turns an alchemical furnace
 * ringed by 8 advanced alchemical constructs, under 4 alembics (corners) and 4 alchemical constructs (edges), into
 * BlockAlchemyFurnace parts 0 core, 1 nozzle (bottom edges), 4 bottom corners, 3 top edges, 2 top corners.
 * The core draws Ignis (heat), Perditio and Aqua from the vis network and melts items dropped on it into up to 500 essentia,
 * which the four nozzles hand to essentia tubes.
 */
public final class AdvancedFurnace {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,4);
    public static final DeferredHolder<Block,FurnaceBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("advanced_alchemical_furnace"));
    public static final DeferredHolder<Block,Block> CONSTRUCT=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("advanced_alchemical_construct"));
    public static final DeferredHolder<Item,Item> CONSTRUCT_ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("advanced_alchemical_construct"));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CoreEntity>> CORE=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("advanced_alchemical_furnace"));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<NozzleEntity>> NOZZLE=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("advanced_alchemical_furnace_nozzle"));
    @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD) static final class Registration {
        @SubscribeEvent static void register(RegisterEvent e){
            e.register(Registries.BLOCK,h->{h.register(BLOCK.getId(),new FurnaceBlock(Block.Properties.of().strength(3,17).sound(SoundType.METAL).noOcclusion().lightLevel(s->0)));
                h.register(CONSTRUCT.getId(),new Block(Block.Properties.of().strength(3,TC4SoundTypes.res(17)).sound(SoundType.METAL).requiresCorrectToolForDrops()));});
            e.register(Registries.ITEM,h->h.register(CONSTRUCT_ITEM.getId(),new BlockItem(CONSTRUCT.get(),new Item.Properties())));
            e.register(Registries.BLOCK_ENTITY_TYPE,h->{h.register(CORE.getId(),BlockEntityType.Builder.of(CoreEntity::new,BLOCK.get()).build(null));h.register(NOZZLE.getId(),BlockEntityType.Builder.of(NozzleEntity::new,BLOCK.get()).build(null));});
        }
    }
    static Block alembic(){return EssentiaDevices.ALEMBIC.get();}
    /** blueprint[layer][a][c]: 1 alembic, 2 alchemical construct, 4 advanced construct, 3 alchemical furnace (layer 0 = furnace level). */
    static final int[][][] BLUEPRINT={{{4,4,4},{4,3,4},{4,4,4}},{{1,2,1},{2,0,2},{1,2,1}}};
    static boolean matches(Level l,BlockPos p,int kind){var s=l.getBlockState(p);return switch(kind){case 1->s.is(alembic());case 2->s.is(AlchemyMachines.CONSTRUCT.get());case 4->s.is(CONSTRUCT.get());case 3->s.is(EssentiaDevices.FURNACE.get());default->true;};}
    /** createAdvancedAlchemicalFurnace: the wand may hit any construct; the furnace must lie in the surrounding 3x3x3. */
    public static boolean create(net.minecraft.world.item.context.UseOnContext c,ItemStack wand){
        var l=c.getLevel();var player=c.getPlayer();var hit=c.getClickedPos();var clicked=l.getBlockState(hit);
        if(player==null||!(clicked.is(CONSTRUCT.get())||clicked.is(AlchemyMachines.CONSTRUCT.get()))||!ResearchProgress.completed(player.getPersistentData()).contains("ADVALCHEMYFURNACE"))return false;
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)for(int cc=-1;cc<=1;cc++){var core=hit.offset(a,b,cc);if(!l.getBlockState(core).is(EssentiaDevices.FURNACE.get()))continue;
            boolean ok=true;for(int aa=-1;aa<=1&&ok;aa++)for(int bb=0;bb<=1&&ok;bb++)for(int k=-1;k<=1&&ok;k++)ok=matches(l,core.offset(aa,bb,k),BLUEPRINT[bb][aa+1][k+1]);
            if(!ok)continue;
            var cost=new AspectList().add(Aspect.FIRE,50).add(Aspect.WATER,50).add(Aspect.ORDER,50);
            if(!WandData.consumeCrafting(wand,cost,false,player))return false;
            if(l.isClientSide)return true;
            if(!WandData.consumeCrafting(wand,cost,true,player))return false;
            for(int aa=-1;aa<=1;aa++)for(int bb=0;bb<=1;bb++)for(int k=-1;k<=1;k++){if(bb==1&&aa==0&&k==0)continue;boolean edge=(aa==0)!=(k==0);int part=bb==0?(aa==0&&k==0?0:edge?1:4):(edge?3:2);
                var at=core.offset(aa,bb,k);if(l.getBlockEntity(at) instanceof AlchemicalFurnaceEntity f)f.clearContent();l.removeBlockEntity(at);l.setBlock(at,BLOCK.get().defaultBlockState().setValue(PART,part),2);}
            for(int aa=-1;aa<=1;aa++)for(int bb=0;bb<=1;bb++)for(int k=-1;k<=1;k++)l.updateNeighborsAt(core.offset(aa,bb,k),BLOCK.get());
            l.playSound(null,core,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("wand")),SoundSource.BLOCKS,1,1);
            if(l instanceof ServerLevel server)for(int aa=-1;aa<=1;aa++)for(int bb=0;bb<=1;bb++)for(int k=-1;k<=1;k++){var at=core.offset(aa,bb,k);server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,at.getX()+.5,at.getY()+.5,at.getZ()+.5,6,.3,.3,.3,.05);}
            return true;
        }
        return false;
    }
    /** getItemDropped/damageDropped: 0 alchemical furnace, 1/4 advanced construct, 3 alchemical construct, 2 alembic. */
    static BlockState original(int part){return switch(part){case 0->EssentiaDevices.FURNACE.get().defaultBlockState();case 1,4->CONSTRUCT.get().defaultBlockState();case 3->AlchemyMachines.CONSTRUCT.get().defaultBlockState();default->alembic().defaultBlockState();};}
    public static final class FurnaceBlock extends BaseEntityBlock {
        static final java.util.Set<Level> RESTORING=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        static final MapCodec<FurnaceBlock> CODEC=simpleCodec(FurnaceBlock::new);static final VoxelShape LOW=Block.box(0,0,0,16,11.2,16);
        FurnaceBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(PART,0));}
        @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PART);}
        @Override protected RenderShape getRenderShape(BlockState s){return s.getValue(PART)==0?RenderShape.ENTITYBLOCK_ANIMATED:RenderShape.INVISIBLE;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(PART)==0?new CoreEntity(p,s):s.getValue(PART)==1?new NozzleEntity(p,s):null;}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==CORE.get()?(level,pos,state,be)->((CoreEntity)be).tick():t==NOZZLE.get()?(level,pos,state,be)->((NozzleEntity)be).tick():null;}
        /* the core lets dropped items sink to 0.7 so they touch it; living things stand on a full block */
        @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(PART)==0&&c instanceof EntityCollisionContext ec&&ec.getEntity()!=null&&!(ec.getEntity() instanceof LivingEntity)?LOW:Shapes.block();}
        @Override public int getLightEmission(BlockState s,BlockGetter l,BlockPos p){return s.getValue(PART)==0&&l.getBlockEntity(p) instanceof CoreEntity c&&c.heat>100?(int)(c.heat/(float)CoreEntity.MAX_POWER*12):0;}
        @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e){if(l.isClientSide||s.getValue(PART)!=0||!(e instanceof ItemEntity item)||!(l.getBlockEntity(p) instanceof CoreEntity core))return;
            if(core.process(item.getItem())){var stack=item.getItem().copy();stack.shrink(1);l.playSound(null,e.getX(),e.getY(),e.getZ(),InfusionContent.MatrixEntity.sound("bubble"),SoundSource.BLOCKS,.2f,1+l.random.nextFloat()*.4f);if(stack.isEmpty())e.discard();else item.setItem(stack);}}
        @Override protected boolean hasAnalogOutputSignal(BlockState s){return true;}
        /* original precedence quirk: (floor(r*14) + vis) > 0 ? 1 : 0 */
        @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){if(l.getBlockEntity(p) instanceof NozzleEntity n&&n.furnace()!=null){var f=n.furnace();float r=f.vis/(float)CoreEntity.MAX_VIS;return net.minecraft.util.Mth.floor(r*14)+f.vis>0?1:0;}return 0;}
        @Override protected List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return List.of(new ItemStack(original(s.getValue(PART)).getBlock()));}
        @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return new ItemStack(original(s.getValue(PART)).getBlock());}
        /** breakBlock: any broken part restores every remaining part (and the core) to its original block. */
        @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){
            super.onRemove(s,l,p,n,moving);if(l.isClientSide||n.is(this)||RESTORING.contains(l))return;
            BlockPos core=null;if(s.getValue(PART)==0)core=p;else for(var at:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1))){var st=l.getBlockState(at);if(st.is(this)&&st.getValue(PART)==0){core=at.immutable();break;}}
            if(core==null)return;
            RESTORING.add(l);try {
            for(int a=-1;a<=1;a++)for(int b=0;b<=1;b++)for(int c=-1;c<=1;c++){var at=core.offset(a,b,c);if(at.equals(p))continue;var st=l.getBlockState(at);if(st.is(this))l.setBlock(at,original(st.getValue(PART)),3);}
            if(!core.equals(p)&&l.getBlockState(core).is(this))l.setBlock(core,original(0),3);
            }finally{RESTORING.remove(l);}
        }
        @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){if(s.getValue(PART)==0&&l.getBlockEntity(p) instanceof CoreEntity c&&c.vis>0)AdvancedFurnaceClient.bubbles(l,p,r);}
    }
    public static final class CoreEntity extends BlockEntity {
        static final int MAX_VIS=500,MAX_POWER=500;public AspectList aspects=new AspectList();public int vis,heat,power1,power2,processed;int count;
        CoreEntity(BlockPos p,BlockState s){super(CORE.get(),p,s);}
        void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
        @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);aspects.writeToNBT(t);t.putShort("power1",(short)power1);t.putShort("power2",(short)power2);t.putShort("vis",(short)vis);t.putShort("heat",(short)heat);}
        @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);aspects=new AspectList();aspects.readFromNBT(t);power1=t.getShort("power1");power2=t.getShort("power2");vis=t.getShort("vis");int old=heat;heat=t.getShort("heat");if(level!=null&&level.isClientSide&&old/50!=heat/50)level.getLightEngine().checkBlock(worldPosition);}
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        void tick(){count++;if(!(level instanceof ServerLevel server))return;if(processed>0)processed--;
            if(count%5==0){int pt=heat--;
                if(heat<=MAX_POWER)heat+=VisNetwork.drain(server,worldPosition,Aspect.FIRE,50,(n,c)->{});
                if(power1<=MAX_POWER)power1+=VisNetwork.drain(server,worldPosition,Aspect.ENTROPY,50,(n,c)->{});
                if(power2<=MAX_POWER)power2+=VisNetwork.drain(server,worldPosition,Aspect.WATER,50,(n,c)->{});
                if(pt/50!=heat/50){sync();level.getLightEngine().checkBlock(worldPosition);}}}
        AspectList tags(ItemStack s){return s.isEmpty()?null:AlchemicalFurnaceEntity.tags(s,level);}
        /** process: needs heat >= 2*vis and both powers >= vis; slows down while the furnace is cold. */
        boolean process(ItemStack stack){if(processed!=0)return false;var al=tags(stack);if(al==null||al.size()==0||al.visSize()+aspects.visSize()>MAX_VIS)return false;int aa=al.visSize();
            if(aa*2>heat||aa>power1||aa>power2)return false;heat-=aa*2;power1-=aa;power2-=aa;processed+=(int)(5+Math.max(0,(1-heat/(float)MAX_POWER)*100));aspects.add(al);vis=aspects.visSize();sync();return true;}
    }
    /** TileAlchemyFurnaceAdvancedNozzle: output-only transport facing away from the core, drawing from the core's essentia. */
    public static final class NozzleEntity extends BlockEntity implements IEssentiaTransport {
        Direction facing;
        NozzleEntity(BlockPos p,BlockState s){super(NOZZLE.get(),p,s);}
        void tick(){if(facing==null)for(var d:Direction.values())if(level.getBlockEntity(worldPosition.relative(d)) instanceof CoreEntity){facing=d.getOpposite();break;}}
        CoreEntity furnace(){return facing!=null&&level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof CoreEntity c?c:null;}
        boolean take(Aspect a,int am){var f=furnace();if(f==null||f.aspects.getAmount(a)<am)return false;f.aspects.remove(a,am);f.vis=f.aspects.visSize();f.sync();return true;}
        public boolean isConnectable(Direction f){return f==facing;}
        public boolean canInputFrom(Direction f){return false;}
        public boolean canOutputTo(Direction f){return f==facing;}
        public void setSuction(Aspect a,int amount){}
        public boolean renderExtendedTube(){return false;}
        public int getMinimumSuction(){return 0;}
        public Aspect getSuctionType(Direction f){return null;}
        public int getSuctionAmount(Direction f){return 0;}
        public Aspect getEssentiaType(Direction f){var c=furnace();return c!=null&&c.aspects.size()>0?c.aspects.getAspects()[0]:null;}
        public int getEssentiaAmount(Direction f){var c=furnace();return c!=null&&c.aspects.size()>0?c.aspects.getAmount(c.aspects.getAspects()[0]):0;}
        public int takeEssentia(Aspect a,int amount,Direction f){return canOutputTo(f)&&take(a,amount)?amount:0;}
        public int addEssentia(Aspect a,int amount,Direction f){return 0;}
    }
}
