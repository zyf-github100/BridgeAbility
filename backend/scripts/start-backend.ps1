$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$appHome = Split-Path -Parent $scriptDir

if (-not $env:APP_NAME) { $env:APP_NAME = "bridgeability-backend" }
if (-not $env:JAVA_BIN) { $env:JAVA_BIN = "java" }
if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE = "prod" }
if (-not $env:APP_TIMEZONE) { $env:APP_TIMEZONE = "Asia/Shanghai" }
if (-not $env:APP_LOG_DIR) { $env:APP_LOG_DIR = Join-Path $appHome "logs" }
if (-not $env:APP_RUN_DIR) { $env:APP_RUN_DIR = Join-Path $appHome "run" }
if (-not $env:JVM_HEAP_DUMP_PATH) { $env:JVM_HEAP_DUMP_PATH = Join-Path $env:APP_LOG_DIR "heapdump.hprof" }
if (-not $env:JVM_GC_LOG_FILE) { $env:JVM_GC_LOG_FILE = Join-Path $env:APP_LOG_DIR "gc.log" }

New-Item -ItemType Directory -Force -Path $env:APP_LOG_DIR | Out-Null
New-Item -ItemType Directory -Force -Path $env:APP_RUN_DIR | Out-Null

if ($env:APP_JAR) {
    $jarPath = $env:APP_JAR
} else {
    $targetDir = Join-Path $appHome "target"
    if (Test-Path $targetDir) {
        $jarPath = Get-ChildItem -Path $targetDir -Filter *.jar -File |
            Where-Object { $_.Name -notlike "*.original" } |
            Sort-Object Name |
            Select-Object -Last 1 -ExpandProperty FullName
    } else {
        $jarPath = $null
    }
}

if (-not $jarPath -or -not (Test-Path $jarPath)) {
    throw "No runnable jar found. Build the project first with: mvn -DskipTests package"
}

if (-not $env:JVM_MEMORY_OPTS) {
    $env:JVM_MEMORY_OPTS = "-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=70.0"
}
if (-not $env:JVM_GC_OPTS) {
    $env:JVM_GC_OPTS = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200"
}
if (-not $env:JVM_DIAG_OPTS) {
    $env:JVM_DIAG_OPTS = "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$($env:JVM_HEAP_DUMP_PATH) -XX:+ExitOnOutOfMemoryError -Xlog:gc*:file=$($env:JVM_GC_LOG_FILE):time,uptime,level,tags:filecount=5,filesize=20M"
}
if (-not $env:JVM_SYSTEM_OPTS) {
    $env:JVM_SYSTEM_OPTS = "-Dfile.encoding=UTF-8 -Djava.awt.headless=true -Duser.timezone=$($env:APP_TIMEZONE)"
}

$argList = @()
foreach ($part in @($env:JVM_MEMORY_OPTS, $env:JVM_GC_OPTS, $env:JVM_DIAG_OPTS, $env:JVM_SYSTEM_OPTS, $env:JAVA_OPTS)) {
    if ($part) {
        $argList += [System.Management.Automation.PSParser]::Tokenize($part, [ref]$null) |
            Where-Object { $_.Type -in @("CommandArgument", "String") } |
            ForEach-Object { $_.Content }
    }
}

$argList += "-jar", $jarPath, "--spring.profiles.active=$($env:SPRING_PROFILES_ACTIVE)"

if ($env:SPRING_ARGS) {
    $argList += [System.Management.Automation.PSParser]::Tokenize($env:SPRING_ARGS, [ref]$null) |
        Where-Object { $_.Type -in @("CommandArgument", "String") } |
        ForEach-Object { $_.Content }
}

Write-Host "Starting $($env:APP_NAME)"
Write-Host "Jar: $jarPath"
Write-Host "Profile: $($env:SPRING_PROFILES_ACTIVE)"
Write-Host "Logs: $($env:APP_LOG_DIR)"

& $env:JAVA_BIN @argList
exit $LASTEXITCODE
