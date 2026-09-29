import urllib.request
import os

base_url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/"
target = "hi/hi_IN/rohan/medium/hi_IN-rohan-medium.onnx.json"
out_path = "app/src/main/assets/vits-piper-hi_IN-rohan-medium/hi_IN-rohan-medium.onnx.json"

print(f"Downloading uncorrupted JSON to {out_path}...")
urllib.request.urlretrieve(base_url + target, out_path)
print("Done.")
