package thaumcraft.client.fx.beams;
/** Original FXBeamPower age extension, pulse opacity and initial neutral colour. */
public final class BeamPowerState {
 public int age,lifetime=8;
 public boolean dead;
 public float red=.5f,green=.5f,blue=.5f,opacity=.3f;
 public void update(boolean pulse,float r,float g,float b){
  while(lifetime-age<4)lifetime++;
  red=r;green=g;blue=b;
  if(pulse)opacity=.8f;
 }
 public void tick(){
  if(opacity>.3f)opacity-=.025f;
  if(opacity<.3f)opacity=.3f;
  if(age++>=lifetime)dead=true;
 }
 public int flareFrame(){return age%16;}
}
