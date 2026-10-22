"""Genere les captures d'ecran des resultats attendus du TP 7 (docs/images/*.png)."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
IMG_DIR = ROOT / "docs" / "images"
IMG_DIR.mkdir(parents=True, exist_ok=True)
RUN = ROOT / "tools" / "outputs" / "run.txt"

FONT = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 15)
TITLE_FONT = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 13)

BG = (30, 30, 32)
TITLE_BG = (45, 45, 48)
FG = (204, 204, 204)
DIM = (128, 128, 132)
COLUMN = (166, 170, 182)
SQL_KW = (138, 180, 235)
GREEN = (106, 190, 96)
YELLOW = (222, 183, 92)
CYAN = (110, 190, 214)
MAGENTA = (200, 140, 210)

PAD = 26
LH = 23
TOP = 46
TITLE_BAR = 34

KEYWORDS = {"select", "distinct", "from", "where", "inner", "left", "outer", "on",
            "insert", "into", "values", "update", "set", "delete", "drop", "create",
            "alter", "table", "add", "constraint", "foreign", "references", "primary",
            "key", "if", "exists"}


def render(lines, title, out_path):
    width = max(FONT.getlength(t) for t, _ in lines) + PAD * 2
    height = TOP + LH * len(lines) + PAD - 8
    img = Image.new("RGB", (int(width), int(height)), BG)
    d = ImageDraw.Draw(img)

    d.rectangle([0, 0, img.width, TITLE_BAR], fill=TITLE_BG)
    for i, c in enumerate([(255, 95, 86), (255, 189, 46), (39, 201, 63)]):
        cx, cy = 20 + i * 20, 17
        d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], fill=c)
    tw = TITLE_FONT.getlength(title)
    d.text(((img.width - tw) / 2, 10), title, font=TITLE_FONT, fill=(160, 160, 165))

    y = TOP
    for text, color in lines:
        d.text((PAD, y), text, font=FONT, fill=color)
        y += LH

    img.save(out_path)
    print(f"{out_path.name} {img.size}")


def timeline(path):
    """Sequence de ('sql', [lignes]) et ('text', ligne) lue dans le log Maven."""
    raw = Path(path).read_text(encoding="utf-8-sig").splitlines()
    events = []
    i = 0
    while i < len(raw):
        line = raw[i]
        if line.strip() == "Hibernate:":
            j = i + 1
            block = []
            while j < len(raw) and (not raw[j].strip() or raw[j].startswith(" ")):
                if raw[j].strip():
                    block.append(raw[j].rstrip())
                j += 1
            events.append(("sql", block))
            i = j
            continue
        events.append(("text", line.rstrip()))
        i += 1
    return events


EVENTS = timeline(RUN)
END = next(i for i, (k, v) in enumerate(EVENTS)
           if k == "text" and v.startswith("Gain JOIN FETCH"))
EVENTS = EVENTS[:END + 1]


def find(prefix):
    return next(i for i, (k, v) in enumerate(EVENTS) if k == "text" and v.startswith(prefix))


def text_at(prefix):
    return EVENTS[find(prefix)][1]


def sql_between(start, end):
    return [v for k, v in EVENTS[start:end] if k == "sql"]


def sql_lines(block):
    out = [("Hibernate:", MAGENTA)]
    for raw in block:
        first = raw.strip().split(" ")[0].lower()
        out.append((raw, SQL_KW if first in KEYWORDS else COLUMN))
    return out


def repeated(count):
    return [(f"            ... ({count} requêtes identiques de plus)", DIM)]


# --- 1. Probleme N+1 -------------------------------------------------------
start = find("=== TP 7")
fin_n1 = find("Sans optimisation")
n1_sql = sql_between(start, fin_n1)

img1 = [(text_at("=== TP 7"), CYAN), (text_at("Données"), FG), ("", FG)]
img1 += sql_lines(n1_sql[0])
img1 += [("", FG)]
img1 += sql_lines(n1_sql[1])
img1 += repeated(len(n1_sql) - 2)
img1 += [("", FG), (text_at("Sans optimisation"), YELLOW)]
render(img1, "TP 7 — Problème N+1 : 11 requêtes SQL pour 10 auteurs",
       IMG_DIR / "01-probleme-n-plus-un.png")

# --- 2. JOIN FETCH et Entity Graph ---------------------------------------
fin_graph = find("Entity Graph")
opt_sql = sql_between(fin_n1, fin_graph)

img2 = sql_lines(opt_sql[0])
img2 += [(text_at("JOIN FETCH"), GREEN), ("", FG)]
if len(opt_sql) > 1:
    img2 += sql_lines(opt_sql[1])
img2 += [(text_at("Entity Graph"), GREEN)]
render(img2, "TP 7 — JOIN FETCH et Entity Graph : une seule requête SQL",
       IMG_DIR / "02-join-fetch-entity-graph.png")

# --- 3. Comparaison -------------------------------------------------------
img3 = [(text_at("Données"), FG), ("", FG),
        (text_at("Sans optimisation"), YELLOW),
        (text_at("JOIN FETCH"), GREEN),
        (text_at("Entity Graph"), GREEN),
        ("", FG),
        (text_at("Gain JOIN FETCH"), CYAN)]
render(img3, "TP 7 — Comparaison des stratégies de chargement",
       IMG_DIR / "03-comparaison.png")

# --- 4. Cache de second niveau -------------------------------------------
cache_sql = sql_between(fin_graph, END)
img4 = sql_lines(cache_sql[0])
img4 += [("", FG), ("Hibernate:", MAGENTA), (cache_sql[1][0], SQL_KW)]
img4 += repeated(len(cache_sql) - 2)
img4 += [("", FG),
         (text_at("Cache L2 - lecture 1"), YELLOW),
         (text_at("Cache L2 - lecture 2"), GREEN),
         ("", FG)]
stats = find("--- Statistiques")
img4 += [(EVENTS[stats][1], CYAN)]
img4 += [(EVENTS[stats + n][1], FG) for n in range(1, 6)]
render(img4, "TP 7 — Cache de second niveau (EhCache 3 / JCache)",
       IMG_DIR / "04-cache-second-niveau.png")
