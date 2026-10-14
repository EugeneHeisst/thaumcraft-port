package dev.eugheist.tc4modern;

/** Literal EnumGolemType values from TC4 4.2.3.5. Ordinals remain the original NBT material IDs. */
public enum GolemMaterial {
 STRAW(10,0,.38f,false,1,1,75,0),WOOD(20,6,.35f,false,1,4,75,1),
 TALLOW(20,9,.33f,false,2,8,75,2),CLAY(25,9,.33f,true,1,8,100,2),
 FLESH(15,6,.35f,false,2,4,40,1),STONE(30,12,.32f,true,1,16,100,3),
 IRON(35,15,.31f,true,1,32,125,4),THAUMIUM(40,15,.32f,true,2,32,100,4);
 public final int health,armor,upgrades,carry,regenDelay,strength;public final float speed;public final boolean fireResist;
 GolemMaterial(int health,int armor,float speed,boolean fireResist,int upgrades,int carry,int regenDelay,int strength){this.health=health;this.armor=armor;this.speed=speed;this.fireResist=fireResist;this.upgrades=upgrades;this.carry=carry;this.regenDelay=regenDelay;this.strength=strength;}
 public String id(){return name().toLowerCase(java.util.Locale.ROOT);}
 public boolean heavy(){return this==STONE||this==IRON||this==THAUMIUM;}
 public static GolemMaterial byId(int id){return id>=0&&id<values().length?values()[id]:STRAW;}
}
