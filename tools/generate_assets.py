#!/usr/bin/env python3
"""Generate original deterministic supporter textures and sound effects."""
from pathlib import Path
import json, math, random, shutil, struct, subprocess, sys, wave
from PIL import Image, ImageDraw

OUT = Path(sys.argv[1])
for folder in ("block", "item", "entity"):
    (OUT / folder).mkdir(parents=True, exist_ok=True)

FONT = {
    "U": ["10001","10001","10001","10001","10001","10001","01110"],
    "L": ["10000","10000","10000","10000","10000","10000","11111"],
    "T": ["11111","00100","00100","00100","00100","00100","00100"],
    "R": ["11110","10001","10001","11110","10100","10010","10001"],
    "A": ["01110","10001","10001","11111","10001","10001","10001"],
    "S": ["01111","10000","10000","01110","00001","00001","11110"],
    "N": ["10001","11001","11001","10101","10011","10011","10001"],
    "O": ["01110","10001","10001","10001","10001","10001","01110"],
    "F": ["11111","10000","10000","11110","10000","10000","10000"],
    "E": ["11111","10000","10000","11110","10000","10000","11111"],
    " ": ["00000"] * 7,
}

def bitmap_text(draw, text, center_x, y, fill, outline):
    scale = 1
    width = len(text) * 6 * scale - scale
    x0 = int(center_x - width / 2)
    for ci, char in enumerate(text):
        for yy, row in enumerate(FONT.get(char, FONT[" "])):
            for xx, bit in enumerate(row):
                if bit == "1":
                    x = x0 + ci * 6 + xx
                    py = y + yy
                    draw.rectangle((x-1,py-1,x+1,py+1),fill=outline)
                    draw.point((x,py),fill=fill)

def flag_frame(kind, frame):
    w = h = 64
    img = Image.new("RGBA",(w,h),(0,0,0,255))
    px = img.load()
    phase = frame * math.pi / 2
    for y in range(h):
        for x in range(w):
            wave = math.sin(x/5.7+phase)*2.3 + math.sin(x/12.5-phase*.7)*.8
            fold = math.sin(x/6.5+phase)*8 + math.cos(x/3.5-phase)*3
            grain = ((x*23+y*17+x*y*3)%9)-4
            if kind == "flag":
                if y < 4 or y > 59: base=(10,25,16)
                elif 24+wave < y < 39+wave: base=(238,239,224)
                elif 22+wave < y < 24+wave or 39+wave < y < 42+wave: base=(12,24,15)
                else: base=(19,119,62)
            else:
                if y < 3 or y > 60: base=(10,15,12)
                elif y < 15 or y > 49: base=(18,102,53)
                elif y < 19 or y > 45: base=(232,233,218)
                else: base=(17,22,19)
            shade = fold + (31-y)*.09 + grain
            px[x,y] = tuple(max(0,min(255,int(c+shade))) for c in base) + (255,)
    d = ImageDraw.Draw(img)
    for y in range(7,58,3):
        d.point((2,y),fill=(203,215,198,255))
        d.point((61,y),fill=(7,28,16,255))
    cx,cy=32,(24 if kind=="flag" else 31)
    shield=[(cx-12,cy-10),(cx+12,cy-10),(cx+11,cy+3),(cx,cy+14),(cx-11,cy+3)]
    d.polygon(shield,fill=(8,29,17,255),outline=(246,246,229,255),width=2)
    d.polygon([(cx-8,cy-7),(cx+8,cy-7),(cx+7,cy+2),(cx,cy+9),(cx-7,cy+2)],fill=(24,145,76,255))
    star=[]
    for i in range(10):
        r=7 if i%2==0 else 3
        a=-math.pi/2+i*math.pi/5
        star.append((cx+math.cos(a)*r,cy-1+math.sin(a)*r))
    d.polygon(star,fill=(255,255,238,255))
    if kind=="flag": bitmap_text(d,"ULTRAS",32,48,(248,249,235,255),(4,30,16,255))
    else: bitmap_text(d,"NO FEAR",32,49,(245,245,231,255),(3,18,11,255))
    sheen=Image.new("RGBA",(w,h),(0,0,0,0)); sd=ImageDraw.Draw(sheen)
    for x in range(w):
        alpha=int(10+13*(.5+.5*math.sin(x/5+phase)))
        sd.line((x,0,x+int(math.sin(x/8+phase)),h),fill=(255,255,255,alpha))
    return Image.alpha_composite(img,sheen)

for kind in ("flag","banner"):
    sheet=Image.new("RGBA",(64,256))
    for frame in range(4):
        sheet.paste(flag_frame(kind,frame),(0,frame*64))
    sheet.save(OUT/"block"/f"{kind}_cloth.png",optimize=True)

