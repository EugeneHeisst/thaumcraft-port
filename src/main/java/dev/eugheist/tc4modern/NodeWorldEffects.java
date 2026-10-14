package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import thaumcraft.api.nodes.NodeType;

/** World interactions from TileNode, separate from vis regeneration and node predation. */
final class NodeWorldEffects {
 static final ResourceKey<net.minecraft.world.level.biome.Biome> FOREST=ResourceKey.create(Registries.BIOME,AlchemyContent.id("magical_forest")),EERIE=ResourceKey.create(Registries.BIOME,AlchemyContent.id("eerie"));
 static BlockPos sample(AuraNodeEntity n,int radius,boolean vertical){var r=n.getLevel().random;return n.getBlockPos().offset(r.nextInt(radius+1)-r.nextInt(radius+1),vertical?r.nextInt(radius+1)-r.nextInt(radius+1):0,r.nextInt(radius+1)-r.nextInt(radius+1));}
 static boolean tick(AuraNodeEntity n){if(!(n.getLevel() instanceof ServerLevel l))return false;var p=n.getBlockPos();var r=l.random;int count=n.reservoir.count;boolean changed=false;
  if(n.nodeType==NodeType.HUNGRY){
   if(WarpConfig.hardNodes())for(var e:l.getEntities((net.minecraft.world.entity.Entity)null,new AABB(p).inflate(15),e->e.isAlive()&&!(e instanceof net.minecraft.world.entity.player.Player player&&player.getAbilities().invulnerable))){var delta=Vec3.atCenterOf(p).subtract(e.position());double d=delta.length();if(delta.lengthSqr()<2&&!e.isInvulnerable()){boolean alive=e.isAlive();e.hurt(l.damageSources().fellOutOfWorld(),1);if(alive&&!e.isAlive())changed|=consume(n,e);}if(d>0&&d<15){double strength=Math.pow(1-d/15,2);e.setDeltaMovement(e.getDeltaMovement().add(delta.x/d*strength*.15,delta.y/d*strength*.25,delta.z/d*strength*.15));e.hurtMarked=true;}}
   if(count%50==0){var target=sample(n,15,true);int surface=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,target.getX(),target.getZ());if(target.getY()>surface)target=target.atY(surface);var hit=l.clip(new net.minecraft.world.level.ClipContext(Vec3.atCenterOf(p),Vec3.atCenterOf(target),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));var at=hit.getBlockPos();if(hit.getType()==HitResult.Type.BLOCK&&!at.equals(p)&&Vec3.atCenterOf(at).distanceToSqr(Vec3.atCenterOf(p))<256){var state=l.getBlockState(at);float hardness=state.getDestroySpeed(l,at);if(!state.isAir()&&hardness>=0&&hardness<5)l.destroyBlock(at,true);}}
  }
  if(count%50==0){
   if(n.nodeType==NodeType.TAINTED){var at=sample(n,7,false);if(!TaintBiomes.isTainted(l,at))TaintBiomes.spread(l,at);if(WarpConfig.hardNodes()&&r.nextBoolean()){var t=sample(n,4,true);thaumcraft.common.blocks.TaintFibresSpread.spread(new TaintWorld(l,t.getY()),t.getX(),t.getY(),t.getZ(),new java.util.Random(){@Override public int nextInt(int bound){return r.nextInt(bound);}@Override public boolean nextBoolean(){return r.nextBoolean();}});}}
   if(l.dimension()!=Level.NETHER&&l.dimension()!=Level.END){if(n.nodeType==NodeType.PURE){var at=sample(n,7,false);if(!l.getBiome(at).is(FOREST)&&(TaintBiomes.isTainted(l,at)||n.getBlockState().is(SilverwoodContent.KNOT.get())))TaintBiomes.setColumn(l,at,l.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FOREST));}
    if(n.nodeType==NodeType.DARK){var at=sample(n,11,false);if(!l.getBiome(at).is(EERIE))TaintBiomes.setColumn(l,at,l.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(EERIE));if(WarpConfig.hardNodes()&&r.nextBoolean()&&l.getNearestPlayer(p.getX()+.5,p.getY()+.5,p.getZ()+.5,24,false)!=null&&l.getEntitiesOfClass(BrainyZombie.class,new AABB(p).inflate(10,6,10),BrainyZombie::giant).size()<=3){var zombie=BrainyContent.GIANT.get().create(l);zombie.moveTo(p.getX()+(r.nextDouble()-r.nextDouble())*5,p.getY()+r.nextInt(3)-1,p.getZ()+(r.nextDouble()-r.nextDouble())*5,r.nextFloat()*360,0);if(BrainyZombie.canSpawn(BrainyContent.GIANT.get(),l,net.minecraft.world.entity.MobSpawnType.NATURAL,zombie.blockPosition(),r)&&zombie.checkSpawnRules(l,net.minecraft.world.entity.MobSpawnType.NATURAL)&&zombie.checkSpawnObstruction(l)){l.addFreshEntity(zombie);l.levelEvent(2004,zombie.blockPosition(),0);zombie.spawnAnim();}}}
   }
  }
  if(count%100==0){if(n.nodeType!=NodeType.PURE&&n.nodeType!=NodeType.TAINTED&&TaintBiomes.isTainted(l,p)&&r.nextInt(500)==0){n.nodeType=NodeType.TAINTED;n.reservoir.regeneration=-1;changed=true;}if(n.nodeType==NodeType.UNSTABLE&&n.reservoir.lock==0&&r.nextBoolean()){var primals=java.util.Arrays.stream(n.reservoir.aspects.getAspects()).filter(a->a!=null&&a.isPrimal()&&n.reservoir.aspects.getAmount(a)>0).toList();if(!primals.isEmpty()){var aspect=primals.get(r.nextInt(primals.size()));n.reservoir.aspects.reduce(aspect,1);l.addFreshEntity(new AspectOrb(l,p.getX()+.5,p.getY()+.5,p.getZ()+.5,aspect,1));changed=true;}}}
  return changed;
 }
 private static boolean consume(AuraNodeEntity n,net.minecraft.world.entity.Entity e){var aspects=new thaumcraft.api.aspects.AspectList();ScanTarget.entity(e).aspects().forEach((tag,amount)->aspects.add(thaumcraft.api.aspects.Aspect.getAspect(tag),amount));var reduced=thaumcraft.common.lib.research.ResearchManager.reduceToPrimals(aspects);if(reduced==null||reduced.size()==0)return false;var aspect=reduced.getAspects()[n.getLevel().random.nextInt(reduced.size())];int base=n.reservoir.aspectsBase.getAmount(aspect);if(n.reservoir.aspects.getAmount(aspect)<base){n.reservoir.aspects.add(aspect,1);return true;}if(n.getLevel().random.nextInt(1+base*2)<reduced.getAmount(aspect)){n.reservoir.aspectsBase.add(aspect,1);return true;}return false;}
}
