param(
    [string]$JmeterHome = "",
    [int]$Threads = 10,
    [int]$RampUp = 10,
    [int]$Duration = 180
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'No se encontro Java. Instala un JDK/JRE y vuelve a ejecutar.'
}

$jar = Join-Path $root 'target\product-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    Write-Host 'Compilando el microservicio...'
    & (Join-Path $root 'mvnw.cmd') -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion del microservicio.' }
}

function Find-JmeterBat {
    param([string[]]$Roots)

    foreach ($rootPath in $Roots) {
        if ([string]::IsNullOrWhiteSpace($rootPath) -or -not (Test-Path -LiteralPath $rootPath)) {
            continue
        }
        $direct = Join-Path $rootPath 'bin\jmeter.bat'
        if (Test-Path -LiteralPath $direct) { return $direct }

        $children = Get-ChildItem -LiteralPath $rootPath -Directory -ErrorAction SilentlyContinue
        foreach ($child in $children) {
            $nested = Join-Path $child.FullName 'bin\jmeter.bat'
            if (Test-Path -LiteralPath $nested) { return $nested }
        }
    }
    return $null
}

$jmeterBat = $null
if (-not [string]::IsNullOrWhiteSpace($JmeterHome)) {
    $candidate = Join-Path $JmeterHome 'bin\jmeter.bat'
    if (Test-Path -LiteralPath $candidate) { $jmeterBat = $candidate }
}

if (-not $jmeterBat -and $env:JMETER_HOME) {
    $candidate = Join-Path $env:JMETER_HOME 'bin\jmeter.bat'
    if (Test-Path -LiteralPath $candidate) { $jmeterBat = $candidate }
}

if (-not $jmeterBat) {
    $inPath = Get-Command 'jmeter.bat' -ErrorAction SilentlyContinue
    if (-not $inPath) { $inPath = Get-Command 'jmeter' -ErrorAction SilentlyContinue }
    if ($inPath) { $jmeterBat = $inPath.Source }
}

if (-not $jmeterBat) {
    $jmeterBat = Find-JmeterBat -Roots @(
        (Join-Path $root 'jmeter'),
        (Join-Path $root 'tools'),
        "$env:USERPROFILE\Downloads\apache-jmeter-5.6.3",
        "$env:USERPROFILE\Downloads",
        'C:\apache-jmeter-5.6.3',
        'C:\jmeter',
        "$env:ProgramFiles\apache-jmeter-5.6.3"
    )
}

if (-not $jmeterBat) {
    Write-Host 'JMeter no encontrado. Descargando Apache JMeter 5.6.3 portable...'
    $tools = Join-Path $root 'tools'
    New-Item -ItemType Directory -Path $tools -Force | Out-Null
    $zip = Join-Path $tools 'apache-jmeter-5.6.3.zip'
    $url = 'https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.zip'
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    Expand-Archive -LiteralPath $zip -DestinationPath $tools -Force
    $jmeterBat = Join-Path $tools 'apache-jmeter-5.6.3\bin\jmeter.bat'
    if (-not (Test-Path -LiteralPath $jmeterBat)) {
        throw 'No se pudo preparar JMeter automaticamente.'
    }
}

$port = 8086
$startedHere = $false
$appProcess = $null
$outLog = Join-Path $root 'target\app-mock.out.log'
$errLog = Join-Path $root 'target\app-mock.err.log'

$listening = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if (-not $listening) {
    Write-Host "Iniciando microservicio con perfil mock en el puerto $port..."
    $appProcess = Start-Process -FilePath 'java' `
        -ArgumentList @('-jar', $jar, '--spring.profiles.active=mock') `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -WindowStyle Hidden `
        -PassThru
    $startedHere = $true
}
else {
    Write-Host "El puerto $port ya esta en uso; se usara el servicio existente."
}

try {
    Write-Host 'Esperando que el microservicio responda...'
    $deadline = (Get-Date).AddSeconds(90)
    $ready = $false
    while ((Get-Date) -lt $deadline) {
        if ($startedHere -and $appProcess.HasExited) {
            throw "El microservicio termino antes de iniciar. Revisa: $outLog"
        }
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$port/api/v1/products" -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        }
        catch {
            Start-Sleep -Milliseconds 700
        }
    }

    if (-not $ready) {
        throw "El microservicio no respondio en 90 segundos. Revisa: $outLog"
    }
    Write-Host 'Microservicio listo (datos en memoria).'

    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $jtl = Join-Path $root "resultado-$stamp.jtl"
    $jmx = Join-Path $root 'load-test-products.jmx'
    $runJmx = Join-Path $root 'target\load-test-products-run.jmx'

    $jmxContent = Get-Content -Raw -LiteralPath $jmx
    $jmxContent = [regex]::Replace($jmxContent, '(<stringProp name="ThreadGroup\.num_threads">).*?(</stringProp>)', '${1}' + $Threads + '${2}')
    $jmxContent = [regex]::Replace($jmxContent, '(<stringProp name="ThreadGroup\.ramp_time">).*?(</stringProp>)', '${1}' + $RampUp + '${2}')
    $jmxContent = [regex]::Replace($jmxContent, '(<stringProp name="ThreadGroup\.duration">).*?(</stringProp>)', '${1}' + $Duration + '${2}')
    Set-Content -LiteralPath $runJmx -Value $jmxContent -Encoding UTF8

    Write-Host "Ejecutando JMeter: $Threads usuarios, ramp-up $RampUp s, duracion $Duration s..."
    & $jmeterBat -n -t $runJmx -l $jtl

    if ($LASTEXITCODE -ne 0) {
        throw "JMeter termino con codigo $LASTEXITCODE. Revisa $jtl"
    }

    Write-Host ''
    Write-Host 'Prueba de carga finalizada correctamente.'
    Write-Host "Resultados: $jtl"
}
finally {
    if ($startedHere -and $appProcess -and -not $appProcess.HasExited) {
        Write-Host 'Deteniendo microservicio...'
        Stop-Process -Id $appProcess.Id -Force -ErrorAction SilentlyContinue
    }
}
