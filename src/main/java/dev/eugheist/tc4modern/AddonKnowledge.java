package dev.eugheist.tc4modern;

/** Package bridge for api.TC4Addons knowledge queries. */
public final class AddonKnowledge {
    private AddonKnowledge() {}
    public static String scanKey(net.minecraft.world.item.ItemStack stack, String id) { return StackAspectBridge.scanKey(stack, id); }
}
