
$ErrorActionPreference = "Stop"

function Test-Endpoint {
    param(
        [string]$Uri,
        [string]$Method,
        [object]$Body,
        [string]$Token
    )
    
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    try {
        $jsonBody = $null
        if ($Body) {
            $jsonBody = $Body | ConvertTo-Json -Depth 10
            # Fix empty array/object serialization if needed, though default usually works for simple objects
        }
        
        $response = Invoke-RestMethod -Uri $Uri -Method $Method -Body $jsonBody -ContentType "application/json" -Headers $headers
        Write-Host "SUCCESS: $Method $Uri" -ForegroundColor Green
        return $response
    }
    catch {
        Write-Host "FAILURE: $Method $Uri" -ForegroundColor Red
        if ($_.Exception.Response) {
            Write-Host "Status Code: " $_.Exception.Response.StatusCode.value__
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                Write-Host "Body: " $reader.ReadToEnd()
            }
            catch {}
        }
        else {
            Write-Host "Error: " $_.Exception.Message
        }
        return $null
    }
}

# 1. Login to get Token
$loginBody = @{
    email    = "testaudit4@example.com"
    password = "password123"
}
$loginResponse = Test-Endpoint -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody
$token = $loginResponse.token
Write-Host "Got Token: $token"

if (-not $token) {
    Write-Error "Failed to login. Exiting."
}

# 2. Create Post
$postBody = @{
    content  = "Test Post from Audit Script"
    imageUrl = "https://via.placeholder.com/150"
}
$postResponse = Test-Endpoint -Uri "http://localhost:8080/api/posts" -Method Post -Body $postBody -Token $token
$postId = $postResponse.id
Write-Host "Created Post ID: $postId"

if ($postId) {
    # 3. Like Post
    Test-Endpoint -Uri "http://localhost:8080/api/posts/$postId/like" -Method Post -Token $token

    # 4. Comment on Post
    $commentBody = @{
        content = "Test Comment"
    }
    Test-Endpoint -Uri "http://localhost:8080/api/comments/post/$postId" -Method Post -Body $commentBody -Token $token

    # 5. Delete Post (Cleanup)
    # Test-Endpoint -Uri "http://localhost:8080/api/posts/$postId" -Method Delete -Token $token
}

# 6. Get Notifications
Test-Endpoint -Uri "http://localhost:8080/api/notifications" -Method Get -Token $token

# 7. Create Story (Mock)
$storyBody = @{
    imageUrl = "https://via.placeholder.com/150"
    caption  = "Test Story"
}
Test-Endpoint -Uri "http://localhost:8080/api/stories" -Method Post -Body $storyBody -Token $token

# 8. Get Stories
Test-Endpoint -Uri "http://localhost:8080/api/stories" -Method Get -Token $token

Write-Host "Functional Test Complete"
