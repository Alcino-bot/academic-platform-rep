$ErrorActionPreference = 'Continue'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $raiz

$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$base = 'http://localhost:8080'
$bd = 'plataforma_academica_dev'
$sufixo = Get-Date -Format 'yyyyMMddHHmmss'
$email = "ana$sufixo@example.org"
$sigla = "D$sufixo"
$palavra = 'UmaSenhaBoa123'
$saidaApp = Join-Path $env:TEMP "pad-app-$sufixo.out.log"
$erroApp = Join-Path $env:TEMP "pad-app-$sufixo.err.log"
$sqlTemp = Join-Path $env:TEMP "pad-verificacao-$sufixo.sql"
$relatorio = Join-Path $env:TEMP 'pad-verificacao-relatorio.txt'
$resultados = New-Object System.Collections.Generic.List[string]
$falhas = 0

function Verificar($nome, $condicao, $detalhe) {
    $estado = 'OK'
    if (-not $condicao) { $estado = 'FALHOU'; $script:falhas++ }
    $linha = "$estado | $nome | $detalhe"
    $resultados.Add($linha)
    Write-Host $linha
}

function LerLog($caminho) {
    try { return (Get-Content -LiteralPath $caminho -Raw -ErrorAction Stop) } catch { return '' }
}

function Sql($texto) {
    [System.IO.File]::WriteAllText($sqlTemp, $texto, (New-Object System.Text.UTF8Encoding($false)))
    $saida = & $psql -X -v ON_ERROR_STOP=1 -h localhost -U postgres -d $bd -f $sqlTemp 2>&1 | ForEach-Object { "$_" }
    if ($LASTEXITCODE -ne 0) {
        $script:falhas++
        $resultados.Add('FALHOU | psql | ' + ($saida -join ' '))
        Write-Host ('FALHOU | psql | ' + ($saida -join ' '))
    }
    return $saida
}

function Escalar($texto) {
    $saida = & $psql -X -At -h localhost -U postgres -d $bd -c $texto 2>&1 | ForEach-Object { "$_" }
    return ($saida -join '').Trim()
}

function Chamar($metodo, $caminho, $corpo) {
    $parametros = @{ Uri = "$base$caminho"; Method = $metodo; WebSession = $sessao; Headers = $cabecalhos; UseBasicParsing = $true }
    if ($corpo) { $parametros.ContentType = 'application/json'; $parametros.Body = $corpo }
    try {
        $r = Invoke-WebRequest @parametros
        return [pscustomobject]@{ Estado = [int]$r.StatusCode; Corpo = $r.Content }
    } catch {
        $resposta = $_.Exception.Response
        if ($resposta -eq $null) { return [pscustomobject]@{ Estado = 0; Corpo = $_.Exception.Message } }
        $leitor = New-Object System.IO.StreamReader($resposta.GetResponseStream())
        $texto = $leitor.ReadToEnd()
        return [pscustomobject]@{ Estado = [int]$resposta.StatusCode; Corpo = $texto }
    }
}

function Encerrar {
    if ($app -and -not $app.HasExited) { & taskkill.exe /PID $app.Id /T /F | Out-Null }
    Start-Sleep -Seconds 2
    $resultados.Add('')
    $resultados.Add("Total de verificacoes com falha: $falhas")
    [System.IO.File]::WriteAllLines($relatorio, $resultados)
    Remove-Item -LiteralPath $saidaApp, $erroApp, $sqlTemp -ErrorAction SilentlyContinue
    Remove-Item Env:DB_PASSWORD, Env:PGPASSWORD, Env:PGCLIENTENCODING -ErrorAction SilentlyContinue
    Write-Host ''
    Write-Host "Relatorio: $relatorio"
    if ($falhas -eq 0) { exit 0 } else { exit 1 }
}

$s = Read-Host 'Palavra-passe do postgres' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $s).Password
$env:PGPASSWORD = $env:DB_PASSWORD
$env:PGCLIENTENCODING = 'UTF8'

