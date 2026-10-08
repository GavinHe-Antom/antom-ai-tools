// SPDX-License-Identifier: Apache-2.0
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {checkContent} from './check-content.mjs';

function fixture(t) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'ais-content-check-'));
    t.after(() => fs.rmSync(root, {recursive: true, force: true}));
    fs.mkdirSync(path.join(root, 'scripts/ais-cli'), {recursive: true});
    fs.mkdirSync(path.join(root, 'references'));
    fs.writeFileSync(path.join(root, 'SKILL.md'), '---\nname: antom-channel-integration\nlicense: Apache-2.0\n---\n# Entry\n[Cases](references/cases.md#exact-cases)\n');
    fs.writeFileSync(path.join(root, 'scripts/ais-cli/pom.xml'), '<project/>');
    fs.writeFileSync(path.join(root, 'references/result-code-catalog.md'), '| Code | Meaning |\n| --- | --- |\n| SYNTHETIC | Test |\n');
    fs.writeFileSync(path.join(root, 'references/cases.md'), '# Exact `Cases`\n## Repeated\n## Repeated\n[Again](#repeated-1)\n');
    return root;
}

test('validates local links and heading anchors without reading ignored SDK material', t => {
    const root = fixture(t);
    fs.mkdirSync(path.join(root, 'scripts/ais-cli/sdk'));
    fs.writeFileSync(path.join(root, 'scripts/ais-cli/sdk/not-public.json'), 'invalid ignored material');
    assert.equal(checkContent(root).files, 4);
    assert.equal(checkContent(root).links, 2);
});

test('rejects a missing local file and a missing section separately', t => {
    const root = fixture(t);
    fs.writeFileSync(path.join(root, 'references/cases.md'), '[Missing](missing.md)');
    assert.throws(() => checkContent(root), /Broken (?:link|anchor)/);
    fs.writeFileSync(path.join(root, 'references/cases.md'), '# Exact Cases\n[Missing](#not-a-heading)');
    assert.throws(() => checkContent(root), /Broken anchor/);
});

test('does not accept headings that occur only inside fenced examples', t => {
    const root = fixture(t);
    fs.writeFileSync(path.join(root, 'references/cases.md'), '# Exact Cases\n```text\n## Fake heading\n```\n[Missing](#fake-heading)');
    assert.throws(() => checkContent(root), /Broken anchor/);
});

test('rejects non-English source, invalid JSON and distributable symlinks', t => {
    const root = fixture(t);
    const source = path.join(root, 'sample.json');
    fs.writeFileSync(source, '{"text":"\u4e2d"}');
    assert.throws(() => checkContent(root), /Use English/);
    fs.writeFileSync(source, '{');
    assert.throws(() => checkContent(root), SyntaxError);
    fs.unlinkSync(source);
    fs.symlinkSync('SKILL.md', path.join(root, 'linked.md'));
    assert.throws(() => checkContent(root), /Symlinks are not distributable/);
});
