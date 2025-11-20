const fs = require('fs');
const path = require('path');

const entityPath = path.join('..', 'backend', 'src', 'main', 'java', 'com', 'creditdefaultswap', 'platform', 'model', 'CDSTrade.java');
const content = fs.readFileSync(entityPath, 'utf-8');
const lines = content.split('\n');

console.log('=== Testing Entity Field Extraction ===\n');

const fields = [];

for (let i = 0; i < lines.length; i++) {
  const line = lines[i].trim();
  
  // Skip non-field lines
  if (!line.match(/^\s*(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?[\w<>[\]]+\s+\w+\s*[;=]/)) {
    continue;
  }
  
  // Extract field declaration
  const fieldMatch = line.match(/(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?([\w<>[\]]+)\s+(\w+)\s*[;=]/);
  if (!fieldMatch) continue;
  
  const javaType = fieldMatch[1];
  const fieldName = fieldMatch[2];
  
  // Skip static/transient fields
  if (line.includes('static') || line.includes('transient')) {
    continue;
  }
  
  // Look backwards for annotations (up to 5 lines)
  // We need to track if we hit an empty line to avoid cross-field contamination
  let columnAnnotation = null;
  let isId = false;
  
  for (let j = i - 1; j >= Math.max(0, i - 5); j--) {
    const prevLine = lines[j].trim();
    
    // Stop if we hit an empty line or another field declaration
    // This prevents annotations from previous fields from being associated with this field
    if (prevLine === '' || prevLine.match(/^\s*(?:private|protected|public)\s+/)) {
      break;
    }
    
    if (prevLine.includes('@Column')) {
      columnAnnotation = prevLine;
    }
    if (prevLine.includes('@Id')) {
      isId = true;
    }
  }
  
  // Skip ID fields in test data generation
  if (isId) {
    continue;
  }
  
  fields.push({
    name: fieldName,
    javaType,
    hasColumn: !!columnAnnotation
  });
}

console.log(`Found ${fields.length} fields:\n`);
fields.forEach(f => {
  console.log(`  - ${f.name} (${f.javaType}) ${f.hasColumn ? '[@Column]' : '[no @Column]'}`);
});

console.log(`\n=== Checking for referenceEntity specifically ===`);
const refEntity = fields.find(f => f.name === 'referenceEntity');
if (refEntity) {
  console.log('✅ referenceEntity FOUND in extraction');
  console.log(`   Type: ${refEntity.javaType}`);
  console.log(`   Has @Column: ${refEntity.hasColumn}`);
} else {
  console.log('❌ referenceEntity NOT FOUND in extraction');
}
