package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class GolemFetter extends Block {
 static final BooleanProperty POWERED=BlockStateProperties.POWERED;
 static final DeferredHolder<Block,Block> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("golem_fetter"));
 GolemFetter(){super(Properties.of().strength(2).sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(POWERED,false));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(POWERED);}
 public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(POWERED,c.getLevel().hasNeighborSignal(c.getClickedPos()));}
 protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(!l.isClientSide&&s.getValue(POWERED)!=l.hasNeighborSignal(p))l.setBlock(p,s.setValue(POWERED,l.hasNeighborSignal(p)),3);}
 static boolean active(BlockState s){return s.is(BLOCK.get())&&s.getValue(POWERED);}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new GolemFetter()));e.register(Registries.ITEM,h->h.register(BLOCK.getId(),new BlockItem(BLOCK.get(),new Item.Properties())));}
}
