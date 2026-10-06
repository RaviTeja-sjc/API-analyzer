import { test, expect } from '@playwright/test';

// End-to-End Production Smoke Test (Playwright)
// Assumes PLAYWRIGHT_TEST_BASE_URL is passed (e.g. https://api-analyzer-frontend.onrender.com)
// Run with: npx playwright test tests/smoke.spec.ts

test.describe('E2E Production Smoke Test - Render to Supabase', () => {

  const timestamp = Date.now();
  const testEmail = `smoketest_${timestamp}@example.com`;
  const testPassword = 'SecurePassword123!';

  test('System Health Checks (Backend & Database)', async ({ request, baseURL }) => {
    // 1. Verify backend health via Frontend environment variable route (CORS & HTTPS implicitly tested)
    const backendUrl = process.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
    // Actually hit the actuator endpoint to verify Render Backend <-> Supabase DB connectivity
    const healthUrl = backendUrl.replace('/api/v1', '/actuator/health/readiness');
    
    const response = await request.get(healthUrl);
    expect(response.status()).toBe(200);
    
    const body = await response.json();
    expect(body.status).toBe('UP');
  });

  test('Browser E2E Workflow: Auth -> Project -> Upload -> Analysis', async ({ page }) => {
    // 1. Navigate to deployed Render frontend over HTTPS
    await page.goto('/');
    
    // 2. Authentication (Register) - Tests DB Persistence
    await page.click('text="Register"');
    await page.fill('input[type="text"]', 'E2E Smoke Tester');
    await page.fill('input[type="email"]', testEmail);
    await page.fill('input[type="password"]', testPassword);
    await page.click('button[type="submit"]');

    // Wait for Dashboard redirect indicating successful JWT generation and storage
    await expect(page.locator('text="API Analyzer"')).toBeVisible({ timeout: 10000 });

    // 3. Create Project
    await page.click('text="New Project"');
    await page.fill('input[name="projectName"]', `E2E API ${timestamp}`);
    await page.fill('input[name="repoUrl"]', 'https://github.com/apianalyzer/mock-repo');
    await page.click('text="Create Project"');
    await expect(page.locator(`text="E2E API ${timestamp}"`)).toBeVisible();

    // 4. OpenAPI Specification Upload
    await page.click(`text="E2E API ${timestamp}"`);
    // Mock the file upload
    const mockSpec = JSON.stringify({
      openapi: '3.0.0',
      info: { title: 'Smoke API', version: '1.0.0' },
      paths: { '/users': { get: { responses: { '200': { description: 'OK' } } } } }
    });
    
    await page.setInputFiles('input[type="file"]', {
      name: 'v1.json',
      mimeType: 'application/json',
      buffer: Buffer.from(mockSpec)
    });
    
    await page.click('text="Upload Spec"');
    await expect(page.locator('text="v1.json uploaded successfully"')).toBeVisible();

    // 5. Trigger Analysis (Tests Backend Async Orchestrator)
    await page.click('text="Run Analysis"');
    
    // 6. Verify Reports & Migration Suggestions (Tests Graph Traversal & DB Persistence)
    await expect(page.locator('text="Analysis Complete"')).toBeVisible({ timeout: 15000 });
    await page.click('text="View Report"');
    await expect(page.locator('text="Migration Suggestions"')).toBeVisible();
    
    // 7. Verify Network Error Handling (Simulate offline)
    await page.context().setOffline(true);
    await page.click('text="Dashboard"'); // Attempt navigation fetching data
    await expect(page.locator('text="Network timeout"').or(page.locator('text="API Error"'))).toBeVisible();
  });
});
