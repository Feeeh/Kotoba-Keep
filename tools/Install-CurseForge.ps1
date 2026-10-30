param(
    [string]$ProfilePath = (Join-Path $env:USERPROFILE 'curseforge\minecraft\Instances\Kotoba Keep'),
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectPath = Split-Path -Parent $PSScriptRoot
$profileResolved = [IO.Path]::GetFullPath($ProfilePath)
$profileInfo = Join-Path $profileResolved 'minecraftinstance.json'
if (-not (Test-Path -LiteralPath $profileInfo)) { throw "Create a Minecraft 26.3 / Fabric profile in CurseForge first: $profileResolved" }
$instance = Get-Content -LiteralPath $profileInfo -Raw | ConvertFrom-Json
if ($instance.gameVersion -ne '26.3' -or $instance.baseModLoader.name -notlike 'fabric-*') { throw 'This installer only supports Minecraft 26.3 with Fabric.' }
if (-not $SkipBuild) {
    Push-Location -LiteralPath $projectPath
    try { & .\gradlew.bat build; if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' } } finally { Pop-Location }
}
$modVersion = [regex]::Match((Get-Content -LiteralPath (Join-Path $projectPath 'gradle.properties') -Raw), '(?m)^mod_version=(.+)\r?$').Groups[1].Value.Trim()
if (-not $modVersion) { throw 'Missing mod_version in gradle.properties' }
$jarName = "kotoba-keep-$modVersion.jar"
$jarPath = Join-Path $projectPath "build\libs\$jarName"
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Build the mod first.' }
$modsPath = Join-Path $profileResolved 'mods'
New-Item -ItemType Directory -Force -Path $modsPath | Out-Null
function Get-VerifiedFile([string]$Url, [string]$Target, [string]$Sha512) {
    if ((Test-Path -LiteralPath $Target) -and (Get-FileHash -LiteralPath $Target -Algorithm SHA512).Hash -eq $Sha512) { return }
    # Download beside the target: a file that fails verification never becomes a loadable .jar.
    $download = "$Target.download"
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $download
    if ((Get-FileHash -LiteralPath $download -Algorithm SHA512).Hash -ne $Sha512) { Remove-Item -LiteralPath $download; throw "Checksum mismatch: $Url" }
    Move-Item -LiteralPath $download -Destination $Target -Force
}
$query = '?loaders=%5B%22fabric%22%5D&game_versions=%5B%2226.3%22%5D'
foreach ($dependency in @(@{Project='P7dR8mSH';Version='0.165.0+26.3'}, @{Project='9eGKb6K1';Version='fabric-2.6.27+26.3'})) {
    $versions = Invoke-RestMethod -Uri ("https://api.modrinth.com/v2/project/$($dependency.Project)/version$query")
    $version = $versions | Where-Object version_number -eq $dependency.Version | Select-Object -First 1
    if (-not $version) { throw "Missing pinned dependency $($dependency.Version)" }
    $file = $version.files | Where-Object primary | Select-Object -First 1
    Get-VerifiedFile $file.url (Join-Path $modsPath $file.filename) $file.hashes.sha512
}
$backupPath = Join-Path $profileResolved 'config\kotobakeep\backups'
foreach ($previousJar in (Get-ChildItem -LiteralPath $modsPath -Filter 'kotoba-keep-*.jar' -File | Where-Object Name -ne $jarName)) {
    if ($previousJar.DirectoryName -ne [IO.Path]::GetFullPath($modsPath)) { throw 'Unexpected mod backup path' }
    New-Item -ItemType Directory -Force -Path $backupPath | Out-Null
    Move-Item -LiteralPath $previousJar.FullName -Destination (Join-Path $backupPath ($previousJar.Name + '.' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff') + '.bak'))
}
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $modsPath $jarName) -Force
$modelBase = Join-Path $profileResolved 'config\kotobakeep'
$modelPath = Join-Path $modelBase 'vosk-model-small-ja-0.22'
if (-not (Test-Path -LiteralPath (Join-Path $modelPath 'am\final.mdl'))) {
    $modelCache = Join-Path $projectPath 'vendor\models'
    New-Item -ItemType Directory -Force -Path $modelCache,$modelBase | Out-Null
    $zip = Join-Path $modelCache 'vosk-model-small-ja-0.22.zip'
    if (-not (Test-Path -LiteralPath $zip)) { Invoke-WebRequest -UseBasicParsing 'https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip' -OutFile $zip }
    if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ne 'EFA092D280153A77615E9E0C7D7283E93E600DE3D19D3BEC686C57EF19D52EAC') { Remove-Item -LiteralPath $zip; throw 'Japanese model checksum mismatch; run the installer again.' }
    Expand-Archive -LiteralPath $zip -DestinationPath $modelBase -Force
}
Copy-Item -LiteralPath (Join-Path $projectPath 'README.md') -Destination (Join-Path $profileResolved 'KOTOBA-KEEP-README.md') -Force
Write-Output "Installed Kotoba Keep by glitchk.id into $profileResolved"
