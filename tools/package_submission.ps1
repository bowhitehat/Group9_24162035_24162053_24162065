param(
    [string]$Repository = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path -LiteralPath $Repository).Path
$zipName = 'Group9_24162035_24162053_24162065.zip'
$destination = Join-Path $repo $zipName
$temporary = Join-Path $env:TEMP ("group9-submit-{0}.zip" -f [guid]::NewGuid().ToString('N'))

Push-Location -LiteralPath $repo
try {
    tar.exe -a -c -f $temporary `
        --exclude='./.git' `
        --exclude='./.m2' `
        --exclude='./target' `
        --exclude='./uploads' `
        --exclude='./artifacts' `
        --exclude='./.env' `
        --exclude="./$zipName" `
        .
    if ($LASTEXITCODE -ne 0) { throw 'Không thể tạo ZIP.' }

    $entries = @(tar.exe -tf $temporary)
    $forbidden = @($entries | Where-Object {
        $_ -match '(^|/)(\.git|\.m2|target|uploads|artifacts)(/|$)' -or
        $_ -match '(^|/)\.env$' -or
        $_ -match [regex]::Escape($zipName)
    })
    if ($forbidden.Count -gt 0) {
        throw "ZIP chứa đường dẫn bị cấm: $($forbidden -join ', ')"
    }

    Copy-Item -LiteralPath $temporary -Destination $destination -Force
    $hash = Get-FileHash -LiteralPath $destination -Algorithm SHA256
    Write-Output "Created: $destination"
    Write-Output "Entries: $($entries.Count)"
    Write-Output "SHA256: $($hash.Hash)"
}
finally {
    Pop-Location
    if (Test-Path -LiteralPath $temporary) {
        Remove-Item -LiteralPath $temporary -Force
    }
}
