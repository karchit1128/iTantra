with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "r", encoding="utf-8") as f:
    code = f.read()

import re
code = re.sub(r'meshManager.sendWithRetry\(msgId, "\[LANG:\$selectedLanguage\]\[ID:\$msgId\]\[PRIO:\$selectedPriority\]\[TTS\]Priority \$selectedPriority Alert from \$myDeviceId: \$\{transcript.text\}"\)',
              r'meshManager.sendWithRetry(msgId, "[LANG:$selectedLanguage][ID:$msgId][PRIO:$selectedPriority][TTS]${transcript.text}")', code)

code = re.sub(r'meshManager.sendWithRetry\(msgId, "\[LANG:\$selectedLanguage\]\[ID:\$msgId\]\[PRIO:\$prio\]\[TTS\]Priority \$prio Alert from \$myDeviceId: \$\{transcript.text\}"\)',
              r'meshManager.sendWithRetry(msgId, "[LANG:$selectedLanguage][ID:$msgId][PRIO:$prio][TTS]${transcript.text}")', code)

code = re.sub(r'meshManager.sendWithRetry\(msgId, "\[LANG:\$selectedLanguage\]\[ID:\$msgId\]\[PRIO:\$prio\]\[TTS\]Priority \$prio Alert from \$myDeviceId: \$msg"\)',
              r'meshManager.sendWithRetry(msgId, "[LANG:$selectedLanguage][ID:$msgId][PRIO:$prio][TTS]$msg")', code)

code = re.sub(r'meshManager.sendWithRetry\(msgId, "\[LANG:\$selectedLanguage\]\[ID:\$msgId\]\[PRIO:\$prio\]\[TTS\]Priority \$prio Alert from \$myDeviceId: \$\{result.rawText\}"\)',
              r'meshManager.sendWithRetry(msgId, "[LANG:$selectedLanguage][ID:$msgId][PRIO:$prio][TTS]${result.rawText}")', code)

with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "w", encoding="utf-8") as f:
    f.write(code)
