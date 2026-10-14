package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.level.block.Blocks;
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class StabilizerAudit {
 static boolean enabled(){return Boolean.getBoolean("tc4modern.stabilizerAudit");}
 static final BlockPos BASE=new BlockPos(0,120,5);
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_STABILIZER_AUDIT FAIL "+s);System.out.println("TC4_STABILIZER_AUDIT PASS "+s);}
 @SubscribeEvent static void join(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(!enabled()||!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p))return;
 var l=p.serverLevel();p.getInventory().clearContent();p.getAbilities().invulnerable=true;p.onUpdateAbilities();
 for(var mob:l.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(BASE).inflate(80)))mob.discard();
 for(var item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BASE).inflate(80)))item.discard();
 for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-1;y<6;y++)l.setBlock(BASE.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
 for(int x:new int[]{-2,2}){var pos=BASE.offset(x,0,0);l.setBlock(pos,(x<0?TC4Modern.NODE_STABILIZER.get():TC4Modern.NODE_STABILIZER_ADV.get()).defaultBlockState(),3);l.setBlock(pos.above(),TC4Modern.AURA_NODE.get().defaultBlockState(),3);var node=(AuraNodeEntity)l.getBlockEntity(pos.above());node.reservoir.aspectsBase.add(thaumcraft.api.aspects.Aspect.AIR,30);node.reservoir.aspects.add(thaumcraft.api.aspects.Aspect.AIR,20);node.changed();}
 p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(TC4Modern.THAUMOMETER.get()));p.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(TC4Modern.NODE_STABILIZER_ADV_ITEM.get()));p.getInventory().selected=2;p.connection.teleport(.5,120,.5,0,0);
 var recipe=InfusionItemRecipes.all().stream().filter(r->r.key().equals("NodeStabilizerAdv")).findFirst().orElseThrow();var comps=recipe.components().stream().map(s->new net.minecraft.world.item.ItemStack(s.get())).toList();check(InfusionItemRecipes.match(comps,new net.minecraft.world.item.ItemStack(recipe.central().get()),java.util.Set.of("NODESTABILIZERADV"))==recipe,"original infusion and research gate");check(InfusionItemRecipes.match(comps,new net.minecraft.world.item.ItemStack(recipe.central().get()),java.util.Set.of())==null,"locked infusion rejected");
 }
 @EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
 static class Client {static int ticks;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!enabled())return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
 if(ticks==20)mc.player.getInventory().selected=2;
 if(ticks==80){var server=mc.getSingleplayerServer();server.execute(()->{var l=server.overworld();for(int x:new int[]{-2,2}){var node=(AuraNodeEntity)l.getBlockEntity(BASE.offset(x,1,0));check(node.reservoir.lock==(x<0?1:2),"lock level "+node.reservoir.lock);check(node.reservoir.regeneration==(x<0?1200:12000),"regeneration "+node.reservoir.regeneration);if(x>0){
 long seed=0;for(;;seed++){var r=net.minecraft.util.RandomSource.create(seed);if(r.nextBoolean()&&r.nextInt(5000)==42)break;}node.nodeType=thaumcraft.api.nodes.NodeType.UNSTABLE;check(NodeStabilizerEntity.stabilize(node,net.minecraft.util.RandomSource.create(seed))&&node.nodeType==thaumcraft.api.nodes.NodeType.NORMAL,"unstable node normalizes");
 for(seed=0;;seed++)if(net.minecraft.util.RandomSource.create(seed).nextInt(6250)==69)break;node.reservoir.modifier=thaumcraft.api.nodes.NodeModifier.FADING;check(NodeStabilizerEntity.stabilize(node,net.minecraft.util.RandomSource.create(seed))&&node.reservoir.modifier==thaumcraft.api.nodes.NodeModifier.PALE,"fading node rescued");node.reservoir.modifier=null;
 }}l.setBlock(BASE.offset(3,0,0),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);});shot(mc,"stabilizers-active.png");}
 if(ticks==150){mc.getSingleplayerServer().execute(()->{var node=(AuraNodeEntity)mc.getSingleplayerServer().overworld().getBlockEntity(BASE.offset(2,1,0));check(node.reservoir.lock==0&&node.reservoir.regeneration==600,"redstone disables lock and restores recharge");});shot(mc,"advanced-disabled.png");}
 if(ticks==170)mc.stop();}
 static void shot(net.minecraft.client.Minecraft mc,String name){try{var p=java.nio.file.Path.of("stabilizer-audit");java.nio.file.Files.createDirectories(p);try(var img=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){img.writeToFile(p.resolve(name));}}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
 }
}
