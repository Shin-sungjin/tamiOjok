<#
.SYNOPSIS
  Cloudflare Quick Tunnel의 현재 URL을 읽어와 .env의 CORS_ALLOWED_ORIGINS에 반영하고
  backend 컨테이너를 재기동합니다.

.DESCRIPTION
  Quick Tunnel은 docker compose로 스택을 띄울 때마다 https://<임의문자열>.trycloudflare.com
  주소가 새로 발급됩니다. 백엔드 CORS 허용 목록이 예전 주소로 남아 있으면 로그인 등
  모든 API 요청이 403으로 막히므로, 매번 이 스크립트로 동기화해야 합니다.

.USAGE
  프로젝트 루트(C:\dev\vibe)가 아니어도 상관없이 어디서든 실행 가능합니다 (스크립트 자신의
  위치 기준으로 ..\.env, ..\docker-compose.yml을 찾습니다). 반드시 Docker Desktop이 떠 있고
  Windows 호스트의 PowerShell에서 실행해야 합니다 (컨테이너 내부 X).

  docker compose up -d 로 스택을 띄운 뒤:
    .\scripts\refresh-tunnel-cors.ps1
#>

param(
    [int]$TimeoutSeconds = 30
)

$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$envPath = Join-Path $repoRoot '.env'

if (-not (Test-Path $envPath)) {
    Write-Error ".env 파일을 찾을 수 없습니다: $envPath (.env.example을 복사해서 먼저 만들어주세요)"
}

Push-Location $repoRoot
try {
    Write-Host "cloudflared 로그에서 Quick Tunnel URL을 찾는 중..."

    $tunnelUrl = $null
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $logs = docker compose logs cloudflared 2>$null
        $matches = [regex]::Matches(($logs -join "`n"), 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($matches.Count -gt 0) {
            $tunnelUrl = $matches[$matches.Count - 1].Value
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $tunnelUrl) {
        Write-Error "cloudflared 로그에서 trycloudflare.com 주소를 $TimeoutSeconds 초 안에 찾지 못했습니다. 'docker compose up -d'로 스택이 떠 있는지 확인해주세요."
    }

    Write-Host "현재 터널 주소: $tunnelUrl"

    $newLine = "CORS_ALLOWED_ORIGINS=http://localhost:5173,$tunnelUrl"
    $lines = Get-Content $envPath
    $updated = $false
    $unchanged = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^CORS_ALLOWED_ORIGINS=') {
            if ($lines[$i] -eq $newLine) {
                $unchanged = $true
            } else {
                $lines[$i] = $newLine
            }
            $updated = $true
            break
        }
    }

    if (-not $updated) {
        Write-Error ".env에서 CORS_ALLOWED_ORIGINS 라인을 찾지 못했습니다."
    }

    if ($unchanged) {
        Write-Host "CORS_ALLOWED_ORIGINS가 이미 최신 상태입니다. 재기동을 건너뜁니다."
        Write-Host ""
        Write-Host "접속 주소: $tunnelUrl"
        return
    }

    $lines | Set-Content -Path $envPath -Encoding utf8
    Write-Host ".env 갱신 완료 -> $newLine"

    Write-Host "backend 컨테이너 재기동 중..."
    docker compose up -d backend | Out-Null

    Write-Host ""
    Write-Host "완료. 접속 주소: $tunnelUrl"
}
finally {
    Pop-Location
}
