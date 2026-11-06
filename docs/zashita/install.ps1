# Установка всего нужного для защиты курсового проекта «ВардКлик» на чистый Windows-ПК.
# Запускается из «Установить_всё.bat». Ставит программы через winget с официальных сайтов,
# создаёт базу WardClick в SQL Server Express и распаковывает проект.
$ErrorActionPreference = 'Continue'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

# права администратора
$admin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $admin) {
    Start-Process powershell -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$($MyInvocation.MyCommand.Path)`""
    exit
}

function Say($text, $color = 'Cyan') { Write-Host ""; Write-Host "=== $text" -ForegroundColor $color }
function Refresh-Path {
    $env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')
}

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Say 'Не найден winget. Установите из Microsoft Store «Установщик приложений» (App Installer) и запустите снова.' 'Red'
    Read-Host 'Нажмите Enter для выхода'; exit 1
}

$results = @()
function Install-Pkg($id, $name, $extra = @()) {
    Say "Проверка: $name"
    $listed = winget list --id $id -e --accept-source-agreements 2>$null | Out-String
    if ($listed -match [regex]::Escape($id)) { Write-Host "$name уже установлен — пропускаю."; $script:results += "$name — уже был"; return $true }
    Say "Установка: $name (может занять несколько минут)"
    winget install --id $id -e --source winget --accept-package-agreements --accept-source-agreements @extra | Out-Host
    # 3010 — установлено, нужна перезагрузка
    $ok = ($LASTEXITCODE -eq 0 -or $LASTEXITCODE -eq 3010)
    $script:results += ($(if ($ok) { "$name — установлен" } else { "$name — ОШИБКА (код $LASTEXITCODE)" }))
    return $ok
}

Install-Pkg 'Google.AndroidStudio' 'Android Studio' | Out-Null
Install-Pkg 'Microsoft.SQLServer.2022.Express' 'SQL Server 2022 Express' | Out-Null
if (-not (Install-Pkg 'Microsoft.SQLServerManagementStudio.22' 'SQL Server Management Studio 22')) {
    Install-Pkg 'Microsoft.SQLServerManagementStudio' 'SQL Server Management Studio' | Out-Null
}
Install-Pkg 'Microsoft.Sqlcmd' 'sqlcmd' | Out-Null
Refresh-Path

# база данных WardClick
Say 'Создание базы данных WardClick'
$sql = Join-Path $here 'WardClick.sql'
$sqlcmd = Get-Command sqlcmd -ErrorAction SilentlyContinue
if (-not $sqlcmd) {
    $found = Get-ChildItem 'C:\Program Files\SqlCmd', 'C:\Program Files\Microsoft SQL Server' -Recurse -Filter sqlcmd.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $sqlcmd = $found.FullName } else { $sqlcmd = $null }
} else { $sqlcmd = $sqlcmd.Source }
Start-Service -Name 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue
if ($sqlcmd -and (Test-Path $sql)) {
    & $sqlcmd -S '.\SQLEXPRESS' -E -C -b -i $sql | Out-Host
    if ($LASTEXITCODE -eq 0) { $results += 'База WardClick — создана' }
    else { $results += 'База WardClick — ОШИБКА: откройте WardClick.sql в SSMS и нажмите Execute' }
} else {
    $results += 'База WardClick — не создана автоматически: откройте WardClick.sql в SSMS и нажмите Execute'
}

# проект для Android Studio
Say 'Распаковка проекта'
$src = Get-ChildItem $here -Filter 'WardClick-*-source.zip' | Select-Object -First 1
if ($src) {
    $dest = 'C:\WardClick\project'
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
    Expand-Archive -Path $src.FullName -DestinationPath $dest -Force
    $results += "Проект — распакован в $dest\WardClick"
} else { $results += 'Проект — архив с кодом не найден рядом со скриптом' }

Say 'ИТОГ' 'Green'
$results | ForEach-Object { Write-Host " - $_" }
Write-Host ''
Write-Host 'Дальше — по ИНСТРУКЦИЯ.txt, начиная с шага 3 (первый запуск Android Studio).'
Read-Host 'Нажмите Enter, чтобы закрыть окно'
