$assets = "app/src/main/assets"
$dirs = Get-ChildItem -Directory $assets
$allPass = $true

$table = @()

foreach ($dir in $dirs) {
    $name = $dir.Name
    $status = "PASS"
    $reason = @()

    # 1. 0-byte file check
    $zeroFiles = Get-ChildItem -File -Recurse $dir.FullName | Where-Object Length -eq 0
    if ($zeroFiles) {
        $status = "FAIL"
        $reason += "0-byte files: $($zeroFiles.Name -join ', ')"
    }

    # 2. tokens.txt CRLF check
    if ($name -match "vits-piper") {
        $tokens = Get-ChildItem -File -Recurse $dir.FullName | Where-Object Name -eq "tokens.txt"
        foreach ($t in $tokens) {
            $bytes = [System.IO.File]::ReadAllBytes($t.FullName)
            for ($i=0; $i -lt $bytes.Length - 1; $i++) {
                if ($bytes[$i] -eq 13 -and $bytes[$i+1] -eq 10) {
                    $status = "FAIL"
                    $reason += "CRLF in $($t.Name)"
                    break
                }
            }
        }
    }

    # 3. specific requirements
    if ($name -match "vits-piper") {
        if (-not (Test-Path "$($dir.FullName)/*.onnx")) { $status="FAIL"; $reason+="Missing .onnx" }
        if (-not (Test-Path "$($dir.FullName)/tokens.txt")) { $status="FAIL"; $reason+="Missing tokens.txt" }
        $espeakPath = "$($dir.FullName)/espeak-ng-data"
        if (-not (Test-Path $espeakPath)) { 
            $status="FAIL"; $reason+="Missing espeak-ng-data" 
        } else {
            $espeakFiles = Get-ChildItem -File -Recurse $espeakPath
            if ($espeakFiles.Count -eq 0) {
                $status="FAIL"; $reason+="Empty espeak-ng-data"
            }
        }
    }
    if ($name -match "sherpa-onnx") {
        if (-not (Test-Path "$($dir.FullName)/*.onnx")) { $status="FAIL"; $reason+="Missing .onnx" }
        if (-not (Test-Path "$($dir.FullName)/tokens.txt")) { $status="FAIL"; $reason+="Missing tokens.txt" }
    }

    $table += [PSCustomObject]@{ Folder=$name; Status=$status; Details=($reason -join " | ") }
    if ($status -eq "FAIL") { $allPass = $false }
}

$table | Format-Table -AutoSize
if (-not $allPass) { exit 1 }
