import test from 'node:test';
import assert from 'node:assert/strict';
import { promises as fs } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { exportTraceabilityMatrix } from '../export-traceability-matrix.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const fixturesDir = path.join(__dirname, 'fixtures');
const fixtureResultsDir = path.join(fixturesDir, 'allure-results');
const fixtureCatalogPath = path.join(fixturesDir, 'story-catalog.json');

async function createTempResultsDir() {
  const tmpRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'traceability-matrix-'));
  const targetDir = path.join(tmpRoot, 'allure-results');
  await fs.mkdir(targetDir, { recursive: true });
  const files = await fs.readdir(fixtureResultsDir);
  for (const fileName of files) {
    const source = path.join(fixtureResultsDir, fileName);
    const dest = path.join(targetDir, fileName);
    const data = await fs.readFile(source);
    await fs.writeFile(dest, data);
  }
  return { tmpRoot, targetDir };
}

test('exportTraceabilityMatrix produces JSON artifact with coverage insights', async () => {
  const { tmpRoot, targetDir } = await createTempResultsDir();
  const outputPath = path.join(tmpRoot, 'matrix.json');

  const { artifact, destination } = await exportTraceabilityMatrix({
    resultsDir: targetDir,
    catalogPath: fixtureCatalogPath,
    output: outputPath,
    quiet: true
  });

  assert.equal(destination, outputPath);
  assert.equal(artifact.stories.length, 3); // two catalog entries plus one orphan

  const catalogStory = artifact.stories.find(story => story.id === 'UTS-401');
  assert.ok(catalogStory);
  assert.equal(catalogStory.testCount, 1);
  assert.equal(catalogStory.overallStatus, 'passed');
  assert.equal(catalogStory.tests[0].microservice, 'cds-platform');

  const missingList = artifact.missingStories ?? [];
  assert.deepEqual(missingList, ['UTS-403']);

  const orphanList = artifact.orphanStories ?? [];
  assert.equal(orphanList.length, 1);
  assert.equal(orphanList[0].id, 'UTS-999');

  const written = await fs.readFile(outputPath, 'utf8');
  const parsed = JSON.parse(written);
  assert.equal(parsed.formatVersion, 1);
});

test('exportTraceabilityMatrix can emit CSV output', async () => {
  const { tmpRoot, targetDir } = await createTempResultsDir();
  const outputPath = path.join(tmpRoot, 'matrix.csv');

  await exportTraceabilityMatrix({
    resultsDir: targetDir,
    catalogPath: fixtureCatalogPath,
    output: outputPath,
    format: 'csv',
    quiet: true
  });

  const csv = await fs.readFile(outputPath, 'utf8');
  const lines = csv.trim().split('\n');
  assert.ok(lines[0].startsWith('storyId,title,epic'));
  assert.ok(lines.some(line => line.includes('UTS-401')));
  assert.ok(lines.some(line => line.includes('UTS-999')));
});

test('exportTraceabilityMatrix fails when requested and coverage missing', async () => {
  const { tmpRoot, targetDir } = await createTempResultsDir();
  const outputPath = path.join(tmpRoot, 'matrix.json');

  await assert.rejects(
    () =>
      exportTraceabilityMatrix({
        resultsDir: targetDir,
        catalogPath: fixtureCatalogPath,
        output: outputPath,
        quiet: true,
        failOnMissing: true
      }),
    error => error.code === 'TRACEABILITY_MISSING'
  );
});
