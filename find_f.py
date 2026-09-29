import json

with open(r'C:\Users\Ayush Yadav\.gemini\antigravity\brain\980191a8-4157-4b50-8b00-020e016c92bb\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'content' in data:
                text = data['content']
                if 'F2' in text and 'F3' in text:
                    print(f"\n--- FOUND IN STEP {data.get('step_index')} ---")
                    lines = text.split('\n')
                    for i, l in enumerate(lines):
                        if 'F' in l or 'Phase B' in l:
                            print(l)
        except:
            pass
