import os

path = "app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Add state
content = content.replace(
    "var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }",
    "var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }\n    var isHandsFreeMode by remember { mutableStateOf(false) }"
)

# Add UI
import re
content = re.sub(
    r'AnimatedContent\([\s\S]*?\{ statusText ->[\s\S]*?Text\(statusText[\s\S]*?\}\n',
    r'''AnimatedContent(
                targetState = when { isPttDisabled -> "Channel Locked"; isRecording -> "Recording..."; else -> "Hold mic to speak  \u00b7  type to chat" },
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status"
            ) { statusText ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isHandsFreeMode) "Hands-Free Active" else statusText, color = when { isPttDisabled -> TextSecondary; isRecording -> DangerRed; else -> TextSecondary }, fontSize = 12.sp, fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("VAD", fontSize = 10.sp, color = TextSecondary)
                    androidx.compose.material3.Switch(checked = isHandsFreeMode, onCheckedChange = { isHandsFreeMode = it }, modifier = Modifier.scale(0.6f))
                }
            }\n''',
    content
)

# Fix PTT disable Vad
content = content.replace("disableVad = true", "disableVad = !isHandsFreeMode")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