def icon():
    return Image.new("RGBA",(64,64),(0,0,0,0))
def save(name,img):
    img.save(OUT/"item"/f"{name}.png",optimize=True)

im=icon(); d=ImageDraw.Draw(im)
d.polygon([(6,22),(43,9),(59,15),(59,43),(43,50),(6,38)],fill=(224,234,226),outline=(23,38,29),width=3)
d.polygon([(10,24),(43,14),(54,18),(54,40),(43,45),(10,36)],fill=(25,141,75),outline=(249,248,229),width=2)
d.polygon([(6,22),(18,20),(18,40),(6,38)],fill=(20,30,23),outline=(235,238,221),width=2)
d.polygon([(27,43),(40,43),(36,61),(24,61)],fill=(48,54,48),outline=(13,20,15),width=2)
d.line((21,27,43,20),fill=(255,255,245),width=2); save("megaphone",im)

im=icon(); d=ImageDraw.Draw(im)
d.ellipse((8,8,56,30),fill=(26,32,27),outline=(12,17,13),width=3)
d.polygon([(8,19),(56,19),(53,47),(11,47)],fill=(25,119,62),outline=(12,20,14),width=3)
d.ellipse((8,36,56,57),fill=(225,231,217),outline=(14,23,16),width=3)
d.ellipse((14,40,50,53),fill=(247,247,234),outline=(90,104,89),width=1)
d.line((10,18,54,18),fill=(245,245,228),width=2)
for x in range(14,55,7): d.line((x,22,x-1,39),fill=(205,217,199),width=1)
d.line((52,8,59,55),fill=(36,39,34),width=4); save("supporter_drum",im)

im=icon(); d=ImageDraw.Draw(im)
d.rounded_rectangle((13,15,26,56),radius=5,fill=(22,137,72),outline=(11,29,17),width=2)
d.rounded_rectangle((37,10,51,51),radius=5,fill=(235,238,222),outline=(21,34,25),width=2)
for y in (25,33,41):
    d.line((17,y,23,y-1),fill=(255,255,239),width=2)
    d.line((41,y-3,47,y-4),fill=(24,137,72),width=2)
d.ellipse((14,9,26,21),fill=(38,159,85),outline=(10,28,17),width=2)
d.ellipse((38,4,51,17),fill=(245,246,228),outline=(18,32,23),width=2); save("clapper",im)

im=icon(); d=ImageDraw.Draw(im)
d.rounded_rectangle((5,20,59,43),radius=9,fill=(12,25,16),outline=(6,15,9),width=3)
d.rounded_rectangle((8,23,56,40),radius=6,fill=(26,143,74),outline=(232,239,221),width=2)
for x in range(13,57,9): d.rectangle((x,24,x+3,39),fill=(237,241,224))
d.rounded_rectangle((17,35,38,61),radius=5,fill=(18,104,53),outline=(7,22,12),width=2)
d.line((22,39,22,57),fill=(247,245,222),width=3); d.line((30,39,30,57),fill=(9,30,15),width=2)
for x in (20,26,32,38): d.line((x,58,x-2,63),fill=(229,234,217),width=2)
save("supporter_scarf",im)

im=icon(); d=ImageDraw.Draw(im)
d.rounded_rectangle((6,7,58,58),radius=15,fill=(122,76,41),outline=(47,29,17),width=3)
d.ellipse((22,38,46,62),fill=(0,0,0,0))
for x,y,c in [(19,19,(24,143,73)),(32,14,(238,238,219)),(46,21,(34,105,173)),(19,34,(198,39,47)),(44,36,(239,186,43))]:
    d.ellipse((x-5,y-5,x+5,y+5),fill=c,outline=(51,37,25),width=1)
save("design_palette",im)

im=icon(); d=ImageDraw.Draw(im)
d.ellipse((8,28,36,59),fill=(158,20,35),outline=(77,11,19),width=2)
d.ellipse((20,12,48,51),fill=(218,37,49),outline=(90,15,24),width=2)
d.ellipse((36,23,61,54),fill=(179,27,43),outline=(81,14,24),width=2)
d.ellipse((23,26,49,55),fill=(235,68,57),outline=(106,18,25),width=1)
d.arc((18,12,55,56),190,315,fill=(255,193,99),width=3)
for x,y in ((6,16),(51,8),(58,20),(4,49)): d.ellipse((x,y,x+4,y+4),fill=(255,183,91))
save("red_supporter_smoke",im)

