param(
    [string]$Repository = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path -LiteralPath $Repository).Path
$packageName = 'Group9_24162035_24162053_24162065'
$zipName = "$packageName.zip"
$destination = Join-Path $repo $zipName
$temporaryRoot = Join-Path $env:TEMP ("group9-submit-{0}" -f [guid]::NewGuid().ToString('N'))
$packageRoot = Join-Path $temporaryRoot $packageName
$temporaryZip = Join-Path $env:TEMP ("group9-submit-{0}.zip" -f [guid]::NewGuid().ToString('N'))

function Copy-RequiredFile {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][string]$DestinationDirectory
    )

    $source = Join-Path $repo $RelativePath
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "Thiếu tệp bắt buộc: $RelativePath"
    }
    New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $DestinationDirectory -Force
}

function Copy-RequiredDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][string]$DestinationDirectory
    )

    $source = Join-Path $repo $RelativePath
    if (-not (Test-Path -LiteralPath $source -PathType Container)) {
        throw "Thiếu thư mục bắt buộc: $RelativePath"
    }
    New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $DestinationDirectory -Recurse -Force
}

try {
    New-Item -ItemType Directory -Path $packageRoot -Force | Out-Null

    Copy-RequiredFile 'BaoCao_DoAn.docx' $packageRoot
    Copy-RequiredFile 'README.md' $packageRoot
    Copy-RequiredFile 'SUBMISSION_CHECKLIST.md' $packageRoot
    Copy-RequiredDirectory 'diagrams' $packageRoot
    Copy-RequiredDirectory 'screenshots' $packageRoot
    Copy-RequiredDirectory 'deployment' $packageRoot

    $sourceCodeRoot = Join-Path $packageRoot 'source-code'
    New-Item -ItemType Directory -Path $sourceCodeRoot -Force | Out-Null

    @(
        '.dockerignore',
        '.env.example',
        '.gitignore',
        'docker-compose.yml',
        'Dockerfile',
        'pom.xml'
    ) | ForEach-Object { Copy-RequiredFile $_ $sourceCodeRoot }

    @(
        'docs',
        'sql',
        'src',
        'tools'
    ) | ForEach-Object { Copy-RequiredDirectory $_ $sourceCodeRoot }

    Push-Location -LiteralPath $temporaryRoot
    try {
        tar.exe -a -c -f $temporaryZip $packageName
        if ($LASTEXITCODE -ne 0) { throw 'Không thể tạo ZIP.' }
    }
    finally {
        Pop-Location
    }

    $entries = @(tar.exe -tf $temporaryZip | ForEach-Object { $_ -replace '\\', '/' })
    $requiredEntries = @(
        "$packageName/BaoCao_DoAn.docx",
        "$packageName/README.md",
        "$packageName/SUBMISSION_CHECKLIST.md",
        "$packageName/diagrams/",
        "$packageName/screenshots/",
        "$packageName/source-code/",
        "$packageName/deployment/"
    )

    foreach ($requiredEntry in $requiredEntries) {
        if (-not ($entries | Where-Object { $_ -eq $requiredEntry -or $_ -like "$requiredEntry*" })) {
            throw "ZIP thiếu đường dẫn bắt buộc: $requiredEntry"
        }
    }

    $forbidden = @($entries | Where-Object {
        $_ -match '(^|/)(\.git|\.m2|target|uploads|artifacts)(/|$)' -or
        $_ -match '(^|/)\.env$' -or
        $_ -match [regex]::Escape("/$zipName")
    })
    if ($forbidden.Count -gt 0) {
        throw "ZIP chứa đường dẫn bị cấm: $($forbidden -join ', ')"
    }

    Copy-Item -LiteralPath $temporaryZip -Destination $destination -Force
    $hash = Get-FileHash -LiteralPath $destination -Algorithm SHA256
    Write-Output "Created: $destination"
    Write-Output "Root: $packageName/"
    Write-Output "Entries: $($entries.Count)"
    Write-Output "SHA256: $($hash.Hash)"
}
finally {
    if (Test-Path -LiteralPath $temporaryRoot) {
        Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
    }
    if (Test-Path -LiteralPath $temporaryZip) {
        Remove-Item -LiteralPath $temporaryZip -Force
    }
}
