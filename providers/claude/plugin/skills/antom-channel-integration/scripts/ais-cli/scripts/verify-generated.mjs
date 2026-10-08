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

function identify(spec, name) {
    spec.artifactId = name;
    spec.packageName = `com.example.${name.replaceAll('-', '')}`;
    spec.channelCode = name;
    return spec;
}

function mainJavaRoot(spec) {
    return `src/main/java/${spec.packageName.replaceAll('.', '/')}`;
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
    const spec = identify({schemaVersion: 1, groupId: 'com.example', paymentType: card ? 'card' : 'non-card',
        threeDS, spi: methods, security: {}}, name);
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
    const spi = fs.readFileSync(path.join(output, mainJavaRoot(spec), 'spi/ChannelPaymentService.java'), 'utf8');
    assert.equal(spi.includes('authenticateAuthorize('), two);
    assert.equal(spi.includes('capture('), card && full);
    const refundFile = path.join(output, mainJavaRoot(spec), 'spi/ChannelRefundService.java');
    assert.equal(fs.existsSync(refundFile), full);
    if (full) {
        const refund = fs.readFileSync(refundFile, 'utf8');
        assert.match(refund, /RefundResponse refund\(/);
        assert.match(refund, /InquiryRefundResponse inquiryRefund\(/);
        assert.match(refund, /new ChannelApiExtension</);
    }
    assert(!fs.existsSync(path.join(output, mainJavaRoot(spec), 'customize/api')));
    run(`compile-${name}`, maven, ['-B', '-Dtest=GeneratedStructureTest,TemplateRuntimeTest,*SecurityContractTest', 'test'], output);
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
    const spec = identify({schemaVersion: 1, groupId: 'com.example', paymentType: 'non-card',
        threeDS: 'none', spi: ['pay', 'refund', 'inquiryRefund', 'notifyPayment', 'notifyRefund'],
        securityFeatures: {signature, encryption}}, name);
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
        mainJavaRoot(spec), 'customize/security/ChannelSecurityCustomization.java'), 'utf8');
    assert.equal(source.includes('platform.sign('), signature);
    assert.equal(source.includes('platform.verify('), signature);
    assert.equal(source.includes('platform.encrypt('), encryption);
    assert.equal(source.includes('platform.decrypt('), encryption);
    run(`compile-${name}`, maven, ['-B', '-Dtest=GeneratedStructureTest,TemplateRuntimeTest,*SecurityContractTest', 'test'], output);
    // Saved configuration must be accepted again after feature expansion, not contain conflicting inputs.
    const repeated = identify(normalized, `${name}-repeat`);
    repeated.sdkJar = sdkJar;
    repeated.sdkPom = sdkPom;
    const repeatConfig = path.join(root, `${name}-repeat.json`);
    fs.writeFileSync(repeatConfig, JSON.stringify(repeated));
    ais(`revalidate-${name}`, ['init', '--config', repeatConfig,
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
identify(secure, 'secure');
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
run('compile-secure', maven, ['-B', '-Dtest=GeneratedStructureTest,TemplateRuntimeTest,*SecurityContractTest', 'test'], path.join(root, 'secure'));

// Exercise reversed operation ordering and each custom key purpose without implementing crypto.
const reversed = JSON.parse(JSON.stringify(secure));
identify(reversed, 'reversed');
reversed.security.pay.request.reverse();
reversed.security.pay.response.reverse();
const reversedConfig = path.join(root, 'reversed.json');
fs.writeFileSync(reversedConfig, JSON.stringify(reversed, null, 2));
ais('reversed-generate', ['init', '--config', reversedConfig, '--output', path.join(root, 'reversed'), '--json']);
run('test-reversed-security', maven, ['-B', '-Dtest=*SecurityContractTest', 'test'], path.join(root, 'reversed'));
const custom = JSON.parse(JSON.stringify(secure));
identify(custom, 'custom');
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
const delivery = {...secure, artifactId: 'synthetic-adapter', packageName: 'com.example.adapter',
    channelCode: 'example', spi: ['pay', 'notifyPayment'], security: {
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
for (const name of ['ChannelPaymentService.java', 'ChannelNotificationService.java', 'ChannelSecurityCustomization.java',
    'ChannelTransportCustomization.java']) {
    let target = 'src/main/java/com/example/adapter/spi/';
    if (name === 'ChannelSecurityCustomization.java') {
        target = 'src/main/java/com/example/adapter/customize/security/';
    } else if (name === 'ChannelTransportCustomization.java') {
        target = 'src/main/java/com/example/adapter/customize/transport/';
    }
    fs.copyFileSync(path.join(fixtures, name), path.join(deliveryProject, target, name));
}
const scenario = path.join(deliveryProject, 'src/test/resources/scenarios/pay');
for (const name of fs.readdirSync(path.join(fixtures, 'scenarios/pay'))) {
    fs.copyFileSync(path.join(fixtures, 'scenarios/pay', name), path.join(scenario, name));
}
const notificationScenario = path.join(deliveryProject, 'src/test/resources/scenarios/notifyPayment');
for (const name of fs.readdirSync(path.join(fixtures, 'scenarios/notifyPayment'))) {
    fs.copyFileSync(path.join(fixtures, 'scenarios/notifyPayment', name), path.join(notificationScenario, name));
}
const packaged = ais('package-synthetic', ['package', '--project', deliveryProject, '--json']);
assert.equal(JSON.parse(packaged.stdout).status, 'verified-locally');
const deliveryReport = path.join(deliveryProject, 'target/surefire-reports/TEST-com.example.adapter.PayDeliveryTest.xml');
assert.match(fs.readFileSync(deliveryReport, 'utf8'), /name="selectedSpiMatchesConfirmedProtocol case=success"/);
assert.match(fs.readFileSync(deliveryReport, 'utf8'), /name="selectedSpiMatchesConfirmedProtocol case=invalid-signature"/);
assert.match(fs.readFileSync(deliveryReport, 'utf8'), /name="selectedSpiMatchesConfirmedProtocol case=business-rejected"/);
fs.copyFileSync(deliveryReport, path.join(root, 'package-synthetic.generated-delivery.xml'));
const notificationReport = path.join(deliveryProject, 'target/surefire-reports/TEST-com.example.adapter.NotifyPaymentDeliveryTest.xml');
assert.match(fs.readFileSync(notificationReport, 'utf8'), /name="selectedSpiMatchesConfirmedProtocol case=success"/);
assert.match(fs.readFileSync(notificationReport, 'utf8'), /name="selectedSpiMatchesConfirmedProtocol case=invalid-payload"/);

// Mutation checks prove that the generated assertions execute; an unrelated build failure is not enough.
function rejectChangedFixture(name, file, changed, expectedFailure, report = deliveryReport) {
    const original = fs.readFileSync(file);
    try {
        fs.writeFileSync(file, JSON.stringify(changed, null, 2));
        const rejected = ais(name, ['package', '--project', deliveryProject, '--json'], undefined, 5);
        assert.equal(JSON.parse(rejected.stdout).exitCode, 5);
        const evidence = fs.readFileSync(report, 'utf8');
        assert.match(evidence, /<testsuite\b[^>]*(?:failures|errors)="[1-9]\d*"/);
        assert(expectedFailure.test(evidence), `${name}: missing expected named-test failure ${expectedFailure}`);
        fs.copyFileSync(report, path.join(root, `${name}.generated-delivery.xml`));
    } finally {
        fs.writeFileSync(file, original);
    }
}

const securityFixture = JSON.parse(fs.readFileSync(path.join(scenario, 'success.json'), 'utf8'));
securityFixture.security.responseVerify.request.content = 'deliberately-wrong-independent-canonical-text';
rejectChangedFixture('reject-wrong-security-expectation', path.join(scenario, 'success.json'),
    securityFixture, /Synthetic response verification failed/);
const requestFixture = JSON.parse(fs.readFileSync(path.join(scenario, 'success.json'), 'utf8'));
requestFixture.expectedRequest.pathParameters = {reference: 'synthetic-wrong-path'};
rejectChangedFixture('reject-wrong-path-expectation', path.join(scenario, 'success.json'),
    requestFixture, /synthetic-wrong-path/);
const contextFixture = JSON.parse(fs.readFileSync(path.join(scenario, 'success.json'), 'utf8'));
contextFixture.context.merchantId = 'synthetic-wrong-context-merchant';
rejectChangedFixture('reject-wrong-context-identity', path.join(scenario, 'success.json'),
    contextFixture, /pay\/success: request.headers/);
const notificationFixture = JSON.parse(fs.readFileSync(path.join(notificationScenario, 'success.json'), 'utf8'));
notificationFixture.expectedResult.extendInfo = 'synthetic-wrong-extension';
rejectChangedFixture('reject-wrong-notification-extension', path.join(notificationScenario, 'success.json'),
    notificationFixture, /synthetic-wrong-extension/, notificationReport);
const mappingFixture = JSON.parse(fs.readFileSync(path.join(scenario, 'success.json'), 'utf8'));
mappingFixture.resultCode[0].request.channelResultCode = 'wrong-institution-code';
rejectChangedFixture('reject-wrong-result-code-expectation', path.join(scenario, 'success.json'),
    mappingFixture, /pay\/success: result/);
const malformedFailure = JSON.parse(fs.readFileSync(path.join(scenario, 'invalid-signature.json'), 'utf8'));
malformedFailure.http.response.statusCode = 'not-a-status-code';
rejectChangedFixture('reject-malformed-failure-case', path.join(scenario, 'invalid-signature.json'),
    malformedFailure, /response.statusCode must be an integer/);
const unfinishedFailure = JSON.parse(fs.readFileSync(path.join(scenario, 'missing-required-field.json'), 'utf8'));
unfinishedFailure.expectedException.type = 'UnsupportedOperationException';
rejectChangedFixture('reject-unfinished-hook-as-expected-failure', path.join(scenario, 'missing-required-field.json'),
    unfinishedFailure, /Unsupported exception type: UnsupportedOperationException/);

// Mutate actual application code as well as fixture expectations. Each regression must fail a named delivery test.
function discardPreviousTestReport(report, project) {
    assert(project.startsWith(root + path.sep), 'Test reports must belong to this temporary verification workspace');
    assert.equal(path.dirname(report), path.join(project, 'target/surefire-reports'));
    assert.match(path.basename(report), /^TEST-[A-Za-z0-9_.]+\.xml$/);
    // Remove only this expected temporary XML, never an entire report directory or project.
    // A compilation failure cannot borrow the previous mutation's named-test failure evidence.
    if (fs.existsSync(report)) fs.unlinkSync(report);
}

function rejectChangedImplementation(name, file, change, expectedFailure) {
    const original = fs.readFileSync(file, 'utf8');
    const changed = change(original);
    assert.notEqual(changed, original, `${name}: mutation must change the real implementation`);
    try {
        fs.writeFileSync(file, changed);
        discardPreviousTestReport(deliveryReport, deliveryProject);
        run(name, maven, ['-B', '-Dtest=PayDeliveryTest', 'test'], deliveryProject, undefined, 1);
        const evidence = fs.readFileSync(deliveryReport, 'utf8');
        assert.match(evidence, /name="selectedSpiMatchesConfirmedProtocol case=/);
        assert.match(evidence, /<testsuite\b[^>]*(?:failures|errors)="[1-9]\d*"/);
        assert(expectedFailure.test(evidence), `${name}: missing expected named-test failure ${expectedFailure}`);
        fs.copyFileSync(deliveryReport, path.join(root, `${name}.generated-delivery.xml`));
    } finally {
        fs.writeFileSync(file, original);
    }
}
const deliveryPayment = path.join(deliveryProject, 'src/main/java/com/example/adapter/spi/ChannelPaymentService.java');
const deliverySecurity = path.join(deliveryProject, 'src/main/java/com/example/adapter/customize/security/ChannelSecurityCustomization.java');
rejectChangedImplementation('reject-hardcoded-amount', deliveryPayment,
    source => source.replace('body.put("amount", request.getPaymentAmount().getValue());', 'body.put("amount", "1250");'),
    /pay\/second-merchant/);
rejectChangedImplementation('reject-ignored-verification', deliverySecurity,
    source => source.replace('if (!platform.verify(request)) {', 'if (false && !platform.verify(request)) {'),
    /Expected a business exception but invocation returned/);
rejectChangedImplementation('reject-extra-result-code-call', deliveryPayment,
    source => source.replace('JSONObject payload = JSONObject.parseObject(response.getBody());',
        'resultCodes.mapping(request.getChannelCode(), "UNEXPECTED", "Extra mapping", "pay");\n'
        + '                        JSONObject payload = JSONObject.parseObject(response.getBody());'),
    /No interactions wanted/);
const deliveryTemplate = path.join(deliveryProject, 'src/main/java/com/example/adapter/template/ChannelInvocationTemplate.java');
rejectChangedImplementation('reject-extra-http-call', deliveryTemplate,
    source => source.replace('request, urlParameters, toHttpRequest(outbound));',
        'request, urlParameters, toHttpRequest(outbound));\n'
        + '        platformHttpService.executeDynamicUrl(request, urlParameters, toHttpRequest(outbound));'),
    /Wanted 1 time/);
rejectChangedImplementation('reject-wrong-header-restored-after-call', deliveryTemplate,
    source => source.replace('HttpResponse httpResponse = platformHttpService.executeDynamicUrl(\n'
        + '                request, urlParameters, toHttpRequest(outbound));',
        'HttpRequest snapshotRequest = toHttpRequest(outbound);\n'
        + '        snapshotRequest.getHeaders().put("Signature", "incorrect-at-call-time");\n'
        + '        HttpResponse httpResponse;\n'
        + '        try {\n'
        + '            httpResponse = platformHttpService.executeDynamicUrl(request, urlParameters, snapshotRequest);\n'
        + '        } finally {\n'
        + '            snapshotRequest.getHeaders().put("Signature", "synthetic-signature");\n'
        + '        }'),
    /request.headers/);
rejectChangedImplementation('reject-unfinished-hook-implementation', deliveryPayment,
    source => source.replace('throw new IllegalArgumentException("paymentRequestId required");',
        'throw new UnsupportedOperationException("paymentRequestId required");'),
    /unfinished code cannot satisfy a failure case/);
assert.equal(fs.readFileSync(generatedDeliveryTest, 'utf8'), generatedDeliverySource,
    'Never replace generated delivery assertions with a maintainer-written test');
const restored = ais('package-synthetic-restored', ['package', '--project', deliveryProject, '--json']);
assert.equal(JSON.parse(restored.stdout).status, 'verified-locally');
fs.copyFileSync(deliveryReport, path.join(root, 'package-synthetic-restored.generated-delivery.xml'));

// Complete only the generated protocol hooks, keeping the actual wrappers and every generated test.
// Fake platform computation proves delegation/control flow, not institution crypto compatibility.
const completedDemo = identify({schemaVersion: 1, groupId: 'com.example', paymentType: 'non-card',
    threeDS: 'none', spi: ['pay'], securityFeatures: {signature: true, encryption: true},
    sdkJar, sdkPom}, 'completed-demo');
const completedDemoConfig = path.join(root, 'completed-demo.json');
fs.writeFileSync(completedDemoConfig, JSON.stringify(completedDemo));
const completedDemoProject = path.join(root, 'completed-demo');
ais('completed-demo-generate', ['init', '--config', completedDemoConfig, '--output', completedDemoProject, '--json']);
const completedMainRoot = path.join(completedDemoProject, mainJavaRoot(completedDemo));
const completedTestRoot = path.join(completedDemoProject, 'src/test/java', completedDemo.packageName.replaceAll('.', '/'));
const preservedDemoTests = new Map(fs.readdirSync(completedTestRoot)
    .map(name => [name, fs.readFileSync(path.join(completedTestRoot, name), 'utf8')]));

function completeHook(source, declaration, lines) {
    const start = source.indexOf(declaration);
    assert(start >= 0, `Missing generated hook: ${declaration}`);
    assert.equal(source.indexOf(declaration, start + declaration.length), -1, 'Hook must be unique');
    const end = source.indexOf('\n    }', start);
    assert(end > start, `Missing hook boundary: ${declaration}`);
    return source.slice(0, start) + declaration + '\n' + lines.map(line => `        ${line}`).join('\n') + source.slice(end);
}

const completedSecurityFile = path.join(completedMainRoot, 'customize/security/ChannelSecurityCustomization.java');
let completedSecurity = fs.readFileSync(completedSecurityFile, 'utf8');
completedSecurity = completeHook(completedSecurity,
    'protected SecuritySignRequest buildPayRequestSign(ChannelOutboundRequest message) {', [
        'SecuritySignRequest request = new SecuritySignRequest();',
        'request.setContent(message.getBody().toJSONString());',
        'request.setAlgorithm(SecuritySignAlgorithm.HMAC_SHA256_BASE64);',
        'return request;'
    ]);
completedSecurity = completeHook(completedSecurity,
    'protected void applyPayRequestSign(ChannelOutboundRequest message, String result) {', [
        'message.putHeader("Signature", result);'
    ]);
const cipherParameters = [
    'SecurityCipherParameters parameters = new SecurityCipherParameters();',
    'parameters.setMode(SecurityCipherMode.GCM);',
    'parameters.setPadding(SecurityCipherPadding.NoPadding);',
    'parameters.setTagBitLength(128);',
    '// Synthetic fixture values only; no crypto executes and these are not production defaults.',
    'parameters.setIvBase64("synthetic-independent-test-iv");',
    'parameters.setAad("synthetic-independent-test-aad");',
    'request.setParameters(parameters);'
];
completedSecurity = completeHook(completedSecurity,
    'protected SecurityEncryptRequest buildPayRequestEncrypt(ChannelOutboundRequest message) {', [
        'SecurityEncryptRequest request = new SecurityEncryptRequest();',
        'request.setContent(message.getBody().toJSONString());',
        'request.setAlgorithm(SecurityCipherAlgorithm.AES);',
        'request.setCipherType(SecurityCipherType.SYMMETRIC);',
        ...cipherParameters,
        'return request;'
    ]);
completedSecurity = completeHook(completedSecurity,
    'protected void applyPayRequestEncrypt(ChannelOutboundRequest message, String result) {', [
        'com.alibaba.fastjson.JSONObject envelope = new com.alibaba.fastjson.JSONObject();',
        'envelope.put("ciphertext", result);',
        'message.setBody(envelope);'
    ]);
completedSecurity = completeHook(completedSecurity,
    'protected SecurityVerifyRequest buildPayResponseVerify(InboundChannelMessage message, String plainBody) {', [
        'SecurityVerifyRequest request = new SecurityVerifyRequest();',
        'request.setContent(plainBody);',
        'request.setSignature(message.getHeader("Signature"));',
        'request.setAlgorithm(SecuritySignAlgorithm.HMAC_SHA256_BASE64);',
        'return request;'
    ]);
completedSecurity = completeHook(completedSecurity,
    'protected SecurityDecryptRequest buildPayResponseDecrypt(InboundChannelMessage message, String plainBody) {', [
        'SecurityDecryptRequest request = new SecurityDecryptRequest();',
        'request.setCiphertext(com.alibaba.fastjson.JSONObject.parseObject(plainBody).getString("ciphertext"));',
        'request.setAlgorithm(SecurityCipherAlgorithm.AES);',
        'request.setCipherType(SecurityCipherType.SYMMETRIC);',
        ...cipherParameters,
        'return request;'
    ]);
fs.writeFileSync(completedSecurityFile, completedSecurity);

// A private mapping helper and supported platform dependency are valid implementation details.
const completedPaymentFile = path.join(completedMainRoot, 'spi/ChannelPaymentService.java');
let completedPayment = fs.readFileSync(path.join(fixtures, 'ChannelPaymentService.java'), 'utf8')
    .replaceAll('com.example.adapter', completedDemo.packageName)
    .replace('body.put("reference", request.getPaymentRequestId());', 'body.put("reference", reference(request));');
const helper = '\n    private String reference(PayRequest request) {\n'
    + '        return request.getPaymentRequestId();\n    }\n';
completedPayment = completedPayment.slice(0, completedPayment.lastIndexOf('}')) + helper + '}\n';
fs.writeFileSync(completedPaymentFile, completedPayment);

const completedScenario = path.join(completedDemoProject, 'src/test/resources/scenarios/pay');
const demoCase = JSON.parse(fs.readFileSync(path.join(fixtures, 'scenarios/pay/success.json'), 'utf8'));
const demoRawResponse = '{"ciphertext":"synthetic-response-ciphertext"}';
const demoPlainRequest = demoCase.expectedRequest.body;
const demoPlainResponse = demoCase.http.response.body;
demoCase.context.channelCode = completedDemo.channelCode;
demoCase.resultCode[0].request.channelCode = completedDemo.channelCode;
demoCase.http.response.body = demoRawResponse;
demoCase.expectedRequest.body = '{"ciphertext":"synthetic-request-ciphertext"}';
const demoCipherParameters = {mode: 'GCM', padding: 'NoPadding', tagBitLength: 128,
    ivBase64: 'synthetic-independent-test-iv', aad: 'synthetic-independent-test-aad'};
demoCase.security = {
    requestSign: {calls: 1, request: {merchantId: 'synthetic-merchant', algorithm: 'HMAC_SHA256_BASE64',
        content: demoPlainRequest}, result: 'synthetic-signature'},
    requestEncrypt: {calls: 1, request: {merchantId: 'synthetic-merchant', algorithm: 'AES', cipherType: 'SYMMETRIC',
        content: demoPlainRequest, parameters: demoCipherParameters}, result: 'synthetic-request-ciphertext'},
    responseVerify: {calls: 1, request: {merchantId: 'synthetic-merchant', algorithm: 'HMAC_SHA256_BASE64',
        content: demoRawResponse, signature: 'synthetic-response-signature'}, result: true},
    responseDecrypt: {calls: 1, request: {merchantId: 'synthetic-merchant', algorithm: 'AES', cipherType: 'SYMMETRIC',
        ciphertext: 'synthetic-response-ciphertext', parameters: demoCipherParameters}, result: demoPlainResponse}
};
fs.writeFileSync(path.join(completedScenario, 'success.json'), JSON.stringify(demoCase, null, 2));
run('test-completed-demo-all', maven, ['-B', 'test'], completedDemoProject);
const completedStructureReport = path.join(completedDemoProject, 'target/surefire-reports',
    `TEST-${completedDemo.packageName}.GeneratedStructureTest.xml`);
assert.match(fs.readFileSync(completedStructureReport, 'utf8'), /failures="0"/);
for (const [name, source] of preservedDemoTests) {
    assert.equal(fs.readFileSync(path.join(completedTestRoot, name), 'utf8'), source,
        'Completing hooks/dependencies must not require changing generated test assertions');
}

// An unselected but correctly typed SDK override still fails the selected-SPI contract.
const extraSpi = '\n    @Override\n'
    + '    public com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.CancelResponse cancel(\n'
    + '            com.alipay.iacqintegrationhub.channel.sdk.spi.payment.request.CancelRequest request) {\n'
    + '        return null;\n    }\n';
try {
    fs.writeFileSync(completedPaymentFile,
        completedPayment.slice(0, completedPayment.lastIndexOf('}')) + extraSpi + '}\n');
    discardPreviousTestReport(completedStructureReport, completedDemoProject);
    run('reject-extra-spi-override', maven, ['-B', '-Dtest=GeneratedStructureTest', 'test'], completedDemoProject, undefined, 1);
    const extraSpiEvidence = fs.readFileSync(completedStructureReport, 'utf8');
    assert.match(extraSpiEvidence, /Missing, extra or incorrectly typed SPI overrides/);
    assert.match(extraSpiEvidence, /cancel\(/);
    fs.copyFileSync(completedStructureReport, path.join(root, 'reject-extra-spi-override.generated-structure.xml'));
} finally {
    fs.writeFileSync(completedPaymentFile, completedPayment);
}
run('test-completed-demo-restored', maven, ['-B', 'test'], completedDemoProject);

ais('reject-existing', ['init', '--config', secureConfig, '--output', deliveryProject, '--json'], undefined, 4);
const invalid = {...secure, sdkVersion: '1.5.1'};
fs.writeFileSync(path.join(root, 'invalid.json'), JSON.stringify(invalid));
ais('reject-sdk-version', ['init', '--config', path.join(root, 'invalid.json'), '--output', path.join(root, 'invalid'), '--json'], undefined, 3);
fs.writeFileSync(path.join(root, 'verification.json'), JSON.stringify({root, results}, null, 2));
console.log(`All checks passed. Evidence: ${root}/verification.json`);
