package thaumcraft.common.items;
/** Original ItemResource metadata 0; EventHandlerWorld fuel value, not a coal substitute. */
public final class ItemAlumentum extends net.minecraft.world.item.Item {
    public static java.util.function.BiFunction<net.minecraft.world.level.Level,net.minecraft.world.entity.player.Player,net.minecraft.world.entity.Entity> projectile;
    public static final int BURN_TIME=6400;
    public ItemAlumentum(Properties properties){super(properties);}
    @Override public int getBurnTime(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> recipe){return BURN_TIME;}
    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!player.getAbilities().instabuild)stack.shrink(1);
        level.playSound(player,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.ARROW_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,.3f,.4f/(level.random.nextFloat()*.4f+.8f));
        if(!level.isClientSide)level.addFreshEntity(projectile.apply(level,player));
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}

