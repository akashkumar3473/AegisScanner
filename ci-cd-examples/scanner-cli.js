#!/usr/bin/env node

/**
 * AegisScanner CI/CD Pipeline integration tool
 * Usage: node scanner-cli.js <api-url> <email> <password> <repo-id> [max-fail-severity]
 */

const http = require('http');
const https = require('https');

const args = process.argv.slice(2);
if (args.length < 4) {
  console.error('Usage: node scanner-cli.js <api-url> <email> <password> <repo-id> [max-fail-severity: CRITICAL|HIGH|MEDIUM|LOW]');
  process.exit(1);
}

const [apiUrl, email, password, repoId, maxSeverity = 'CRITICAL'] = args;

const client = apiUrl.startsWith('https') ? https : http;

const postJson = (url, path, headers, body) => {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url + path);
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + urlObj.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    };

    const req = client.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            resolve(data);
          }
        } else {
          reject(new Error(`POST ${path} failed with code ${res.statusCode}: ${data}`));
        }
      });
    });

    req.on('error', reject);
    req.write(JSON.stringify(body));
    req.end();
  });
};

const getJson = (url, path, headers) => {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url + path);
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + urlObj.search,
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    };

    const req = client.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            resolve(data);
          }
        } else {
          reject(new Error(`GET ${path} failed with code ${res.statusCode}: ${data}`));
        }
      });
    });

    req.on('error', reject);
    req.end();
  });
};

async function run() {
  try {
    console.log('1. Authenticating with AegisScanner API...');
    const authData = await postJson(apiUrl, '/auth/login', {}, { email, password });
    const token = authData.token;
    const authHeader = { 'Authorization': `Bearer ${token}` };
    console.log(`✓ Authenticated as ${authData.name} (${authData.role})`);

    console.log(`2. Starting scan runner for Repository ID: ${repoId}...`);
    const scan = await postJson(apiUrl, '/scans/start', authHeader, { repoId: parseInt(repoId) });
    const scanId = scan.id;
    console.log(`✓ Scan triggered. Assigned Scan ID: #${scanId}`);

    console.log('3. Waiting for scan completion (polling state changes)...');
    let scanResult = null;
    const startTime = Date.now();
    
    while (true) {
      scanResult = await getJson(apiUrl, `/scans/${scanId}`, authHeader);
      console.log(`[Status: ${scanResult.status}] polling...`);

      if (scanResult.status === 'COMPLETED' || scanResult.status === 'FAILED') {
        break;
      }

      // Check timeout (10 mins)
      if (Date.now() - startTime > 10 * 60 * 1000) {
        throw new Error('Scan operation timed out after 10 minutes.');
      }

      await new Promise(r => setTimeout(r, 5000));
    }

    if (scanResult.status === 'FAILED') {
      console.error('✗ Scan failed to run. Build aborted.');
      process.exit(1);
    }

    console.log('\n--- Scan Completed ---');
    console.log(`Repository: ${scanResult.repository.name}`);
    console.log(`Maintainability: ${scanResult.maintainabilityIndex}/100`);
    console.log(`Complexity: ${scanResult.cyclomaticComplexity}`);
    console.log(`Duplicate Code: ${scanResult.duplicateCodePercentage}%`);
    console.log('\nVulnerability counts:');
    console.log(`- Critical: ${scanResult.criticalCount}`);
    console.log(`- High: ${scanResult.highCount}`);
    console.log(`- Medium: ${scanResult.mediumCount}`);
    console.log(`- Low: ${scanResult.lowCount}`);

    // Check thresholds
    const isFailed = 
      (maxSeverity === 'CRITICAL' && scanResult.criticalCount > 0) ||
      (maxSeverity === 'HIGH' && (scanResult.criticalCount > 0 || scanResult.highCount > 0)) ||
      (maxSeverity === 'MEDIUM' && (scanResult.criticalCount > 0 || scanResult.highCount > 0 || scanResult.mediumCount > 0)) ||
      (maxSeverity === 'LOW' && (scanResult.criticalCount > 0 || scanResult.highCount > 0 || scanResult.mediumCount > 0 || scanResult.lowCount > 0));

    if (isFailed) {
      console.error(`\n✗ Build failed: Vulnerabilities found exceed the maximum allowed severity threshold [${maxSeverity}]`);
      process.exit(1);
    } else {
      console.log('\n✓ Build Passed: Vulnerability counts are within threshold tolerances.');
      process.exit(0);
    }

  } catch (error) {
    console.error('✗ Pipeline Scanner Error:', error.message);
    process.exit(1);
  }
}

run();
