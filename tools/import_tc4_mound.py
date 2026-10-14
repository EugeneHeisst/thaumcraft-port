#!/usr/bin/env python3
"""Extract every literal WorldGenMound placement, preserving legacy order and metadata."""
import argparse
import csv
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path


def extract(source, mappings):
    text = source.read_text()
    body = text.split('        world.func_147449_b(', 1)[1]
    body = '        world.func_147449_b(' + body.split('        ChestGenHooks.getInfo', 1)[0]
    pattern = re.compile(r'world\.func_(147449_b|147465_d)\(i \+ (\d+), j \+ (\d+), k \+ (\d+), (?:\(Block\))?Blocks\.(field_\w+)(?:, (\d+), 3)?\);')
    body = re.sub(r'/\*.*?\*/', '', body, flags=re.S)
    palette, blocks = [], []
    for line in body.splitlines():
        if not line.strip() or line.strip() in {'this.generate2(world, rand, i, j, k);', 'return true;', '}', 'public boolean generate2(World world, Random rand, int i, int j, int k) {'}:
            continue
        match = pattern.fullmatch(line.strip())
        if not match:
            raise ValueError(f'Unparsed original geometry: {line}')
        method, x, y, z, field, metadata = match.groups()
        if (method == '147465_d') != (metadata is not None):
            raise ValueError(f'Incomplete metadata: {line}')
        state = [mappings[field], int(metadata or 0)]
        if state not in palette:
            palette.append(state)
        blocks.append([int(x), int(y), int(z), palette.index(state)])
    assert len(blocks) == 2441, len(blocks)
    return {'source': 'Thaumcraft 4.2.3.5 WorldGenMound', 'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(), 'placement_count': len(blocks), 'palette': palette, 'blocks': blocks}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('mappings', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    with zipfile.ZipFile(args.mappings) as archive:
        mappings = {row['searge']: row['name'] for row in csv.DictReader(io.TextIOWrapper(archive.open('fields.csv')))}
    result = extract(args.source, mappings)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, separators=(',', ':')) + '\n')
    print(f"Extracted {len(result['blocks'])} placements, {len(result['palette'])} legacy states; source {result['source_sha256']}")


if __name__ == '__main__':
    main()
