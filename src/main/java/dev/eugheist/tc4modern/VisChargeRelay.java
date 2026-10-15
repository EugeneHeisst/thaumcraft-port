package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.Aspect;

/** BlockMetalDevice metadata2 / TileMagicWorkbenchCharger. Network lifecycle stays inherited from TileVisRelay. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class VisChargeRelay {
 private VisChargeRelay(){}
 public static final DeferredHolder<Block,ChargerBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("vis_charge_relay"));
 public static final DeferredHolder<Item,Item> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("vis_charge_relay"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Entity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("vis_charge_relay"));
 @SubscribeEvent static void register(RegisterEvent event){
  event.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new ChargerBlock(BlockBehaviour.Properties.of().strength(3,TC4SoundTypes.res(17)).sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())));
  event.register(Registries.ITEM,h->h.register(ITEM.getId(),new net.minecraft.world.item.BlockItem(BLOCK.get(),new Item.Properties())));
  event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(Entity::new,BLOCK.get()).build(null)));
 }
 public static final class ChargerBlock extends VisRelayBlock {
  public static final MapCodec<ChargerBlock> CODEC=simpleCodec(ChargerBlock::new);
  ChargerBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,net.minecraft.core.Direction.DOWN));}
  @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
  @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState();}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Entity(p,s);}
  // Unlike metadata14, metadata2 is a floating charger and survives removal of the block below it.
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){}
  @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return box(5,8,5,11,16,11);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,ENTITY.get(),Entity::tick);}
 }
 public static final class Entity extends VisRelayEntity {
  public Entity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}
  public static void tick(Level l,BlockPos p,BlockState s,Entity tile){
   VisRelayEntity.tick(l,p,s,tile);
   if(l.isClientSide)return;var below=l.getBlockEntity(p.below());if(below==null)return;
   // Addon wand holders (Thaumic Tinkerer osmotic enchanter) charge like the original workbench slot.
   var wand=below instanceof ArcaneWorkbenchEntity table?table.inventory.getItem(9):dev.eugheist.tc4modern.api.TC4Addons.heldWand(below);
   if(wand==null||!WandData.isWand(wand))return;boolean changed=false;
   for(var aspect:Aspect.getPrimalAspects()){
    int drain=Math.min(5,WandData.capacity(wand)-WandData.get(wand,aspect));if(drain<=0)continue;
    int taken=tile.consumeVis(aspect,drain);if(taken<=0)continue;
    var tag=NoteData.tag(wand);thaumcraft.common.lib.vis.VisStorage.addRealVis(tag,aspect,taken,true,WandData.capacity(wand));
    wand.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));changed=true;
   }
   if(!changed)return;
   if(below instanceof ArcaneWorkbenchEntity table)table.inventory.setChanged();
   else{below.setChanged();l.sendBlockUpdated(below.getBlockPos(),below.getBlockState(),below.getBlockState(),3);}
  }
 }
}
