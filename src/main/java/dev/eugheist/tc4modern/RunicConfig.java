package dev.eugheist.tc4modern;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original Runic_Shielding category, in original millisecond/tick/centivis units. */
final class RunicConfig {
    static ModConfigSpec.IntValue SPEED,DELAY,COST;
    static void define(ModConfigSpec.Builder builder){
        builder.push("Runic_Shielding");
        SPEED=builder.comment("Milliseconds between shield recharge points, before charged-ring reductions of 500ms each. Original minimum 500.").defineInRange("runic_recharge_speed",2000,500,1000000);
        DELAY=builder.comment("Game ticks before recharge resumes after shielding is exhausted. Original minimum zero.").defineInRange("runic_recharge_delay",80,0,1000000);
        COST=builder.comment("Air and Earth centivis cost per recharge point: 50 equals 0.5 vis of each. Original minimum zero.").defineInRange("runic_cost",50,0,1000000);
        builder.pop();
    }
    static int speed(){return WarpConfig.SPEC.isLoaded()?SPEED.get():2000;}
    static int delay(){return WarpConfig.SPEC.isLoaded()?DELAY.get():80;}
    static int cost(){return WarpConfig.SPEC.isLoaded()?COST.get():50;}
}
