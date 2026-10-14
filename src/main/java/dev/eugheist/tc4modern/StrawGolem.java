package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** EntityGolemBase straw + original AIItemPickup/AIHomePlace priorities with modern navigation/inventory capabilities. */
public final class StrawGolem extends PathfinderMob implements MenuProvider {
    static final EntityDataAccessor<ItemStack> FLUID_VISUAL=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.ITEM_STACK);
    net.neoforged.neoforge.fluids.FluidStack fluidCarried=net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    thaumcraft.api.aspects.Aspect essentia;int essentiaAmount;
    static final EntityDataAccessor<String> DECORATION=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.STRING);
    static final EntityDataAccessor<Integer> USE_FLAGS=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Boolean> ADVANCED=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Integer> ATTACK_FLAGS=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> UPGRADES=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Byte> MATERIAL=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Byte> CORE=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.BYTE);
    /** Addon state (Thaumic Energistics backpack etc.): saved with the entity and synced to clients, but not kept on the golem item. */
    static final EntityDataAccessor<CompoundTag> ADDON=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.COMPOUND_TAG);
    public CompoundTag addonData(){return entityData.get(ADDON).copy();}public void addonData(CompoundTag t){entityData.set(ADDON,t.copy());}
    static final EntityDataAccessor<ItemStack> CARRIED=SynchedEntityData.defineId(StrawGolem.class,EntityDataSerializers.ITEM_STACK);
    public BlockPos home=BlockPos.ZERO;public Direction homeFace=Direction.UP;public UUID owner;
    public final SimpleContainer filters=new SimpleContainer(18);
    /** EntityGolemBase.colors: one marker colour per filter slot, -1 meaning any colour. */
    public final byte[] colors=new byte[18];{java.util.Arrays.fill(colors,(byte)-1);}
    /** EntityGolemBase.getColorsMatching: colours of the filter slots holding the item, or every slot colour when no filter is set. */
    java.util.List<Byte> colorsMatching(ItemStack stack){var out=new java.util.ArrayList<Byte>();boolean empty=true;for(int i=0;i<filterCount();i++){var f=filters.getItem(i);if(f.isEmpty())continue;empty=false;if(!stack.isEmpty()&&matches(f,stack))out.add(colors[i]);}if(empty)for(int i=0;i<filterCount();i++)out.add(colors[i]);return out;}
    /** Marker.equalsFuzzy over the matching colours: an any-colour slot accepts every marker, a coloured slot only markers of its colour. */
    boolean colorAllows(ItemStack stack,int markerColor){for(byte c:colorsMatching(stack))if(c==-1||c==markerColor)return true;return false;}
    public ListTag markers=new ListTag();public boolean paused,fillAny;int regen,action;
    StrawGolem(EntityType<? extends StrawGolem> t,Level l){super(t,l);setPersistenceRequired();getNavigation().setCanFloat(true);getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1);}
    static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,10).add(Attributes.MOVEMENT_SPEED,.38).add(Attributes.FOLLOW_RANGE,16).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.ARMOR,0);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(FLUID_VISUAL,ItemStack.EMPTY);b.define(DECORATION,"");b.define(ADVANCED,false);b.define(USE_FLAGS,0);b.define(ATTACK_FLAGS,0);b.define(UPGRADES,0);b.define(MATERIAL,(byte)0);b.define(CORE,(byte)-1);b.define(CARRIED,ItemStack.EMPTY);b.define(ADDON,new CompoundTag());}
    public GolemMaterial material(){return GolemMaterial.byId(entityData.get(MATERIAL));}
    /** EntityGolemBase.getCommandSenderName: custom name, else item.ItemGolemPlacer.<material>.name (e.g. "Таум-голем"). */
    @Override protected net.minecraft.network.chat.Component getTypeName(){return Golemancy.placer(material()).get().getDescription();}
    public String decoration(){return entityData.get(DECORATION);}public boolean deco(char c){return decoration().indexOf(c)>=0;}public boolean advanced(){return entityData.get(ADVANCED);}public int useFlags(){return entityData.get(USE_FLAGS);}
    /** EntityGolemBase.addDecoration: one of each, hat/fez, glasses/visor and bowtie/plating exclude each other. */
    boolean addDecoration(char c){String d=decoration();if(d.indexOf(c)>=0||(c=='F'||c=='H')&&(deco('F')||deco('H'))||(c=='G'||c=='V')&&(deco('G')||deco('V'))||(c=='B'||c=='P')&&(deco('P')||deco('B')))return false;entityData.set(DECORATION,d+c);material(material(),false);restrictTo(home,(int)Math.ceil(range()));return true;}
    int attackSpeed(){return 20-(advanced()?2:0);}
    /** attackEntityWithRangedAttack: velocity 1.6, inaccuracy 7-1.75*water, damage 40% of melee. */
    void shootDart(LivingEntity target){var dart=new GolemDart(this,target,1.6f,7-upgrades(2)*1.75f);dart.setBaseDamage(getAttributeValue(Attributes.ATTACK_DAMAGE)*.4);playSound(InfusionContent.MatrixEntity.sound("golemironshoot"),.5f,1/(random.nextFloat()*.4f+.6f));level().addFreshEntity(dart);act();}
    /** setupGolem/getSpeed/getTotalArmorValue: hat +5 health, mace +2 damage, bowtie x1.1, plating x.88 and +4 armour, visor +1 armour, advanced x1.1. */
    void material(GolemMaterial m,boolean heal){entityData.set(MATERIAL,(byte)m.ordinal());getAttribute(Attributes.MAX_HEALTH).setBaseValue(m.health+(deco('H')?5:0));getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(m.speed*(deco('B')?1.1f:1f)*(deco('P')?.88f:1f)*(1+.15f*upgrades(0))*(advanced()?1.1f:1f));getAttribute(Attributes.ARMOR).setBaseValue(Math.min(20,m.armor+(deco('V')?1:0)+(deco('P')?4:0)));getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2+m.strength+2*upgrades(1)+(deco('M')?2:0));getNavigation().setCanFloat(!m.heavy());if(heal)setHealth(getMaxHealth());}
    public int upgrades(int type){return (entityData.get(UPGRADES)>>(type*2))&3;}
    int upgradeCount(){int count=0;for(int i=0;i<6;i++)count+=upgrades(i);return count;}
    public int filterCount(){return core()==5?1+upgrades(3):6+6*upgrades(3);}
    public int fluidCarryLimit(){return (int)Math.sqrt(carryLimit())*1000;}
    public net.neoforged.neoforge.fluids.FluidStack fluidCarried(){return fluidCarried.copy();}
    public void fluidCarried(net.neoforged.neoforge.fluids.FluidStack fluid){fluidCarried=fluid.copy();entityData.set(FLUID_VISUAL,fluid.isEmpty()?ItemStack.EMPTY:net.neoforged.neoforge.fluids.FluidUtil.getFilledBucket(fluid));}
    /** EntityGolemBase.updateCarried core 6: always a jar, filled to 64*amount/carryLimit of the carried aspect. */
    public thaumcraft.api.aspects.Aspect essentia(){return essentia;}public int essentiaAmount(){return essentiaAmount;}
    public void essentia(thaumcraft.api.aspects.Aspect aspect,int amount){if(aspect==null||amount<=0){aspect=null;amount=0;}essentia=aspect;essentiaAmount=amount;var jar=new ItemStack(EssentiaDevices.JAR.get());if(aspect!=null)EssentiaItems.setAspects(jar,new thaumcraft.api.aspects.AspectList().add(aspect,(int)(64f*amount/carryLimit())));entityData.set(FLUID_VISUAL,jar);}
    ItemStack displayCarried(){return core()==5||core()==6?entityData.get(FLUID_VISUAL):carried();}
    public boolean matches(ItemStack sample,ItemStack stack){return ItemStack.isSameItemSameComponents(sample,stack)||(upgrades(5)>0&&OreTagBridge.equivalent(sample,stack));}
    @Override public boolean doHurtTarget(Entity target){boolean hit=super.doHurtTarget(target);if(hit&&deco('V')&&target instanceof LivingEntity living&&level() instanceof ServerLevel server)living.setLastHurtByPlayer(net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(server));if(hit&&upgrades(3)>0)target.igniteForSeconds(upgrades(3)*4);return hit;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){var attacker=source.getEntity();int entropy=upgrades(5);if(!level().isClientSide&&entropy>0&&attacker!=null&&attacker!=this&&!source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)&&!isInvulnerableTo(source)){attacker.hurt(damageSources().thorns(this),entropy*2+getRandom().nextInt(entropy*2));playSound(net.minecraft.sounds.SoundEvents.THORNS_HIT,.5f,1); }return super.hurt(source,amount);}
    /** getRange: spectacles +max(10%,1), advanced +max(20%,2). */
    public float range(){float d=16+upgrades(2)*4;if(deco('G'))d+=Math.max(d*.1f,1);if(advanced())d+=Math.max(d*.2f,2);return d;}
    int strength(){return material().strength+upgrades(1);}
    public int carryLimit(){int base=material().carry;return base+Math.min(16,Math.max(4,base))*upgrades(1);}
    boolean installUpgrade(int type){if(type<0||type>=6||upgrades(type)>=2||upgradeCount()>=material().upgrades+(advanced()?1:0))return false;entityData.set(UPGRADES,entityData.get(UPGRADES)+(1<<(type*2)));material(material(),false);restrictTo(home,(int)Math.ceil(range()));return true;}
    void restoreUpgrades(int packed){entityData.set(UPGRADES,0);for(int i=0;i<6;i++)for(int n=0;n<((packed>>(i*2))&3);n++)installUpgrade(i);material(material(),false);restrictTo(home,(int)Math.ceil(range()));}
    @Override public boolean fireImmune(){return material().fireResist;}
    @Override protected int decreaseAirSupply(int air){return air;}
    public byte core(){return entityData.get(CORE);}public ItemStack carried(){return entityData.get(CARRIED);}public void carried(ItemStack s){entityData.set(CARRIED,s.copy());}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this){@Override public boolean canUse(){return !material().heavy()&&super.canUse();}});goalSelector.addGoal(1,new Gather(this));goalSelector.addGoal(3,new GolemCombatGoal(this));goalSelector.addGoal(2,new GolemInventoryGoal(this));goalSelector.addGoal(2,new GolemHarvestGoal(this));goalSelector.addGoal(2,new GolemLumberGoal(this));goalSelector.addGoal(1,new GolemLiquidGoal(this));goalSelector.addGoal(1,new GolemEssentiaGoal(this));goalSelector.addGoal(2,new GolemFishGoal(this));goalSelector.addGoal(2,new GolemSortingGoal(this));goalSelector.addGoal(2,new GolemUseGoal(this));goalSelector.addGoal(6,new Goal(){ {setFlags(EnumSet.of(Flag.MOVE));}public boolean canUse(){return (core()==3||core()==4||core()==7||core()==9)&&working()&&tickCount%20==0&&distanceToSqr(home.getX()+.5,home.getY()+.5,home.getZ()+.5)>=3;}public boolean canContinueToUse(){return (core()==3||core()==4||core()==7||core()==9)&&working()&&!getNavigation().isDone();}public void start(){getNavigation().moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1);}public void stop(){getNavigation().stop();}});goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(8,new RandomLookAroundGoal(this));for(var hook:dev.eugheist.tc4modern.api.TC4Addons.golemHooks())hook.goals(this,goalSelector::addGoal);}
    public boolean working(){return core()>=0&&!paused&&!GolemFetter.active(level().getBlockState(blockPosition().below()));}
    boolean accepts(ItemStack stack){boolean empty=true;for(int i=0;i<filterCount();i++){var s=filters.getItem(i);if(!s.isEmpty()){empty=false;if(matches(s,stack))return true;}}return empty;}
    @Override public void aiStep(){super.aiStep();if(action>0)action--;if(!level().isClientSide){if(regen>0)regen--;else{regen=deco('F')?(int)(material().regenDelay*.66f):material().regenDelay;if(getHealth()<getMaxHealth()){heal(1);level().broadcastEntityEvent(this,(byte)5);}}if(distanceToSqr(home.getX(),home.getY(),home.getZ())>=2304||isInWall())returnHome();}}
    void returnHome(){for(int dy=1;dy>=-1;dy--)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){var p=home.offset(dx,dy,dz);if(level().getBlockState(p.below()).isFaceSturdy(level(),p.below(),Direction.UP)&&level().getBlockState(p).getCollisionShape(level(),p).isEmpty()){moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,getYRot(),getXRot());getNavigation().stop();return;}}}
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){
        var held=p.getItemInHand(hand);
        // Player.interactOn uses a disposable item copy for fallback item interactions in creative.
        // Handle the bell here so its selected golem is written to the actual held stack in both modes.
        if(held.is(Golemancy.BELL.get()))return held.interactLivingEntity(p,this,hand);
        for(var hook:dev.eugheist.tc4modern.api.TC4Addons.golemHooks()){var result=hook.interact(this,p,hand);if(result!=InteractionResult.PASS)return result;}
        if(p.isShiftKeyDown())return InteractionResult.PASS;
        if(held.getItem() instanceof Golemancy.Decoration d){if(level().isClientSide)return InteractionResult.SUCCESS;if(addDecoration(d.code)){held.shrink(1);Golemancy.sound(this,"cameraclack");}p.swing(hand,true);return InteractionResult.CONSUME;}
        if(Golemancy.upgradeId(held)>=0){int upgrade=Golemancy.upgradeId(held);if(level().isClientSide)return InteractionResult.SUCCESS;if(installUpgrade(upgrade)){held.shrink(1);Golemancy.sound(this,"upgrade");return InteractionResult.CONSUME;}return InteractionResult.PASS;}
        if(core()==-1&&Golemancy.coreId(held)>=0){if(!level().isClientSide){entityData.set(CORE,(byte)Golemancy.coreId(held));if(core()==6)essentia(null,0);held.shrink(1);Golemancy.sound(this,"upgrade");level().broadcastEntityEvent(this,(byte)7);}return InteractionResult.sidedSuccess(level().isClientSide);}
        if(core()>=0&&core()!=3&&core()!=6&&core()!=7&&core()!=9&&core()!=11&&!(held.getItem() instanceof BasicWandItem)){if(!level().isClientSide)p.openMenu(this,b->b.writeVarInt(getId()));return InteractionResult.sidedSuccess(level().isClientSide);}
        return super.mobInteract(p,hand);
    }
    @Override public Component getDisplayName(){return hasCustomName()?getCustomName():Component.translatable("item.tc4modern.golem_"+material().id());}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv,Player p){return new GolemMenu(id,inv,this);}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return InfusionContent.MatrixEntity.sound("cameraclack");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s){return getAmbientSound();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return getAmbientSound();}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public boolean canBeLeashed(){return false;}
    void dropCarried(){if(!carried().isEmpty()){spawnAtLocation(carried(),.5f);carried(ItemStack.EMPTY);}}
    @Override protected void dropCustomDeathLoot(ServerLevel l,net.minecraft.world.damagesource.DamageSource d,boolean recent){dropCarried();super.dropCustomDeathLoot(l,d,recent);}
    CompoundTag itemData(){var t=new CompoundTag();if(core()>=0)t.putByte("core",core());var list=new ListTag();for(int i=0;i<filters.getContainerSize();i++)if(!filters.getItem(i).isEmpty()){var n=new CompoundTag();n.putByte("Slot",(byte)i);n.put("item",filters.getItem(i).save(registryAccess()));list.add(n);}t.put("Inventory",list);t.put("markers",markers.copy());if(!fluidCarried.isEmpty())t.put("FluidCarried",fluidCarried.saveOptional(registryAccess()));if(essentia!=null&&essentiaAmount>0){t.putString("essentia",essentia.getTag());t.putInt("essentiaAmount",essentiaAmount);}t.putInt("AttackFlags",entityData.get(ATTACK_FLAGS));t.putInt("UseFlags",entityData.get(USE_FLAGS));t.putBoolean("FillAny",fillAny);t.putByteArray("colors",colors.clone());t.putInt("Upgrades",entityData.get(UPGRADES));if(!decoration().isEmpty())t.putString("deco",decoration());if(advanced())t.putByte("advanced",(byte)1);return t;}
    ItemStack asItem(){var s=new ItemStack(Golemancy.placer(material()).get());Golemancy.tag(s,itemData());if(hasCustomName())s.set(DataComponents.CUSTOM_NAME,getCustomName());return s;}
    void restoreItem(ItemStack s){restoreData(NoteData.tag(s));}
    void restoreData(CompoundTag t){entityData.set(DECORATION,t.getString("deco"));entityData.set(ADVANCED,t.contains("advanced"));entityData.set(CORE,t.contains("core")&&t.getByte("core")>=0&&(t.getByte("core")<=7||t.getByte("core")==9||t.getByte("core")==8||t.getByte("core")==10||t.getByte("core")==11)?t.getByte("core"):(byte)-1);fluidCarried(net.neoforged.neoforge.fluids.FluidStack.parseOptional(registryAccess(),t.getCompound("FluidCarried")));entityData.set(ATTACK_FLAGS,t.getInt("AttackFlags")&15);entityData.set(USE_FLAGS,t.getInt("UseFlags")&7);filters.clearContent();for(var value:t.getList("Inventory",10)){var n=(CompoundTag)value;int slot=n.getByte("Slot");if(slot>=0&&slot<filters.getContainerSize())filters.setItem(slot,ItemStack.parseOptional(registryAccess(),n.getCompound("item"))); }fillAny=t.getBoolean("FillAny");java.util.Arrays.fill(colors,(byte)-1);var savedColors=t.getByteArray("colors");System.arraycopy(savedColors,0,colors,0,Math.min(savedColors.length,colors.length));restoreUpgrades(t.getInt("Upgrades"));if(core()==6)essentia(thaumcraft.api.aspects.Aspect.getAspect(t.getString("essentia")),t.getInt("essentiaAmount"));else{essentia=null;essentiaAmount=0;}markers=t.getList("markers",10).copy();}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putByte("Material",(byte)material().ordinal());t.put("Golem",itemData());t.putLong("Home",home.asLong());t.putByte("HomeFacing",(byte)homeFace.get3DDataValue());if(owner!=null)t.putUUID("Owner",owner);t.put("ItemCarried",carried().saveOptional(registryAccess()));if(!entityData.get(ADDON).isEmpty())t.put("AddonData",entityData.get(ADDON).copy());}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);float savedHealth=getHealth();material(GolemMaterial.byId(t.getByte("Material")),false);setHealth(savedHealth);home=BlockPos.of(t.getLong("Home"));homeFace=Direction.from3DDataValue(t.getByte("HomeFacing"));if(t.hasUUID("Owner"))owner=t.getUUID("Owner");restoreData(t.getCompound("Golem"));carried(ItemStack.parseOptional(registryAccess(),t.getCompound("ItemCarried")));addonData(t.getCompound("AddonData"));restrictTo(home,(int)Math.ceil(range()));}
    @Override public void handleEntityEvent(byte event){if(event==7)action=6;else super.handleEntityEvent(event);}
    public void act(){action=6;level().broadcastEntityEvent(this,(byte)7);}
    static final class Gather extends Goal {
        final StrawGolem g;ItemEntity target;int retry;
        Gather(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return g.core()==2&&g.working();}
        @Override public boolean canContinueToUse(){return g.core()==2&&g.working();}
        @Override public void stop(){target=null;g.getNavigation().stop();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            if(!g.carried().isEmpty()){
                target=null;double d=g.distanceToSqr(g.home.getX()+.5,g.home.getY()+.5,g.home.getZ()+.5);
                if(d>5){if(g.tickCount%5==0)g.getNavigation().moveTo(g.home.getX()+.5,g.home.getY(),g.home.getZ()+.5,1);return;}
                g.getNavigation().stop();if(g.tickCount%GolemConfig.delay()!=0)return;
                var inv=g.level().getCapability(Capabilities.ItemHandler.BLOCK,g.home.relative(g.homeFace.getOpposite()),g.homeFace);
                if(inv!=null){var remaining=ItemHandlerHelper.insertItemStacked(inv,g.carried().copy(),false);if(remaining.getCount()!=g.carried().getCount()){g.carried(remaining);g.act();GolemChests.interact(g.level(),g.home.relative(g.homeFace.getOpposite()));}}
                return;
            }
            if(target!=null&&(!target.isAlive()||target.getItem().isEmpty()||!g.accepts(target.getItem())))target=null;
            if(target==null&&g.tickCount%GolemConfig.delay()==0){
                double closest=Double.MAX_VALUE;
                for(var e:g.level().getEntitiesOfClass(ItemEntity.class,new AABB(g.home).inflate(g.range()))){if(e.hasPickUpDelay()||!g.accepts(e.getItem())||e.distanceToSqr(g.home.getX()+.5,g.home.getY()+.5,g.home.getZ()+.5)>g.range()*g.range())continue;double d=g.distanceToSqr(e);if(d<closest){closest=d;target=e;}}
                if(target!=null){retry=200;g.getNavigation().moveTo(target,1);}
            }
            if(target!=null){g.getLookControl().setLookAt(target,30,30);if(g.distanceToSqr(target)<=2){var stack=target.getItem();g.carried(stack.split(Math.min(g.carryLimit(),stack.getMaxStackSize())));if(stack.isEmpty())target.discard();else target.setItem(stack);g.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP,.2f,2);g.act();target=null;g.getNavigation().stop();}else if(--retry<=0){target=null;g.getNavigation().stop();}else if(g.getNavigation().isDone()&&g.distanceToSqr(target)<4){/* Modern paths can finish just outside the original pickup radius. Walk the final fraction of a block. */g.getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),1);}else if(g.tickCount%10==0)g.getNavigation().moveTo(target,1);}
            else if(g.distanceToSqr(g.home.getX()+.5,g.home.getY(),g.home.getZ()+.5)>4&&g.tickCount%20==0)g.getNavigation().moveTo(g.home.getX()+.5,g.home.getY(),g.home.getZ()+.5,1);
        }
    }
}
