package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterHazard {
 public static final DeferredHolder<Block,Field> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("eldritch_field"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new Field()));}
 static final class Field extends Block {
  Field(){super(Properties.of().strength(-1,50).lightLevel(s->8).noCollission().noOcclusion().replaceable().noLootTable().isValidSpawn((s,l,p,t)->false));}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
  @Override protected void entityInside(BlockState s,Level level,BlockPos p,Entity entity){if(entity instanceof thaumcraft.api.entities.IEldritchMob)return;if(level.random.nextInt(100)==0)entity.hurt(level.damageSources().wither(),1);var v=entity.getDeltaMovement();entity.setDeltaMovement(v.x*.66,v.y,v.z*.66);if(entity instanceof net.minecraft.world.entity.player.Player player)player.causeFoodExhaustion(.05f);if(entity instanceof LivingEntity living)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,100,1,true,false));}
  @Override protected void tick(BlockState s,ServerLevel level,BlockPos p,net.minecraft.util.RandomSource r){level.removeBlock(p,false);}
  @Override public void animateTick(BlockState s,Level level,BlockPos p,net.minecraft.util.RandomSource r){OuterBossClient.fieldEffects(level,p);}
 }
}
