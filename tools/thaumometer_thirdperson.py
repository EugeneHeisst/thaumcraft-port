import numpy as np, math, json
def T(x,y,z): m=np.eye(4); m[:3,3]=[x,y,z]; return m
def S(x,y,z): return np.diag([x,y,z,1.0])
def R(axis,deg):
    a=math.radians(deg); c,s=math.cos(a),math.sin(a); m=np.eye(4)
    if axis=='x': m[1,1]=c;m[1,2]=-s;m[2,1]=s;m[2,2]=c
    if axis=='y': m[0,0]=c;m[0,2]=s;m[2,0]=-s;m[2,2]=c
    if axis=='z': m[0,0]=c;m[0,1]=-s;m[1,0]=s;m[1,1]=c
    return m
A=R('x',-90)@R('y',180)@T(1/16,.125,-.625)
O=T(-1/16,7/16,1/16)@T(0,3/16,-5/16)@R('x',20)@R('y',45)@S(-.375,-.375,.375)@T(-.5,-.5,-.5)@S(.5,.5,.5)@T(1.6,.3,2)@R('x',-90)@R('z',-30)
G=np.eye(4); G[:3,:3]=[[.25,0,0],[0,0,.25],[0,-.25,0]]
Ginv=np.linalg.inv(G)
D=np.linalg.inv(A)@O@Ginv@T(-.5,-.5,-.525)@T(.5,.5,.5)
L=D[:3,:3]; s=np.cbrt(np.linalg.det(L)); Rm=L/s
print('scale',s,'orth err',np.abs(Rm@Rm.T-np.eye(3)).max())
# euler XYZ with R = Rx*Ry*Rz
y=math.asin(max(-1,min(1,Rm[0,2])))
x=math.atan2(-Rm[1,2],Rm[2,2]); z=math.atan2(-Rm[0,1],Rm[0,0])
rot=[math.degrees(v) for v in (x,y,z)]
chk=R('x',rot[0])@R('y',rot[1])@R('z',rot[2])
print('euler err',np.abs(chk[:3,:3]-Rm).max())
t=D[:3,3]*16
print(json.dumps({'rotation':[round(v,3) for v in rot],'translation':[round(v,4) for v in t],'scale':[round(s,5)]*3}))
