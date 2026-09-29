param([string]$PostgresBin, [string]$PlaywrightModule = $env:PLAYWRIGHT_MODULE)
. "$PSScriptRoot\ambiente.ps1"
if (-not $PostgresBin) {
    $installation = Get-ChildItem "$env:ProgramFiles\PostgreSQL\*" -Directory | Sort-Object Name -Descending | Select-Object -First 1
    $PostgresBin = Join-Path $installation.FullName 'bin'
}
if ($PlaywrightModule) { $env:PLAYWRIGHT_MODULE = $PlaywrightModule }
$env:TEST_PSQL = Join-Path $PostgresBin 'psql.exe'
$env:TEST_BASE_URL = 'http://localhost:8084'
$testData = Join-Path $projectRoot 'tmp\postgres-test'
$testLog = Join-Path $projectRoot 'tmp\postgres-test.log'
New-Item -ItemType Directory -Force (Join-Path $projectRoot 'tmp') | Out-Null
if (-not (Test-Path (Join-Path $testData 'PG_VERSION'))) {
    & (Join-Path $PostgresBin 'initdb.exe') -D $testData -U agendaplay_test -A trust --encoding=UTF8 --no-locale
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar PostgreSQL isolado.' }
}
& (Join-Path $PostgresBin 'pg_ctl.exe') -D $testData status *> $null
$alreadyRunning = $LASTEXITCODE -eq 0
$app = $null
try {
    $listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, 8084)
    try { $listener.Start() } finally { $listener.Stop() }
    if (-not $alreadyRunning) {
        & (Join-Path $PostgresBin 'pg_ctl.exe') -D $testData -l $testLog -o '-p 55432 -h 127.0.0.1' -w start
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar banco isolado.' }
    }
    $port = & $env:TEST_PSQL -h 127.0.0.1 -p 55432 -U agendaplay_test -d postgres -At -c 'SELECT inet_server_port()'
    if ($LASTEXITCODE -ne 0 -or $port.Trim() -ne '55432') { throw 'Banco de teste nao confirmado.' }
    # Only the browser-test schema in the isolated database is reset.
    & $env:TEST_PSQL -h 127.0.0.1 -p 55432 -U agendaplay_test -d postgres -v ON_ERROR_STOP=1 -c 'DROP SCHEMA IF EXISTS agendaplay_browser4 CASCADE'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar schema de navegador.' }
    & "$projectRoot\mvnw.cmd" -B '-DskipTests' package
    if ($LASTEXITCODE -ne 0) { throw 'Compilacao falhou.' }
    $arguments = @('-jar', 'target/agendaplay-0.1.0.jar', '--server.port=8084', '--server.address=127.0.0.1', '--spring.config.import=',
        '--spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/postgres', '--spring.datasource.username=agendaplay_test', '--spring.datasource.password=',
        '"--spring.datasource.hikari.connection-init-sql=SET search_path TO agendaplay_browser4, public"',
        '--spring.flyway.default-schema=agendaplay_browser4', '--spring.flyway.schemas=agendaplay_browser4',
        '--agendaplay.renovacao-automatica=false', '--spring.mail.host=127.0.0.1', '--spring.mail.port=2526',
        '--spring.mail.properties.mail.smtp.auth=false', '--spring.mail.properties.mail.smtp.starttls.enable=false', '--agendaplay.url-publica=http://localhost:8084')
    $app = Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin\java.exe') -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput 'tmp/browser-server.log' -RedirectStandardError 'tmp/browser-server-error.log'
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($app.HasExited) { throw 'Servidor encerrou; consulte tmp/browser-server.log.' }
        try { $ready = (Invoke-WebRequest 'http://127.0.0.1:8084' -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200 } catch { }
        if ($ready) { break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Servidor nao ficou pronto. Consulte tmp/browser-server.log.' }
    & node scripts/verificar_sprint3.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Regressao Sprint 3 falhou.' }
    Copy-Item 'tmp/sprint3-browser-result.json' 'docs/evidencias/sprint3/navegador-revalidado.json' -Force
    & node scripts/verificar_sprint4.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Sprint 4 falhou.' }
} finally {
    if ($app -and -not $app.HasExited) { Stop-Process -Id $app.Id }
    if (-not $alreadyRunning) { & (Join-Path $PostgresBin 'pg_ctl.exe') -D $testData -m fast -w stop }
}
