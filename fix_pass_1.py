import os
import re

def patch_file(filepath, patches):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    for old, new in patches:
        if old not in content:
            print(f"WARNING: Chunk not found in {filepath}:\n{old[:50]}...")
        else:
            content = content.replace(old, new)
            
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Patched {filepath}")

# 1. BUG-16: TTSEngine Thread.sleep -> delay
patch_file("app/src/main/java/com/example/itantra/ml/TTSEngine.kt", [
    ("Thread.sleep(durationMs)", "delay(durationMs)"),
    ("Thread.sleep(durationMs2)", "delay(durationMs2)")
])

# 2. BUG-17: WifiDirect GO assignment determinism
patch_file("app/src/main/java/com/example/itantra/mesh/WifiDirectHelper.kt", [
    (
        "val config = WifiP2pConfig().apply {\n            deviceAddress = address\n            // Removed PBC requirement just in case it breaks Samsung A04s\n        }",
        "val config = WifiP2pConfig().apply {\n            deviceAddress = address\n            groupOwnerIntent = 15 // BUG-17 Fix: Force determinism\n        }"
    )
])

# 3. BUG-14: MeshNetworkManager chunk eviction (avoid OOM)
patch_file("app/src/main/java/com/example/itantra/mesh/MeshNetworkManager.kt", [
    (
        "private val chunkBuffers = java.util.concurrent.ConcurrentHashMap<String, MutableMap<Int, ByteArray>>()",
        "private val chunkBuffers = java.util.concurrent.ConcurrentHashMap<String, MutableMap<Int, ByteArray>>()\n    private val chunkTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()"
    ),
    (
        "val map = chunkBuffers.getOrPut(msgId) { java.util.concurrent.ConcurrentHashMap() }",
        """// BUG-14 Fix: Evict stale chunks
                        val now = System.currentTimeMillis()
                        val staleKeys = chunkTimestamps.filter { now - it.value > 30000 }.keys
                        staleKeys.forEach { chunkBuffers.remove(it); chunkTimestamps.remove(it) }
                        
                        val map = chunkBuffers.getOrPut(msgId) { 
                            chunkTimestamps[msgId] = now
                            java.util.concurrent.ConcurrentHashMap() 
                        }"""
    ),
    (
        "chunkBuffers.remove(msgId)",
        "chunkBuffers.remove(msgId)\n                            chunkTimestamps.remove(msgId)"
    )
])

# 4. BUG-19: JuryEvaluationScreen shutdown() removal
patch_file("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", [
    (
        "                                } finally {\n                                    com.example.itantra.ml.TTSEngine.shutdown()\n                                }",
        "                                } finally {\n                                    // BUG-19 Fix: Do not shutdown TTS between languages to avoid espeak state destruction\n                                }"
    )
])

# 5. BUG-22: WalkieTalkieScreen remove English-only keyword override
patch_file("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", [
    (
        "val prio = if (lower.contains(\"bleeding\") || lower.contains(\"heart\") || lower.contains(\"broken\")) \"RED\" else selectedPriority",
        "val prio = selectedPriority // BUG-22 Fix: Trust the user's UI selection instead of English-only keywords"
    )
])