$ping = & $psql -X -At -h localhost -U postgres -d $bd -c "SELECT 'ligacao ok'" 2>&1 | ForEach-Object { "$_" }
Verificar 'Ligacao a base de desenvolvimento' ($LASTEXITCODE -eq 0) ($ping -join ' ')
if ($falhas -gt 0) { $app = $null; Encerrar }

$argumentos = @('spring-boot:run', '"-Dspring-boot.run.jvmArguments=-Xmx512m -XX:TieredStopAtLevel=1"')
$app = Start-Process -FilePath 'mvn.cmd' -ArgumentList $argumentos -WorkingDirectory $raiz -RedirectStandardOutput $saidaApp -RedirectStandardError $erroApp -PassThru -WindowStyle Hidden

$arrancou = $false
for ($i = 0; $i -lt 120; $i++) {
    Start-Sleep -Seconds 5
    if ($app.HasExited) { break }
    if ((LerLog $saidaApp) -match 'Started PlataformaAcademicaApplication') { $arrancou = $true; break }
}
Verificar 'A aplicacao arrancou' $arrancou "segundos de espera ate $($i * 5)"
if (-not $arrancou) {
    $resultados.Add('--- fim do registo da aplicacao ---')
    foreach ($linha in ((LerLog $saidaApp) -split "`n" | Select-Object -Last 30)) { $resultados.Add($linha) }
    foreach ($linha in ((LerLog $erroApp) -split "`n" | Select-Object -Last 15)) { $resultados.Add($linha) }
    Encerrar
}
$linhasFlyway = (LerLog $saidaApp) -split "`n" | Where-Object { $_ -match 'Successfully applied|is up to date' } | Select-Object -First 2
foreach ($linha in $linhasFlyway) { $resultados.Add('Flyway: ' + $linha.Trim()) }

$sessao = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$cabecalhos = @{}

$r = Chamar 'GET' '/api/estado' $null
$j = $null
if ($r.Estado -eq 200) { $j = $r.Corpo | ConvertFrom-Json }
Verificar 'GET /api/estado com as migracoes aplicadas' (($r.Estado -eq 200) -and ($j -ne $null) -and ([int]$j.ultimaMigracao -ge 20)) "estado=$($r.Estado) migracoes=$($j.migracoes) ultima=$($j.ultimaMigracao)"

$r = Chamar 'GET' '/api/auth/csrf' $null
$tokenCsrf = ''
if ($r.Estado -eq 200) { $tokenCsrf = ($r.Corpo | ConvertFrom-Json).token }
Verificar 'GET /api/auth/csrf devolve um token' (($r.Estado -eq 200) -and ($tokenCsrf.Length -gt 0)) "estado=$($r.Estado)"
$cabecalhos = @{ 'X-CSRF-TOKEN' = $tokenCsrf }

$corpoRegisto = '{"nomeCompleto":"Ana Teste","email":"' + $email + '"}'
$r = Chamar 'POST' '/api/contas/registar' $corpoRegisto
Verificar 'Registar conta nova (202)' ($r.Estado -eq 202) "estado=$($r.Estado)"

Start-Sleep -Seconds 1
$ligacoes = [regex]::Matches((LerLog $saidaApp), 'activar-conta\?token=([A-Za-z0-9_-]+)')
Verificar 'A ligacao de activacao aparece no registo da aplicacao' ($ligacoes.Count -eq 1) "ligacoes encontradas=$($ligacoes.Count)"
$token = ''
if ($ligacoes.Count -gt 0) { $token = $ligacoes[$ligacoes.Count - 1].Groups[1].Value }

$r = Chamar 'POST' '/api/contas/registar' $corpoRegisto
Start-Sleep -Seconds 1
$ligacoes2 = [regex]::Matches((LerLog $saidaApp), 'activar-conta\?token=([A-Za-z0-9_-]+)')
Verificar 'Segundo registo no intervalo minimo: 202 e nenhum email novo' (($r.Estado -eq 202) -and ($ligacoes2.Count -eq 1)) "estado=$($r.Estado) ligacoes=$($ligacoes2.Count)"

