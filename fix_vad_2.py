import re

path = "app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. State
content = content.replace(
    "var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }",
    "var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }\n    var isHandsFreeMode by remember { mutableStateOf(false) }"
)

# 2. Block
old_block = """            // Status hint
            AnimatedContent(
                targetState = when { isPttDisabled -> "Channel Locked"; isRecording -> "Recording..."; else -> "Hold mic to speak  \\u00b7  type to chat" },
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status"
            ) { statusText ->
                Text(statusText, color = when { isPttDisabled -> TextSecondary; isRecording -> DangerRed; else -> TextSecondary },
                    fontSize = 12.sp, fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 4.dp))
            }"""

new_block = """            // Status hint
            AnimatedContent(
                targetState = when { isPttDisabled -> "Channel Locked"; isRecording -> "Recording..."; else -> "Hold mic to speak  \\u00b7  type to chat" },
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status"
            ) { statusText ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isHandsFreeMode) "Hands-Free Active" else statusText, color = when { isPttDisabled -> TextSecondary; isRecording -> DangerRed; else -> TextSecondary },
                        fontSize = 12.sp, fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("VAD", fontSize = 10.sp, color = TextSecondary)
                    Switch(checked = isHandsFreeMode, onCheckedChange = { isHandsFreeMode = it }, modifier = androidx.compose.ui.draw.scale(0.6f))
                }
            }"""

content = content.replace(old_block, new_block)
content = content.replace("disableVad = true", "disableVad = !isHandsFreeMode")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("WalkieTalkieScreen updated correctly.")
