package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Isolated visible primal focus, seeker, vis payment, terrain impact and recipe checks. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class PrimalAudit {
    static boolean enabled(){return Boolean.getBoolean("tc4modern.primalAudit");}
    static final BlockPos BASE=new BlockPos(0,120,0);
    static int before,wallBefore;
    static net.minecraft.world.entity.monster.Husk target;
    static void check(boolean b,String text){if(!b)throw new IllegalStateException("TC4_PRIMAL_AUDIT FAIL "+text);System.out.println("TC4_PRIMAL_AUDIT PASS "+text);}
    @SubscribeEvent static void join(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){
        if(!enabled()||!(e.getEntity() instanceof ServerPlayer p))return;
        var l=p.serverLevel();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.onUpdateAbilities();
        l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
        for(var entity:l.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new AABB(BASE).inflate(60)))entity.discard();
        for(var entity:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(BASE).inflate(60)))entity.discard();
        for(int x=-12;x<=12;x++)for(int z=-6;z<=28;z++)for(int y=-1;y<=8;y++)l.setBlock(BASE.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        p.getInventory().clearContent();p.getInventory().setItem(0,FociClientAudit.wand(new ItemStack(PrimalFocus.FOCUS.get())));p.getInventory().selected=0;
        p.connection.teleport(.5,120,.5,0,0);
        before=vis(p.getMainHandItem());
        for(int x=-2;x<=2;x++)for(int y=0;y<5;y++)l.setBlock(BASE.offset(x,y,10),Blocks.STONE_BRICKS.defaultBlockState(),3);wallBefore=25;
        var f=(PrimalFocus.Focus)PrimalFocus.FOCUS.get();var cost=f.getVisCost(new ItemStack(f));
        check(cost.size()==6&&java.util.Arrays.stream(cost.getAspects()).allMatch(a->cost.getAmount(a)>=50&&cost.getAmount(a)<=250&&cost.getAmount(a)%50==0),"six randomized original vis costs");
        check(java.util.Arrays.equals(f.getPossibleUpgradesByRank(new ItemStack(f),3),new int[]{1,16}),"seeker offered at rank3");
        var recipe=ArcaneRecipes.entries().stream().filter(r->r.key().equals("FocusPrimal")).findFirst().orElseThrow();
        var grid=new java.util.ArrayList<ItemStack>();for(var ingredient:recipe.ingredients())grid.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copy());
        check(!recipe.matches(grid,java.util.Set.of())&&recipe.matches(grid,java.util.Set.of("FOCUSPRIMAL")),"arcane recipe research gate");
        var saved=new PrimalFocus.Orb(l,p,true);var tag=new net.minecraft.nbt.CompoundTag();saved.saveWithoutId(tag);
        var loaded=new PrimalFocus.Orb(PrimalFocus.ORB.get(),l);loaded.load(tag);check(!loaded.seeker(),"original seeker reset after NBT reload");
    }
    static void seedEffect(PrimalFocus.Orb orb,boolean taint){
        long seed=0;for(;;seed++){var r=net.minecraft.util.RandomSource.create(seed);if(r.nextInt(100)<=1&&r.nextBoolean()==taint)break;}
        try{var field=net.minecraft.world.entity.Entity.class.getDeclaredField("random");field.setAccessible(true);((net.minecraft.util.RandomSource)field.get(orb)).setSeed(seed);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    static int vis(ItemStack w){int n=0;for(var a:Aspect.getPrimalAspects())n+=WandData.get(w,a);return n;}
    @EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
    static final class Client {
        static int ticks;
        @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
            if(!enabled())return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
            ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
            if(ticks==50)shot(mc,"primal-focus-depth.png");
            if(ticks==60)mc.options.keyUse.setDown(true);
            if(ticks==62)mc.options.keyUse.setDown(false);
            if(ticks==64)server.execute(()->{var p=server.getPlayerList().getPlayer(id);check(vis(p.getMainHandItem())<before,"native use consumes vis");check(p.serverLevel().getEntitiesOfClass(PrimalFocus.Orb.class,new AABB(BASE).inflate(60)).size()==1,"native use spawns one orb");p.connection.teleport(-6,120,6,-90,0);});
            if(ticks==70||ticks==78)shot(mc,"primal-flight-"+ticks+".png");
            if(ticks==135)server.execute(()->{var p=server.getPlayerList().getPlayer(id);int blocks=0;for(int x=-2;x<=2;x++)for(int y=0;y<5;y++)if(p.level().getBlockState(BASE.offset(x,y,10)).is(Blocks.STONE_BRICKS))blocks++;check(blocks<wallBefore,"normal orb explosion destroys terrain; remaining="+blocks);});
            if(ticks==140)shot(mc,"primal-impact.png");
            if(ticks==160)server.execute(()->{
                var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();
                for(var orb:l.getEntitiesOfClass(PrimalFocus.Orb.class,new AABB(BASE).inflate(60)))orb.discard();
                for(int x=-4;x<=4;x++)for(int z=8;z<=12;z++)for(int y=0;y<6;y++)l.setBlock(BASE.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
                var f=new ItemStack(PrimalFocus.FOCUS.get());((FociContent.Focus)f.getItem()).applyUpgrade(f,16,3);p.getInventory().setItem(0,FociClientAudit.wand(f));p.connection.teleport(.5,120,.5,0,0);
                target=EntityType.HUSK.create(l);target.setNoAi(true);target.moveTo(5.5,120,15.5,180,0);l.addFreshEntity(target);
            });
            if(ticks==180)mc.options.keyUse.setDown(true);
            if(ticks==182)mc.options.keyUse.setDown(false);
            if(ticks==205)server.execute(()->{var p=server.getPlayerList().getPlayer(id);var orbs=p.serverLevel().getEntitiesOfClass(PrimalFocus.Orb.class,new AABB(BASE).inflate(60));check(orbs.size()==1&&orbs.getFirst().seeker()&&orbs.getFirst().getDeltaMovement().x>0,"seeker turns toward offset target");p.connection.teleport(-6,120,13,-90,0);});
            if(ticks==215||ticks==230)shot(mc,"primal-seeker-"+ticks+".png");
            if(ticks==300)server.execute(()->check(target.getHealth()<20,"seeker hits target; health="+target.getHealth()));
            if(ticks==305)shot(mc,"primal-seeker-impact.png");
            if(ticks==320)server.execute(()->{
                var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();
                var at=BASE.offset(8,5,20);var orb=new PrimalFocus.Orb(l,p,false);orb.setPos(at.getX()+.5,at.getY()+.5,at.getZ()+.5);
                seedEffect(orb,false);orb.onHit(new net.minecraft.world.phys.BlockHitResult(at.getCenter(),net.minecraft.core.Direction.UP,at,false));
                check(l.getBlockEntity(at) instanceof AuraNodeEntity,"rare original node creation");
                var taint=BASE.offset(-8,1,20);orb=new PrimalFocus.Orb(l,p,false);orb.setPos(taint.getX()+.5,taint.getY()+.5,taint.getZ()+.5);
                seedEffect(orb,true);orb.onHit(new net.minecraft.world.phys.BlockHitResult(taint.getCenter(),net.minecraft.core.Direction.UP,taint,false));
                boolean changed=false;for(int x=-6;x<=1;x++)for(int z=-6;z<=1;z++)if(TaintBiomes.isTainted(l,taint.offset(x,0,z)))changed=true;
                check(changed,"rare original taint biome spread");
                p.connection.teleport(-8,120,14,0,10);
            });
            if(ticks==340)shot(mc,"primal-taint.png");
            if(ticks==350)mc.stop();
        }
        static void shot(net.minecraft.client.Minecraft mc,String name){try{var p=java.nio.file.Path.of("primal-audit");java.nio.file.Files.createDirectories(p);try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(p.resolve(name));}System.out.println("TC4_PRIMAL_AUDIT "+name);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    }
}
