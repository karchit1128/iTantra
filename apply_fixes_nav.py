import os

# 1. Update WalkieTalkieScreen.kt
wt_path = "app/src/main/java/com/example/itantra/ui/screens/WalkieTalkieScreen.kt"
with open(wt_path, "r", encoding="utf-8") as f:
    wt_content = f.read()

wt_content = wt_content.replace(
    "fun WalkieTalkieScreen() {",
    "fun WalkieTalkieScreen(onNavigateToSettings: () -> Unit = {}) {"
)

wt_content = wt_content.replace(
    "                    IconButton(onClick = {}, modifier = Modifier.pointerInput(Unit) {\n                        detectTapGestures(onLongPress = { PanicWipeManager.executeWipe(context) { meshStatus = \"ALL DATA WIPED.\" } })\n                    }) {\n                        Icon(Icons.Default.Settings, contentDescription = \"Settings\", tint = TextSecondary)\n                    }",
    "                    IconButton(onClick = { onNavigateToSettings() }, modifier = Modifier.pointerInput(Unit) {\n                        detectTapGestures(onLongPress = { PanicWipeManager.executeWipe(context) { meshStatus = \"ALL DATA WIPED.\" } })\n                    }) {\n                        Icon(Icons.Default.Settings, contentDescription = \"Settings\", tint = TextSecondary)\n                    }"
)

with open(wt_path, "w", encoding="utf-8") as f:
    f.write(wt_content)

# 2. Update MainActivity.kt
ma_path = "app/src/main/java/com/example/itantra/MainActivity.kt"
with open(ma_path, "r", encoding="utf-8") as f:
    ma_content = f.read()

ma_content = ma_content.replace(
    "            composable(\"walkie_talkie\") { WalkieTalkieScreen() }",
    "            composable(\"walkie_talkie\") { WalkieTalkieScreen(onNavigateToSettings = { navController.navigate(\"settings\") }) }\n            composable(\"settings\") { com.example.itantra.ui.screens.SettingsScreen() }"
)

with open(ma_path, "w", encoding="utf-8") as f:
    f.write(ma_content)

print("Navigation fixed.")
