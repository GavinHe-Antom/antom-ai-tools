// SPDX-License-Identifier: Apache-2.0
// Validate this Skill's local resources; this is not a security or license clearance.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const excluded = new Set(['target', 'sdk', 'node_modules', '.git', '.gitnexus', '.idea', '.codefuse', '.DS_Store', '__pycache__']);
const files = [];
const pending = [root];
while (pending.length) {
    const directory = pending.pop();
    for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
        if (excluded.has(entry.name)) continue;
        const file = path.join(directory, entry.name);
        assert(!entry.isSymbolicLink(), `Symlinks are not distributable: ${file}`);
        if (entry.isDirectory()) pending.push(file);
        else files.push(file);
    }
}

let links = 0;
let textFiles = 0;
for (const file of files) {
    const relative = path.relative(root, file);
    const textFile = /\.(?:md|txt|ftl|java|xml|mjs|json|ya?ml)$/.test(file) || relative === 'scripts/ais-cli/bin/ais';
    if (textFile) {
        const content = fs.readFileSync(file, 'utf8');
        assert(!/\p{Script=Han}/u.test(content), `Use English in Skill and CLI content: ${relative}`);
        textFiles++;
    }
    if (file.endsWith('.json')) JSON.parse(fs.readFileSync(file, 'utf8'));
    if (!file.endsWith('.md')) continue;
    for (const match of fs.readFileSync(file, 'utf8').matchAll(/\[[^\]\n]*\]\(([^)\n]+)\)/g)) {
        const target = match[1].replace(/^<|>$/g, '').split(/\s+"/)[0];
        if (/^(?:https?:|mailto:|#)/.test(target)) continue;
        const pathname = decodeURIComponent(target.split('#')[0]);
        assert(pathname && fs.existsSync(path.resolve(path.dirname(file), pathname)), `Broken link in ${relative}: ${target}`);
        links++;
    }
}

const skill = fs.readFileSync(path.join(root, 'SKILL.md'), 'utf8');
assert(skill.startsWith('---\nname: iais-channel-integration\n'));
assert(skill.includes('license: Apache-2.0'));
assert(skill.split('\n').length < 300, 'Keep Skill routing instructions concise');
assert(fs.existsSync(path.join(root, 'LICENSE')), 'Retain the component license');
assert(fs.existsSync(path.join(root, 'scripts/ais-cli/pom.xml')), 'Bundle CLI source with the Skill');
const resultRows = fs.readFileSync(path.join(root, 'references/result-code-catalog.md'), 'utf8')
    .split('\n').filter(line => line.trim().startsWith('|'));
assert(resultRows.length > 0, 'Result-code tables must exist');
for (const row of resultRows) assert.equal(row.trim().split('|').length, 4, 'Keep the two-column result-code tables');
console.log(`PASS Skill content: ${files.length} files, ${links} local links, ${textFiles} English text files; JSON and metadata checked`);