$corpoEntrar = '{"email":"' + $email + '","palavraPasse":"' + $palavra + '"}'
$r = Chamar 'POST' '/api/auth/entrar' $corpoEntrar
Verificar 'Conta pendente nao consegue entrar (401)' ($r.Estado -eq 401) "estado=$($r.Estado)"

$r = Chamar 'POST' '/api/contas/activar' ('{"token":"' + $token + '","palavraPasse":"curta"}')
Verificar 'Palavra-passe fraca recusada (422)' ($r.Estado -eq 422) "estado=$($r.Estado)"

$corpoActivar = '{"token":"' + $token + '","palavraPasse":"' + $palavra + '"}'
$r = Chamar 'POST' '/api/contas/activar' $corpoActivar
Verificar 'Activar com o mesmo token depois da recusa (200)' ($r.Estado -eq 200) "estado=$($r.Estado)"

$r = Chamar 'POST' '/api/contas/activar' $corpoActivar
Verificar 'Token de uso unico: segunda activacao recusada (422)' ($r.Estado -eq 422) "estado=$($r.Estado)"

$r = Chamar 'POST' '/api/auth/entrar' $corpoEntrar
$j = $null
if ($r.Estado -eq 200) { $j = $r.Corpo | ConvertFrom-Json }
Verificar 'Entrar com a conta activada (200) e receber novo token CSRF' (($r.Estado -eq 200) -and ($j -ne $null) -and ($j.csrfToken.Length -gt 0)) "estado=$($r.Estado)"
if ($j -ne $null) { $cabecalhos = @{ 'X-CSRF-TOKEN' = $j.csrfToken } }
Verificar 'Sem vinculos a lista de instituicoes esta vazia' ((($j.instituicoesDisponiveis | Measure-Object).Count) -eq 0) "instituicoes=$(($j.instituicoesDisponiveis | Measure-Object).Count)"

$semente = @"
INSERT INTO tipo_instituicao (nome, estado, data_aprovacao) VALUES ('Universidade', 'APROVADA', now()) ON CONFLICT DO NOTHING;
INSERT INTO instituicao (nome, sigla, tipo_instituicao_id, estado) SELECT 'Instituicao Demo $sufixo', '$sigla', id, 'ACTIVA' FROM tipo_instituicao WHERE lower(btrim(nome)) = 'universidade';
INSERT INTO vinculo_institucional (utilizador_id, instituicao_id, tipo_vinculo, estado) SELECT u.id, i.id, 'ESTUDANTE', 'ACTIVO' FROM utilizador u, instituicao i WHERE u.email = '$email' AND i.sigla = '$sigla';
INSERT INTO vinculo_papel (vinculo_id, instituicao_id, papel_id) SELECT v.id, v.instituicao_id, p.id FROM vinculo_institucional v, utilizador u, papel p WHERE u.email = '$email' AND v.utilizador_id = u.id AND p.codigo = 'ESTUDANTE';
"@
Sql $semente | Out-Null

$r = Chamar 'GET' '/api/auth/eu' $null
$j = $null
if ($r.Estado -eq 200) { $j = $r.Corpo | ConvertFrom-Json }
$totalInstituicoes = ($j.instituicoesDisponiveis | Measure-Object).Count
Verificar 'Sem voltar a entrar a nova instituicao aparece (calculada a cada pedido)' (($r.Estado -eq 200) -and ($totalInstituicoes -eq 1)) "estado=$($r.Estado) instituicoes=$totalInstituicoes"
$idInstituicao = ''
if ($totalInstituicoes -ge 1) { $idInstituicao = @($j.instituicoesDisponiveis)[0].id }

$r = Chamar 'POST' '/api/auth/instituicao' ('{"instituicaoId":"' + $idInstituicao + '"}')
$j = $null
if ($r.Estado -eq 200) { $j = $r.Corpo | ConvertFrom-Json }
$autoridades = @($j.autoridades)
Verificar 'Escolher a instituicao (200) com PAPEL_ESTUDANTE e VINCULO_ESTUDANTE' (($r.Estado -eq 200) -and ($autoridades -contains 'PAPEL_ESTUDANTE') -and ($autoridades -contains 'VINCULO_ESTUDANTE')) "estado=$($r.Estado) autoridades=$($autoridades -join ',')"

