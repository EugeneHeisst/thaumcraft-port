package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.*;

@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class Thaumatorium {
    public static final BooleanProperty TOP=BooleanProperty.create("top");
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final DeferredHolder<Block,Body> BODY=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("thaumatorium"));
    public static final DeferredHolder<Block,Brain> BRAIN=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("mnemonic_matrix"));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ThaumatoriumEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("thaumatorium"));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<ThaumatoriumMenu>> MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("thaumatorium"));
    @SubscribeEvent static void register(RegisterEvent e){
        e.register(Registries.BLOCK,h->{h.register(BODY.getId(),new Body(BlockBehaviour.Properties.of().strength(3,TC4SoundTypes.res(17)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));h.register(BRAIN.getId(),new Brain(BlockBehaviour.Properties.of().strength(3,TC4SoundTypes.res(17)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));});
        e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(ThaumatoriumEntity::new,BODY.get()).build(null)));
        e.register(Registries.MENU,h->h.register(MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->ThaumatoriumMenu.fromNetwork(id,inv,buf))));
        e.register(Registries.ITEM,h->h.register(BRAIN.getId(),new BlockItem(BRAIN.get(),new Item.Properties())));
    }
    @SubscribeEvent static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,ENTITY.get(),(t,d)->new net.neoforged.neoforge.items.wrapper.InvWrapper(t));}
    static void open(Player p,ThaumatoriumEntity t){p.openMenu(t,b->{b.writeBlockPos(t.getBlockPos());b.writeCollection(ResearchProgress.completed(p.getPersistentData()),net.minecraft.network.FriendlyByteBuf::writeUtf);b.writeNbt(t.getUpdateTag(p.registryAccess()));});}
    public static boolean create(net.minecraft.world.item.context.UseOnContext c,ItemStack wand){
        var l=c.getLevel();var player=c.getPlayer();var p=c.getClickedPos();
        if(player==null||!ResearchProgress.completed(player.getPersistentData()).contains("THAUMATORIUM")||!l.getBlockState(p).is(AlchemyMachines.CONSTRUCT.get()))return false;
        if(!l.getBlockState(p.above()).is(AlchemyMachines.CONSTRUCT.get())||!l.getBlockState(p.below()).is(TC4Modern.CRUCIBLE.get()))p=p.below();
        if(!l.getBlockState(p).is(AlchemyMachines.CONSTRUCT.get())||!l.getBlockState(p.above()).is(AlchemyMachines.CONSTRUCT.get())||!l.getBlockState(p.below()).is(TC4Modern.CRUCIBLE.get()))return false;
        var cost=new AspectList().add(Aspect.FIRE,15).add(Aspect.ORDER,30).add(Aspect.WATER,30);
        if(!WandData.consumeCrafting(wand,cost,false,player))return false;
        if(!l.isClientSide){
            if(!WandData.consumeCrafting(wand,cost,true,player))return false;
            /* Original accepts UP/DOWN as the output direction too (unusual, preserved). */
            l.setBlock(p,BODY.get().defaultBlockState().setValue(FACING,c.getClickedFace()),2);
            l.setBlock(p.above(),BODY.get().defaultBlockState().setValue(TOP,true).setValue(FACING,c.getClickedFace()),3);
            l.updateNeighborsAt(p,BODY.get());
            l.playSound(null,p,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("wand")),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
        return true;
    }
    public static final class Body extends BaseEntityBlock {
        public static final MapCodec<Body> CODEC=simpleCodec(Body::new);
        Body(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(TOP,false).setValue(FACING,Direction.NORTH));}
        @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(TOP,FACING);}
        @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
        @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ThaumatoriumEntity(p,s);}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(TOP)?Block.box(0,-16,0,16,16,16):Block.box(0,0,0,16,32,16);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,ENTITY.get(),ThaumatoriumEntity::tick);}
        @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(player.isShiftKeyDown())return InteractionResult.PASS;if(!l.isClientSide&&l.getBlockEntity(s.getValue(TOP)?p.below():p) instanceof ThaumatoriumEntity t)open(player,t);return InteractionResult.sidedSuccess(l.isClientSide);}
        @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){return useWithoutItem(s,l,p,player,hit).consumesAction()?ItemInteractionResult.sidedSuccess(l.isClientSide):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
        @Override protected java.util.List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return java.util.List.of(new ItemStack(AlchemyMachines.CONSTRUCT.get()));}
        @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return new ItemStack(AlchemyMachines.CONSTRUCT.get());}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block source,BlockPos from,boolean moving){
            if(l.isClientSide)return;
            boolean ok=s.getValue(TOP)?l.getBlockState(p.below()).is(this)&&!l.getBlockState(p.below()).getValue(TOP):l.getBlockState(p.above()).is(this)&&l.getBlockState(p.above()).getValue(TOP)&&l.getBlockState(p.below()).is(TC4Modern.CRUCIBLE.get());
            if(!ok)l.setBlock(p,AlchemyMachines.CONSTRUCT.get().defaultBlockState(),3);
            else if(l.getBlockEntity(s.getValue(TOP)?p.below():p) instanceof ThaumatoriumEntity t)t.refreshUpgrades();
        }
        @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&!s.getValue(TOP)&&l.getBlockEntity(p) instanceof ThaumatoriumEntity t)Containers.dropContents(l,p,t);super.onRemove(s,l,p,next,moving);}
    }
    public static final class Brain extends Block {
        public static final MapCodec<Brain> CODEC=simpleCodec(Brain::new);
        Brain(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN));}
        @Override protected MapCodec<? extends Block> codec(){return CODEC;}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace().getOpposite());}
        @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,3,3,13,13,13);}
        @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(!l.isClientSide&&l.isEmptyBlock(p.relative(s.getValue(FACING))))l.destroyBlock(p,true);}
    }
}
