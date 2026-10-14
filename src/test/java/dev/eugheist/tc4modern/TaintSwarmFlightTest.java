package dev.eugheist.tc4modern;
import thaumcraft.common.entities.monster.TaintSwarmFlight;
public final class TaintSwarmFlightTest {
    static void require(boolean result,String message){if(!result)throw new AssertionError(message);}
    public static void main(String[] args){
        var zero=new java.util.Random(){public int nextInt(int bound){return 0;}};
        var target=TaintSwarmFlight.choose(-.9,12.9,-.9,zero);
        require(target.x()==0&&target.y()==10&&target.z()==0,"legacy target uses truncation, not floor");
        require(TaintSwarmFlight.summoned((byte)3,false)==0,"legacy clearing summoned also clears flag bit zero");
        require(TaintSwarmFlight.summoned((byte)4,true)==6,"summoning retains other flags");
        require(!TaintSwarmFlight.canAttack(0,3,2,0,2,0),"attack range is strictly less than three");
        require(!TaintSwarmFlight.canAttack(0,2,4,2,2,0),"touching vertical bounds is not overlap");
        require(TaintSwarmFlight.canAttack(0,2,3,1,2,0),"overlapping target in range");
        var still=TaintSwarmFlight.steer(0,0,0,0,0,0,true);
        require(still.x()==0&&still.y()==0&&still.z()==0,"aligned velocity remains zero");
        var pursuit=TaintSwarmFlight.steer(0,0,0,1,1,1,true);
        require(pursuit.x()>pursuit.z(),"original pursuit X and Z floating-point coefficients differ");
        var roaming=TaintSwarmFlight.steer(0,0,0,1,1,1,false);
        require(roaming.x()==roaming.z()&&roaming.x()<pursuit.x(),"roaming is symmetric and slower than pursuit");
        var escape=thaumcraft.common.entities.LegacyBlockEscape.escape((x,y,z)->true,true,.5,.01,.5,1,2,3,new java.util.Random(){public float nextFloat(){return 0;}});
        require(escape.x()==1&&escape.y()==(double).1f&&escape.z()==3,"fully enclosed legacy entity still selects up, never nearest down");
        var free=thaumcraft.common.entities.LegacyBlockEscape.escape((x,y,z)->false,false,.5,.5,.5,1,2,3,new java.util.Random(){public float nextFloat(){throw new AssertionError("unblocked escape consumed randomness");}});
        require(free.x()==1&&free.y()==2&&free.z()==3,"unblocked entity retains movement");
        verifyParticleEscape();
        System.out.println("Original taint swarm flight and attack checks passed");
    }
    private static void verifyParticleEscape(){
        var particle=new thaumcraft.client.fx.particles.SwarmEscape();
        var random=new java.util.Random(){public float nextFloat(){return 0;}};
        var dry=new thaumcraft.client.fx.particles.SwarmEscape.World(){
            public boolean fibres(int x,int y,int z){return false;}
            public boolean air(int x,int y,int z){return false;}
            public boolean liquidInBounds(){return false;}
            public boolean opaqueNormalCube(int x,int y,int z){return false;}
        };
        require(particle.pushOutOfBlocks(.5,.01,.5,dry,random),"particle inside dry block attempts escape");
        require(particle.vy==-(double).025f&&particle.vx==0&&particle.vz==0,"particle selects nearest down unlike entity escape");
        var enclosed=new thaumcraft.client.fx.particles.SwarmEscape.World(){
            public boolean fibres(int x,int y,int z){return false;}
            public boolean air(int x,int y,int z){return false;}
            public boolean liquidInBounds(){return false;}
            public boolean opaqueNormalCube(int x,int y,int z){return true;}
        };
        particle.vx=1;particle.vy=2;particle.vz=3;
        require(particle.pushOutOfBlocks(.5,.5,.5,enclosed,random),"enclosed particle reports attempted escape");
        require(particle.vx==1&&particle.vy==2&&particle.vz==3,"enclosed particle keeps velocity; no default upward escape");
        var wet=new thaumcraft.client.fx.particles.SwarmEscape.World(){
            public boolean fibres(int x,int y,int z){return false;}
            public boolean air(int x,int y,int z){return false;}
            public boolean liquidInBounds(){return true;}
            public boolean opaqueNormalCube(int x,int y,int z){throw new AssertionError("liquid particle checked escape neighbors");}
        };
        require(!particle.pushOutOfBlocks(.5,.5,.5,wet,new java.util.Random(){public float nextFloat(){throw new AssertionError("liquid escape consumed random");}}),"liquid touching particle suppresses escape");
    }
}
