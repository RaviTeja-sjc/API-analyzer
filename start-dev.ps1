Write-Host "Starting API Analyzer Local Environment..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend; .\mvnw spring-boot:run -pl app-runner" -WindowStyle Normal
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm install; npm run dev" -WindowStyle Normal
