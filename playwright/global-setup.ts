/// <reference types="node" />
import { chromium, FullConfig } from '@playwright/test';

/**
 * Global setup: verify the Docker stack (frontend + backend/gateway) is running.
 * If not, fail fast with a helpful message.
 * 
 * This prevents tests from timing out mysteriously when Docker isn't up.
 */
async function globalSetup(config: FullConfig) {
  const baseURL = process.env.BASE_URL || 'http://localhost:3000';
  const gatewayHealthURL = 'http://localhost:8081/actuator/health'; // Gateway health endpoint
  const backendHealthURL = 'http://localhost:8080/actuator/health';  // Backend health endpoint

  console.log('\n🔍 Verifying Docker stack is running...\n');

  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  let allHealthy = true;
  const errors: string[] = [];

  // Check frontend
  try {
    console.log(`   Frontend (${baseURL})...`);
    const resp = await page.goto(baseURL, { timeout: 5000, waitUntil: 'domcontentloaded' });
    if (!resp || resp.status() >= 500) {
      errors.push(`❌ Frontend not responding correctly at ${baseURL} (status: ${resp?.status()})`);
      allHealthy = false;
    } else {
      console.log(`   ✅ Frontend reachable`);
    }
  } catch (e) {
    console.warn('Frontend check error:', e);
    errors.push(`❌ Frontend unreachable at ${baseURL}`);
    allHealthy = false;
  }

  // Check gateway health
  try {
    console.log(`   Gateway (${gatewayHealthURL})...`);
    const resp = await page.goto(gatewayHealthURL, { timeout: 5000 });
    if (!resp || resp.status() !== 200) {
      errors.push(`❌ Gateway health check failed at ${gatewayHealthURL}`);
      allHealthy = false;
    } else {
      console.log(`   ✅ Gateway healthy`);
    }
  } catch (e) {
    console.warn('Gateway check error:', e);
    errors.push(`❌ Gateway unreachable at ${gatewayHealthURL}`);
    allHealthy = false;
  }

  // Check backend health
  try {
    console.log(`   Backend (${backendHealthURL})...`);
    const resp = await page.goto(backendHealthURL, { timeout: 5000 });
    if (!resp || resp.status() !== 200) {
      errors.push(`❌ Backend health check failed at ${backendHealthURL}`);
      allHealthy = false;
    } else {
      console.log(`   ✅ Backend healthy`);
    }
  } catch (e) {
    console.warn('Backend check error:', e);
    errors.push(`❌ Backend unreachable at ${backendHealthURL}`);
    allHealthy = false;
  }

  await browser.close();

  if (!allHealthy) {
    console.error('\n❌ Docker stack is not fully running. Errors:\n');
    for (const err of errors) {
      console.error(`   ${err}`);
    }
    console.error('\n💡 Start the stack with:\n');
    console.error('   docker-compose up -d\n');
    console.error('   Wait ~30s for health checks, then retry tests.\n');
    throw new Error('Prerequisites not met: Docker stack not running');
  }

  console.log('\n✅ All stack health checks passed. Proceeding with tests.\n');
}

export default globalSetup;
