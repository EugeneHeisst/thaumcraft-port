package dev.eugheist.tc4modern;

import static dev.eugheist.tc4modern.WarpState.Kind.*;

public final class WarpStateTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        require(!WarpEventRoll.due(0, false, false) && !WarpEventRoll.due(1999, false, false)
            && WarpEventRoll.due(2000, false, false) && WarpEventRoll.due(4000, false, false), "Original positive 2000 player tick cadence");
        require(!WarpEventRoll.due(2000, true, false) && !WarpEventRoll.due(2000, false, true), "Wuss mode and ward suppress the entire event check");
        require(WarpResearch.unlocks(10, 0, key -> false).isEmpty(), "Bath salts threshold is strict");
        require(WarpResearch.unlocks(25, 0, key -> false).size() == 1
            && WarpResearch.unlocks(50, 0, key -> false).size() == 2, "Eldritch thresholds are strict");
        var unlocks = WarpResearch.unlocks(30, 21, key -> false);
        require(unlocks.stream().map(WarpResearch.Unlock::key).toList().equals(java.util.List.of("@BATHSALTS", "ELDRITCHMINOR", "ELDRITCHMAJOR"))
            && unlocks.get(1).researchRollBound() == 10 && unlocks.get(2).researchRollBound() == 20, "Original ordered unlocks and award bounds");
        require(WarpResearch.unlocks(51, 0, key -> true).isEmpty(), "Completed unlocks never award again");
        require(WarpResearch.unlocks(11, 0, key -> key.equals("BATHSALTS")).isEmpty(), "Completed bath salts also suppresses its discovery marker");
        require(ResearchPopup.slide(0) == -36 && ResearchPopup.slide(1) == -36
            && ResearchPopup.slide(.5) == 0 && ResearchPopup.slide(.125) == 0
            && ResearchPopup.slide(.875) == 0, "Original research popup hides at endpoints and remains fully visible in the middle");
        require(ResearchPopup.slide(.0625) == ResearchPopup.slide(.9375), "Original popup slide is symmetric");
        var automatic = new net.minecraft.nbt.CompoundTag();
        require(WarpResearchApplication.persistUnlock(automatic, "@BATHSALTS")
            && ResearchProgress.completed(automatic).contains("@BATHSALTS"), "Automatic discovery persists without a browser research entry");
        require(!WarpResearchApplication.persistUnlock(automatic, "@BATHSALTS"), "Repeated automatic discovery reports no new completion");
        var grantCalls = new java.util.ArrayList<Integer>();
        var prizes = new java.util.ArrayList<Integer>();
        WarpResearch.grant(10, bound -> { grantCalls.add(bound); return bound == 10 ? 2 : 4; }, prizes::add);
        require(grantCalls.equals(java.util.List.of(10, 6, 6, 6)) && prizes.equals(java.util.List.of(4, 4, 4)), "Primal grants preserve random call order and repeated aspects");
        var rolling = new WarpState();
        rolling.set(PERMANENT, 20); rolling.set(STICKY, 10); rolling.set(TEMPORARY, 10); rolling.setCounter(100);
        var appliedRoll = WarpEventRoll.check(rolling, 5, false, bound -> bound == 100 ? 10 : 20);
        require(appliedRoll.triggered() && rolling.counter() == 80 && rolling.get(TEMPORARY) == 9
            && rolling.get(PERMANENT) == 20 && rolling.get(STICKY) == 10, "Event decay retains reduced counter instead of resetting to total");
        WarpEventRoll.check(rolling, 0, false, bound -> 99);
        require(rolling.counter() == 80 && rolling.get(TEMPORARY) == 8, "Missed event still decays temporary warp without resetting counter");
        for(int effect:new int[]{-5,0,5,8,85,88}) require(WarpEventPlan.select(effect,100).kind()==WarpEventPlan.Kind.NONE,"Original quiet roll gaps");
        require(WarpEventPlan.select(73,100).kind()==WarpEventPlan.Kind.UNNATURAL_HUNGER && WarpEventPlan.select(75,100).duration()==6000, "73 through 75 follow the original hunger branch");
        require(WarpEventPlan.select(76,100).kind()==WarpEventPlan.Kind.REMOVE_STICKY && WarpEventPlan.select(77,100).kind()==WarpEventPlan.Kind.UNNATURAL_HUNGER,"Sticky removal occurs only at 76");
        require(WarpEventPlan.select(32,10).count()==1 && WarpEventPlan.select(68,10).count()==0 && WarpEventPlan.select(99,100).count()==6,"Original guardian counts differ between mist ranges");
        require(WarpEventPlan.select(60,100).count()==50 && WarpEventPlan.select(92,100).kind()==WarpEventPlan.Kind.REAL_MIND_SPIDERS,"Hallucination and real spider branches retain the spawn cap");
        var noEvent=WarpEventRoll.check(5,0,2,0,0,false,bound->99);
        require(!noEvent.triggered() && noEvent.temporary()==1 && noEvent.counter()==0, "Temporary warp decays even with no event counter");
        var calls=new java.util.ArrayList<Integer>();
        var event=WarpEventRoll.check(20,10,10,100,5,true,bound->{calls.add(bound);return bound==100?10:bound==4?3:20;});
        require(event.triggered() && event.strength()==63 && event.effect()==15 && event.counter()==80 && event.temporary()==9, "Original inclusive probability boundary, gear strength, mask and counter reduction");
        require(calls.equals(java.util.List.of(100,63,4)), "Original random call bounds and ordering");
        var miss=WarpEventRoll.check(20,10,0,100,0,false,bound->11);
        require(!miss.triggered() && miss.counter()==100 && miss.temporary()==0, "Failed roll preserves counter and clamps temporary decay");
        require(ResearchWarp.permanent("UNKNOWN") == 0 && ResearchWarp.sticky("UNKNOWN") == 0, "Ordinary research must not add warp");
        require(ResearchWarp.permanent("RESEARCHER2") == 1 && ResearchWarp.sticky("RESEARCHER2") == 0, "Single warp is entirely permanent");
        require(ResearchWarp.permanent("JARBRAIN") == 2 && ResearchWarp.sticky("JARBRAIN") == 1, "Odd research warp leaves remainder permanent");
        require(ResearchWarp.permanent("OCULUS") == 3 && ResearchWarp.sticky("OCULUS") == 3, "Even research warp splits equally");
        var axis = new net.minecraft.world.phys.Vec3(0, 0, 8);
        require(WarpDeathGaze.inCone(new net.minecraft.world.phys.Vec3(0, 0, 7.99), axis), "Gaze reaches just before flat cap");
        require(!WarpDeathGaze.inCone(new net.minecraft.world.phys.Vec3(0, 0, 8), axis), "Original gaze excludes exact cap");
        require(!WarpDeathGaze.inCone(new net.minecraft.world.phys.Vec3(0, 0, -1), axis), "Gaze excludes behind viewer");
        require(!WarpDeathGaze.inCone(net.minecraft.world.phys.Vec3.ZERO, axis), "Coincident body center preserves original undefined direction exclusion");
        require(WarpDeathGaze.inCone(new net.minecraft.world.phys.Vec3(3, 0, 7.9), axis), "Original gaze uses axial cap, not spherical distance");
        require(!WarpDeathGaze.inCone(new net.minecraft.world.phys.Vec3(3.2, 0, 7.9), axis), "Gaze excludes points beyond angular aperture");
        require(WarpDeathGaze.inCone(new net.minecraft.world.phys.Vec3(0, 3, 7.9), axis), "Cone includes vertical aperture too");
        var fog = new WarpFogState();
        fog.tick(); require(!fog.active() && fog.density() == 0, "No spontaneous warp fog");
        fog.mist(); fog.tick();
        require(fog.duration() == 2399 && Math.abs(fog.density() - .001f) < .000001f, "Mist starts original two-minute envelope");
        fog.guardianPulse(); require(fog.duration() == 2399, "Guardian pulse cannot shorten mist");
        for (int i = 0; i < 2349; i++) fog.tick();
        require(fog.duration() == 50, "Fog countdown follows client ticks");
        fog.tick(); require(Math.abs(fog.density() - .05f) < .000001f, "Final hundred ticks fade density linearly");
        fog.guardianPulse(); require(fog.duration() == 200, "Guardian pulse extends fading fog");
        for (int i = 0; i < 201; i++) fog.tick();
        require(!fog.active() && fog.duration() == -1 && fog.density() == 0, "Original fog ends after zero-duration tick");
        var state = new WarpState();
        state.setCounter(19);
        require(!state.add(PERMANENT, -1) && !state.add(STICKY, -1) && !state.add(TEMPORARY, -1), "Negative additions cannot change empty buckets");
        require(!state.add(TEMPORARY, 0) && state.counter() == 19, "Rejected updates must preserve event counter");
        state.add(PERMANENT, 4); state.add(STICKY, 2); state.add(TEMPORARY, 3);
        require(state.total() == 9 && state.counter() == 9, "Accepted additions reset event counter to total");
        state.setCounter(1);
        require(!state.add(PERMANENT, -4) && state.get(PERMANENT) == 4 && state.counter() == 1, "API forbids permanent removal");
        require(state.add(STICKY, -20) && state.get(STICKY) == 0 && state.counter() == 7, "Sticky removal clamps at zero and resets counter");
        require(state.add(TEMPORARY, -1) && state.get(TEMPORARY) == 2 && state.counter() == 6, "Temporary decay keeps other buckets");
        state.setCounter(2);
        var saved = state.save();
        require(saved.getAllKeys().equals(java.util.Set.of("Thaumcraft.eldritch", "Thaumcraft.eldritch.sticky", "Thaumcraft.eldritch.temp", "Thaumcraft.eldritch.counter")), "Original NBT names");
        var loaded = WarpState.load(saved);
        require(loaded.get(PERMANENT) == 4 && loaded.get(TEMPORARY) == 2 && loaded.get(STICKY) == 0 && loaded.counter() == 2, "Persist event counter independently of total");
        saved.putInt("Thaumcraft.eldritch", -5); saved.remove("Thaumcraft.eldritch.counter");
        loaded = WarpState.load(saved);
        require(loaded.get(PERMANENT) == 0 && loaded.counter() == 0, "Original load clamps buckets and defaults missing counter");
        var playerData = new net.minecraft.nbt.CompoundTag();
        playerData.putString("unrelated", "preserve");
        PlayerWarp.save(playerData, state);
        require(playerData.getString("unrelated").equals("preserve"), "Saving warp must preserve other player data");
        var copiedPlayerData = new net.minecraft.nbt.CompoundTag();
        PlayerWarp.save(copiedPlayerData, WarpState.load(playerData));
        require(WarpState.load(copiedPlayerData).save().equals(state.save()), "Clone bridge copies all buckets and event counter");
        require(new WarpMessagePayload(1, 0).whispers() && new WarpMessagePayload(1, 1).whispers(), "Permanent and sticky gains play whispers");
        require(!new WarpMessagePayload(1, 2).whispers(), "Temporary gains are silent");
        require(new WarpMessagePayload(-1, 2).notificationKey() == null, "Temporary decay must not enqueue notification");
        require(new WarpMessagePayload(-1, 1).notificationKey().equals("tc.removewarpsticky"), "Sticky removal has original notification");
        System.out.println("Warp state tests passed: buckets, API removal rules, counter, original NBT.");
    }
}
