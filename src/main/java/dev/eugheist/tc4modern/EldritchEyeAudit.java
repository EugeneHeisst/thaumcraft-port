package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class EldritchEyeAudit {
 static final BlockPos BASE=new BlockPos(880,121,-90),ALTAR=BASE.offset(0,0,3);static int ticks;static volatile String failed;
 static void check(boolean yes,String message){if(!yes)failed=message;System.out.println("TC4_EYE "+(yes?"PASS ":"FAIL ")+message);}
 static void setup(net.minecraft.server.level.ServerPlayer p){var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(18000);p.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(40)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
  l.setBlock(ALTAR,CrimsonContent.ALTAR.get().defaultBlockState(),3);var altar=(CrimsonAltarEntity)l.getBlockEntity(ALTAR);var eye=new ItemStack(WarpEntities.ELDRITCH_EYE.get());p.setItemInHand(InteractionHand.MAIN_HAND,eye);var hit=new BlockHitResult(Vec3.atCenterOf(ALTAR),Direction.UP,ALTAR,false);
  p.setShiftKeyDown(true);l.getBlockState(ALTAR).useItemOn(eye,l,p,InteractionHand.MAIN_HAND,hit);p.setShiftKeyDown(false);check(altar.getEyes()==0&&!eye.isEmpty(),"sneaking does not insert an eye");
  for(int a=0;a<4;a++){eye=new ItemStack(WarpEntities.ELDRITCH_EYE.get());p.setItemInHand(InteractionHand.MAIN_HAND,eye);l.getBlockState(ALTAR).useItemOn(eye,l,p,InteractionHand.MAIN_HAND,hit);check(altar.getEyes()==a+1&&eye.isEmpty(),"eye "+(a+1)+" inserts and consumes even in creative");}
  eye=new ItemStack(WarpEntities.ELDRITCH_EYE.get());p.setItemInHand(InteractionHand.MAIN_HAND,eye);l.getBlockState(ALTAR).useItemOn(eye,l,p,InteractionHand.MAIN_HAND,hit);check(altar.getEyes()==4&&!eye.isEmpty(),"fifth eye rejected");
  var tag=altar.saveWithoutMetadata(l.registryAccess());check(tag.getBoolean("spawner")&&tag.getByte("spawntype")==1,"third eye enables guardian spawning");var copy=new CrimsonAltarEntity(ALTAR,l.getBlockState(ALTAR));copy.loadWithComponents(tag,l.registryAccess());check(copy.getEyes()==4&&copy.getUpdateTag(l.registryAccess()).getByte("eyes")==4,"eyes persist and sync to client");
  var recipe=InfusionItemRecipes.all().stream().filter(r->r.key().equals("EldritchEye")).findFirst().orElseThrow();check(recipe.instability()==5&&recipe.central().get()==Items.ENDER_EYE&&InfusionItemRecipes.match(recipe.componentStacks(),new ItemStack(Items.ENDER_EYE),java.util.Set.of("OCULUS"))==recipe,"original research-gated infusion resolves");check(BasicsBookRecipes.researchIcon("OCULUS")!=null,"OCULUS research icon");
  p.connection.teleport(BASE.getX()+2,BASE.getY()+1.2,BASE.getZ(),30,20);p.containerMenu.broadcastChanges();
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.eldritchEyeAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();if(ticks==100)s.execute(()->{try{setup(s.getPlayerList().getPlayer(id));}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
  if(ticks==180)LiquidDeathAudit.shot(mc,"eldritch-eye-altar.png");
  if(ticks==650)s.execute(()->{try{var l=s.getPlayerList().getPlayer(id).serverLevel();var guardians=l.getEntitiesOfClass(EldritchGuardian.class,new AABB(BASE).inflate(40));check(guardians.size()==1,"altar naturally spawns exactly one nearby guardian: "+guardians.size());check(guardians.stream().allMatch(g->g.hasRestriction()&&g instanceof thaumcraft.api.entities.IEldritchMob),"guardian keeps altar home and eldritch identity");}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
  if(ticks==675){System.out.println(failed==null?"TC4_EYE ALL PASS":"TC4_EYE FAILED "+failed);mc.stop();}
 }
}
