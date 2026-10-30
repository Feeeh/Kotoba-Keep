$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$releasePath = Join-Path $projectPath 'release'
New-Item -ItemType Directory -Force -Path $releasePath | Out-Null
$modVersion = [regex]::Match((Get-Content -LiteralPath (Join-Path $projectPath 'gradle.properties') -Raw), '(?m)^mod_version=(.+)\r?$').Groups[1].Value.Trim()
if (-not $modVersion) { throw 'Missing mod_version in gradle.properties' }
$releaseJar = Join-Path $releasePath "kotoba-keep-$modVersion+26.3.jar"
Copy-Item -LiteralPath (Join-Path $projectPath "build\libs\kotoba-keep-$modVersion.jar") -Destination $releaseJar -Force
(Get-FileHash -LiteralPath $releaseJar -Algorithm SHA256).Hash | Set-Content -Encoding ASCII ($releaseJar + '.sha256')
$archivePath = Join-Path $releasePath "kotoba-keep-$modVersion-source.zip"
if (Test-Path -LiteralPath $archivePath) { Remove-Item -LiteralPath $archivePath }
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$archive = [IO.Compression.ZipFile]::Open($archivePath, [IO.Compression.ZipArchiveMode]::Create)
try {
    $files = @()
    foreach ($dir in @('src','gradle')) { $files += Get-ChildItem -LiteralPath (Join-Path $projectPath $dir) -File -Recurse }
    foreach ($file in @('gradlew','gradlew.bat','gradle.properties','settings.gradle','build.gradle','LICENSE','THIRD_PARTY_NOTICES.md','README.md','VALIDATION.md','.gitignore','tools\Install-CurseForge.ps1','tools\Package-Release.ps1')) { $files += Get-Item -LiteralPath (Join-Path $projectPath $file) }
    foreach ($file in $files) {
        $relative = $file.FullName.Substring($projectPath.Length + 1).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $relative, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $archive.Dispose() }
Get-ChildItem -LiteralPath $releasePath | Select-Object Name,Length