im=icon(); d=ImageDraw.Draw(im)
d.rounded_rectangle((11,5,16,60),radius=2,fill=(122,132,119),outline=(26,38,29),width=2)
d.ellipse((10,2,17,9),fill=(235,239,221),outline=(27,38,28),width=1)
d.polygon([(16,8),(60,10),(57,43),(16,40)],fill=(22,132,68),outline=(8,29,16),width=2)
d.line((19,13,53,15),fill=(248,249,229),width=2)
d.polygon([(20,23),(55,24),(53,29),(20,28)],fill=(240,242,226))
d.ellipse((31,26,44,38),fill=(9,44,23),outline=(246,247,232),width=1)
d.line((34,35,40,35),fill=(255,255,238),width=2); save("supporter_flag",im)

teams={"green":(23,135,70),"blue":(31,106,178),"red":(194,37,46),"yellow":(235,187,39),
       "black":(35,40,37),"white":(228,231,218),"purple":(126,67,157),"orange":(224,112,28)}
for name,colour in teams.items():
    im=icon(); d=ImageDraw.Draw(im)
    d.ellipse((13,6,53,60),fill=(238,234,214),outline=(24,32,26),width=3)
    d.ellipse((17,10,49,56),fill=colour,outline=(250,249,232),width=2)
    d.polygon([(20,27),(47,21),(46,31),(20,38)],fill=(248,247,226))
    d.ellipse((27,34,39,46),fill=(12,29,18),outline=(251,248,226),width=1)
    d.line((23,13,31,10),fill=(255,255,255),width=2)
    save(f"ultras_fan_spawn_egg_{name}",im)

# Zombie-compatible 64x64 skin atlas with a human face and green supporter shirt.
im=Image.new("RGBA",(64,64),(0,0,0,0)); d=ImageDraw.Draw(im)
skin=(181,140,108); hair=(39,34,29); green=(22,127,65); pale=(236,239,222); dark=(18,39,26)
for box in ((8,0,16,8),(0,8,8,16),(8,8,16,16),(16,8,24,16),(24,8,32,16),(16,0,24,8)):
    d.rectangle(box,fill=skin)
d.rectangle((8,0,16,3),fill=hair); d.rectangle((0,8,2,15),fill=hair); d.rectangle((24,8,26,15),fill=hair)
for x in (9,13):
    d.rectangle((x,10,x+1,11),fill=(246,244,225)); d.point((x+1,11),fill=(28,37,30))
d.line((8,9,11,9),fill=hair); d.line((13,9,16,9),fill=hair); d.line((10,14,14,14),fill=(100,55,45))
for box in ((20,20,28,32),(16,20,20,32),(28,20,32,32),(32,20,40,32)):
    d.rectangle(box,fill=green)
d.polygon(((23,20),(25,20),(24,24)),fill=pale); d.rectangle((20,26,28,27),fill=pale); d.rectangle((20,30,28,31),fill=dark)
for box in ((44,20,48,32),(40,20,44,32),(48,20,52,32),(52,20,56,32),(36,52,40,64),(40,52,44,64),(44,52,48,64),(48,52,52,64)):
    d.rectangle(box,fill=skin)
d.rectangle((44,20,48,25),fill=green); d.rectangle((40,20,44,25),fill=green); d.rectangle((44,25,48,27),fill=pale)
d.rectangle((52,20,56,25),fill=green); d.rectangle((48,20,52,25),fill=green); d.rectangle((48,25,52,27),fill=pale)
for box in ((20,36,24,48),(24,36,28,48),(16,36,20,48),(28,36,32,48),(4,20,8,32),(0,20,4,32),(8,20,12,32),(12,20,16,32)):
    d.rectangle(box,fill=dark)
d.rectangle((20,36,28,39),fill=green)
for box in ((20,45,24,48),(24,45,28,48),(4,29,8,32),(8,29,12,32)):
    d.rectangle(box,fill=(35,40,36))
im.save(OUT/"entity"/"ultras_fan.png",optimize=True)

# Original procedural crowd-supporter sound effects, encoded as mono OGG Vorbis.
sound_dir=OUT.parent/"sounds"
sound_dir.mkdir(parents=True,exist_ok=True)
rate=44100

def render_ogg(name,duration,fn):
    wav_path=sound_dir/f"{name}.wav"
    ogg_path=sound_dir/f"{name}.ogg"
    rng=random.Random(4200+len(name))
    pcm=bytearray()
    for i in range(int(rate*duration)):
        value=max(-0.95,min(0.95,fn(i/rate,rng)))
        pcm.extend(struct.pack("<h",int(value*32767)))
    with wave.open(str(wav_path),"wb") as out:
        out.setnchannels(1); out.setsampwidth(2); out.setframerate(rate); out.writeframes(pcm)
    ffmpeg=shutil.which("ffmpeg")
    if not ffmpeg: raise RuntimeError("ffmpeg must be installed to generate OGG files")
    subprocess.run([ffmpeg,"-y","-loglevel","error","-i",str(wav_path),"-ac","1","-c:a","libvorbis","-q:a","4",str(ogg_path)],check=True)
    wav_path.unlink()

