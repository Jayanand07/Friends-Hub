
$ErrorActionPreference = "Stop"

function Test-Endpoint {
    param(
        [string]$Uri,
        [string]$Method,
        [object]$Body
    )
    
    try {
        $jsonBody = $Body | ConvertTo-Json
        $response = Invoke-RestMethod -Uri $Uri -Method $Method -Body $jsonBody -ContentType "application/json"
        Write-Host "SUCCESS: $Method $Uri" -ForegroundColor Green
        return $response
    } catch {
        Write-Host "FAILURE: $Method $Uri" -ForegroundColor Red
        if ($_.Exception.Response) {
             Write-Host "Status Code: " $_.Exception.Response.StatusCode.value__
             $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
             Write-Host "Body: " $reader.ReadToEnd()
        } else {
             Write-Host "Error: " $_.Exception.Message
        }
        return $null
    }
}

# 1. Register
$registerBody = @{
    firstName = "Test"
    lastName = "User"
    email = "testaudit4@example.com"
    password = "password123"
}
$registerResponse = Test-Endpoint -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $registerBody

if ($registerResponse) {
    Write-Host "Response: $registerResponse"
}

# 2. Login
$loginBody = @{
    email = "testaudit4@example.com"
    password = "password123"
}
$loginResponse = Test-Endpoint -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody

if ($loginResponse) {
    Write-Host "Login Token: $($loginResponse.token)"
}
