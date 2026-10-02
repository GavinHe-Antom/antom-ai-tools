// SPDX-License-Identifier: Apache-2.0
// Maintainer regression: use an authorized SDK supplied as arguments, never download or embed it.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const cliRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const [sdkJarInput, sdkPomInput] = process.argv.slice(2);
assert(sdkJarInput && sdkPomInput, 'Usage: node scripts/verify-generated.mjs SDK_JAR SDK_CONSUMER_POM');
const sdkJar = path.resolve(sdkJarInput);
const sdkPom = path.resolve(sdkPomInput);
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : 'java';
const maven = process.env.MAVEN_HOME ? path.join(process.env.MAVEN_HOME, 'bin/mvn') : 'mvn';
const root = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'ais-cli-e2e-')));
console.log(`Verification workspace: ${root}`);
const results = [];
const distribution = path.join(root, 'CLI distribution');
fs.mkdirSync(path.join(distribution, 'sdk'), {recursive: true});
fs.copyFileSync(path.join(cliRoot, 'target/ais-cli.jar'), path.join(distribution, 'ais-cli.jar'));
fs.copyFileSync(sdkJar, path.join(distribution, 'sdk/common-sdk-1.5.2.jar'));
fs.copyFileSync(sdkPom, path.join(distribution, 'sdk/common-sdk-1.5.2.pom'));

function run(name, binary, args, cwd, input, expected = 0) {
    const result = spawnSync(binary, args, {cwd, input, encoding: 'utf8', timeout: 180000, maxBuffer: 8 * 1024 * 1024});
    fs.writeFileSync(path.join(root, `${name}.stdout.log`), result.stdout || '');
    fs.writeFileSync(path.join(root, `${name}.stderr.log`), result.stderr || '');
    assert.equal(result.status, expected, `${name}: ${result.error || ''}\n${result.stdout}\n${result.stderr}`);
    results.push({name, exitCode: result.status, expected});
    console.log(`PASS ${name} (exit ${result.status})`);
    return result;
}

function ais(name, args, input, expected = 0) {
    return run(name, java, ['-jar', path.join(distribution, 'ais-cli.jar'), ...args], root, input, expected);
}

function noneSteps(direction) {
    const operations = direction === 'request' ? ['sign', 'encrypt'] : ['verify', 'decrypt'];
    return operations.map(operation => ({operation, implementation: 'none', rule: 'synthetic-protocol-none'}));
}

