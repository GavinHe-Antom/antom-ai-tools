// SPDX-License-Identifier: Apache-2.0
// Verify the packaged CLI as a recipient would use it, including default SDK discovery.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const cliRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
assert(process.argv.length <= 3, 'Usage: node scripts/verify-distribution.mjs [CLI_ZIP]');
const archive = path.resolve(process.argv[2] || path.join(cliRoot, 'target/aci-cli-0.1.0.zip'));
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : 'java';
const workspace = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'aci-distribution-')));

function run(binary, args, cwd = workspace) {
    const result = spawnSync(binary, args, {cwd, encoding: 'utf8', timeout: 60000, maxBuffer: 4 * 1024 * 1024});
    assert.equal(result.status, 0, `${binary}: ${result.error || ''}\n${result.stdout}\n${result.stderr}`);
    return result.stdout;
}

function hash(file) {
    return createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

try {
    const entries = run('unzip', ['-Z1', archive]).trim().split(/\r?\n/).filter(entry => !entry.endsWith('/'));
    assert.deepEqual(entries.sort(), [
        'aci-cli.jar', 'sdk/common-sdk-1.5.2.jar', 'sdk/common-sdk-1.5.2.pom',
        'README.md', 'DISTRIBUTION.md', 'THIRD-PARTY-NOTICES.md'
    ].sort(), 'The distribution must contain the complete CLI, SDK pair and redistribution notices');
    const distribution = path.join(workspace, 'CLI distribution');
    fs.mkdirSync(distribution);
    run('unzip', ['-q', archive, '-d', distribution]);
    for (const entry of ['aci-cli.jar', 'sdk/common-sdk-1.5.2.jar', 'sdk/common-sdk-1.5.2.pom']) {
        const source = entry === 'aci-cli.jar' ? path.join(cliRoot, 'target', entry) : path.join(cliRoot, entry);
        assert.equal(hash(path.join(distribution, entry)), hash(source), `Distribution changed ${entry}`);
    }
    const cliJar = path.join(distribution, 'aci-cli.jar');
    const cliEntries = run('unzip', ['-Z1', cliJar]).trim().split(/\r?\n/);
    assert(!cliEntries.some(entry => entry.startsWith('com/alipay/iacqintegrationhub/') || entry.endsWith('.jar')),
        'SDK classes and nested JARs must remain outside the CLI shaded JAR');
    assert.match(run(java, ['-jar', cliJar, '--version']), /^aci 0\.1\.0 \(AIS SDK 1\.5\.2\)/);
    const workingDirectory = path.join(workspace, 'unrelated working directory');
    fs.mkdirSync(workingDirectory);
    const config = path.join(workingDirectory, 'config.json');
    fs.writeFileSync(config, JSON.stringify({schemaVersion: 1, groupId: 'com.example',
        artifactId: 'distribution-adapter', version: '1.0.0-SNAPSHOT', packageName: 'com.example.distribution',
        channelCode: 'distributiontest', paymentType: 'non-card', threeDS: 'none', spi: ['pay'],
        securityFeatures: {signature: false, encryption: false}}));
    const project = path.join(workingDirectory, 'adapter');
    const report = JSON.parse(run(java,
        ['-jar', cliJar, 'init', '--config', config, '--output', project, '--json'], workingDirectory));
    assert.equal(report.status, 'generated');
    const library = path.join(project, 'lib/repository/com/alipay/iacqintegrationhub/common-sdk/1.5.2');
    for (const file of ['common-sdk-1.5.2.jar', 'common-sdk-1.5.2.pom']) {
        assert.equal(hash(path.join(library, file)), hash(path.join(distribution, 'sdk', file)));
    }
    const pom = fs.readFileSync(path.join(project, 'pom.xml'), 'utf8');
    assert.match(pom, /<artifactId>common-sdk<\/artifactId>\s*<version>\$\{common-sdk.version\}<\/version>\s*<scope>provided<\/scope>/);
    assert(pom.includes('<IAIS-SDK-Version>${common-sdk.version}</IAIS-SDK-Version>'));
    console.log('PASS distribution inventory, SDK hashes, separate SDK classes, launcher and default-SDK init');
} finally {
    fs.rmSync(workspace, {recursive: true, force: true});
}
