#!/usr/bin/env python3
"""Convert original ModelRenderer cuboids to an OBJ without embedding model data."""
import argparse
import math
import re
from pathlib import Path


def convert(text, texture, *, materials=None, exclude=(), origin_y=1, cap_scale=1):
    width = int(re.search(r'field_78090_t = (\d+)', text)[1])
    height = int(re.search(r'field_78089_u = (\d+)', text)[1])
    out = ['mtllib research_table.mtl']
    vertex = 1
    pattern = r'this\.(\w+) = new ModelRenderer\(\(ModelBase\)this, (\d+), (\d+)\);'
    for match in re.finditer(pattern, text):
        name, u, v = match.groups()
        if name.startswith('Scroll') or name == 'Inkwell' or name in exclude: continue  # Conditional research-note renderer is a separate mesh.
        section = text[match.end():]
        section = section.split('new ModelRenderer', 1)[0]
        box = re.search(r'func_78789_a\(([^;]+)\);', section)
        pivot = re.search(r'func_78793_a\(([^;]+)\);', section)
        if not box or not pivot: raise ValueError('Unsupported model part: ' + name)
        numbers = lambda s: [float(x.strip().rstrip('f')) for x in s.split(',')]
        x, y, z, dx, dy, dz = numbers(box[1])
        px, py, pz = numbers(pivot[1])
        rotation = re.search(r'this\.setRotation\(this\.' + re.escape(name) + r', ([^;]+)\);', section)
        rx, ry, rz = numbers(rotation[1]) if rotation else (0, 0, 0)
        mirror_assignment = re.findall(r'this\.' + re.escape(name) + r'\.field_78809_i = (true|false);', section[:box.start()])
        mirrored = bool(mirror_assignment and mirror_assignment[-1] == 'true')
        u, v = float(u), float(v)
        # Standard ModelBox unfolded UV layout. Geometry is transformed exactly as the old tile renderer:
        # translate(.5, 1, .5), rotate 180 degrees around X, render at 1/16 scale.
        coords = [(x,y,z),(x+dx,y,z),(x+dx,y+dy,z),(x,y+dy,z),
                  (x,y,z+dz),(x+dx,y,z+dz),(x+dx,y+dy,z+dz),(x,y+dy,z+dz)]
        if mirrored:
            coords = [(2*x+dx-cx, cy, cz) for cx, cy, cz in coords]
        faces = [([5,1,2,6],(u+dz+dx,v+dz,u+dz+dx+dz,v+dz+dy)),
                 ([0,4,7,3],(u,v+dz,u+dz,v+dz+dy)),
                 ([5,4,0,1],(u+dz,v,u+dz+dx,v+dz)),
                 ([2,3,7,6],(u+dz+dx,v+dz,u+dz+dx+dx,v)),
                 ([1,0,3,2],(u+dz,v+dz,u+dz+dx,v+dz+dy)),
                 ([4,5,6,7],(u+dz+dx+dz,v+dz,u+dz+dx+dz+dx,v+dz+dy))]
        out.append('o ' + name)
        out.append('usemtl ' + (materials or {}).get(name, 'table'))
        for indices, rect in faces:
            a,b,c,d = rect
            uv = [(c,b),(a,b),(a,d),(c,d)]
            vertices = list(zip(indices, uv))
            if mirrored: vertices.reverse()  # ModelBox swaps X bounds then TexturedQuad flips winding.
            for i,(tu,tv) in vertices:
                cx,cy,cz=coords[i]
                # ModelRenderer applies local X, Y, Z rotations before its pivot translation.
                cy,cz = cy*math.cos(rx)-cz*math.sin(rx), cy*math.sin(rx)+cz*math.cos(rx)
                cx,cz = cx*math.cos(ry)+cz*math.sin(ry), -cx*math.sin(ry)+cz*math.cos(ry)
                cx,cy = cx*math.cos(rz)-cy*math.sin(rz), cx*math.sin(rz)+cy*math.cos(rz)
                cx += px; cy += py; cz += pz
                scale = cap_scale if name.startswith('Cap') else 1
                out.append(f'v {0.5+cx*scale/16:.6f} {origin_y-cy/16:.6f} {0.5-cz*scale/16:.6f}')
                out.append(f'vt {tu/width:.6f} {1-tv/height:.6f}')
            out.append('f ' + ' '.join(f'{i}/{i}' for i in range(vertex,vertex+4)))
            vertex += 4
    return '\n'.join(out)+'\n'

if __name__ == '__main__':
    p=argparse.ArgumentParser()
    p.add_argument('source',type=Path);p.add_argument('output',type=Path)
    args=p.parse_args()
    args.output.write_text(convert(args.source.read_text(),'tc4modern:models/restable'))
