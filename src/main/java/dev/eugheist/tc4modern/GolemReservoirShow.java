package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;

/** Visible end-to-end fixture: actual dropped coal -> furnace -> reservoirs -> autonomous golems -> labelled jars. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class GolemReservoirShow {
    static int ticks;static volatile String failed;static volatile boolean done;
    static final BlockPos CORE=AdvancedFurnaceAudit.CORE;
    static final BlockPos[] RES={CORE.east(2),CORE.west(2)};
    static final java.util.List<StrawGolem> golems=new java.util.ArrayList<>();
    static final java.util.List<BlockPos> jars=new java.util.ArrayList<>();
    static AspectList expected;
    static final java.util.Map<BlockPos,Aspect> labels=new java.util.LinkedHashMap<>();
    static void check(boolean ok,String message){System.out.println("TC4_RESERVOIR_SHOW "+(ok?"PASS ":"FAIL ")+message);if(!ok)failed=message;}
    static void setup(ServerPlayer p){
        AdvancedFurnaceAudit.setup(p);var l=p.serverLevel();
        expected=AdvancedFurnaceAudit.core(l).tags(new ItemStack(net.minecraft.world.item.Items.COAL)).copy();
        for(var a:expected.getAspects())expected.add(a,expected.getAmount(a));
        for(int i=0;i<2;i++){
            l.setBlock(RES[i],EssentiaReservoir.BLOCK.get().defaultBlockState(),3);
            var r=(EssentiaReservoir.ReservoirEntity)l.getBlockEntity(RES[i]);r.facing=i==0?Direction.WEST:Direction.EAST;r.sync();
            var g=GolemHarvestAudit.golem(p,GolemMaterial.THAUMIUM,RES[i].above(),6);g.owner=p.getUUID();g.paused=true;golems.add(g);
        }
        int n=0;for(var a:expected.getAspects()){
            var at=CORE.offset(-2+n*2,0,-4);jars.add(at);labels.put(at,a);GolemEssentiaAudit.jar(l,at,false,a,null,0);
            for(var g:golems)GolemEssentiaAudit.mark(g,at,Direction.UP);n++;
        }
        var at=CORE.south(4).above();l.setBlock(at,TC4Modern.ENERGIZED_NODE.get().defaultBlockState(),3);
        var node=(EnergizedNodeEntity)l.getBlockEntity(at);for(var a:new Aspect[]{Aspect.FIRE,Aspect.WATER,Aspect.ENTROPY}){node.auraBase.add(a,100);node.energy.visBase.add(a,50);node.energy.vis.add(a,50);}VisNetwork.register(l,node);VisNetwork.invalidate();
        var furnace=AdvancedFurnaceAudit.core(l);furnace.heat=0;furnace.power1=0;furnace.power2=0;furnace.sync();
        var known=new java.util.HashMap<String,Integer>();for(var a:expected.getAspects())known.put(a.getTag(),1);ResearchKnowledge.discover(p.getPersistentData(),"reservoir_show",known);ThaumometerItem.sync(p,"");
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TC4Modern.GOGGLES.get()));p.getInventory().setItem(0,new ItemStack(Golemancy.BELL.get()));p.inventoryMenu.broadcastChanges();
        p.connection.teleport(CORE.getX()+.5,CORE.getY()+3.5,CORE.getZ()-8.5,0,24);
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        p.sendSystemMessage(Component.literal("Проверка: печь → 2 резервуара → 2 голема → банки. Сначала накопление, затем перенос."));
    }
    static void progress(ServerPlayer p){var l=p.serverLevel();var f=AdvancedFurnaceAudit.core(l);var msg=new StringBuilder("Печь: "+f.vis+" | Резервуары: ");for(var at:RES)msg.append(((EssentiaReservoir.ReservoirEntity)l.getBlockEntity(at)).essentia.visSize()).append(' ');msg.append("| Банки: ");for(var at:jars){var j=(WardedJarEntity)l.getBlockEntity(at);msg.append(labels.get(at).getName()).append('=').append(j.state.amount).append(' ');}p.displayClientMessage(Component.literal(msg.toString()),true);System.out.println("TC4_RESERVOIR_SHOW "+msg);}
    static void verify(ServerPlayer p){var l=p.serverLevel();var f=AdvancedFurnaceAudit.core(l);check(f.vis==0,"furnace completely drained by reservoirs");for(var at:RES)check(((EssentiaReservoir.ReservoirEntity)l.getBlockEntity(at)).essentia.visSize()==0,"reservoir emptied by autonomous golem "+at);
        for(var at:jars){var j=(WardedJarEntity)l.getBlockEntity(at);check(j.state.amount==expected.getAmount(labels.get(at))&&j.state.aspect==labels.get(at)&&j.state.aspectFilter==labels.get(at),"sorted exact amount "+labels.get(at).getTag()+" expected="+expected.getAmount(labels.get(at))+" actual="+j.state.amount);}
        for(var g:golems)check(g.essentiaAmount==0,"golem delivered carried essentia");done=true;p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.sendSystemMessage(Component.literal(failed==null?"ПРОВЕРКА ПРОЙДЕНА: вся эссенция разложена по банкам без потерь.":"Проверка выявила ошибку: "+failed));System.out.println(failed==null?"TC4_RESERVOIR_SHOW ALL PASS":"TC4_RESERVOIR_SHOW FAILED "+failed);
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.golemReservoirShowOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
        if(ticks==1){mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.fov().set(65);if(org.lwjgl.glfw.GLFW.glfwGetPlatform()==org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11)org.lwjgl.glfw.GLFW.glfwSetWindowPos(mc.getWindow().getWindow(),540,142);}
        if(ticks==60)server.execute(()->{try{setup(server.getPlayerList().getPlayer(id));}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
        if(ticks==400)server.execute(()->{var p=server.getPlayerList().getPlayer(id);check(AdvancedFurnaceAudit.core(p.serverLevel()).vis==0,"actual furnace outputs transferred to reservoirs");int total=0;for(var at:RES)total+=((EssentiaReservoir.ReservoirEntity)p.serverLevel().getBlockEntity(at)).essentia.visSize();check(total==expected.visSize(),"all dropped coal converted and conserved in reservoirs: "+total);for(var g:golems)g.paused=false;p.sendSystemMessage(Component.literal("Големы включены: наблюдай перенос в подписанные банки."));});
        if(ticks==700)server.execute(()->{
            var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();var f=AdvancedFurnaceAudit.core(l);
            check(f.heat>400&&f.power1>400&&f.power2>400,"energized node powered the initially cold furnace");
            var coal=new ItemStack(net.minecraft.world.item.Items.COAL,6);var tags=f.tags(coal);for(int i=0;i<6;i++)expected.add(tags);
            var item=new net.minecraft.world.entity.item.ItemEntity(l,CORE.getX()+.5,CORE.getY()+1.2,CORE.getZ()+.5,coal);item.setNeverPickUp();l.addFreshEntity(item);
            p.sendSystemMessage(Component.literal("Вторая партия: ещё 6 угля при работающих големах. Проверяем одновременную переработку и перенос."));
        });
        if(ticks>=100&&ticks%40==0&&!done)server.execute(()->progress(server.getPlayerList().getPlayer(id)));
        if(ticks==1600)server.execute(()->verify(server.getPlayerList().getPlayer(id)));
        if(ticks==1620)LiquidDeathAudit.shot(mc,"golem-reservoir-result.png");
        if(ticks==1800&&Boolean.getBoolean("tc4modern.closeReservoirShow"))mc.stop();
    }
}
