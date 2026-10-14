"""Import the original inventory crystal arrangement into book-only meshes."""
import math
import json
import io
from PIL import Image
from convert_tc4_model import convert

class Random:
    def __init__(self, seed): self.seed=(seed ^ 0x5DEECE66D)&((1<<48)-1)
    def bits(self, n):
        self.seed=(self.seed*0x5DEECE66D+11)&((1<<48)-1)
        return self.seed>>(48-n)
    def float(self): return self.bits(24)/(1<<24)
    def integer(self,n):
        if n&(n-1)==0:return (n*self.bits(31))>>31
        while True:
            b=self.bits(31);v=b%n
            if b-v+n-1 < 1<<31:return v

def import_crystals(pack, source, reference, display):
    model=reference/'decompiled/thaumcraft/client/renderers/models/ModelCrystal.java'
    base=convert(model.read_text(),'tc4modern:models/crystal',origin_y=0)
    # Original BlockCustomOreItem palette, indexed by metadata + 1.
    colors=(0xffff7e,16727041,37119,40960,0xeeccff,0x555577)
    original=Image.open(io.BytesIO(source.read('assets/thaumcraft/textures/models/crystal.png'))).convert('RGBA')
    for md in range(7):
        rand=Random(md);out=['mtllib cluster_%d.mtl'%md];vertex_offset=0
        for a in range(6):
            if a==0: ry=(rand.float()-rand.float())*5;rx=(rand.float()-rand.float())*5;size=1.1
            else: ry=rand.integer(36)+72*a;rx=15+rand.integer(15);size=.8
            # drawCrystal consumes shade RNG before the three scale draws.
            rand.integer(10);rand.float()
            sx=(.15+rand.float()*.075)*size;sy=(.5+rand.float()*.1)*size;sz=(.15+rand.float()*.05)*size
            color=colors[md if md<6 else (4 if a==0 else 5 if a==5 else a-1)]
            material='crystal_%06x'%color;out.append('usemtl '+material)
            for line in base.splitlines():
                if line.startswith('v '):
                    x,y,z=map(float,line.split()[1:]);x-=.5;y=-y;z=.5-z;x*=sx;y*=sy;z*=sz
                    angle=math.radians(rx);y,z=y*math.cos(angle)-z*math.sin(angle),y*math.sin(angle)+z*math.cos(angle)
                    angle=math.radians(ry);x,z=x*math.cos(angle)+z*math.sin(angle),-x*math.sin(angle)+z*math.cos(angle)
                    out.append('v %.8f %.8f %.8f'%(x+.5,y-.3,z+.5))
                elif line.startswith('f '):out.append('f '+' '.join('/'.join(str(int(c)+vertex_offset) if c else '' for c in v.split('/')) for v in line[2:].split()))
                elif line.startswith(('vt ','vn ')):out.append(line)
            vertex_offset+=sum(line.startswith('v ') for line in base.splitlines())
        materials=[]
        for color in set(colors):
            texture='book_crystal_%06x'%color;img=original.copy();r,g,b=(color>>16&255)/220,(color>>8&255)/220,(color&255)/220
            img.putdata([(min(255,int(cr*r)),min(255,int(cg*g)),min(255,int(cb*b)),ca) for cr,cg,cb,ca in original.getdata()]);buf=io.BytesIO();img.save(buf,format='PNG')
            pack.writestr('assets/tc4modern/textures/models/'+texture+'.png',buf.getvalue()) if md==0 else None
            materials.append('newmtl crystal_%06x\nKd 1 1 1\nmap_Kd tc4modern:models/%s\n'%(color,texture))
        pack.writestr('assets/tc4modern/models/book/cluster_%d.obj'%md,'\n'.join(out))
        pack.writestr('assets/tc4modern/models/book/cluster_%d.mtl'%md,'\n'.join(materials))
        # Block base rests on its attachment face; inventory/book retains the old translation.
        block=[]
        for line in out:
            if line.startswith('v '):
                x,y,z=map(float,line.split()[1:]);line='v %.8f %.8f %.8f'%(x,y+.3,z)
            block.append(line)
        pack.writestr('assets/tc4modern/models/block/cluster_%d.obj'%md,'\n'.join(block))
        pack.writestr('assets/tc4modern/models/block/cluster_%d.mtl'%md,'\n'.join(materials))
        pack.writestr('assets/tc4modern/models/book/cluster_%d.json'%md,json.dumps({'loader':'neoforge:obj','model':'tc4modern:models/book/cluster_%d.obj'%md,'automatic_culling':False,'flip_v':True,'ambientocclusion':False,'display':display}))
