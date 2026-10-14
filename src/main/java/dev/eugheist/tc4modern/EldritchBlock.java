package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

/**
 * BlockEldritch metadata 0-3: stone, hardness 50, resistance 20000, light 8, no mob spawns, no drops, rendered only by
 * tile renderers. Breaking any of them takes the rest of the structure (7x5x7) with it in a harmless explosion.
 */
public final class EldritchBlock extends BaseEntityBlock {
    public enum Kind {
        ALTAR("eldritch_altar"), OBELISK("eldritch_obelisk"), OBELISK_BODY("eldritch_obelisk_body"), CAP("eldritch_cap");
        final String id;
        Kind(String id) { this.id = id; }
    }

    public static boolean isStructure(net.minecraft.world.level.block.Block block){return block instanceof EldritchBlock||block instanceof Body;}
    /** Metadata two has collision but no tile; avoid modern chunks trying to load a null tile. */
    public static final class Body extends net.minecraft.world.level.block.Block {
        public Body(){super(Properties.of().mapColor(MapColor.COLOR_BLACK).strength(50,12000).sound(SoundType.STONE).lightLevel(s->8).noOcclusion().noLootTable().isValidSpawn((s,l,p,t)->false));}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){super.onRemove(state,level,pos,next,moved);removeStructure(state,level,pos,next,moved);}
    }
    public final Kind kind;
    public EldritchBlock(Kind kind) {
        super(Properties.of().mapColor(MapColor.COLOR_BLACK).strength(50, 12000).sound(SoundType.STONE).lightLevel(s -> 8)
            .noOcclusion().noLootTable().isValidSpawn((s, l, p, t) -> false));
        this.kind = kind;
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new EldritchBlock(kind)); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1; }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case ALTAR -> new CrimsonAltarEntity(pos, state);
            case OBELISK -> new ObeliskEntity(pos, state);
            case CAP -> new CapEntity(pos, state);
            case OBELISK_BODY -> null;
        };
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (kind == Kind.ALTAR && !level.isClientSide) return createTickerHelper(type, CrimsonContent.ALTAR_ENTITY.get(), CrimsonAltarEntity::tick);
        if (kind == Kind.OBELISK) return createTickerHelper(type, CrimsonContent.OBELISK_ENTITY.get(), ObeliskEntity::tick);
        return null;
    }

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(kind!=Kind.ALTAR||player.isShiftKeyDown()||!stack.is(WarpEntities.ELDRITCH_EYE.get())||!(level.getBlockEntity(pos) instanceof CrimsonAltarEntity altar)||altar.getEyes()>=4)return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide){altar.insertEye();stack.shrink(1);level.playSound(null,pos,InfusionContent.MatrixEntity.sound("crystal"),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1);}
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** breakBlock: remove every other metadata 0-3 eldritch block in range, then explode with strength 1, no block damage. */
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        removeStructure(state,level,pos,newState,moved);
    }
    private static void removeStructure(BlockState state,Level level,BlockPos pos,BlockState newState,boolean moved){
        boolean replaced=state.is(newState.getBlock());
        if (replaced || level.isClientSide || moved || removing) return;
        removing = true;
        try {
            var pending=new java.util.ArrayDeque<BlockPos>();pending.add(pos);
            while(!pending.isEmpty()){var origin=pending.removeFirst();for(var at:BlockPos.betweenClosed(origin.offset(-3,-2,-3),origin.offset(3,2,3)))if(isStructure(level.getBlockState(at).getBlock())){pending.add(at.immutable());level.removeBlock(at,false);}}
        } finally { removing = false; }
        level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 1, Level.ExplosionInteraction.NONE);
    }
    /** Shared guard so the cascade does not recurse into every removed neighbour. */
    private static boolean removing;

    /** TileEldritchCap: rendered capstone, no logic. */
    public static final class CapEntity extends BlockEntity {
        public CapEntity(BlockPos pos, BlockState state) { super(CrimsonContent.CAP_ENTITY.get(), pos, state); }
    }

    /** TileEldritchObelisk: every tick, eldritch mobs within 6 get Strength and Regeneration; the client draws wisps to them. */
    public static final class ObeliskEntity extends BlockEntity {
        public ObeliskEntity(BlockPos pos, BlockState state) { super(CrimsonContent.OBELISK_ENTITY.get(), pos, state); }
        static void tick(Level level, BlockPos pos, BlockState state, ObeliskEntity obelisk) {
            var center = pos.getBottomCenter();
            var nearby = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(6),
                e -> e instanceof thaumcraft.api.entities.IEldritchMob && e.distanceToSqr(center) < 36);
            for (var mob : nearby) {
                if (!level.isClientSide) {
                    if (mob.hasEffect(MobEffects.REGENERATION)) continue;
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, true));
                    mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, true));
                } else {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.WITCH, center.x, pos.getY() + 1 + level.random.nextFloat() * 3, center.z,
                        (mob.getX() - center.x) * .05, (mob.getEyeY() - pos.getY() - 2) * .05, (mob.getZ() - center.z) * .05);
                }
            }
        }
        public AABB getRenderBoundingBox() { return new AABB(worldPosition).expandTowards(0, 4, 0); }
    }

}
