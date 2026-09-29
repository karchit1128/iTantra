import os
import sys
import wave
import json

try:
    import jiwer
except ImportError:
    print("jiwer not installed. Please install it using: pip install jiwer")
    sys.exit(1)

def calculate_wer(reference, hypothesis):
    return jiwer.wer(reference, hypothesis)

def main():
    print("STT Word Error Rate (WER) Benchmark Script")
    print("==========================================")
    print("To run this benchmark:")
    print("1. Create a directory with 16kHz .wav files.")
    print("2. Create a reference.json mapping filename -> actual transcript.")
    print("3. Run the Sherpa-ONNX CLI or Python API on the audio.")
    print("4. Compare the outputs here.")
    print("\nSince we cannot run Sherpa-ONNX directly in this Python env without the Python bindings,")
    print("you should run this script on the actual device or a machine with Sherpa-ONNX installed.")
    print("Example calculation:")
    ref = "hello world this is a test"
    hyp = "hello word this is test"
    print(f"Reference: {ref}")
    print(f"Hypothesis: {hyp}")
    print(f"WER: {calculate_wer(ref, hyp):.2%}")

if __name__ == "__main__":
    main()
