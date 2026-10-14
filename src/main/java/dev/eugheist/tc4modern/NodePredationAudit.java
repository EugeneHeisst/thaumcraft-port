package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;

/** Isolated native world: exact original RNG branches, natural ticking and client zap/sound packets. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class NodePredationAudit {
    static final BlockPos BASE=new BlockPos(0,122,5);
    static boolean enabled(){return Boolean.getBoolean("tc4modern.nodePredationAudit");}
    static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("TC4_NODE_PREDATION FAIL "+text);System.out.println("TC4_NODE_PREDATION PASS "+text);}
    static AuraNodeEntity node(ServerLevel l,BlockPos p,int amount){l.removeBlock(p,false);l.setBlock(p,TC4Modern.AURA_NODE.get().defaultBlockState(),3);var n=(AuraNodeEntity)l.getBlockEntity(p);n.reservoir.setAspects(new AspectList().add(Aspect.AIR,amount));n.reservoir.count=2;n.reservoir.regeneration=600;return n;}
    /** Six coordinate draws, aspect draw, then chosen growth/degradation rolls. */
    static net.minecraft.util.RandomSource rolls(int...tail){return new net.minecraft.world.level.levelgen.LegacyRandomSource(0){int i;final int[] head={2,0,0,0,0,0,0};@Override public int nextInt(int bound){int v=i<head.length?head[i]:i-head.length<tail.length?tail[i-head.length]:0;i++;return v<0?bound-1:v;}@Override public boolean nextBoolean(){return false;}};}
    static void rules(ServerLevel l){
        var a=node(l,BASE,100);var b=node(l,BASE.offset(2,0,0),10);
        a.reservoir.aspects.reduce(Aspect.AIR,10);check(NodePredation.discharge(a,rolls())&&a.reservoir.aspects.getAmount(Aspect.AIR)==91&&b.reservoir.aspects.getAmount(Aspect.AIR)==9,"refill steals exactly one; unchanged capacity");check(a.reservoir.aspectsBase.getAmount(Aspect.AIR)==100&&b.reservoir.wait==300,"refill capacity and victim regeneration delay");
        b.reservoir.count=600;b.reservoir.recharge(System.currentTimeMillis());check(b.reservoir.aspects.getAmount(Aspect.AIR)==9&&b.reservoir.wait==299,"victim wait suppresses recharge");
        a.reservoir.aspects.add(Aspect.AIR,9);check(NodePredation.discharge(a,rolls(-1))&&a.reservoir.aspectsBase.getAmount(Aspect.AIR)==100,"failed growth still consumes victim vis");
        check(NodePredation.discharge(a,rolls(0,0))&&a.reservoir.aspectsBase.getAmount(Aspect.AIR)==101&&b.reservoir.aspectsBase.getAmount(Aspect.AIR)==9,"successful growth and one-third victim capacity loss");
        b.reservoir.lock=1;check(!NodePredation.discharge(a,rolls()),"normal stabilizer protects victim");b.reservoir.lock=2;check(!NodePredation.discharge(a,rolls()),"advanced stabilizer protects victim");b.reservoir.lock=0;
        a.reservoir.lock=1;check(!NodePredation.discharge(a,rolls()),"normal stabilizer prevents attacking");a.reservoir.lock=2;check(NodePredation.discharge(a,rolls()),"advanced stabilizer allows attacking");a.reservoir.lock=0;
        a.reservoir.modifier=NodeModifier.FADING;check(!NodePredation.discharge(a,rolls()),"fading node cannot attack");a.reservoir.modifier=null;a.reservoir.count=1;check(!NodePredation.discharge(a,rolls()),"normal attacks every second tick");a.nodeType=thaumcraft.api.nodes.NodeType.HUNGRY;check(!NodePredation.discharge(a,rolls()),"unmodified hungry node keeps original second-tick interval");a.nodeType=thaumcraft.api.nodes.NodeType.NORMAL;a.reservoir.modifier=NodeModifier.BRIGHT;check(NodePredation.discharge(a,rolls()),"bright node attacks every tick");
        a.reservoir.modifier=NodeModifier.PALE;a.reservoir.count=2;check(!NodePredation.discharge(a,rolls()),"pale third-tick interval");a.reservoir.count=3;a.reservoir.aspects=a.reservoir.aspectsBase.copy();check(NodePredation.discharge(a,rolls(0,0,0))&&a.reservoir.modifier==null&&a.reservoir.regeneration==-1,"pale node can improve on growth");
        a=node(l,BASE,10);b=node(l,BASE.offset(2,0,0),10);check(!NodePredation.discharge(a,rolls()),"equal nodes do not steal");b.reservoir.setAspects(new AspectList().add(Aspect.AIR,20));check(!NodePredation.discharge(a,rolls()),"weaker node cannot attack stronger");
        a=node(l,BASE,100);b=node(l,BASE.offset(2,0,0),5);l.setBlock(BASE.offset(1,0,0),Blocks.STONE.defaultBlockState(),3);check(NodePredation.discharge(a,rolls()),"wall does not stop original discharge");l.removeBlock(BASE.offset(1,0,0),false);
        b.reservoir.setAspects(new AspectList().add(Aspect.FIRE,3));check(NodePredation.discharge(a,rolls(0,0))&&a.reservoir.aspectsBase.getAmount(Aspect.FIRE)==1&&a.reservoir.aspects.getAmount(Aspect.FIRE)==0,"new aspect first grows capacity only");check(NodePredation.discharge(a,rolls())&&a.reservoir.aspects.getAmount(Aspect.FIRE)==1,"next theft fills new aspect");
        b=node(l,BASE.offset(2,0,0),1);b.reservoir.aspects.reduce(Aspect.AIR,1);b.reservoir.count=1199;AuraNodeEntity.tick(l,b.getBlockPos(),b.getBlockState(),b);check(l.getBlockEntity(BASE.offset(2,0,0))==null,"empty last aspect decays and node disappears on 1200th tick");
        l.removeBlock(BASE,false);
    }
    @SubscribeEvent static void join(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(!enabled()||!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p))return;
        var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(TC4Modern.GOGGLES.get()));
        l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-3;y<=4;y++)l.setBlock(BASE.offset(x,y,z),y==-3?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        rules(l);
        var n=node(l,BASE,200);n.reservoir.modifier=NodeModifier.BRIGHT;n.changed();
        for(var off:java.util.List.of(new BlockPos(2,0,0),new BlockPos(-2,0,0),new BlockPos(0,0,2),new BlockPos(0,0,-2))){var v=node(l,BASE.offset(off),20);v.changed();}
        p.connection.teleport(.5,121.5,-2,0,0);
    }
    @EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        static int ticks,effects,shotAt=-1,shots;
        @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){if(!enabled())return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();
            if(ticks==20)server.execute(()->{for(var e:server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BASE).inflate(30)))e.discard();});
            if(ticks==40){effects=NodeClientEffects.predationEffects;shot(mc,"nodes-before.png");}
            if(ticks>40&&NodeClientEffects.predationEffects>effects){effects=NodeClientEffects.predationEffects;if(shots<5&&shotAt<0)shotAt=ticks+2;}
            if(ticks==shotAt){shot(mc,"node-zap-"+shots+++".png");shotAt=-1;}
            if(ticks==500){check(shots>0,"natural ticking delivers client bolt and zap sound");server.execute(()->{var l=server.overworld();var n=(AuraNodeEntity)l.getBlockEntity(BASE);check(n.reservoir.aspectsBase.getAmount(Aspect.AIR)>=200,"natural predator capacity retained");for(var off:java.util.List.of(new BlockPos(2,0,0),new BlockPos(-2,0,0),new BlockPos(0,0,2),new BlockPos(0,0,-2)))l.removeBlock(BASE.offset(off),false);var victim=node(l,BASE.offset(2,0,0),1);n.reservoir.modifier=null;n.reservoir.count=2;check(NodePredation.discharge(n,rolls(0,0)),"final victim depleted by original theft");n.changed();victim.reservoir.count=1199;});}
            if(ticks==505)server.execute(()->check(server.overworld().getBlockEntity(BASE.offset(2,0,0))==null,"depleted victim removed by actual next tick"));
            if(ticks==520)shot(mc,"node-consumed.png");
            if(ticks==550)mc.stop();
        }
        static void shot(net.minecraft.client.Minecraft mc,String name){try{var out=java.nio.file.Path.of("node-predation-audit");java.nio.file.Files.createDirectories(out);try(var im=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(out.resolve(name));}}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    }
}
