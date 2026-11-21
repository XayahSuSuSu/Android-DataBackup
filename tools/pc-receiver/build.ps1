$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$classesDir = Join-Path $root "build\classes"
$distDir = Join-Path $root "dist"

if (Test-Path $classesDir) { Remove-Item -Recurse -Force $classesDir }
New-Item -ItemType Directory -Force -Path $classesDir, $distDir, (Join-Path $distDir "tools") | Out-Null

$javaFiles = Get-ChildItem -Path (Join-Path $root "src") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
Write-Host "Compiling $($javaFiles.Count) Java files..."
& javac -encoding UTF-8 --release 8 -d $classesDir $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Copy-Item (Join-Path $root "src\messages_*.properties") -Destination $classesDir -Force

$jarPath = Join-Path $distDir "pc-receiver.jar"
& jar cfm $jarPath (Join-Path $root "src\META-INF\MANIFEST.MF") -C $classesDir .
if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }

Get-ChildItem (Join-Path $root "tools\*") | ForEach-Object {
    $targetTool = Join-Path (Join-Path $distDir "tools") $_.Name
    if (-not (Test-Path $targetTool)) {
        Copy-Item $_.FullName -Destination $targetTool -Force
    }
}

$batContent = "@echo off`r`nsetlocal`r`nset `"APP_DIR=%~dp0`"`r`ncd /d `"%APP_DIR%`"`r`nif `"%~1`"==`"`" (`r`n  java -Dfile.encoding=UTF-8 -XX:MaxDirectMemorySize=2G -jar `"%APP_DIR%pc-receiver.jar`" -c adb --ask-dir`r`n) else (`r`n  java -Dfile.encoding=UTF-8 -XX:MaxDirectMemorySize=2G -jar `"%APP_DIR%pc-receiver.jar`" -c adb -d `"%~1`"`r`n)`r`npause`r`n"
[System.IO.File]::WriteAllText((Join-Path $distDir "start-adb.bat"), $batContent, [System.Text.Encoding]::ASCII)

Write-Host "Build succeeded: $jarPath"

