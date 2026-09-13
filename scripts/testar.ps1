param([string]$PostgresBin)
. "$PSScriptRoot\ambiente.ps1"
if (-not $PostgresBin) {
    $installation = Get-ChildItem -Path "$env:ProgramFiles\PostgreSQL\*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($installation) { $PostgresBin = Join-Path $installation.FullName 'bin' }
}
if (-not $PostgresBin -or -not (Test-Path -LiteralPath (Join-Path $PostgresBin 'initdb.exe'))) {
    throw 'PostgreSQL nao encontrado. Informe -PostgresBin com a pasta bin do PostgreSQL 17.'
}
$testData = Join-Path $projectRoot 'tmp\postgres-test'
$testLog = Join-Path $projectRoot 'tmp\postgres-test.log'
New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot 'tmp') | Out-Null
if (-not (Test-Path -LiteralPath (Join-Path $testData 'PG_VERSION'))) {
    & (Join-Path $PostgresBin 'initdb.exe') -D $testData -U agendaplay_test -A trust --encoding=UTF8 --no-locale
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel preparar o banco isolado.' }
}
& (Join-Path $PostgresBin 'pg_ctl.exe') -D $testData status *> $null
$alreadyRunning = $LASTEXITCODE -eq 0
if (-not $alreadyRunning) {
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $testData -l $testLog -o '-p 55432 -h 127.0.0.1' -w start
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel iniciar a instancia de testes na porta 55432. Confira tmp/postgres-test.log.' }
}
try {
    & "$projectRoot\mvnw.cmd" -B test
    $testResult = $LASTEXITCODE
} finally {
    if (-not $alreadyRunning) { & (Join-Path $PostgresBin 'pg_ctl.exe') -D $testData -m fast -w stop }
}
if ($testResult -ne 0) { throw 'Algum teste falhou. Consulte target/surefire-reports.' }
Write-Host 'Testes aprovados. Consulte target/surefire-reports.'
