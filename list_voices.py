import urllib.request
import json
url = "https://huggingface.co/rhasspy/piper-voices/raw/main/voices.json"
try:
    with urllib.request.urlopen(url) as response:
        data = json.loads(response.read().decode())
        indians = {k: v for k, v in data.items() if "IN" in k}
        for k, v in indians.items():
            print(f"{k}: {v['files'].keys()}")
except Exception as e:
    print(e)
