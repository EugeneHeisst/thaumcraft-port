import math, json, sys
def rx(a):
    c,s=math.cos(a),math.sin(a); return [[1,0,0],[0,c,-s],[0,s,c]]
def ry(a):
    c,s=math.cos(a),math.sin(a); return [[c,0,s],[0,1,0],[-s,0,c]]
def rz(a):
    c,s=math.cos(a),math.sin(a); return [[c,-s,0],[s,c,0],[0,0,1]]
def mm(a,b): return [[sum(a[i][k]*b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def mv(a,v): return [sum(a[i][k]*v[k] for k in range(3)) for i in range(3)]
def R(r): x,y,z=[math.radians(v) for v in r]; return mm(mm(rx(x),ry(y)),rz(z))
# vanilla block display, with original extra 90deg Y model-space rotation from BlockTableRenderer.renderInventoryBlock
base = {
 'gui': ([30,225,0],.625),
 'ground': ([0,0,0],.25),
 'fixed': ([0,0,0],.5),
 'thirdperson_righthand': ([75,45,0],.375),
 'thirdperson_lefthand': ([75,45,0],.375),
 'firstperson_righthand': ([0,45,0],.4),
 'firstperson_lefthand': ([0,225,0],.4),
}
base_t={'ground':[0,3,0],'thirdperson_righthand':[0,2.5,0],'thirdperson_lefthand':[0,2.5,0]}
def build(center_offset, scale_factor, extra_y=90):
    out={}
    for k,(rot,s) in base.items():
        rot=[rot[0],rot[1]+extra_y,rot[2]]
        s2=s*scale_factor
        off=mv(R(rot),[c*s2 for c in center_offset])
        t=[base_t.get(k,[0,0,0])[i]-off[i]*16 for i in range(3)]
        out[k]={'rotation':[round(v,3) for v in rot],'translation':[round(v,4) for v in t],'scale':[s2]*3}
    return out
which=sys.argv[1]
if which=='table': d=build([0,0,0],1)
else: d=build([0.5,0,0],float(sys.argv[2]))
print(json.dumps(d))
