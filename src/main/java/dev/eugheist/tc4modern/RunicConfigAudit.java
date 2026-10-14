package dev.eugheist.tc4modern;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Real dedicated config and production tick/damage handlers with a local test player. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class RunicConfigAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_RUNIC_CONFIG FAIL "+text);System.out.println("TC4_RUNIC_CONFIG PASS "+text);}
    static void clearSpawnInvulnerability(net.minecraft.server.level.ServerPlayer p){try{var field=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(p,0);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
    static void tick(net.minecraft.server.level.ServerPlayer p){RunicRingShield.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));}
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("tc4modern.runicConfigAudit"))return;
        var server=event.getServer();try{
            check(WarpConfig.SPEC.isLoaded()&&RunicConfig.speed()==500&&RunicConfig.delay()==3&&RunicConfig.cost()==125,"loaded original-category TOML: 500ms / 3ticks / 125centivis");
            var p=new net.neoforged.neoforge.common.util.FakePlayer(server.overworld(),new com.mojang.authlib.GameProfile(java.util.UUID.fromString("54433400-0000-0000-0000-000000000112"),"RunicConfigAudit")){
                @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
            };
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);clearSpawnInvulnerability(p);p.setHealth(20);p.setPos(0,180,0);BaublesInventory.of(p).setItem(1,new net.minecraft.world.item.ItemStack(RunicAccessories.Kind.RING.item()));RunicRingShield.dirty=true;RunicRingShield.rechargeDelay=0;
            var wand=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));for(var aspect:Aspect.getPrimalAspects())WandData.add(wand,aspect,50,true);p.getInventory().setItem(0,wand);
            long before=System.currentTimeMillis();tick(p);var clock=RunicRingShield.clocks.get(p.getUUID());check(p.getPersistentData().getInt("TC4RunicCharge")==1&&WandData.get(wand,Aspect.AIR)==4875&&WandData.get(wand,Aspect.EARTH)==4875,"actual recharge spends configured 1.25 Air and Earth vis");
            check(clock[0]>=before+500&&clock[0]<=System.currentTimeMillis()+500,"actual recharge clock uses milliseconds from configuration");tick(p);check(p.getPersistentData().getInt("TC4RunicCharge")==1,"actual interval prevents immediate second recharge");
            RunicConfig.COST.set(0);clock[0]=0;tick(p);check(p.getPersistentData().getInt("TC4RunicCharge")==2&&WandData.get(wand,Aspect.AIR)==4875,"configured zero cost recharges without spending vis");
            RunicConfig.COST.set(7000);clock[0]=0;tick(p);check(p.getPersistentData().getInt("TC4RunicCharge")==2&&WandData.get(wand,Aspect.AIR)==4875,"insufficient configured vis leaves charge and wand unchanged");
            RunicConfig.COST.set(125);p.getPersistentData().putInt("TC4RunicCharge",1);p.invulnerableTime=0;p.hurt(p.damageSources().generic(),1);check(p.getPersistentData().getInt("TC4RunicCharge")==0&&RunicRingShield.rechargeDelay==3&&p.getHealth()==20,"actual shield exhaustion absorbs damage and uses configured three-tick delay");
            for(int i=0;i<3;i++){tick(p);check(p.getPersistentData().getInt("TC4RunicCharge")==0,"actual exhausted shield waits configured tick "+(i+1));}tick(p);check(p.getPersistentData().getInt("TC4RunicCharge")==1,"recharge resumes after all configured delay ticks");
            RunicConfig.SPEED.set(5000);BaublesInventory.of(p).setItem(1,new net.minecraft.world.item.ItemStack(RunicAccessories.Kind.CHARGED.item()));BaublesInventory.of(p).setItem(2,new net.minecraft.world.item.ItemStack(RunicAccessories.Kind.CHARGED.item()));RunicRingShield.dirty=true;clock[0]=0;before=System.currentTimeMillis();tick(p);check(clock[0]>=before+4000&&clock[0]<=System.currentTimeMillis()+4000,"two charged rings subtract their original 1000ms from configured interval");
            RunicRingShield.clocks.remove(p.getUUID());System.out.println("TC4_RUNIC_CONFIG ALL PASS actual configuration, vis units, charge timing, zero/insufficient cost, combat exhaustion and ring upgrades");
        }finally{server.halt(false);}
    }
}
