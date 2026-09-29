import json
import glob
import os

for json_file in glob.glob("app/src/main/assets/*/*.json"):
    dir_name = os.path.dirname(json_file)
    tokens_file = os.path.join(dir_name, "tokens.txt")
    with open(json_file, "r", encoding="utf-8") as f:
        data = json.load(f)
    if "phoneme_id_map" in data:
        with open(tokens_file, "w", encoding="utf-8", newline="\n") as out:
            for token, ids in data["phoneme_id_map"].items():
                for i in ids:
                    out.write(f"{token} {i}\n")
        print(f"Generated {tokens_file} with Unix line endings.")
