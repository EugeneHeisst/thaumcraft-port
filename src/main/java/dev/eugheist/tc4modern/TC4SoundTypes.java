package dev.eugheist.tc4modern;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/**
 * Original block sound types and the 1.7 explosion-resistance conversion.
 * CustomStepSound / CustomSoundType return "thaumcraft:" + name for both the break and the step path, and 1.7 used the
 * break sound for placing and the step sound for hits and falls, so every slot plays the same event.
 */
public final class TC4SoundTypes {
    private TC4SoundTypes() {}
    static SoundType custom(float volume, float pitch, String name) {
        var event = SoundEvent.createVariableRangeEvent(AlchemyContent.id(name));
        return new SoundType(volume, pitch, event, event, event, event, event);
    }
    /** BlockJar: CustomStepSound("jar", 1, 1). */
    public static final SoundType JAR = custom(1, 1, "jar");
    /** BlockMirror: CustomStepSound("jar", 0.5, 2.0). */
    public static final SoundType MIRROR = custom(.5f, 2, "jar");
    /** BlockCrystal: CustomStepSound("crystal", 1, 1). */
    public static final SoundType CRYSTAL = custom(1, 1, "crystal");
    /** BlockFluxGoo: CustomSoundType("gore", 1, 1). */
    public static final SoundType GORE = custom(1, 1, "gore");
    /** BlockTaint / BlockTaintFibres: CustomSoundType("gore", 0.5, 0.8). */
    public static final SoundType TAINT = custom(.5f, .8f, "gore");
    /** 1.7 setResistance(x) stores 3x and Block.getExplosionResistance divides by 5. */
    public static float res(float original) { return original * .6f; }
}