$r = Chamar 'GET' '/api/auth/eu' $null
$j = $null
if ($r.Estado -eq 200) { $j = $r.Corpo | ConvertFrom-Json }
Verificar 'A sessao lembra a instituicao activa' (($r.Estado -eq 200) -and ($j.instituicaoActivaId -eq $idInstituicao)) "estado=$($r.Estado)"

Sql "UPDATE vinculo_institucional SET estado = 'TERMINADO', data_fim = CURRENT_DATE WHERE utilizador_id = (SELECT id FROM utilizador WHERE email = '$email')" | Out-Null
$r = Chamar 'GET' '/api/auth/eu' $null
$j = $null
if ($r.Estado -eq 200) { $j = $r.Corpo | ConvertFrom-Json }
$autoridades = @($j.autoridades)
Verificar 'Vinculo terminado a meio da sessao: perde a instituicao e os papeis no pedido seguinte' (($r.Estado -eq 200) -and ($j.instituicaoActivaId -eq $null) -and ($autoridades -notcontains 'PAPEL_ESTUDANTE')) "estado=$($r.Estado) autoridades=$($autoridades -join ',')"

Sql "UPDATE utilizador SET estado = 'SUSPENSO' WHERE email = '$email'" | Out-Null
$r = Chamar 'GET' '/api/auth/eu' $null
Verificar 'Conta suspensa a meio da sessao: o pedido seguinte da 401' ($r.Estado -eq 401) "estado=$($r.Estado)"
Sql "UPDATE utilizador SET estado = 'ACTIVO' WHERE email = '$email'" | Out-Null

$sessao = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$r = Chamar 'GET' '/api/auth/csrf' $null
$cabecalhos = @{ 'X-CSRF-TOKEN' = ($r.Corpo | ConvertFrom-Json).token }
$estadosErrados = @()
for ($i = 1; $i -le 5; $i++) {
    $r = Chamar 'POST' '/api/auth/entrar' ('{"email":"' + $email + '","palavraPasse":"PalavraErrada' + $i + '"}')
    $estadosErrados += $r.Estado
}
Verificar 'Cinco palavras-passe erradas dao 401 todas' (($estadosErrados | Where-Object { $_ -eq 401 }).Count -eq 5) "estados=$($estadosErrados -join ',')"
$r = Chamar 'POST' '/api/auth/entrar' $corpoEntrar
Verificar 'Conta bloqueada: ate a palavra-passe certa da 401' ($r.Estado -eq 401) "estado=$($r.Estado)"
$bloqueada = Escalar "SELECT (bloqueado_ate IS NOT NULL AND bloqueado_ate > now()) FROM utilizador WHERE email = '$email'"
Verificar 'A base regista o bloqueio' ($bloqueada -eq 't') "bloqueada=$bloqueada"

Sql "UPDATE utilizador SET bloqueado_ate = now() - interval '1 minute' WHERE email = '$email'" | Out-Null
$r = Chamar 'POST' '/api/auth/entrar' $corpoEntrar
$j = $null
if ($r.Estado -eq 200) { $j = $r.Corpo | ConvertFrom-Json }
Verificar 'Depois de o bloqueio expirar a palavra-passe certa entra (200)' ($r.Estado -eq 200) "estado=$($r.Estado)"
if ($j -ne $null) { $cabecalhos = @{ 'X-CSRF-TOKEN' = $j.csrfToken } }

$r = Chamar 'POST' '/api/auth/sair' $null
Verificar 'Sair (204)' ($r.Estado -eq 204) "estado=$($r.Estado)"
$r = Chamar 'GET' '/api/auth/eu' $null
Verificar 'Depois de sair o pedido da 401' ($r.Estado -eq 401) "estado=$($r.Estado)"

Encerrar