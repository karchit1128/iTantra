import os
import json
import glob

assets_dir = "app/src/main/assets"
json_files = glob.glob(f"{assets_dir}/*/*.json")

results = []
for f in json_files:
    try:
        data = json.load(open(f, "r", encoding="utf-8"))
        sr = data.get("audio", {}).get("sample_rate", "N/A")
        lang = data.get("language", {}).get("code", "Unknown")
        quality = data.get("audio", {}).get("quality", "Unknown")
        name = os.path.basename(f)
        results.append(f"{lang} | {name} | {sr} | {quality}")
    except:
        pass

for r in sorted(results):
    print(r)
