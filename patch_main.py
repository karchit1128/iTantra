import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/MainActivity.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''    companion object {
        private val _isHardwarePttPressed = MutableStateFlow(false)
        val isHardwarePttPressed: StateFlow<Boolean> = _isHardwarePttPressed
    }'''

replacement = '''    companion object {
        private val _isHardwarePttPressed = MutableStateFlow(false)
        val isHardwarePttPressed: StateFlow<Boolean> = _isHardwarePttPressed
        
        var isAppInForeground = false
            private set
    }
    
    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }
    
    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched MainActivity successfully!')
else:
    print('Target not found in MainActivity.kt.')
