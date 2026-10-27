$ErrorActionPreference = 'Stop'

$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$compose = Join-Path $PSScriptRoot 'compose.example'
$secretDirectory = Join-Path $PSScriptRoot 'secrets'
$adminSecret = Join-Path $secretDirectory 'postgres_admin_password'
$catalogSecret = Join-Path $secretDirectory 'catalog_db_password'
$envFile = Join-Path $root '.env'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function New-LocalPassword {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

[IO.Directory]::CreateDirectory($secretDirectory) | Out-Null
if (-not (Test-Path -LiteralPath $adminSecret)) {
    [IO.File]::WriteAllText($adminSecret, (New-LocalPassword), $utf8)
}
if (-not (Test-Path -LiteralPath $catalogSecret)) {
    [IO.File]::WriteAllText($catalogSecret, (New-LocalPassword), $utf8)
}

$catalogPassword = [IO.File]::ReadAllText($catalogSecret, $utf8).Trim()
if ([string]::IsNullOrWhiteSpace($catalogPassword)) {
    throw 'The catalog password file is empty.'
}
if (-not (Test-Path -LiteralPath $envFile)) {
    $settings = "SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5433/catalog`nSPRING_DATASOURCE_USERNAME=catalog_app`nSPRING_DATASOURCE_PASSWORD=$catalogPassword`n"
    [IO.File]::WriteAllText($envFile, $settings, $utf8)
}

$envSettings = @{}
foreach ($line in [IO.File]::ReadAllLines($envFile, $utf8)) {
    if ($line -match '^\s*(#|$)') { continue }
    $parts = $line.Split('=', 2)
    if ($parts.Length -eq 2) { $envSettings[$parts[0].Trim()] = $parts[1].Trim() }
}
if ($envSettings['SPRING_DATASOURCE_URL'] -ne 'jdbc:postgresql://127.0.0.1:5433/catalog' -or
    $envSettings['SPRING_DATASOURCE_USERNAME'] -ne 'catalog_app' -or
    $envSettings['SPRING_DATASOURCE_PASSWORD'] -cne $catalogPassword) {
    throw 'Existing .env catalog settings do not match the local catalog secret; no file was overwritten.'
}

& docker compose -f $compose up -d --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL did not become healthy.' }
$container = (& docker compose -f $compose ps -q postgres).Trim()
if ([string]::IsNullOrWhiteSpace($container)) { throw 'PostgreSQL container was not found.' }

$escapedPassword = $catalogPassword.Replace("'", "''")
$sql = @'
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'catalog_app') THEN
        CREATE ROLE catalog_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD '__PASSWORD__';
    ELSE
        ALTER ROLE catalog_app WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD '__PASSWORD__';
    END IF;
END $$;

SELECT 'CREATE DATABASE catalog OWNER catalog_app'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'catalog')
\gexec

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_database
        WHERE datname = 'catalog' AND datdba <> (SELECT oid FROM pg_roles WHERE rolname = 'catalog_app')
    ) THEN
        RAISE EXCEPTION 'Existing catalog database has an unexpected owner';
    END IF;
END $$;

REVOKE CONNECT, TEMPORARY ON DATABASE postgres FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE template1 FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE catalog FROM PUBLIC;
'@.Replace('__PASSWORD__', $escapedPassword)

$result = $sql | & docker exec -i $container psql -X -q -v ON_ERROR_STOP=1 -U postgres -d postgres 2>&1
if ($LASTEXITCODE -ne 0) {
    $message = ($result | Out-String).Replace($catalogPassword, '<redacted>')
    throw "Catalog provisioning failed: $message"
}

Write-Output 'Catalog database and role are provisioned; local credentials remain in ignored files.'
