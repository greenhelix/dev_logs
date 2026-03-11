const cors = require('cors');
const express = require('express');
const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');
const { onRequest } = require('firebase-functions/v2/https');

admin.initializeApp();

const app = express();
app.use(cors({ origin: true }));
app.use(express.json({ limit: '10mb' }));

const databaseId = process.env.FIRESTORE_DATABASE_ID || 'google-auth';
const db = getFirestore(admin.app(), databaseId);

app.get('/api/health', (_request, response) => {
  response.json({
    ok: true,
    databaseId,
    mode: 'dashboard-upload-proxy',
  });
});

app.get('/api/test-cases', async (request, response) => {
  response.json({
    documents: await listCollection('TestCases', request.query.limit),
  });
});

app.get('/api/failed-tests', async (request, response) => {
  response.json({
    documents: await listCollection('FailedTests', request.query.limit),
  });
});

app.get('/api/test-metrics', async (request, response) => {
  response.json({
    documents: await listCollection('TestMetrics', request.query.limit),
  });
});

app.post('/api/upload-health', (_request, response) => {
  response.json({
    ok: true,
    databaseId,
    canWrite: true,
  });
});

app.post('/api/sync-import', async (request, response) => {
  const payload = request.body || {};
  await upsertBundle(payload);
  response.json({
    ok: true,
    databaseId,
  });
});

app.post('/api/redmine-health', async (request, response) => {
  try {
    const { baseUrl, apiKey, projectId } = request.body || {};
    const headers = {
      'X-Redmine-API-Key': apiKey || '',
    };
    const normalizedBaseUrl = normalizeBaseUrl(baseUrl);
    const projectUrl = projectId
      ? `${normalizedBaseUrl}projects/${projectId}.json`
      : `${normalizedBaseUrl}projects.json?limit=1`;
    const results = {
      connection: await runRedmineProbe(
        `${normalizedBaseUrl}issues.json?limit=1`,
        headers,
      ),
      currentUser: await runRedmineProbe(
        `${normalizedBaseUrl}users/current.json`,
        headers,
      ),
      projectAccess: await runRedmineProbe(
        projectUrl,
        headers,
      ),
    };
    response.json({
      ok: Object.values(results).every((item) => item.ok),
      results,
    });
  } catch (error) {
    response.status(502).json({
      ok: false,
      error: String(error),
    });
  }
});

app.post('/api/redmine-issues', async (request, response) => {
  try {
    const { baseUrl, apiKey, projectId, issue } = request.body || {};
    if (!baseUrl || !apiKey || !projectId || !issue?.subject || !issue?.description) {
      response.status(400).json({
        ok: false,
        error: 'Missing baseUrl, apiKey, projectId, or issue payload.',
      });
      return;
    }

    const payload = {
      issue: {
        project_id: projectId,
        subject: issue.subject,
        description: issue.description,
      },
    };

    const redmineResponse = await fetchRedmine(`${normalizeBaseUrl(baseUrl)}issues.json`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Redmine-API-Key': apiKey,
      },
      body: JSON.stringify(payload),
    });
    response.json({
      ok: true,
      result: redmineResponse,
    });
  } catch (error) {
    response.status(502).json({
      ok: false,
      error: String(error),
    });
  }
});

app.use('/api', (_request, response) => {
  response.status(404).json({
    error: 'not_found',
    message: '지원하지 않는 API 경로입니다.',
  });
});

exports.api = onRequest(app);

async function listCollection(collectionName, limitValue) {
  const limit = normalizeLimit(limitValue);
  const snapshot = await db.collection(collectionName).limit(limit).get();
  return snapshot.docs.map((document) => normalizeDocument(document));
}

function normalizeLimit(rawValue) {
  const parsed = Number.parseInt(String(rawValue || '50'), 10);
  if (Number.isNaN(parsed)) {
    return 50;
  }
  return Math.max(1, Math.min(parsed, 200));
}

function normalizeDocument(document) {
  return {
    id: document.id,
    ...normalizeValue(document.data()),
  };
}

async function upsertBundle(payload) {
  const metric = payload.metric || null;
  const testCases = Array.isArray(payload.testCases) ? payload.testCases : [];
  const failedTests = Array.isArray(payload.failedTests) ? payload.failedTests : [];

  if (metric && metric.id) {
    await db.collection('TestMetrics').doc(metric.id).set(metric, { merge: true });
  }

  for (const item of testCases) {
    if (!item.id) {
      continue;
    }
    await db.collection('TestCases').doc(item.id).set(item, { merge: true });
  }

  for (const item of failedTests) {
    if (!item.id) {
      continue;
    }
    await db.collection('FailedTests').doc(item.id).set(item, { merge: true });
  }
}

function normalizeValue(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeValue);
  }
  if (value && typeof value === 'object') {
    if (typeof value.toDate === 'function') {
      return value.toDate().toISOString();
    }
    return Object.fromEntries(
      Object.entries(value).map(([key, nestedValue]) => [
        key,
        normalizeValue(nestedValue),
      ]),
    );
  }
  return value;
}

function normalizeBaseUrl(baseUrl) {
  const trimmed = String(baseUrl || '').trim();
  if (!trimmed) {
    throw new Error('Redmine baseUrl is empty.');
  }
  return trimmed.replace(/\/$/, '') + '/';
}

async function fetchRedmine(url, options) {
  const response = await fetch(url, options);
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`Redmine request failed: ${response.status} ${text}`);
  }

  try {
    return JSON.parse(text);
  } catch (_) {
    return { raw: text };
  }
}

async function runRedmineProbe(url, headers) {
  try {
    await fetchRedmine(url, {
      method: 'GET',
      headers,
    });
    return {
      ok: true,
      message: '응답 정상',
    };
  } catch (error) {
    return {
      ok: false,
      message: String(error),
    };
  }
}
