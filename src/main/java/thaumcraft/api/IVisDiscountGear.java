package thaumcraft.api;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.aspects.Aspect;
public interface IVisDiscountGear { int getVisDiscount(ItemStack stack,Player player,Aspect aspect); }
