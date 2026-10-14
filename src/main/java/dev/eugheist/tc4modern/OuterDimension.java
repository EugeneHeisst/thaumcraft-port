package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
public final class OuterDimension {
    public static final ResourceKey<Level> KEY=ResourceKey.create(Registries.DIMENSION,AlchemyContent.id("outer_lands"));
    private OuterDimension(){}
}
