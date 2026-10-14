package dev.eugheist.tc4modern;

/** Exact original effect intervals; deliberately preserves the quiet gaps in the roll table. */
public final class WarpEventPlan {
    private WarpEventPlan() {}
    public enum Kind { NONE, KNOWLEDGE, MESSAGE, VIS_EXHAUST, THAUMARHIA, UNNATURAL_HUNGER, MIST,
        BLURRED_VISION, SUN_SCORNED, MINING_FATIGUE, INFECTIOUS_VIS_EXHAUST, BLINDNESS,
        DEATH_GAZE, MIND_SPIDERS, NIGHT_VISION, REMOVE_STICKY, REAL_MIND_SPIDERS }
    public record Plan(Kind kind,int duration,int amplifier,int count,String message) {}
    public static Plan select(int effect,int warp) {
        int amplifier=Math.min(3,warp/15);
        if(effect<=0) return plan(Kind.NONE,0,0,0,null);
        if(effect<=4) return plan(Kind.KNOWLEDGE,0,0,1,"3");
        if(effect<=8) return plan(Kind.NONE,0,0,0,null);
        if(effect<=12) return plan(Kind.MESSAGE,0,0,0,"11");
        if(effect<=16) return plan(Kind.VIS_EXHAUST,5000,amplifier,0,"1");
        if(effect<=20) return plan(Kind.THAUMARHIA,Math.min(32000,10*warp),0,0,"15");
        if(effect<=24) return plan(Kind.UNNATURAL_HUNGER,5000,amplifier,0,"2");
        if(effect<=28) return plan(Kind.MESSAGE,0,0,0,"12");
        if(effect<=32) return plan(Kind.MIST,0,0,1,"6");
        if(effect<=36) return plan(Kind.BLURRED_VISION,Math.min(32000,10*warp),0,0,null);
        if(effect<=40) return plan(Kind.SUN_SCORNED,5000,amplifier,0,"5");
        if(effect<=44) return plan(Kind.MINING_FATIGUE,1200,amplifier,0,"9");
        if(effect<=48) return plan(Kind.INFECTIOUS_VIS_EXHAUST,6000,amplifier,0,"1");
        if(effect<=52) return plan(Kind.BLINDNESS,Math.min(40*warp,6000),0,0,"10");
        if(effect<=56) return plan(Kind.DEATH_GAZE,6000,amplifier,0,"4");
        if(effect<=60) return plan(Kind.MIND_SPIDERS,0,0,Math.min(50,warp),"7");
        if(effect<=64) return plan(Kind.MESSAGE,0,0,0,"13");
        if(effect<=68) return plan(Kind.MIST,0,0,Math.min(8,warp/30),"6");
        if(effect<=72) return plan(Kind.NIGHT_VISION,Math.min(32000,5*warp),0,0,null);
        if(effect==76) return plan(Kind.REMOVE_STICKY,0,0,1,"14");
        if(effect<=80) return plan(Kind.UNNATURAL_HUNGER,6000,amplifier,0,"2");
        if(effect<=84) return plan(Kind.KNOWLEDGE,0,0,warp/10,"3");
        if(effect<=88) return plan(Kind.NONE,0,0,0,null);
        if(effect<=92) return plan(Kind.REAL_MIND_SPIDERS,0,0,Math.min(50,warp),"7");
        return plan(Kind.MIST,0,0,Math.min(8,warp/15),"6");
    }
    private static Plan plan(Kind kind,int duration,int amplifier,int count,String message) {
        return new Plan(kind,duration,amplifier,count,message==null?null:"warp.text."+message);
    }
}
