import urllib.request
url = "https://huggingface.co/csukuangfj/vits-piper-hi_IN-rohan-medium/raw/main/tokens.txt"
out_path = "app/src/main/assets/vits-piper-hi_IN-rohan-medium/tokens.txt"
try:
    urllib.request.urlretrieve(url, out_path)
    print("Successfully downloaded tokens.txt from k2-fsa")
except Exception as e:
    print(f"Failed: {e}")
