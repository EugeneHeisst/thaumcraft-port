package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.*;

/**
 * Use core (8): AIHomeTake + AIUseItem + AIHomeReplace. Toggles: 1 empty space instead of block, 2 left click, 4 sneaking.
 * With a home container the golem fetches a filtered item and clicks marked blocks with it through a fake player;
 * without one it clicks bare-handed. Whatever the fake player ends up holding returns to the golem's hand, extras drop.
 */
final class GolemUseGoal extends Goal {
    record Target(BlockPos pos,Direction side){}
    static final com.mojang.authlib.GameProfile PROFILE=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes()),"FakeThaumcraftGolem");
    final StrawGolem g;final Map<ItemStack,Long> timeout=new HashMap<>();Target target;int count,nextTick;static int clicks,taken,returned;
    GolemUseGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    boolean toggle(int bit){return (g.useFlags()&bit)!=0;}
    @Override public boolean canUse(){return g.core()==8&&g.working();}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void stop(){g.getNavigation().stop();target=null;}
    IItemHandler home(){return g.level().getCapability(Capabilities.ItemHandler.BLOCK,g.home.relative(g.homeFace.getOpposite()),g.homeFace);}
    boolean nearHome(){return g.distanceToSqr(g.home.getX()+.5,g.home.getY()+.5,g.home.getZ()+.5)<=5;}
    boolean onTimeout(ItemStack s){long now=System.currentTimeMillis();timeout.entrySet().removeIf(e->e.getValue()<=now);return timeout.keySet().stream().anyMatch(k->g.matches(k,s));}
    /** findSomethingUseCore: a marker whose block is air (empty-space mode) or solid, with air in front of the marked face. */
    Target find(ItemStack stack){for(var value:g.markers){var m=(net.minecraft.nbt.CompoundTag)value;var pos=BlockPos.of(m.getLong("pos"));if(!g.level().hasChunkAt(pos)||!g.colorAllows(stack,m.getByte("color")))continue;var side=Direction.from3DDataValue(m.getByte("side"));
        if(toggle(1)!=g.level().isEmptyBlock(pos)||!g.level().isEmptyBlock(pos.relative(side)))continue;return new Target(pos,side);}return null;}
    @Override public void tick(){
        var home=home();
        if(home!=null&&g.carried().isEmpty()){/* AIHomeTake */
            if(!nearHome()){if(g.tickCount%10==0||g.getNavigation().isDone())g.getNavigation().moveTo(g.home.getX()+.5,g.home.getY(),g.home.getZ()+.5,1);return;}
            if(g.tickCount%GolemConfig.delay()!=0)return;
            for(int f=0;f<g.filterCount();f++){var filter=g.filters.getItem(f);if(filter.isEmpty()||onTimeout(filter))continue;if(find(filter)==null){timeout.put(filter.copyWithCount(1),System.currentTimeMillis()+GolemConfig.ignoreDelay());continue;}
                for(int i=0;i<home.getSlots();i++)if(g.matches(filter,home.getStackInSlot(i))){var got=home.extractItem(i,g.carryLimit(),false);if(!got.isEmpty()){g.carried(got);g.act();GolemChests.interact(g.level(),g.home.relative(g.homeFace.getOpposite()));taken++;return;}}}
            return;
        }
        if(target==null){
            if(home!=null&&nearHome()&&g.tickCount%GolemConfig.delay()==0&&(find(g.carried())==null||!g.accepts(g.carried()))){/* AIHomeReplace */var left=ItemHandlerHelper.insertItemStacked(home,g.carried().copy(),false);if(left.getCount()!=g.carried().getCount()){g.carried(left);g.act();GolemChests.interact(g.level(),g.home.relative(g.homeFace.getOpposite()));returned++;}return;}
            if(g.tickCount<nextTick||!g.getNavigation().isDone())return;nextTick=g.tickCount+3*Math.max(1,5-g.tickCount);
            target=find(g.carried());if(target==null){/* AIReturnHome: idle golems walk back within sqrt(3) of home */if(g.distanceToSqr(g.home.getX()+.5,g.home.getY()+.5,g.home.getZ()+.5)>=3)g.getNavigation().moveTo(g.home.getX()+.5,g.home.getY(),g.home.getZ()+.5,1);return;}count=200;g.getNavigation().moveTo(target.pos().getX()+.5,target.pos().getY()+.5,target.pos().getZ()+.5,1);return;
        }
        boolean wantAir=toggle(1);if(wantAir!=g.level().isEmptyBlock(target.pos())||count--<=0){target=null;g.getNavigation().stop();return;}
        g.getLookControl().setLookAt(target.pos().getX()+.5,target.pos().getY()+.5,target.pos().getZ()+.5,30,30);
        if(g.distanceToSqr(target.pos().getX()+.5,target.pos().getY()+.5,target.pos().getZ()+.5)<=4)click();
        else if(g.getNavigation().isDone())g.getNavigation().moveTo(target.pos().getX()+.5,target.pos().getY()+.5,target.pos().getZ()+.5,1);
    }
    void click(){
        if(!(g.level() instanceof ServerLevel server)){target=null;return;}
        if(home()!=null&&g.carried().isEmpty()){target=null;return;}
        FakePlayer p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server,PROFILE);p.getInventory().clearContent();p.moveTo(g.getX(),g.getY(),g.getZ(),g.getYRot(),g.getXRot());p.setItemInHand(InteractionHand.MAIN_HAND,g.carried().copy());p.setShiftKeyDown(toggle(4));
        /* an empty marked block is clicked through the face of the block behind it */
        var at=g.level().isEmptyBlock(target.pos())?target.pos().relative(target.side().getOpposite()):target.pos();var side=target.side();
        try{
            if(toggle(2)){g.act();p.gameMode.handleBlockBreakAction(at,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,side,server.getMaxBuildHeight(),0);}
            else{var hit=new BlockHitResult(Vec3.atCenterOf(at).add(Vec3.atLowerCornerOf(side.getNormal()).scale(.5)),side,at,false);if(p.gameMode.useItemOn(p,server,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction())g.act();}
            clicks++;
            var held=p.getMainHandItem().copy();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(int i=0;i<p.getInventory().getContainerSize();i++){var extra=p.getInventory().getItem(i);if(extra.isEmpty())continue;if(held.isEmpty())held=extra.copy();else g.spawnAtLocation(extra.copy(),.5f);p.getInventory().setItem(i,ItemStack.EMPTY);}
            g.carried(held);
        }finally{p.setShiftKeyDown(false);p.getInventory().clearContent();target=null;g.getNavigation().stop();}
    }
}
