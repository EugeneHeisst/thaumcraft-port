package thaumcraft.common.tiles.essentia;
import java.util.ArrayList;
import java.util.List;
import thaumcraft.api.aspects.Aspect;
/** Config.aspectOrder: aspects in registration order, used to encode vent colours in block events. */
public final class AspectOrder {
    private AspectOrder() {}
    private static List<Aspect> order;
    public static List<Aspect> get() { if (order == null) order = new ArrayList<>(Aspect.aspects.values()); return order; }
    public static int indexOf(Aspect aspect) { return get().indexOf(aspect); }
    public static Aspect at(int index) { return index < 0 || index >= get().size() ? null : get().get(index); }
}
