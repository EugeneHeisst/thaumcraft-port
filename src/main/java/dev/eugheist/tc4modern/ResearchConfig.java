package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.ModConfigSpec;
import thaumcraft.api.research.ResearchItem;

/** Original research difficulty and scanning soft cap; server rules accompany knowledge snapshots. */
final class ResearchConfig {
    static ModConfigSpec.IntValue DIFFICULTY, ASPECT_CAP;
    static ModConfigSpec.BooleanValue CHEAT_SHEET;
    static void define(ModConfigSpec.Builder builder) {
        builder.push("general");CHEAT_SHEET=builder.comment("Enables a version of the Thauminomicon in creative mode that grants you all the research when you first use it.").define("allow_cheat_sheet",false);builder.pop();
        builder.push("Research");
        DIFFICULTY=builder.comment("0 = normal; -1 = easy (buy research with aspect points); 1 = hard (solve research notes at the table).").defineInRange("research_difficulty",0,-1,1);
        ASPECT_CAP=builder.comment("Per-aspect point pool at which scanning awards diminish. This is a soft cap, not a maximum.").defineInRange("aspect_total_cap",100,1,32767);
        builder.pop();
    }
    static boolean cheatSheet(){return WarpConfig.SPEC.isLoaded()&&CHEAT_SHEET.get();}
    static int difficulty(){return WarpConfig.SPEC.isLoaded()?DIFFICULTY.get():0;}
    static int aspectCap(){return WarpConfig.SPEC.isLoaded()?ASPECT_CAP.get():100;}
    static boolean purchaseMode(ResearchItem research,int difficulty){return research.tags!=null&&research.tags.size()>0&&(difficulty==-1||difficulty==0&&research.isSecondary());}
    static CompoundTag snapshot(CompoundTag knowledge){
        var result=knowledge.copy();var rules=new CompoundTag();rules.putInt("researchDifficulty",difficulty());rules.putInt("aspectTotalCap",aspectCap());result.put("Rules",rules);return result;
    }
}
