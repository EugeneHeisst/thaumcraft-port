package dev.eugheist.tc4modern;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Registry/raycast adapter; aspect values come from the original registrations. */
public record ScanTarget(String identity, String knowledgeKey, Map<String,Integer> aspects) {
    public String entityClue() {
        if (!knowledgeKey.startsWith("entity:")) return null;
        // Original clue uses EntityList ID, without baby/creeper scan-hash suffixes.
        String id = knowledgeKey.substring(7).replaceFirst("(?:CHILD|FLASHING)+$", "");
        return switch (id) {
            case "minecraft:enderman" -> "Enderman";
            case "tc4modern:firebat" -> "Thaumcraft.Firebat";
            case "tc4modern:brainy_zombie" -> "Thaumcraft.BrainyZombie";
            case "tc4modern:giant_brainy_zombie" -> "Thaumcraft.GiantBrainyZombie";
            case "tc4modern:primal_orb" -> "Thaumcraft.PrimalOrb";
            default -> null;
        };
    }
    /** Block position of a "block:<pos>:<key>" identity, or null for other targets. */
    public static net.minecraft.core.BlockPos blockPos(String identity) {
        if(!identity.startsWith("block:")) return null;
        int end=identity.indexOf(':',6);
        return net.minecraft.core.BlockPos.of(Long.parseLong(end<0?identity.substring(6):identity.substring(6,end)));
    }
    public static ScanTarget item(ItemStack stack, net.minecraft.world.level.Level level) {
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String key=StackAspectBridge.scanKey(stack,id);
        return new ScanTarget("item:"+key,key,RecipeAspects.get(stack,level));
    }
    public static ScanTarget entity(Entity entity) {
        if(entity instanceof ItemEntity item) return item(item.getItem(),entity.level());
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if(entity instanceof ThaumcraftVillager)id="minecraft:villager";
        if(entity instanceof Creeper creeper && creeper.isPowered()) id="minecraft:charged_creeper";
        AspectList list=ModernAspectCatalog.entity(id);
        if(entity instanceof Wisp wisp && wisp.aspect()!=null)list=new AspectList().add(wisp.aspect(),2).add(Aspect.MAGIC,1).add(Aspect.AIR,1);
        if(entity instanceof AspectOrb orb)list=new AspectList().add(orb.aspect(),Math.max(1,orb.value()));
        if(entity instanceof PechEntity pech){list=new AspectList().add(Aspect.MAN,2).add(Aspect.MAGIC,2).add(Aspect.EXCHANGE,2);switch(pech.variant()){case 0->list.add(Aspect.GREED,2);case 1->list.add(Aspect.WEAPON,2);case 2->list.add(Aspect.MAGIC,2);}}
        if(entity instanceof Player player) {
            String name=player.getGameProfile().getName();
            id="player_"+name;
            list=thaumcraft.common.config.ConfigEntityAspects.player(name);
        }
        String key=id;
        // ScanManager.generateEntityHash appends matching entity-tag NBT selectors.
        if(entity instanceof PechEntity pech)key+="PechType"+pech.variant();
        if(entity instanceof Wisp wisp&&wisp.aspect()!=null)key+="Type"+wisp.aspect().getTag();
        if(entity instanceof net.minecraft.world.entity.LivingEntity living && living.isBaby()) key+="CHILD";
        if(entity instanceof Creeper creeper && creeper.getSwelling(1)>0) key+="FLASHING";

        Map<String,Integer> entries=new LinkedHashMap<>();
        for(Aspect aspect:list.getAspects()) entries.put(aspect.getTag(),list.getAmount(aspect));
        return new ScanTarget("entity:"+entity.getId(),"entity:"+key,entries);
    }
    public static ScanTarget look(Player player) {
        Vec3 start=player.getEyePosition(), end=start.add(player.getLookAngle().scale(10));
        var block=player.level().clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        double nearest=block.getType()==HitResult.Type.MISS?100:start.distanceToSqr(block.getLocation());
        Entity found=null;
        for(Entity entity:player.level().getEntities(player,player.getBoundingBox().expandTowards(player.getLookAngle().scale(10)).inflate(1),e->e.isAlive() && !e.isSpectator())) {
            var intersection=entity.getBoundingBox().inflate(.15).clip(start,end);
            if(intersection.isPresent()) {
                double distance=start.distanceToSqr(intersection.get());
                if(distance<nearest) { nearest=distance;found=entity; }
            }
        }
        if(found!=null) return entity(found);
        block=player.level().clip(new ClipContext(start,start.add(player.getLookAngle().scale(5)),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,player));
        if(block.getType()!=HitResult.Type.BLOCK) return null;
        if(player.level().getBlockEntity(block.getBlockPos()) instanceof AuraNodeEntity node){
            Map<String,Integer> entries=new LinkedHashMap<>();
            var scan=node.reservoir.scanAspects(node.nodeType);
            for(Aspect aspect:scan.getAspects())entries.put(aspect.getTag(),scan.getAmount(aspect));
            return new ScanTarget("node:"+node.nodeId,"node:"+node.nodeId,entries);
        }
        var state=player.level().getBlockState(block.getBlockPos());
        String key=BlockAspectBridge.key(state);
        return new ScanTarget("block:"+block.getBlockPos().asLong()+":"+key,key,BlockAspectBridge.get(state,player.level()));
    }
}
