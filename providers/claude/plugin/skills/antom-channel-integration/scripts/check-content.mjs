// SPDX-License-Identifier: Apache-2.0
// Validate local resources; this is not a security or license clearance.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const skillRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const excluded = new Set(['target', 'sdk', 'node_modules', '.git', '.gitnexus', '.idea', '.codefuse', '.DS_Store', '__pycache__']);

/** Match rendered heading anchors, including repeated headings and explicit HTML IDs. */
function markdownAnchors(content) {
    const anchors = new Set();
    const counts = new Map();
    const prose = content.replace(/^```[^\n]*\n[\s\S]*?^```[^\n]*$/gm, '');
    for (const heading of prose.matchAll(/^#{1,6}\s+(.+?)(?:\s+#+)?\s*$/gm)) {
        const slug = heading[1].replace(/<[^>]*>/g, '').toLowerCase()
            .replace(/[^\p{L}\p{N}_ -]/gu, '').replace(/ /g, '-');
        const count = counts.get(slug) || 0;
        counts.set(slug, count + 1);
        let anchor = slug;
        if (count > 0) anchor = `${slug}-${count}`;
        anchors.add(anchor);
    }
    for (const match of prose.matchAll(/\b(?:id|name)=["']([^"']+)["']/g)) anchors.add(match[1]);
    return anchors;
}

/** Check the distributable tree without changing it or reading ignored SDK/build directories. */
export function checkContent(root = skillRoot) {
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
    const anchorCache = new Map();
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
            if (/^(?:https?:|mailto:)/.test(target)) continue;
            const separator = target.indexOf('#');
            let pathname = target;
            let fragment = '';
            if (separator >= 0) {
                pathname = target.slice(0, separator);
                fragment = decodeURIComponent(target.slice(separator + 1));
            }
            let resolved = file;
            if (pathname) resolved = path.resolve(path.dirname(file), decodeURIComponent(pathname));
            assert(fs.existsSync(resolved), `Broken link in ${relative}: ${target}`);
            if (fragment && resolved.endsWith('.md')) {
                if (!anchorCache.has(resolved)) anchorCache.set(resolved, markdownAnchors(fs.readFileSync(resolved, 'utf8')));
                assert(anchorCache.get(resolved).has(fragment), `Broken anchor in ${relative}: ${target}`);
            }
            links++;
        }
    }

    const skill = fs.readFileSync(path.join(root, 'SKILL.md'), 'utf8');
    assert(skill.startsWith('---\nname: antom-channel-integration\n'));
    assert(skill.includes('license: Apache-2.0'));
    assert(skill.split('\n').length < 300, 'Keep Skill routing instructions concise');
    assert(fs.existsSync(path.join(root, 'scripts/ais-cli/pom.xml')), 'Bundle CLI source with the Skill');
    const resultRows = fs.readFileSync(path.join(root, 'references/result-code-catalog.md'), 'utf8')
        .split('\n').filter(line => line.trim().startsWith('|'));
    assert(resultRows.length > 0, 'Result-code tables must exist');
    for (const row of resultRows) assert.equal(row.trim().split('|').length, 4, 'Keep the two-column result-code tables');
    return {files: files.length, links, textFiles};
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        const report = checkContent();
        console.log(`PASS Skill content: ${report.files} files, ${report.links} local links/anchors, ${report.textFiles} English text files; JSON and metadata checked`);
    } catch (error) {
        console.error(error.message);
        process.exitCode = 1;
    }
}
