package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;

/**
 * Sorting core (10): AIHomeTakeSorting + AISortingGoto + AISortingPlace + AIHomeReplace.
 * Takes a stack from the home container only when some marked container (not home, within range) already holds that item
 * on its marked face and has room; carries it to the nearest such container. Items with no destination are ignored for
 * golemIgnoreDelay (10 s); a load that can no longer be placed goes back home.
 */
final class GolemSortingGoal extends Goal {
    record Target(BlockPos pos,Direction side){}
    final StrawGolem g;final Map<ItemStack,Long> timeout=new HashMap<>();static int taken,placed,returned;
    GolemSortingGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){return g.core()==10&&g.working();}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void stop(){g.getNavigation().stop();}
    IItemHandler inventory(BlockPos p,Direction face){return g.level().getCapability(Capabilities.ItemHandler.BLOCK,p,face);}
    BlockPos homeContainer(){return g.home.relative(g.homeFace.getOpposite());}
    boolean onTimeout(ItemStack s){long now=System.currentTimeMillis();timeout.entrySet().removeIf(e->e.getValue()<=now);return timeout.keySet().stream().anyMatch(k->ItemStack.isSameItemSameComponents(k,s));}
    static boolean contains(IItemHandler inv,ItemStack s,StrawGolem g){for(int i=0;i<inv.getSlots();i++)if(g.matches(s,inv.getStackInSlot(i)))return true;return false;}
    /** findSomethingSortCore / AISortingGoto: nearest marked container with room that already contains the item on a marked side. */
    Target destination(ItemStack s){
        Target best=null;double nearest=Double.MAX_VALUE,range=g.range()*(double)g.range();
        for(var value:g.markers){var mark=(net.minecraft.nbt.CompoundTag)value;var pos=BlockPos.of(mark.getLong("pos"));if(pos.equals(homeContainer())||!g.level().hasChunkAt(pos))continue;
            var side=Direction.from3DDataValue(mark.getByte("side"));var inv=inventory(pos,side);if(inv==null)continue;double d=g.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            if(d>=nearest||d>range||!contains(inv,s,g)||ItemHandlerHelper.insertItemStacked(inv,s.copy(),true).getCount()==s.getCount())continue;best=new Target(pos,side);nearest=d;}
        return best;
    }
    boolean near(BlockPos p,double sq){return g.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)<sq;}
    void walk(BlockPos p){if(g.tickCount%10==0||g.getNavigation().isDone())g.getNavigation().moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,1);}
    @Override public void tick(){
        var home=inventory(homeContainer(),g.homeFace);
        if(g.carried().isEmpty()){
            if(!near(g.home,5.01)){walk(g.home);return;}
            if(home==null||g.tickCount%GolemConfig.delay()!=0)return;
            for(int i=0;i<home.getSlots();i++){var s=home.getStackInSlot(i);if(s.isEmpty()||!g.accepts(s)||onTimeout(s))continue;
                if(destination(s)==null){timeout.put(s.copyWithCount(1),System.currentTimeMillis()+GolemConfig.ignoreDelay());continue;}
                var got=home.extractItem(i,Math.min(g.carryLimit(),s.getMaxStackSize()),false);if(!got.isEmpty()){g.carried(got);g.act();GolemChests.interact(g.level(),homeContainer());g.getNavigation().stop();taken++;return;}}
            return;
        }
        var target=destination(g.carried());
        if(target==null){/* AIHomeReplace */if(!near(g.home,5.01)){walk(g.home);return;}if(home!=null&&g.tickCount%GolemConfig.delay()==0){var left=ItemHandlerHelper.insertItemStacked(home,g.carried().copy(),false);if(left.getCount()!=g.carried().getCount()){g.carried(left);g.act();GolemChests.interact(g.level(),homeContainer());returned++;}}return;}
        if(!near(target.pos(),4)){walk(target.pos().relative(target.side()));return;}
        g.getNavigation().stop();g.getLookControl().setLookAt(target.pos().getX()+.5,target.pos().getY()+.5,target.pos().getZ()+.5);
        if(g.tickCount%5!=0)return;var inv=inventory(target.pos(),target.side());if(inv==null)return;
        var left=ItemHandlerHelper.insertItemStacked(inv,g.carried().copy(),false);if(left.getCount()!=g.carried().getCount()){g.carried(left);g.act();GolemChests.interact(g.level(),target.pos());placed++;}
    }
}
