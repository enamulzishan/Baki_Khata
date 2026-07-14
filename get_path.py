from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
import os

fonts = [
    'C:/Windows/Fonts/NirmalaB.ttf',
    'C:/Windows/Fonts/nirmalab.ttf',
    'C:/Windows/Fonts/vrindab.ttf',
    'C:/Windows/Fonts/segoeuib.ttf'
]

font_path = None
for f in fonts:
    if os.path.exists(f):
        font_path = f
        break

if font_path:
    font = TTFont(font_path)
    cmap = font.getBestCmap()
    char = '?'
    if ord(char) in cmap:
        glyph_name = cmap[ord(char)]
        glyph_set = font.getGlyphSet()
        pen = SVGPathPen(glyph_set)
        glyph = glyph_set[glyph_name]
        glyph.draw(pen)
        print("PATH:", pen.getCommands())
    else:
        print("Character not found in font.")
else:
    print("No bold font found.")