def horn(t,rng):
    for start,end,hz in ((0,.36,392),(.34,.72,523.25),(.70,1.18,659.25)):
        if start<=t<=end:
            env=max(0,min(1,(t-start)/.025,(end-t)/.09))
            f=hz*(1+.006*math.sin(2*math.pi*5*t))
            return .30*env*(math.sin(2*math.pi*f*t)+.42*math.sin(4*math.pi*f*t)+.18*math.sin(6*math.pi*f*t))
    return 0

def drum(t,rng):
    kick=math.sin(2*math.pi*(48+92*math.exp(-18*t))*t)*math.exp(-8*t)
    noise=rng.uniform(-1,1)*math.exp(-27*t) if t<.16 else 0
    return .62*kick+.20*noise

def clap(t,rng):
    total=0
    for start in (.015,.105,.205):
        dt=t-start
        if 0<=dt<.085:
            total+=.26*math.exp(-42*dt)*rng.uniform(-1,1)*math.exp(-68*dt)
            total+=.12*math.sin(2*math.pi*1680*dt)*math.exp(-38*dt)
    return total

render_ogg("megaphone_call",1.22,horn)
render_ogg("drum_hit",.52,drum)
render_ogg("clapper",.34,clap)

sound_defs={
 "megaphone_call":{"subtitle":"subtitles.ultrasmod.megaphone_call","sounds":[{"name":"ultrasmod:megaphone_call","volume":.9}]},
 "drum_hit":{"subtitle":"subtitles.ultrasmod.drum_hit","sounds":[{"name":"ultrasmod:drum_hit","volume":1.0}]},
 "clapper":{"subtitle":"subtitles.ultrasmod.clapper","sounds":[{"name":"ultrasmod:clapper","volume":.95}]}
}
(OUT.parent/"sounds.json").write_text(json.dumps(sound_defs,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
for locale,translations in {
 "ko_kr":{"subtitles.ultrasmod.megaphone_call":"확성기 응원","subtitles.ultrasmod.drum_hit":"응원 북","subtitles.ultrasmod.clapper":"클래퍼 박수"},
 "en_us":{"subtitles.ultrasmod.megaphone_call":"Supporter megaphone","subtitles.ultrasmod.drum_hit":"Supporter drum","subtitles.ultrasmod.clapper":"Clapper"}
}.items():
    path=OUT.parent/"lang"/f"{locale}.json"
    data=json.loads(path.read_text(encoding="utf-8")); data.update(translations)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

print(f"Generated enhanced pixel art, animated cloth, and original OGG audio in {OUT}")


# Large supporter canvases: each placed block is one tile of a single continuous image.
def write_canvas_models(block_name, texture_name, width, height):
    assets = OUT.parent
    models_dir = assets / "models" / "block"
    states_dir = assets / "blockstates"
    item_dir = assets / "models" / "item"
    models_dir.mkdir(parents=True, exist_ok=True)
    states_dir.mkdir(parents=True, exist_ok=True)
    item_dir.mkdir(parents=True, exist_ok=True)

    variants = {}
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    for part in range(36):
        column = part % width
        row = part // width
        u0 = 16.0 * column / width
        u1 = 16.0 * (column + 1) / width
        v0 = 16.0 * row / height
        v1 = 16.0 * (row + 1) / height
        model = {
            "ambientocclusion": False,
            "textures": {
                "cloth": f"ultrasmod:block/{texture_name}",
                "particle": f"ultrasmod:block/{texture_name}"
            },
            "elements": [{
                "from": [0, 0, 7.5],
                "to": [16, 16, 8.5],
                "faces": {
                    "north": {"uv": [u0, v0, u1, v1], "texture": "#cloth"},
                    "south": {"uv": [u0, v0, u1, v1], "texture": "#cloth"}
                }
            }]
        }
        (models_dir / f"{block_name}_part_{part}.json").write_text(
            json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        for facing, rotation in rotations.items():
            variants[f"facing={facing},part={part}"] = {
                "model": f"ultrasmod:block/{block_name}_part_{part}",
                "y": rotation
            }

    (states_dir / f"{block_name}.json").write_text(
        json.dumps({"variants": variants}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8"
    )
    (models_dir / f"{block_name}.json").write_text(
        json.dumps({"parent": f"ultrasmod:block/{block_name}_part_0"}, indent=2) + "\n",
        encoding="utf-8"
    )
    (item_dir / f"{block_name}.json").write_text(
        json.dumps({"parent": f"ultrasmod:block/{block_name}_part_0"}, indent=2) + "\n",
        encoding="utf-8"
    )

write_canvas_models("supporter_flag", "flag_cloth", 3, 5)
write_canvas_models("supporter_banner", "banner_cloth", 12, 3)
