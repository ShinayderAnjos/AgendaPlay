param([switch]$SemCompilar)
. "$PSScriptRoot\ambiente.ps1"

$configFile = Join-Path $projectRoot 'config\application-local.properties'
if (-not (Test-Path -LiteralPath $configFile)) {
    Copy-Item -LiteralPath (Join-Path $projectRoot 'config\application-local.properties.example') -Destination $configFile
    Write-Host 'Preencha usuario e senha em config/application-local.properties e execute novamente.'
    exit 1
}
if (-not $SemCompilar) {
    & "$projectRoot\mvnw.cmd" -B '-DskipTests' package
    if ($LASTEXITCODE -ne 0) { throw 'A compilacao falhou. Confira a mensagem acima.' }
}
$jarFile = Join-Path $projectRoot 'target\agendaplay-0.1.0.jar'
if (-not (Test-Path -LiteralPath $jarFile)) { throw 'JAR nao encontrado. Execute iniciar.cmd para compilar.' }
Write-Host 'Ao aparecer Started AgendaPlayApplication, abra http://localhost:8080.'
Write-Host 'Para encerrar o servidor, pressione Ctrl+C.'
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar $jarFile
exit $LASTEXITCODE
