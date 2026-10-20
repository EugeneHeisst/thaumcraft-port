package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Issue #4: wizard and banker render as modern villagers with the original skins, level badges and translated job names. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class VillagerLookAudit {
    static int ticks;static boolean ready;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_VILLAGER_LOOK FAIL "+text);System.out.println("TC4_VILLAGER_LOOK PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.villagerLookAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;if(ticks==60&&!ready)return;ticks++;
        var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==50)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var level=p.serverLevel();var base=new BlockPos(64,200,64);
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);level.setDayTime(6000);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){level.setBlock(base.offset(x,-1,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);for(int y=0;y<4;y++)level.setBlock(base.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
            Object[][] row={{VillageContent.WIZARD_PROFESSION.get(),1},{VillageContent.WIZARD_PROFESSION.get(),5},{VillagerProfession.CLERIC,1},{VillageContent.BANKER_PROFESSION.get(),1},{VillageContent.BANKER_PROFESSION.get(),5}};
            for(int i=0;i<row.length;i++){var v=EntityType.VILLAGER.create(level);v.setVillagerData(v.getVillagerData().setProfession((VillagerProfession)row[i][0]).setLevel((int)row[i][1]));v.setVillagerXp(1);v.setNoAi(true);v.moveTo(base.getX()-3.5+i*1.75,base.getY(),base.getZ()+.5,180,0);v.setYHeadRot(180);v.yBodyRot=180;check(level.addFreshEntity(v),"spawned "+row[i][0]);}
            p.connection.teleport(base.getX()+.5,base.getY()+1.2,base.getZ()-5.5,0,8);ready=true;
        });
        if(ticks==100){
            var seen=new java.util.ArrayList<net.minecraft.world.entity.npc.Villager>();for(var e:mc.level.entitiesForRendering())if(e instanceof net.minecraft.world.entity.npc.Villager v)seen.add(v);
            check(seen.size()==5,"five fixture villagers reach the client: "+seen.size()+" "+seen.stream().map(v->v.getVillagerData().getProfession()+"@"+v.blockPosition()).toList()+" player="+mc.player.blockPosition());
            check(net.minecraft.client.resources.language.I18n.exists("entity.minecraft.villager.wizard")&&net.minecraft.client.resources.language.I18n.exists("entity.minecraft.villager.banker"),"job titles translated: "+net.minecraft.client.resources.language.I18n.get("entity.minecraft.villager.wizard")+" / "+net.minecraft.client.resources.language.I18n.get("entity.minecraft.villager.banker"));
            check(ArcaneRecipeIds.enabled()!=null&&ArcaneRecipeIds.enabled().size()==108&&ArcaneRecipes.entries().size()>=108,"client received all 108 arcane recipe ids over the network: "+(ArcaneRecipeIds.enabled()==null?null:ArcaneRecipeIds.enabled().size()));
            for(String job:new String[]{"wizard","banker"})check(mc.getResourceManager().getResource(AlchemyContent.id("textures/entity/villager/profession/"+job+".png")).isPresent(),job+" profession layer texture is packaged");
        }
        if(ticks==140){LiquidDeathAudit.shot(mc,"villager-professions.png");System.out.println("TC4_VILLAGER_LOOK ALL PASS modern villager professions rendered");mc.stop();}
    }
}
