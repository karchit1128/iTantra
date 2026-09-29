
# Fix BUG-1: Replace corrupted test strings in JuryEvaluationScreen.kt
# Writes correct UTF-8 Unicode strings directly as bytes

with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "rb") as f:
    raw = f.read()

content = raw.decode("utf-8", errors="replace")

# Find the when(lang) block
start_marker = "val testText = when(lang) {"
end_marker_search = "else ->"
if start_marker not in content:
    print("START MARKER NOT FOUND")
    exit(1)

block_start = content.find(start_marker)
# Find the else -> line after the block start
else_line_start = content.find(end_marker_search, block_start)
# Find the closing brace of the when block (the line after else -> )
close_brace = content.find("}", else_line_start) + 1

print(f"Block found: chars {block_start} to {close_brace}")

# Build the replacement with correct UTF-8 strings
replacement = '''val testText = when(lang) {
                                    "Hindi" -> "\u0928\u092e\u0938\u094d\u0924\u0947"
                                    "Marathi" -> "\u0928\u092e\u0938\u094d\u0915\u093e\u0930"
                                    "Gujarati" -> "\u0aa8\u0aae\u0ab8\u0acd\u0aa4\u0ac7"
                                    "Tamil" -> "\u0bb5\u0ba3\u0b95\u0bcd\u0b95\u0bae\u0bcd"
                                    "Telugu" -> "\u0c28\u0c2e\u0c38\u0c4d\u0c15\u0c3e\u0c30\u0c02"
                                    "Kannada" -> "\u0ca8\u0cae\u0cb8\u0ccd\u0c95\u0cbe\u0cb0"
                                    "Malayalam" -> "\u0d28\u0d2e\u0d38\u0d4d\u0d15\u0d3e\u0d30\u0d02"
                                    "Bengali" -> "\u09a8\u09ae\u09b8\u09cd\u0995\u09be\u09b0"
                                    "Odia" -> "\u0b28\u0b2e\u0b38\u0b4d\u0b15\u0b3e\u0b30"
                                    else -> "Hello, this is a test."
                                }'''

new_content = content[:block_start] + replacement + content[close_brace:]

with open("app/src/main/java/com/example/itantra/ui/screens/JuryEvaluationScreen.kt", "wb") as f:
    f.write(new_content.encode("utf-8"))

print("DONE - BUG-1 fixed")
