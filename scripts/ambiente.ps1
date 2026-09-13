$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot

function Select-AgendaJava {
    $javaFolders = @()
    if ($env:JAVA_HOME) { $javaFolders += $env:JAVA_HOME }
    $javaFolders += Get-ChildItem -Path "$env:ProgramFiles\Java\*", "$env:ProgramFiles\Eclipse Adoptium\*" -Directory -ErrorAction SilentlyContinue | Sort-Object { if ($_.Name -match '21') { 0 } else { 1 } } | Select-Object -ExpandProperty FullName
    foreach ($javaFolder in $javaFolders) {
        $javaExe = Join-Path $javaFolder 'bin\java.exe'
        $javacExe = Join-Path $javaFolder 'bin\javac.exe'
        if ((Test-Path -LiteralPath $javaExe) -and (Test-Path -LiteralPath $javacExe)) {
            $javaInfo = Get-Content -LiteralPath (Join-Path $javaFolder 'release') -Raw
            if ($javaInfo -match 'JAVA_VERSION="(\d+)' -and [int]$Matches[1] -ge 21) {
                $env:JAVA_HOME = $javaFolder
                return
            }
        }
    }
    throw 'JDK 21 ou superior nao encontrado. Instale o JDK e configure JAVA_HOME.'
}
Select-AgendaJava
