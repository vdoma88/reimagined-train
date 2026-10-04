"""Render the same registered paths as LayeredCharacter to an SVG contact sheet.

Usage: python tools/preview_character.py --output /tmp/characters.svg
SVG is for review only; Android continues using the JSON asset offline.
"""
import argparse
import json
import math
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
PALETTE = dict(skin='#f3d0b9', skinShadow='#d8a18f', skinLight='#f9e5d8',
    hair='#654337', hairShadow='#46303b', hairLight='#99715a',
    cloth='#344663', clothShadow='#29384f', clothLight='#5d6b82',
    iris='#478ca9', irisShadow='#28445c', irisLight='#95d9e3',
    ink='#292431', paper='#fff8ec', white='#ffffff', gold='#d8b976',
    leather='#755346', accent='#c7758d', lip='#b97677', blush='#e4a199')

def validate(layers):
    required = ['body', 'shoes', 'blush', 'freckles', 'eyes.closed']
    for category, count in [('back', 14), ('front', 14), ('outfit', 12),
                            ('face', 3), ('eyes', 4), ('brows', 3), ('mouth', 3), ('accessory', 5)]:
        required.extend(f'{category}.{i}' for i in range(count))
    assert set(required) <= layers.keys(), 'Missing registered part'
    for key, pieces in layers.items():
        for p in pieces:
            assert p['path'].strip(), f'{key}: empty path'
            for color in ['fill', 'stroke']:
                assert p.get(color, 'none' if color == 'fill' else 'ink') in {*PALETTE, 'none'}, f'{key}: unknown color'
            for number in ['width', 'tx', 'ty', 'sx', 'sy', 'opacity']:
                assert math.isfinite(p.get(number, 1)), f'{key}: invalid {number}'
            assert 0 <= p.get('opacity', 1) <= 1, f'{key}: invalid opacity'
            assert p.get('sy', 1) > 0, f'{key}: flipped anatomy'

def pieces_svg(pieces, prefix):
    result = []
    for i, p in enumerate(pieces):
        fill = PALETTE.get(p.get('fill', 'none'), p.get('fill', 'none'))
        stroke = PALETTE.get(p.get('stroke', 'ink'), p.get('stroke', 'ink'))
        gradient = ''
        if p.get('fill') in ('hair', 'cloth'):
            ident = f'{prefix}_g{i}'
            gradient = f'<defs><linearGradient id="{ident}" x2="1" y2="1"><stop stop-color="{fill}"/><stop offset="1" stop-color="{PALETTE[p["fill"]+"Shadow"]}"/></linearGradient></defs>'
            fill = f'url(#{ident})'
        clip = ''
        if p.get('clip'):
            ident = f'{prefix}_c{i}'
            result.append(f'<defs><clipPath id="{ident}"><path d="{escape(p["clip"])}"/></clipPath></defs>')
            clip = f' clip-path="url(#{ident})"'
        result.append(f'<g transform="translate({p.get("tx",0)} {p.get("ty",0)}) scale({p.get("sx",1)} {p.get("sy",1)})">{gradient}<path d="{escape(p["path"])}" fill="{fill}" stroke="{stroke}" stroke-width="{p.get("width",1.5)}" opacity="{p.get("opacity",1)}" stroke-linecap="round" stroke-linejoin="round"{clip}/></g>')
    return ''.join(result)

def character_svg(layers, hair=0, outfit=0, face=0, eyes=0, prefix='a'):
    keys = [f'back.{hair}', 'body', f'outfit.{outfit}', 'shoes', f'face.{face}',
            f'eyes.{eyes}', 'brows.0', 'mouth.0', 'blush', f'front.{hair}', 'accessory.0']
    return ''.join(pieces_svg(layers[key], prefix+str(i)) for i,key in enumerate(keys))

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True)
    parser.add_argument('--asset', default=str(ROOT/'app/src/main/assets/character_layers.json'))
    args = parser.parse_args()
    layers = json.loads(Path(args.asset).read_text())
    validate(layers)
    # Every hairstyle and outfit plus all face/eye variants are shown together.
    out = ['<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="1760" viewBox="0 0 1600 1760"><rect width="1600" height="1760" fill="#f4f0ec"/>']
    for i in range(16):
        x,y = (i%8)*200, (i//8)*880
        out.append(f'<g transform="translate({x} {y+28}) scale(.5 .5)">{character_svg(layers,i%14,i%12,i%3,i%4,str(i))}</g>')
        out.append(f'<text x="{x+12}" y="{y+650}" font-size="14" fill="#292431">Hair {i%14} / Outfit {i%12}</text>')
        out.append(f'<svg x="{x}" y="{y+675}" width="200" height="190" viewBox="125 65 150 145">{character_svg(layers,i%14,i%12,i%3,i%4,"p"+str(i))}</svg>')
    out.append('</svg>')
    Path(args.output).write_text(''.join(out))

if __name__ == '__main__':
    main()
