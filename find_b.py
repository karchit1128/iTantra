import json

with open(r'C:\Users\Ayush Yadav\.gemini\antigravity\brain\980191a8-4157-4b50-8b00-020e016c92bb\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'content' in data:
                text = data['content']
                if 'Phase B' in text and 'B1' in text and 'B2' in text:
                    print(f"FOUND IN STEP {data.get('step_index')}:")
                    # print lines around B1
                    lines = text.split('\n')
                    for i, l in enumerate(lines):
                        if 'B1' in l or 'B2' in l or 'B3' in l or 'B4' in l or 'B5' in l or 'B6' in l:
                            print(l)
        except:
            pass
