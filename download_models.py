import os
import urllib.request

models = {
    "bn_IN": "bn/bn_IN/geeta/medium/bn_IN-geeta-medium",
    "gu_IN": "gu/gu_IN/nirali/medium/gu_IN-nirali-medium",
    "kn_IN": "kn/kn_IN/shruthi/medium/kn_IN-shruthi-medium",
    "ml_IN": "ml/ml_IN/ammu/medium/ml_IN-ammu-medium",
    "mr_IN": "mr/mr_IN/anuradha/medium/mr_IN-anuradha-medium",
    "or_IN": "or/or_IN/suhas/medium/or_IN-suhas-medium",
    "ta_IN": "ta/ta_IN/kani/medium/ta_IN-kani-medium",
    "te_IN": "te/te_IN/shruthi/medium/te_IN-shruthi-medium"
}

base_url = "https://huggingface.co/rhasspy/piper-voices/resolve/main/"
assets_dir = "app/src/main/assets"

for lang, path in models.items():
    model_dir = os.path.join(assets_dir, f"vits-piper-{lang}-medium")
    os.makedirs(model_dir, exist_ok=True)
    
    onnx_file = f"{lang}-medium.onnx"
    json_file = f"{lang}-medium.onnx.json"
    
    onnx_path = os.path.join(model_dir, onnx_file)
    json_path = os.path.join(model_dir, json_file)
    
    # Download ONNX
    if not os.path.exists(onnx_path) or os.path.getsize(onnx_path) == 0:
        url = f"{base_url}{path}.onnx"
        print(f"Downloading {url}...")
        try:
            urllib.request.urlretrieve(url, onnx_path)
            print(f"Downloaded {onnx_file}")
        except Exception as e:
            print(f"Failed to download {onnx_file}: {e}")
            
    # Download JSON
    if not os.path.exists(json_path) or os.path.getsize(json_path) == 0:
        url = f"{base_url}{path}.onnx.json"
        print(f"Downloading {url}...")
        try:
            urllib.request.urlretrieve(url, json_path)
            print(f"Downloaded {json_file}")
        except Exception as e:
            print(f"Failed to download {json_file}: {e}")

