package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/** Original warp research thresholds and random primal-aspect awards. */
public final class WarpResearch {
    private WarpResearch() {}
    public record Unlock(String key, int researchRollBound, int message) {}
    /** Called only after a successful event roll; temporary and equipment warp do not count. */
    public static List<Unlock> unlocks(int permanent, int sticky, Predicate<String> completed) {
        int actual = permanent + sticky;
        List<Unlock> result = new ArrayList<>();
        if (actual > 10 && !completed.test("BATHSALTS") && !completed.test("@BATHSALTS"))
            result.add(new Unlock("@BATHSALTS", 0, 8));
        if (actual > 25 && !completed.test("ELDRITCHMINOR"))
            result.add(new Unlock("ELDRITCHMINOR", 10, 0));
        if (actual > 50 && !completed.test("ELDRITCHMAJOR"))
            result.add(new Unlock("ELDRITCHMAJOR", 20, 0));
        return List.copyOf(result);
    }
    /** Each award chooses a primal independently; repeats are permitted and synchronized separately. */
    public static void grant(int times, IntUnaryOperator random, IntConsumer awardPrimalIndex) {
        int amount = 1 + random.applyAsInt(times);
        for (int i = 0; i < amount; i++) awardPrimalIndex.accept(random.applyAsInt(6));
    }
}
