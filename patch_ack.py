import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''                if (actualMessageInner.startsWith("[PRIO:")) {
                    val pEnd = actualMessageInner.indexOf("]")
                    if (pEnd != -1) {
                        msgPriority = actualMessageInner.substring(6, pEnd)
                        actualMessageInner = actualMessageInner.substring(pEnd + 1)
                    }
                }
                
                val rawMsg = actualMessageInner.removePrefix("[TTS]")'''

replacement = '''                if (actualMessageInner.startsWith("[PRIO:")) {
                    val pEnd = actualMessageInner.indexOf("]")
                    if (pEnd != -1) {
                        msgPriority = actualMessageInner.substring(6, pEnd)
                        actualMessageInner = actualMessageInner.substring(pEnd + 1)
                    }
                }
                
                if (actualMessageInner.startsWith("[ACK:")) {
                    val ackedId = actualMessageInner.substringAfter("[ACK:").substringBefore("]")
                    triageRepo.markAsAcked(ackedId)
                    return@launch
                }
                
                val rawMsg = actualMessageInner.removePrefix("[TTS]")'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched successfully!')
else:
    print('Target not found in MeshNetworkManager.kt.')
