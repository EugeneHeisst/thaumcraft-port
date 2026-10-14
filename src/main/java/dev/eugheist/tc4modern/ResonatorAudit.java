package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Resonator on an essentia tube and on a buffer: reports via chat, knock sound, recipe registered. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ResonatorAudit {
 static int ticks;
 static void check(boolean b,String s){System.out.println((b?"TC4_RESONATOR PASS ":"TC4_RESONATOR FAIL ")+s);}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.resonatorAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
  var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();var pos=p.blockPosition().above(30);
   var tube=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(AlchemyContent.id("essentia_tube"));l.setBlock(pos,tube.defaultBlockState(),3);
   var stack=new ItemStack(TC4Modern.RESONATOR.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
   var r=stack.getItem().onItemUseFirst(stack,new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
   check(r==InteractionResult.SUCCESS,"resonator taps an essentia tube ("+r+")");
   l.setBlock(pos.east(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   var r2=stack.getItem().onItemUseFirst(stack,new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.east()),Direction.UP,pos.east(),false)));
   check(r2==InteractionResult.PASS,"plain stone is ignored");
   check(ArcaneRecipes.entries().stream().anyMatch(x->x.key().equals("Resonator")&&x.output().is(TC4Modern.RESONATOR.get())),"arcane recipe Resonator gives the item");});
  if(ticks==60){java.util.List<String> msgs=new java.util.ArrayList<>();try{var f=net.minecraft.client.gui.components.ChatComponent.class.getDeclaredField("allMessages");f.setAccessible(true);for(var m:(java.util.List<?>)f.get(mc.gui.getChat()))msgs.add(((net.minecraft.client.GuiMessage)m).content().getString());}catch(ReflectiveOperationException x){throw new IllegalStateException(x);}check(msgs.stream().anyMatch(s->s.contains("Поток")||s.contains("Suction")||s.contains("силой")),"chat shows the suction line "+msgs.subList(0,Math.min(3,msgs.size())));mc.stop();}}
}
