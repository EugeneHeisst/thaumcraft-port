package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Audit dump (flag blockDumpOnly via runFocalAudit): sound events, strength, light, tool and particle sprite of every tc4modern block. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class BlockPropertyDump {
 static int ticks;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.blockDumpOnly"))return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  if(++ticks!=40)return;
  if(Boolean.getBoolean("tc4modern.iconDumpOnly")){thaumcraft.common.config.ConfigResearchData.init();int bad=0;for(var cat:thaumcraft.api.research.ResearchCategories.researchCategories.values())for(var r:cat.research.values()){var icon=BasicsBookRecipes.researchIcon(r.key);if(icon!=null&&icon.stack().isEmpty()&&icon.texture()==null&&icon.model()==null){bad++;System.out.println("TC4_ICON EMPTY "+r.key);}}System.out.println("TC4_ICON done bad="+bad);mc.stop();return;}
  var out=new StringBuilder();var level=mc.level;var pos=BlockPos.ZERO;
  for(var block:BuiltInRegistries.BLOCK){var id=BuiltInRegistries.BLOCK.getKey(block);if(!id.getNamespace().equals(TC4Modern.MODID))continue;
   for(var state:block.getStateDefinition().getPossibleStates()){
    if(state!=block.defaultBlockState()&&block.getStateDefinition().getPossibleStates().size()>8)continue;
    var s=state.getSoundType(level,pos,null);
    String sprite;try{sprite=mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(state).contents().name().toString();}catch(Exception e){sprite="ERR";}
    out.append(id).append(state==block.defaultBlockState()?"":" "+state.toString().replaceAll("^[^\\[]*",""))
     .append("\tbreak=").append(s.getBreakSound().getLocation()).append(" step=").append(s.getStepSound().getLocation()).append(" place=").append(s.getPlaceSound().getLocation())
     .append(" hit=").append(s.getHitSound().getLocation()).append(" fall=").append(s.getFallSound().getLocation()).append(" vol=").append(s.getVolume()).append(" pitch=").append(s.getPitch())
     .append("\thard=").append(state.getDestroySpeed(level,pos)).append(" res=").append(block.getExplosionResistance()).append(" light=").append(state.getLightEmission())
     .append(" tool=").append(state.requiresCorrectToolForDrops()).append(" pick=").append(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)).append(" axe=").append(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)).append(" shovel=").append(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)).append(" hoe=").append(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE))
     .append(" render=").append(state.getRenderShape()).append("\tparticle=").append(sprite).append('\n');
   }}
  try{java.nio.file.Files.writeString(java.nio.file.Path.of("block-dump.tsv"),out);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
  System.out.println("TC4_BLOCK_DUMP done "+out.toString().split("\n").length);mc.stop();
 }
}
