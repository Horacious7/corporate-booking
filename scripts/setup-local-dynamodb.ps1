# Setup DynamoDB Local tables for local development
# Prerequisites: Docker running with amazon/dynamodb-local on port 8000
# Usage: .\scripts\setup-local-dynamodb.ps1

$env:AWS_ACCESS_KEY_ID = "dummy"
$env:AWS_SECRET_ACCESS_KEY = "dummy"
$ENDPOINT = "http://localhost:8000"
$REGION = "eu-central-1"

Write-Host "=== DynamoDB Local Setup ===" -ForegroundColor Cyan

# Check if DynamoDB Local is running
try {
    aws dynamodb list-tables --endpoint-url $ENDPOINT --region $REGION 2>$null | Out-Null
} catch {
    Write-Host "ERROR: DynamoDB Local is not running on port 8000!" -ForegroundColor Red
    Write-Host "Start it with: docker run -d -p 8000:8000 amazon/dynamodb-local" -ForegroundColor Yellow
    exit 1
}

# Delete existing tables (ignore errors if they don't exist)
Write-Host "`nDeleting existing tables..." -ForegroundColor Yellow
aws dynamodb delete-table --table-name Bookings --endpoint-url $ENDPOINT --region $REGION 2>$null
aws dynamodb delete-table --table-name Employees --endpoint-url $ENDPOINT --region $REGION 2>$null
Start-Sleep -Seconds 1

# Create Bookings table with employee-index GSI
Write-Host "Creating Bookings table (PK: bookingReferenceId, GSI: employee-index)..." -ForegroundColor Green
aws dynamodb create-table `
    --endpoint-url $ENDPOINT `
    --region $REGION `
    --table-name Bookings `
    --attribute-definitions `
        AttributeName=bookingReferenceId,AttributeType=S `
        AttributeName=employeeId,AttributeType=S `
    --key-schema `
        AttributeName=bookingReferenceId,KeyType=HASH `
    --global-secondary-indexes "IndexName=employee-index,KeySchema=[{AttributeName=employeeId,KeyType=HASH}],Projection={ProjectionType=ALL}" `
    --billing-mode PAY_PER_REQUEST `
    2>$null | Out-Null

# Create Employees table with email-index GSI
Write-Host "Creating Employees table (PK: employeeId, GSI: email-index)..." -ForegroundColor Green
aws dynamodb create-table `
    --endpoint-url $ENDPOINT `
    --region $REGION `
    --table-name Employees `
    --attribute-definitions `
        AttributeName=employeeId,AttributeType=S `
        AttributeName=email,AttributeType=S `
    --key-schema `
        AttributeName=employeeId,KeyType=HASH `
    --global-secondary-indexes "IndexName=email-index,KeySchema=[{AttributeName=email,KeyType=HASH}],Projection={ProjectionType=ALL}" `
    --billing-mode PAY_PER_REQUEST `
    2>$null | Out-Null

# Verify
Write-Host "`n=== Verification ===" -ForegroundColor Cyan
$tables = aws dynamodb list-tables --endpoint-url $ENDPOINT --region $REGION 2>$null | ConvertFrom-Json
Write-Host "Tables: $($tables.TableNames -join ', ')" -ForegroundColor White

# Check GSIs
$bookingsDesc = aws dynamodb describe-table --table-name Bookings --endpoint-url $ENDPOINT --region $REGION 2>$null | ConvertFrom-Json
$employeesDesc = aws dynamodb describe-table --table-name Employees --endpoint-url $ENDPOINT --region $REGION 2>$null | ConvertFrom-Json

$bookingsGSI = $bookingsDesc.Table.GlobalSecondaryIndexes.IndexName
$employeesGSI = $employeesDesc.Table.GlobalSecondaryIndexes.IndexName

Write-Host "Bookings GSI: $bookingsGSI" -ForegroundColor White
Write-Host "Employees GSI: $employeesGSI" -ForegroundColor White

if ($bookingsGSI -eq "employee-index" -and $employeesGSI -eq "email-index") {
    Write-Host "`n✅ All tables created successfully with GSIs!" -ForegroundColor Green
} else {
    Write-Host "`n❌ Something went wrong - check GSIs!" -ForegroundColor Red
}

Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "  sam build" -ForegroundColor White
Write-Host "  sam local start-api --env-vars env.json" -ForegroundColor White

