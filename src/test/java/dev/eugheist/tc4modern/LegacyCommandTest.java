package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ConfigResearchData;
import thaumcraft.common.lib.events.CommandThaumcraft;

public final class LegacyCommandTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        ConfigResearchData.init();
        var data=new CompoundTag();
        require(CommandThaumcraft.giveAspects(data,"AER",125)&&ResearchKnowledge.points(data,"aer")==125,"Case-insensitive name and no gameplay cap");
        CommandThaumcraft.giveAspects(data,"aer",5);require(ResearchKnowledge.points(data,"aer")==130,"Aspect command adds rather than sets");
        CommandThaumcraft.giveAspects(data,"ALL",7);
        for(Aspect aspect:Aspect.aspects.values())require(ResearchKnowledge.points(data,aspect.getTag())==(aspect==Aspect.AIR?137:7),"All aspects, including undiscovered");
        var snapshot=data.copy();require(!CommandThaumcraft.giveAspects(data,"missing",5)&&data.equals(snapshot),"Invalid aspect leaves knowledge unchanged");
        var original=new thaumcraft.common.lib.research.PlayerKnowledge();original.addAspectPool("current",Aspect.AIR,(short)137);original.addAspectPool("current",Aspect.AIR,(short)40000);
        CommandThaumcraft.giveAspects(data,"aer",40000);require(ResearchKnowledge.points(data,"aer")==original.getAspectPoolFor("current",Aspect.AIR),"Original short conversion retained");
        data.getCompound(ResearchKnowledge.KEY).putString("keep","scanning data");
        var callbacks=new ArrayList<String>();var granted=CommandThaumcraft.grantResearch(data,"RESEARCHDUPE",callbacks::add);
        var completed=ResearchProgress.completed(data);
        require(completed.containsAll(List.of("RESEARCHDUPE","RESEARCHER1","RESEARCHER2")),"Recursive ordinary parents");
        require(callbacks.equals(new ArrayList<>(granted)),"Completion and warp callbacks once in original order");
        require(CommandThaumcraft.grantResearch(data,"RESEARCHDUPE",callbacks::add).isEmpty(),"Repeated grant awards nothing");
        var item=ResearchCategories.getResearch("RESEARCHDUPE");var oldParents=item.parentsHidden;var oldSiblings=item.siblings;
        try {
            item.parentsHidden=new String[]{"NODEJAR"};item.siblings=new String[]{"DECONSTRUCTOR","RESEARCHDUPE"};
            var fresh=new CompoundTag();CommandThaumcraft.grantResearch(fresh,item.key,key->{});
            require(ResearchProgress.completed(fresh).containsAll(List.of("NODEJAR","DECONSTRUCTOR")),"Hidden parents and cyclic siblings");
        } finally {item.parentsHidden=oldParents;item.siblings=oldSiblings;}
        var all=CommandThaumcraft.grantAllResearch(data,key->{});
        for(var category:ResearchCategories.researchCategories.values())for(var research:category.research.values())require(ResearchProgress.completed(data).contains(research.key),"All includes hidden/lost research");
        require(!all.isEmpty(),"All granted missing keys");
        CommandThaumcraft.resetResearch(data);
        require(!ResearchProgress.completed(data).contains("RESEARCHDUPE")&&data.getCompound(ResearchKnowledge.KEY).getString("keep").equals("scanning data"),"Reset affects research only");
        for(var category:ResearchCategories.researchCategories.values())for(var research:category.research.values())require(ResearchProgress.completed(data).contains(research.key)==research.isAutoUnlock(),"Reset keeps exactly auto-unlocked research");
        var warp=new WarpState();warp.setCounter(42);
        for(var kind:WarpState.Kind.values()){CommandThaumcraft.changeWarp(warp,kind,20,false);CommandThaumcraft.changeWarp(warp,kind,-8,true);require(warp.get(kind)==12,"Commands reduce even permanent warp");CommandThaumcraft.changeWarp(warp,kind,-100,true);require(warp.get(kind)==0,"Negative warp clamps to zero");}
        require(warp.counter()==42,"Command mutators do not reset event counter");
        require(CommandThaumcraft.warpKind("perm")==WarpState.Kind.PERMANENT&&CommandThaumcraft.warpKind("TeMp")==WarpState.Kind.TEMPORARY&&CommandThaumcraft.warpKind("unknown")==WarpState.Kind.STICKY,"Original kind defaults and case handling");
        System.out.println("PASS: legacy command state, additive aspects, dependency/sibling grants, research-only reset and low-level warp semantics");
    }
}
