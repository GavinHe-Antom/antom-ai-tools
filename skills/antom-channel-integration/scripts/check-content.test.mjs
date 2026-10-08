// SPDX-License-Identifier: Apache-2.0
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {checkContent} from './check-content.mjs';

function fixture(t) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'aci-content-check-'));
    t.after(() => fs.rmSync(root, {recursive: true, force: true}));
    fs.mkdirSync(path.join(root, 'scripts/aci-cli/sdk'), {recursive: true});
    fs.writeFileSync(path.join(root, 'scripts/aci-cli/sdk/common-sdk-1.5.2.jar'), Buffer.from([0x50, 0x4b, 0x03, 0x04, 0, 255]));
    fs.writeFileSync(path.join(root, 'scripts/aci-cli/sdk/common-sdk-1.5.2.pom'), '<project/>');
    fs.mkdirSync(path.join(root, 'references'));
    fs.writeFileSync(path.join(root, 'SKILL.md'), '---\nname: antom-channel-integration\nlicense: Apache-2.0\n---\n# Entry\n[Cases](references/cases.md#exact-cases)\n');
    fs.writeFileSync(path.join(root, 'scripts/aci-cli/THIRD-PARTY-NOTICES.md'), '# Third-party notices\n## Component license\nApache License\nVersion 2.0\nEND OF TERMS AND CONDITIONS\n');
    fs.writeFileSync(path.join(root, 'scripts/aci-cli/pom.xml'), '<project/>');
    fs.writeFileSync(path.join(root, 'references/result-code-catalog.md'), '| Code | Meaning |\n| --- | --- |\n| SYNTHETIC | Test |\n');
    fs.writeFileSync(path.join(root, 'references/cases.md'), '# Exact `Cases`\n## Repeated\n## Repeated\n[Again](#repeated-1)\n');
    return root;
}

test('counts bundled SDK binary and consumer POM while validating local links and anchors', t => {
    const root = fixture(t);
    assert.equal(checkContent(root).files, 7);
    assert.equal(checkContent(root).links, 2);
    assert.equal(checkContent(root).textFiles, 6);
    fs.writeFileSync(path.join(root, 'scripts/aci-cli/sdk/sample.json'), 'invalid content');
    assert.throws(() => checkContent(root), SyntaxError);
});

test('requires both nonempty SDK artifacts in the distributable CLI', t => {
    const root = fixture(t);
    for (const name of ['common-sdk-1.5.2.jar', 'common-sdk-1.5.2.pom']) {
        const file = path.join(root, 'scripts/aci-cli/sdk', name);
        const bytes = fs.readFileSync(file);
        fs.unlinkSync(file);
        assert.throws(() => checkContent(root), /Bundle the SDK JAR and standalone consumer POM/);
        fs.writeFileSync(file, '');
        assert.throws(() => checkContent(root), /Bundle the SDK JAR and standalone consumer POM/);
        fs.writeFileSync(file, bytes);
    }
});

test('requires component license notices and its dedicated section', t => {
    const root = fixture(t);
    const notices = path.join(root, 'scripts/aci-cli/THIRD-PARTY-NOTICES.md');
    fs.unlinkSync(notices);
    assert.throws(() => checkContent(root), /Retain the component license in THIRD-PARTY-NOTICES/);
    fs.writeFileSync(notices, '# Third-party notices\nApache License\nVersion 2.0\nEND OF TERMS AND CONDITIONS\n');
    assert.throws(() => checkContent(root), /Retain the component license section/);
});

test('rejects incomplete component terms even when other dependency notices contain them', t => {
    const root = fixture(t);
    const notices = path.join(root, 'scripts/aci-cli/THIRD-PARTY-NOTICES.md');
    for (const missing of ['Apache License', 'Version 2.0', 'END OF TERMS AND CONDITIONS']) {
        const body = ['Apache License', 'Version 2.0', 'END OF TERMS AND CONDITIONS'].filter(marker => marker !== missing).join('\n');
        fs.writeFileSync(notices, `# Third-party notices\n## Component license\n${body}\n## Dependency licenses\nApache License\nVersion 2.0\nEND OF TERMS AND CONDITIONS\n`);
        assert.throws(() => checkContent(root), /Retain the complete component Apache-2.0 license/);
    }
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
