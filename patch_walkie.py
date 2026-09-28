import re

with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "r", encoding="utf-8") as f:
    code = f.read()

state_repl = """    var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }
    var showRawStt by remember { mutableStateOf(false) }
    var lastSttDebugInfo by remember { mutableStateOf("") }"""
code = code.replace("    var showLangMismatchDialog by remember { mutableStateOf<com.example.itantra.ml.SttResult.LangMismatch?>(null) }", state_repl)

# Add UI toggle above Dropdown
ui_search = """                        DropdownMenu(expanded = expandedLang, onDismissRequest = { expandedLang = false }) {"""
ui_replace = """                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showRawStt, onCheckedChange = { showRawStt = it })
                            Text("Debug STT", fontSize = 10.sp, color = TextSecondary)
                        }
                        DropdownMenu(expanded = expandedLang, onDismissRequest = { expandedLang = false }) {"""
code = code.replace(ui_search, ui_replace)

# Display lastSttDebugInfo if showRawStt is true (put it under the TopAppBar equivalent or list)
# We can put it in the LazyColumn as item 0, or just an overlay Box. Let's put it above LazyColumn
box_search = "                    LazyColumn("
box_replace = """                    if (showRawStt && lastSttDebugInfo.isNotEmpty()) {
                        Text(lastSttDebugInfo, color = Color.White, modifier = Modifier.background(Color.Black).padding(8.dp))
                    }
                    LazyColumn("""
code = code.replace(box_search, box_replace)

# Populate lastSttDebugInfo inside VAD callback
vad_success_search = """                        is com.example.itantra.ml.SttResult.Success -> {
                            val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)"""
vad_success_replace = """                        is com.example.itantra.ml.SttResult.Success -> {
                            if (showRawStt) lastSttDebugInfo = "VAD RAW:\\n${transcript.rawText}\\nGATE:\\n${transcript.text}"
                            val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)"""
code = code.replace(vad_success_search, vad_success_replace)

vad_empty_search = """                        is com.example.itantra.ml.SttResult.Empty -> {
                            android.widget.Toast.makeText(context, "Could not hear you. Speak louder.", android.widget.Toast.LENGTH_SHORT).show()"""
vad_empty_replace = """                        is com.example.itantra.ml.SttResult.Empty -> {
                            if (showRawStt) lastSttDebugInfo = "VAD RAW:\\n${transcript.rawText}\\nGATE:\\n[EMPTY]"
                            android.widget.Toast.makeText(context, "Could not hear you. Speak louder.", android.widget.Toast.LENGTH_SHORT).show()"""
code = code.replace(vad_empty_search, vad_empty_replace)


# Populate lastSttDebugInfo inside PTT callback
ptt_success_search = """                                                is com.example.itantra.ml.SttResult.Success -> {
                                                    val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)"""
ptt_success_replace = """                                                is com.example.itantra.ml.SttResult.Success -> {
                                                    if (showRawStt) lastSttDebugInfo = "PTT RAW:\\n${transcript.rawText}\\nGATE:\\n${transcript.text}"
                                                    val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)"""
code = code.replace(ptt_success_search, ptt_success_replace)

ptt_empty_search = """                                                is com.example.itantra.ml.SttResult.Empty -> {
                                                    android.widget.Toast.makeText(context, "Could not hear you. Speak louder.", android.widget.Toast.LENGTH_SHORT).show()"""
ptt_empty_replace = """                                                is com.example.itantra.ml.SttResult.Empty -> {
                                                    if (showRawStt) lastSttDebugInfo = "PTT RAW:\\n${transcript.rawText}\\nGATE:\\n[EMPTY]"
                                                    android.widget.Toast.makeText(context, "Could not hear you. Speak louder.", android.widget.Toast.LENGTH_SHORT).show()"""
code = code.replace(ptt_empty_search, ptt_empty_replace)


with open("app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt", "w", encoding="utf-8") as f:
    f.write(code)