function interactive(name, card, two, full) {
    const methods = ['pay'];
    if (two) methods.push('authenticateAuthorize');
    for (const method of ['inquiryPayment', 'cancel', ...(card ? ['capture'] : []), 'refund']) {
        if (full) methods.push(method);
    }
    if (full) methods.push('inquiryRefund');
    if (full) {
        methods.push('notifyPayment');
        if (card) methods.push('notifyCapture');
        methods.push('notifyRefund');
    }
    let threeDS = 'none';
    if (two) {
        threeDS = 'two';
    } else if (card) {
        threeDS = 'one';
    }
    const spec = {schemaVersion: 1, groupId: 'com.example', artifactId: name, packageName: 'com.example.adapter',
        channelCode: 'example', paymentType: card ? 'card' : 'non-card',
        threeDS, spi: methods, security: {}};
    for (const method of methods) {
        const directions = method.startsWith('notify') ? ['notification'] : ['request', 'response'];
        spec.security[method] = {};
        for (const direction of directions) {
            spec.security[method][direction] = noneSteps(direction);
        }
    }
    const config = path.join(root, `${name}.json`);
    fs.writeFileSync(config, JSON.stringify(spec));
    const output = path.join(root, name);
    const generated = ais(`init-${name}`, ['init', '--config', config, '--output', output, '--json']);
    const report = JSON.parse(generated.stdout);
    assert.deepEqual(report.methods, methods);
    const spi = fs.readFileSync(path.join(output, 'src/main/java/com/example/adapter/spi/ChannelPaymentService.java'), 'utf8');
    assert.equal(spi.includes('authenticateAuthorize('), two);
    assert.equal(spi.includes('capture('), card && full);
    const refundFile = path.join(output, 'src/main/java/com/example/adapter/spi/ChannelRefundService.java');
    assert.equal(fs.existsSync(refundFile), full);
    if (full) {
        const refund = fs.readFileSync(refundFile, 'utf8');
        assert.match(refund, /RefundResponse refund\(/);
        assert.match(refund, /InquiryRefundResponse inquiryRefund\(/);
        assert.match(refund, /new ChannelApiExtension</);
    }
    assert(!fs.existsSync(path.join(output, 'src/main/java/com/example/adapter/customize/api')));
    run(`compile-${name}`, maven, ['-B', '-Dtest=GeneratedStructureTest,*SecurityContractTest', 'test'], output);
    return output;
}

interactive('card-two', true, true, true);
interactive('card-one', true, false, false);
const nonCard = interactive('non-card', false, false, true);

// Two feature choices create platform examples without selecting institution algorithms.
for (const [name, signature, encryption] of [
    ['signature-demo', true, false],
    ['cipher-demo', false, true],
    ['combined-demo', true, true],
    ['no-security-demo', false, false]
]) {
    const spec = {schemaVersion: 1, groupId: 'com.example', artifactId: name,
        packageName: 'com.example.adapter', channelCode: 'example', paymentType: 'non-card',
        threeDS: 'none', spi: ['pay', 'refund', 'inquiryRefund', 'notifyPayment', 'notifyRefund'],
        securityFeatures: {signature, encryption}};
    const config = path.join(root, `${name}.json`);
    fs.writeFileSync(config, JSON.stringify(spec));
    const output = path.join(root, name);
    ais(`generate-${name}`, ['init', '--config', config, '--output', output, '--json']);
    const normalized = JSON.parse(fs.readFileSync(path.join(output, 'adapter-spec.json'), 'utf8'));
    for (const directions of Object.values(normalized.security)) {
        for (const steps of Object.values(directions)) {
            for (const step of steps) {
                const enabled = ['sign', 'verify'].includes(step.operation) ? signature : encryption;
                assert.equal(step.implementation, enabled ? 'demo' : 'none');
                assert.equal(step.algorithm, null);
            }
        }
    }
    const source = fs.readFileSync(path.join(output,
        'src/main/java/com/example/adapter/customize/security/ChannelSecurityCustomization.java'), 'utf8');
    assert.equal(source.includes('platform.sign('), signature);
    assert.equal(source.includes('platform.verify('), signature);
    assert.equal(source.includes('platform.encrypt('), encryption);
    assert.equal(source.includes('platform.decrypt('), encryption);
    run(`compile-${name}`, maven, ['-B', '-Dtest=GeneratedStructureTest,*SecurityContractTest', 'test'], output);
    // Saved configuration must be accepted again after feature expansion, not contain conflicting inputs.
    ais(`revalidate-${name}`, ['init', '--config', path.join(output, 'adapter-spec.json'),
        '--output', path.join(root, `${name}-repeat`), '--dry-run', '--json']);
}

const noTerminalOutput = path.join(root, 'no-terminal');
const noTerminal = ais('reject-piped-interaction', ['init', '--output', noTerminalOutput, '--json'], '', 2);
assert.match(JSON.parse(noTerminal.stdout).error, /--config/);
assert(!fs.existsSync(noTerminalOutput));

// A protocol-unimplemented project must fail full packaging, not silently skip its delivery tests.
const rejectedPackage = ais('package-unimplemented', ['package', '--project', nonCard, '--json'], undefined, 5);
assert.equal(JSON.parse(rejectedPackage.stdout).exitCode, 5);

// Render and compile all four platform security calls and custom queryKey hooks against the real SDK.
const secure = JSON.parse(fs.readFileSync(path.join(nonCard, 'adapter-spec.json')));
secure.sdkJar = sdkJar;
secure.sdkPom = sdkPom;
secure.spi = ['pay', 'notifyPayment'];
secure.security = {
    pay: {request: noneSteps('request'), response: noneSteps('response')},
    notifyPayment: {notification: noneSteps('notification')}
};
for (const direction of ['request', 'response']) {
    for (const step of secure.security.pay[direction]) {
        step.implementation = 'platform';
        if (step.operation === 'sign' || step.operation === 'verify') {
            step.algorithm = 'HMAC_SHA256_BASE64';
        } else {
            step.algorithm = 'AES'; step.cipherType = 'SYMMETRIC';
            step.parameters = {mode: 'GCM', padding: 'NoPadding', tagBitLength: 128};
        }
    }
}
secure.security.notifyPayment.notification[0] = {
    operation: 'verify', implementation: 'adapter', keyAlgorithm: 'HMAC_SHA256_BASE64', rule: 'synthetic-custom-verify'
};
const secureConfig = path.join(root, 'secure.json');
fs.writeFileSync(secureConfig, JSON.stringify(secure, null, 2));
ais('secure-dry-run', ['init', '--config', secureConfig, '--output', path.join(root, 'secure'), '--dry-run', '--json']);
assert(!fs.existsSync(path.join(root, 'secure')));
ais('secure-generate', ['init', '--config', secureConfig, '--output', path.join(root, 'secure'), '--json']);
run('compile-secure', maven, ['-B', '-Dtest=GeneratedStructureTest,*SecurityContractTest', 'test'], path.join(root, 'secure'));

// Exercise reversed operation ordering and each custom key purpose without implementing crypto.
const reversed = JSON.parse(JSON.stringify(secure));
reversed.security.pay.request.reverse();
reversed.security.pay.response.reverse();
const reversedConfig = path.join(root, 'reversed.json');
fs.writeFileSync(reversedConfig, JSON.stringify(reversed, null, 2));
ais('reversed-generate', ['init', '--config', reversedConfig, '--output', path.join(root, 'reversed'), '--json']);
run('test-reversed-security', maven, ['-B', '-Dtest=*SecurityContractTest', 'test'], path.join(root, 'reversed'));
const custom = JSON.parse(JSON.stringify(secure));
for (const directions of Object.values(custom.security)) {
    for (const steps of Object.values(directions)) {
        for (const step of steps) {
            step.implementation = 'adapter';
            step.keyAlgorithm = ['sign', 'verify'].includes(step.operation) ? 'HMAC_SHA256_BASE64' : 'AES';
            delete step.algorithm;
            delete step.cipherType;
            delete step.parameters;
        }
    }
}
const customConfig = path.join(root, 'custom.json');
fs.writeFileSync(customConfig, JSON.stringify(custom, null, 2));
ais('custom-generate', ['init', '--config', customConfig, '--output', path.join(root, 'custom'), '--json']);
run('test-custom-security', maven, ['-B', '-Dtest=*SecurityContractTest', 'test'], path.join(root, 'custom'));

// A deterministic synthetic protocol completes one generated adapter. This is not an institution implementation.
const delivery = {...secure, artifactId: 'synthetic-adapter', spi: ['pay', 'notifyPayment'], security: {
    pay: {request: noneSteps('request'), response: noneSteps('response')},
    notifyPayment: {notification: noneSteps('notification')}
}};
delivery.security.pay.request[0] = {operation: 'sign', implementation: 'platform', algorithm: 'HMAC_SHA256_BASE64', rule: 'synthetic-sign'};
delivery.security.pay.response[0] = {operation: 'verify', implementation: 'platform', algorithm: 'HMAC_SHA256_BASE64', rule: 'synthetic-verify'};
const deliveryConfig = path.join(root, 'delivery.json');
fs.writeFileSync(deliveryConfig, JSON.stringify(delivery, null, 2));
const deliveryProject = path.join(root, 'delivery');
ais('delivery-generate', ['init', '--config', deliveryConfig, '--output', deliveryProject, '--json']);
const fixtures = path.join(cliRoot, 'src/test/fixtures');
const generatedDeliveryTest = path.join(deliveryProject, 'src/test/java/com/example/adapter/PayDeliveryTest.java');
const generatedDeliverySource = fs.readFileSync(generatedDeliveryTest, 'utf8');
assert.match(generatedDeliverySource, /selectedSpiMatchesConfirmedProtocol/);
// Keep the actual generated delivery test. Only complete its application hooks and independent fixture inputs.
for (const name of ['ChannelPaymentService.java', 'ChannelNotificationService.java', 'ChannelSecurityCustomization.java']) {
    let target = 'src/main/java/com/example/adapter/spi/';
    if (name === 'ChannelSecurityCustomization.java') {
        target = 'src/main/java/com/example/adapter/customize/security/';
    }
    fs.copyFileSync(path.join(fixtures, name), path.join(deliveryProject, target, name));
}
const scenario = path.join(deliveryProject, 'src/test/resources/scenarios/pay');
for (const name of ['input.json', 'context.json', 'expected-request.json', 'response.json', 'response-headers.json',
    'expected-result.json', 'security.json']) {
    fs.copyFileSync(path.join(fixtures, 'scenarios/pay', name), path.join(scenario, name));
}
const notificationScenario = path.join(deliveryProject, 'src/test/resources/scenarios/notifyPayment');
for (const name of ['input.json', 'context.json', 'expected-result.json', 'security.json']) {
    fs.copyFileSync(path.join(fixtures, 'scenarios/notifyPayment', name), path.join(notificationScenario, name));
}
const packaged = ais('package-synthetic', ['package', '--project', deliveryProject, '--json']);
assert.equal(JSON.parse(packaged.stdout).status, 'verified-locally');
const deliveryReport = path.join(deliveryProject, 'target/surefire-reports/TEST-com.example.adapter.PayDeliveryTest.xml');
assert.match(fs.readFileSync(deliveryReport, 'utf8'), /selectedSpiMatchesConfirmedProtocol/);
fs.copyFileSync(deliveryReport, path.join(root, 'package-synthetic.generated-delivery.xml'));
const notificationReport = path.join(deliveryProject, 'target/surefire-reports/TEST-com.example.adapter.NotifyPaymentDeliveryTest.xml');
assert.match(fs.readFileSync(notificationReport, 'utf8'), /selectedSpiMatchesConfirmedProtocol/);

// Mutation checks prove that the generated assertions execute; an unrelated build failure is not enough.
function rejectChangedFixture(name, file, changed, expectedFailure, report = deliveryReport) {
    const original = fs.readFileSync(file);
    try {
        fs.writeFileSync(file, JSON.stringify(changed, null, 2));
        const rejected = ais(name, ['package', '--project', deliveryProject, '--json'], undefined, 5);
        assert.equal(JSON.parse(rejected.stdout).exitCode, 5);
        const evidence = fs.readFileSync(report, 'utf8');
        assert.match(evidence, /selectedSpiMatchesConfirmedProtocol/);
        assert.match(evidence, /<testsuite\b[^>]*(?:failures|errors)="[1-9]\d*"/);
        assert.match(evidence, expectedFailure);
        fs.copyFileSync(report, path.join(root, `${name}.generated-delivery.xml`));
    } finally {
        fs.writeFileSync(file, original);
    }
}

const securityFixture = JSON.parse(fs.readFileSync(path.join(scenario, 'security.json'), 'utf8'));
securityFixture.responseVerifyRequest.content = 'deliberately-wrong-independent-canonical-text';
rejectChangedFixture('reject-wrong-security-expectation', path.join(scenario, 'security.json'),
    securityFixture, /Synthetic response verification failed/);
const requestFixture = JSON.parse(fs.readFileSync(path.join(scenario, 'expected-request.json'), 'utf8'));
requestFixture.pathParameters = {reference: 'synthetic-wrong-path'};
rejectChangedFixture('reject-wrong-path-expectation', path.join(scenario, 'expected-request.json'),
    requestFixture, /synthetic-wrong-path/);
const contextFixture = JSON.parse(fs.readFileSync(path.join(scenario, 'context.json'), 'utf8'));
contextFixture.merchantId = 'synthetic-wrong-context-merchant';
rejectChangedFixture('reject-wrong-context-identity', path.join(scenario, 'context.json'),
    contextFixture, /Synthetic response verification failed/);
const notificationFixture = JSON.parse(fs.readFileSync(path.join(notificationScenario, 'expected-result.json'), 'utf8'));
notificationFixture.extendInfo = 'synthetic-wrong-extension';
rejectChangedFixture('reject-wrong-notification-extension', path.join(notificationScenario, 'expected-result.json'),
    notificationFixture, /synthetic-wrong-extension/, notificationReport);
assert.equal(fs.readFileSync(generatedDeliveryTest, 'utf8'), generatedDeliverySource,
    'Never replace generated delivery assertions with a maintainer-written test');
const restored = ais('package-synthetic-restored', ['package', '--project', deliveryProject, '--json']);
assert.equal(JSON.parse(restored.stdout).status, 'verified-locally');
fs.copyFileSync(deliveryReport, path.join(root, 'package-synthetic-restored.generated-delivery.xml'));

ais('reject-existing', ['init', '--config', secureConfig, '--output', deliveryProject, '--json'], undefined, 4);
const invalid = {...secure, sdkVersion: '1.5.1'};
fs.writeFileSync(path.join(root, 'invalid.json'), JSON.stringify(invalid));
ais('reject-sdk-version', ['init', '--config', path.join(root, 'invalid.json'), '--output', path.join(root, 'invalid'), '--json'], undefined, 3);
fs.writeFileSync(path.join(root, 'verification.json'), JSON.stringify({root, results}, null, 2));
console.log(`All checks passed. Evidence: ${root}/verification.json`);
