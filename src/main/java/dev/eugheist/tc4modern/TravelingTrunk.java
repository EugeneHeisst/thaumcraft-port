package dev.eugheist.tc4modern;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.*;

/**
 * EntityTravelingTrunk: a hopping, owner-following 27-slot chest (36 with the earth upgrade).
 * Upgrades use the original metadata: 0 air faster hops, 1 earth extra row, 2 fire defends the owner,
 * 3 water owner-only and invulnerable, 4 order keeps its contents in the item, 5 entropy pulls in nearby items.
 */
public final class TravelingTrunk extends Mob {
    public static final DeferredHolder<EntityType<?>,EntityType<TravelingTrunk>> ENTITY=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("traveling_trunk"));
    public static final DeferredHolder<Item,Item> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("trunk_spawner"));
    public static final DeferredHolder<MenuType<?>,MenuType<Menu>> MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("traveling_trunk"));
    @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD) static final class Registration {
        @SubscribeEvent static void register(RegisterEvent e){
            e.register(Registries.ITEM,h->h.register(ITEM.getId(),new Spawner()));
            e.register(Registries.ENTITY_TYPE,h->h.register(ENTITY.getId(),EntityType.Builder.<TravelingTrunk>of(TravelingTrunk::new,MobCategory.MISC).sized(.8f,.8f).fireImmune().clientTrackingRange(4).updateInterval(3).build(ENTITY.getId().toString())));
            e.register(Registries.MENU,h->h.register(MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new Menu(id,inv,(TravelingTrunk)inv.player.level().getEntity(buf.readVarInt()),buf.readByte()))));
        }
        @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(ENTITY.get(),Mob.createMobAttributes().add(Attributes.MAX_HEALTH,75).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3).build());}
    }
    static final EntityDataAccessor<Boolean> OPEN=SynchedEntityData.defineId(TravelingTrunk.class,EntityDataSerializers.BOOLEAN),STAY=SynchedEntityData.defineId(TravelingTrunk.class,EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(TravelingTrunk.class,EntityDataSerializers.OPTIONAL_UUID);
    static final EntityDataAccessor<String> OWNER_NAME=SynchedEntityData.defineId(TravelingTrunk.class,EntityDataSerializers.STRING);
    static final EntityDataAccessor<Byte> UPGRADE=SynchedEntityData.defineId(TravelingTrunk.class,EntityDataSerializers.BYTE),ROWS=SynchedEntityData.defineId(TravelingTrunk.class,EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(TravelingTrunk.class,EntityDataSerializers.INT);
    /** Port upgrade items are ordered air, earth, water, fire, order, entropy; the trunk keeps the original metadata. */
    static final int[] ORIGINAL={0,1,3,2,4,5};
    static int portIndex(int original){for(int i=0;i<6;i++)if(ORIGINAL[i]==original)return i;return -1;}
    /** Linked trunks per owner, followed through dimension changes (EventHandlerEntity.linkedEntities). */
    static final Map<UUID,List<WeakReference<TravelingTrunk>>> LINKED=new HashMap<>();
    public final SimpleContainer inventory=new SimpleContainer(36);
    public float lidrot,lidrotO;int jumpDelay,eatDelay,attackDelay;float airSpeed=.02f;
    TravelingTrunk(EntityType<? extends TravelingTrunk> type,Level l){super(type,l);setPersistenceRequired();jumpDelay=random.nextInt(20)+10;
        /* movement is driven by the original hop logic below, not by vanilla controls */
        moveControl=new MoveControl(this){@Override public void tick(){}};jumpControl=new JumpControl(this){@Override public void tick(){}};}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(OPEN,false);b.define(STAY,false);b.define(OWNER,Optional.empty());b.define(OWNER_NAME,"");b.define(UPGRADE,(byte)-1);b.define(ROWS,(byte)3);b.define(ANGER,0);}
    public int upgrade(){return entityData.get(UPGRADE);}void upgrade(int u){entityData.set(UPGRADE,(byte)u);entityData.set(ROWS,(byte)(u==1?4:3));}
    public int rows(){return entityData.get(ROWS);}public int slots(){return rows()*9;}
    public boolean stay(){return entityData.get(STAY);}void stay(boolean s){entityData.set(STAY,s);}
    public boolean open(){return entityData.get(OPEN);}void open(boolean o){entityData.set(OPEN,o);}
    public int anger(){return entityData.get(ANGER);}void anger(int a){entityData.set(ANGER,a);}
    public String ownerName(){return entityData.get(OWNER_NAME);}
    boolean isOwner(Player p){return entityData.get(OWNER).map(p.getUUID()::equals).orElse(false);}
    void owner(Player p){entityData.set(OWNER,Optional.of(p.getUUID()));entityData.set(OWNER_NAME,p.getGameProfile().getName());}
    Player owner(){return entityData.get(OWNER).map(id->level().getPlayerByUUID(id)).orElse(null);}
    @Override public boolean hurt(DamageSource source,float amount){if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return super.hurt(source,amount);if(source.is(DamageTypes.CACTUS)||upgrade()==3)return false;return super.hurt(source,amount);}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override protected float getFlyingSpeed(){return airSpeed;}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putBoolean("Stay",stay());t.putByte("upgrade",(byte)upgrade());entityData.get(OWNER).ifPresent(id->t.putUUID("OwnerUUID",id));t.putString("Owner",ownerName());t.put("Inventory",save(inventory,registryAccess()));}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);stay(t.getBoolean("Stay"));upgrade(t.contains("upgrade")?t.getByte("upgrade"):-1);if(t.hasUUID("OwnerUUID"))entityData.set(OWNER,Optional.of(t.getUUID("OwnerUUID")));entityData.set(OWNER_NAME,t.getString("Owner"));load(inventory,t.getList("Inventory",10),registryAccess());}
    static ListTag save(SimpleContainer inv,net.minecraft.core.HolderLookup.Provider r){var list=new ListTag();for(int i=0;i<inv.getContainerSize();i++)if(!inv.getItem(i).isEmpty()){var n=new CompoundTag();n.putByte("Slot",(byte)i);list.add(inv.getItem(i).save(r,n));}return list;}
    static void load(SimpleContainer inv,ListTag list,net.minecraft.core.HolderLookup.Provider r){inv.clearContent();for(var v:list){var n=(CompoundTag)v;int s=n.getByte("Slot")&255;if(s<inv.getContainerSize())inv.setItem(s,ItemStack.parseOptional(r,n));}}
    @Override public void aiStep(){super.aiStep();if(upgrade()==5)pullItems();}
    @Override public void tick(){
        super.tick();if(isInWater())setDeltaMovement(getDeltaMovement().add(0,.033,0));
        if(level().isClientSide){lidrotO=lidrot;if(!onGround()&&getDeltaMovement().y<0&&!isInWater())lidrot+=.015f;if((onGround()||isInWater())&&!open())lidrot=Math.max(0,lidrot-.1f);if(open())lidrot+=.035f;float max=open()?.5f:.2f;if(lidrot>max)lidrot=max;}
        else if(getHealth()<getMaxHealth()&&(upgrade()==3||tickCount%50==0))heal(1);
    }
    /** updateEntityActionState: link, teleport beside a distant owner, defend (fire), hop towards the owner or the target. */
    @Override protected void customServerAiStep(){
        if(anger()>0)anger(anger()-1);if(eatDelay>0)eatDelay--;if(attackDelay>0)attackDelay--;fallDistance=0;var owner=owner();
        if(owner==null){zza=xxa=0;setJumping(false);return;}
        var list=LINKED.computeIfAbsent(owner.getUUID(),k->new ArrayList<>());list.removeIf(r->r.get()==null||r.get().isRemoved());if(list.stream().noneMatch(r->r.get()==this))list.add(new WeakReference<>(this));
        if(!stay()&&distanceTo(owner)>20&&teleportNear(owner))return;
        var target=getTarget();if(target!=null&&!target.isAlive()){setTarget(null);anger(5);}
        if(!stay()&&upgrade()==2&&anger()==0&&getTarget()==null){var attacker=owner.getLastHurtByMob();if(attacker!=null&&attacker.isAlive()&&hasLineOfSight(attacker)){anger(600);setTarget(attacker);}}
        boolean move=false;target=getTarget();
        if(anger()>0&&target!=null&&target.isAlive()&&target!=owner){face(target);move=true;
            if(attackDelay<=0&&distanceTo(target)<1.5&&target.getBoundingBox().maxY>getBoundingBox().minY&&target.getBoundingBox().minY<getBoundingBox().maxY){attackDelay=10+random.nextInt(5);target.hurt(damageSources().mobAttack(this),4);level().broadcastEntityEvent(this,(byte)17);playSound(SoundEvents.BLAZE_HURT,.5f,random.nextFloat()*.1f+.9f);}}
        if(distanceTo(owner)>5&&anger()==0&&!stay()){face(owner);move=true;}
        if((onGround()||isInWater())&&jumpDelay--<=0&&move){boolean fast=upgrade()==0;jumpDelay=(random.nextInt(10)+5)/3;setJumping(true);xxa=1-random.nextFloat()*2;zza=1;if(isInWater())zza*=.75f;airSpeed=fast?.04f:.03f;playSound(SoundEvents.CHEST_CLOSE,.1f,random.nextFloat()*.1f+.9f);}
        else{setJumping(false);if(onGround()){zza=0;xxa=0;airSpeed=.02f;}}
    }
    void face(Entity e){double dx=e.getX()-getX(),dz=e.getZ()-getZ();float yaw=(float)(Mth.atan2(dz,dx)*180/Math.PI)-90;setYRot(Mth.approachDegrees(getYRot(),yaw,10));yBodyRot=yHeadRot=getYRot();}
    /** Ring of 5x5 positions around the owner (inner 3x3 skipped): solid or water floor, two free blocks above. */
    boolean teleportNear(Player owner){int i=Mth.floor(owner.getX())-2,j=Mth.floor(owner.getZ())-2,k=Mth.floor(owner.getBoundingBox().minY);
        for(int a=0;a<=4;a++)for(int b=0;b<=4;b++){if(a>=1&&b>=1&&a<=3&&b<=3)continue;var floor=new BlockPos(i+a,k-1,j+b);var l=level();
            if(!l.getBlockState(floor).isRedstoneConductor(l,floor)&&!l.getFluidState(floor).is(net.minecraft.tags.FluidTags.WATER))continue;if(l.getBlockState(floor.above()).isRedstoneConductor(l,floor.above())||l.getBlockState(floor.above(2)).isRedstoneConductor(l,floor.above(2)))continue;
            l.playSound(null,i+a+.5,k,j+b+.5,SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.NEUTRAL,.5f,1);moveTo(i+a+.5,k,j+b+.5,getYRot(),getXRot());setTarget(null);return true;}
        return false;}
    /** Entropy: swallow items touching the trunk, attract items within 3 blocks. */
    void pullItems(){if(isRemoved()||getHealth()<=0)return;
        if(!level().isClientSide)for(var item:level().getEntitiesOfClass(ItemEntity.class,new AABB(getX()-.5,getY()-.5,getZ()-.5,getX()+.5,getY()+.5,getZ()+.5))){int before=item.getItem().getCount();var left=insert(item.getItem().copy());if(left.getCount()==before)continue;playSound(SoundEvents.GENERIC_EAT,.5f,random.nextFloat()*.5f+.5f);level().broadcastEntityEvent(this,(byte)17);if(left.isEmpty())item.discard();else item.setItem(left);}
        for(var item:level().getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(3))){double dx=item.getX()-getX(),dy=item.getY()-getY()+getBbHeight()*.8,dz=item.getZ()-getZ(),d=Math.sqrt(dx*dx+dy*dy+dz*dz);if(d<1e-6)continue;item.setDeltaMovement(item.getDeltaMovement().subtract(dx/d*.075,dy/d*.075,dz/d*.075));}}
    ItemStack insert(ItemStack stack){for(int i=0;i<slots()&&!stack.isEmpty();i++){var s=inventory.getItem(i);if(!s.isEmpty()&&ItemStack.isSameItemSameComponents(s,stack)){int move=Math.min(stack.getCount(),s.getMaxStackSize()-s.getCount());if(move>0){s.grow(move);stack.shrink(move);}}}
        for(int i=0;i<slots()&&!stack.isEmpty();i++)if(inventory.getItem(i).isEmpty()){inventory.setItem(i,stack.copy());stack.setCount(0);}inventory.setChanged();return stack;}
    void dropAll(){for(int i=0;i<inventory.getContainerSize();i++){var s=inventory.removeItemNoUpdate(i);if(!s.isEmpty())spawnAtLocation(s,0);}}
    @Override public void die(DamageSource source){if(!level().isClientSide)dropAll();super.die(source);}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.isShiftKeyDown())return InteractionResult.PASS;var held=player.getItemInHand(hand);
        if(held.is(Golemancy.BELL.get()))return InteractionResult.PASS;
        int port=Golemancy.upgradeId(held);
        if(upgrade()==-1&&port>=0){if(!level().isClientSide){upgrade(ORIGINAL[port]);if(!player.getAbilities().instabuild)held.shrink(1);Golemancy.sound(this,"upgrade");}player.swing(hand);return InteractionResult.sidedSuccess(level().isClientSide);}
        var food=held.get(net.minecraft.core.component.DataComponents.FOOD);
        if(food!=null&&getHealth()<getMaxHealth()){if(!level().isClientSide){heal(food.nutrition());held.consume(1,player);playSound(getHealth()>=getMaxHealth()?SoundEvents.PLAYER_BURP:SoundEvents.GENERIC_EAT,.5f,random.nextFloat()*.5f+.5f);level().broadcastEntityEvent(this,(byte)18);}return InteractionResult.sidedSuccess(level().isClientSide);}
        if(upgrade()==3&&!isOwner(player))return InteractionResult.SUCCESS;
        if(!level().isClientSide&&player instanceof ServerPlayer sp)sp.openMenu(new SimpleMenuProvider((id,inv,p)->new Menu(id,inv,this,rows()),getDisplayName()),b->{b.writeVarInt(getId());b.writeByte(rows());});
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void handleEntityEvent(byte id){if(id==17)lidrot=.15f;else if(id==18){lidrot=.15f;level().addParticle(ParticleTypes.HEART,getX()+random.nextFloat()*getBbWidth()*2-getBbWidth(),getY()+.5+random.nextFloat()*getBbHeight(),getZ()+random.nextFloat()*getBbWidth()*2-getBbWidth(),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);}else super.handleEntityEvent(id);}
    /** ItemGolemBell.onLeftClickEntity for trunks: owner-locked with water; sneaking strips the upgrade (50%) and spills everything. */
    boolean retrieve(Player player){
        if(upgrade()==3&&!isOwner(player))return false;
        var dropped=new ItemStack(ITEM.get());var t=new CompoundTag();
        if(player.isShiftKeyDown()){if(upgrade()>-1&&random.nextBoolean())spawnAtLocation(new ItemStack(Golemancy.upgrade(portIndex(upgrade()))),.5f);}
        else{if(hasCustomName())dropped.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,getCustomName());t.putByte("upgrade",(byte)upgrade());if(upgrade()==4)t.put("inventory",save(inventory,registryAccess()));}
        if(!t.isEmpty())Golemancy.tag(dropped,t);spawnAtLocation(dropped,.5f);if(upgrade()!=4||player.isShiftKeyDown())dropAll();Golemancy.sound(this,"zap");discard();return true;
    }
    /** Owner changed dimension: linked trunks that are not told to stay follow (EventHandlerEntity -> travelToDimension). */
    @EventBusSubscriber(modid=TC4Modern.MODID) static final class Follow {
        @SubscribeEvent static void changed(PlayerEvent.PlayerChangedDimensionEvent e){if(!(e.getEntity() instanceof ServerPlayer p))return;var list=LINKED.get(p.getUUID());if(list==null)return;
            for(var ref:List.copyOf(list)){var trunk=ref.get();if(trunk==null||trunk.isRemoved()||trunk.stay()||trunk.level()==p.level())continue;trunk.changeDimension(new DimensionTransition(p.serverLevel(),p.position().add(0,.25,0),Vec3.ZERO,trunk.getYRot(),trunk.getXRot(),DimensionTransition.DO_NOTHING));}}
    }
    /** ItemTrunkSpawner: places the trunk for the player, restoring name, upgrade and (order) contents. */
    public static final class Spawner extends Item {
        Spawner(){super(new Properties().stacksTo(1));}
        @Override public InteractionResult useOn(UseOnContext c){if(!(c.getLevel() instanceof ServerLevel l)||c.getPlayer()==null)return InteractionResult.SUCCESS;var stack=c.getItemInHand();var pos=c.getClickedPos().relative(c.getClickedFace());
            var trunk=new TravelingTrunk(ENTITY.get(),l);trunk.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,Mth.wrapDegrees(l.random.nextFloat()*360),0);trunk.yHeadRot=trunk.yBodyRot=trunk.getYRot();trunk.owner(c.getPlayer());
            if(stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))trunk.setCustomName(stack.getHoverName());var t=NoteData.tag(stack);if(t.contains("upgrade"))trunk.upgrade(t.getByte("upgrade"));if(t.contains("inventory"))load(trunk.inventory,t.getList("inventory",10),l.registryAccess());
            l.addFreshEntity(trunk);trunk.playAmbientSound();if(!c.getPlayer().getAbilities().instabuild)stack.shrink(1);return InteractionResult.CONSUME;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext c,List<Component> text,TooltipFlag f){var t=NoteData.tag(stack);if(t.contains("upgrade")&&t.getByte("upgrade")>-1)text.add(Golemancy.upgrade(portIndex(t.getByte("upgrade"))).getDescription().copy().withStyle(net.minecraft.ChatFormatting.BLUE));if(t.contains("inventory"))text.add(Component.translatable("item.tc4modern.trunk_spawner.contents"));}
    }
    /** ContainerTravelingTrunk: rows of 9 every 23 px, player inventory at 118/176; button 1 toggles stay. */
    public static final class Menu extends AbstractContainerMenu {
        public final TravelingTrunk trunk;final int rows;
        /* the row count travels with the open packet: the client entity may not have synced a fresh earth upgrade yet */
        Menu(int id,Inventory inv,TravelingTrunk trunk,int rows){super(MENU.get(),id);this.trunk=trunk;this.rows=Math.max(3,Math.min(4,rows));Container box=trunk==null?new SimpleContainer(36):trunk.inventory;
            for(int j=0;j<rows;j++)for(int i=0;i<9;i++)addSlot(new Slot(box,i+j*9,8+i*18,15+j*23));
            for(int k=0;k<3;k++)for(int j=0;j<9;j++)addSlot(new Slot(inv,j+k*9+9,8+j*18,118+k*18));for(int l=0;l<9;l++)addSlot(new Slot(inv,l,8+l*18,176));
            if(trunk!=null&&!inv.player.level().isClientSide){trunk.open(true);trunk.playSound(SoundEvents.CHEST_OPEN,.5f,trunk.random.nextFloat()*.1f+.9f);}}
        @Override public boolean clickMenuButton(Player p,int id){if(id==1&&trunk!=null){trunk.stay(!trunk.stay());return true;}return false;}
        @Override public ItemStack quickMoveStack(Player p,int index){var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();int box=rows*9;
            if(index<box?!moveItemStackTo(s,box,slots.size(),true):!moveItemStackTo(s,0,box,false))return ItemStack.EMPTY;if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;}
        @Override public boolean stillValid(Player p){return trunk!=null&&trunk.isAlive()&&p.distanceToSqr(trunk)<64;}
        @Override public void removed(Player p){super.removed(p);if(trunk!=null&&!p.level().isClientSide){trunk.open(false);trunk.playSound(SoundEvents.CHEST_CLOSE,.5f,trunk.random.nextFloat()*.1f+.9f);}}
    }
}
