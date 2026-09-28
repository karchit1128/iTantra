import os
import urllib.request

models = {
    "ml_IN": ("ml/ml_IN/arjun/medium/ml_IN-arjun-medium", "ml_IN-medium"),
    "mr_IN": ("mr/mr_IN/google/medium/mr_IN-google-medium", "mr_IN-medium"),
    "te_IN": ("te/te_IN/maya/medium/te_IN-maya-medium", "te_IN-medium")
}

base_url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/"
assets_dir = "app/src/main/assets"

for lang, (hf_path, target_name) in models.items():
    model_dir = os.path.join(assets_dir, f"vits-piper-{target_name}")
    os.makedirs(model_dir, exist_ok=True)
    
    onnx_file = f"{target_name}.onnx"
    json_file = f"{target_name}.onnx.json"
    
    onnx_path = os.path.join(model_dir, onnx_file)
    json_path = os.path.join(model_dir, json_file)
    
    # Download ONNX
    if not os.path.exists(onnx_path) or os.path.getsize(onnx_path) == 0:
        url = f"{base_url}{hf_path}.onnx"
        print(f"Downloading {url}...")
        urllib.request.urlretrieve(url, onnx_path)
            
    # Download JSON
    if not os.path.exists(json_path) or os.path.getsize(json_path) == 0:
        url = f"{base_url}{hf_path}.onnx.json"
        print(f"Downloading {url}...")
        urllib.request.urlretrieve(url, json_path)

print("Downloads complete.")
