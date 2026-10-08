// SPDX-License-Identifier: Apache-2.0
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {readBytecode, compareSnapshots} from './check-sdk-contracts.mjs';

function fixture(t) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'ais-contract-check-'));
    t.after(() => fs.rmSync(root, {recursive: true, force: true}));
    fs.mkdirSync(path.join(root, 'models'));
    fs.writeFileSync(path.join(root, 'models/Example.md'), '| `labels` | `Map<String, List<String>>` | Example |\n');
    const contracts = {types: [{name: 'Example', qualifiedName: 'demo.Example', extends: [],
        fields: [{name: 'labels', type: 'Map<String, List<String>>'}], enumValues: [], methods: [
            {name: 'read', parameters: [{type: 'Map<String, List<String>>'}], returnType: 'String'}
        ]}], spi: []};
    const bytecode = 'public class demo.Example {\n'
        + ' private java.util.Map<java.lang.String, java.util.List<java.lang.String>> labels;\n'
        + ' private static final java.lang.String INTERNAL;\n'
        + ' public java.lang.String read(java.util.Map<java.lang.String, java.util.List<java.lang.String>>);\n}\n';
    return {root, contracts, actual: readBytecode(bytecode)};
}

test('compares generic types and Markdown without mistaking static implementation fields for contract data', t => {
    const {root, contracts, actual} = fixture(t);
    assert.deepEqual(compareSnapshots(contracts, actual, root), {errors: [], types: 1, spiMethods: 0, markdownTables: 1});
});

test('reports SDK field additions and removed documented methods', t => {
    const {root, contracts, actual} = fixture(t);
    actual.get('demo.Example').fields.push('unexpected:String');
    actual.get('demo.Example').methods = [];
    const report = compareSnapshots(contracts, actual, root);
    assert(report.errors.some(error => error.includes('unexpected unexpected:String')));
    assert(report.errors.some(error => error.includes('method missing: read(')));
});

test('reports a missing SDK class and a changed Markdown field type', t => {
    const {root, contracts, actual} = fixture(t);
    assert(compareSnapshots(contracts, new Map(), root).errors.some(error => error.includes('SDK type missing')));
    fs.writeFileSync(path.join(root, 'models/Example.md'), '| `labels` | `String` | Example |\n');
    assert(compareSnapshots(contracts, actual, root).errors.some(error => error.includes('Example Markdown:')));
});

test('distinguishes enum constants and default method signatures', () => {
    const actual = readBytecode('public final class demo.Mode extends java.lang.Enum<demo.Mode> {\n'
        + ' public static final demo.Mode FIRST;\n private static final demo.Mode[] $VALUES;\n}\n'
        + 'public interface demo.Service {\n public default java.lang.String invoke(java.lang.String);\n}\n');
    assert.deepEqual(actual.get('demo.Mode').enums, ['FIRST']);
    assert.deepEqual(actual.get('demo.Service').methods, [{name: 'invoke', defaultMethod: true, signature: 'invoke(String):String'}]);
});

function inheritedFixture(t) {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'ais-inherited-contract-'));
    t.after(() => fs.rmSync(root, {recursive: true, force: true}));
    fs.mkdirSync(path.join(root, 'models'));
    fs.writeFileSync(path.join(root, 'models/Request.md'), '| `amount` | `String` | Amount |\n| `channelCode` | `String` | Context |\n');
    const contracts = {types: [
        {name: 'Base', qualifiedName: 'demo.Base', extends: [], fields: [{name: 'channelCode', type: 'String'}], methods: [], enumValues: []},
        {name: 'Request', qualifiedName: 'demo.Request', imports: ['demo.Base', 'java.io.Serializable'], extends: ['Base'], implements: ['Serializable'],
            fields: [{name: 'amount', type: 'String'}], methods: [], enumValues: []}
    ], spi: []};
    const bytecode = 'public class demo.Base {\n private java.lang.String channelCode;\n}\n'
        + 'public class demo.Request extends demo.Base implements java.io.Serializable {\n private java.lang.String amount;\n}\n';
    return {root, contracts, actual: readBytecode(bytecode)};
}

test('checks qualified direct parents, implemented interfaces and actual inherited SDK fields', t => {
    const {root, contracts, actual} = inheritedFixture(t);
    assert.deepEqual(compareSnapshots(contracts, actual, root).errors, []);
    actual.get('demo.Request').extends = [];
    const errors = compareSnapshots(contracts, actual, root).errors;
    assert(errors.some(error => error === 'Request parents: missing demo.Base'));
    assert(errors.some(error => error === 'Request SDK inherited fields: missing channelCode:String'));
});

test('detects a changed parent with an equal simple name from another package', t => {
    const {root, contracts, actual} = inheritedFixture(t);
    actual.get('demo.Request').extends = ['other.Base'];
    actual.set('other.Base', {fields: ['channelCode:String'], extends: [], implements: [], methods: [], enums: []});
    const errors = compareSnapshots(contracts, actual, root).errors;
    assert(errors.includes('Request parents: missing demo.Base'));
    assert(errors.includes('Request parents: unexpected other.Base'));
});

test('detects removed, added and changed implemented interfaces', t => {
    const {root, contracts, actual} = inheritedFixture(t);
    actual.get('demo.Request').implements = [];
    assert(compareSnapshots(contracts, actual, root).errors.includes('Request interfaces: missing java.io.Serializable'));
    actual.get('demo.Request').implements = ['other.Serializable', 'java.lang.Cloneable'];
    const errors = compareSnapshots(contracts, actual, root).errors;
    assert(errors.includes('Request interfaces: unexpected other.Serializable'));
    assert(errors.includes('Request interfaces: unexpected java.lang.Cloneable'));
});

test('parses generic class bounds, nested generic parents and interface inheritance without losing qualification', () => {
    const actual = readBytecode('public abstract class demo.Generic<T extends java.lang.Number, U> extends demo.Base<java.util.Map<java.lang.String, T>> implements demo.Reader<T>, demo.Writer<U> {\n}\n'
        + 'public interface demo.Combined<T> extends demo.Reader<T>, demo.Writer<java.util.Map<java.lang.String, java.util.List<T>>> {\n}\n');
    assert.deepEqual(actual.get('demo.Generic').extends, ['demo.Base<java.util.Map<java.lang.String,T>>']);
    assert.deepEqual(actual.get('demo.Generic').implements, ['demo.Reader<T>', 'demo.Writer<U>']);
    assert.deepEqual(actual.get('demo.Combined').extends, ['demo.Reader<T>', 'demo.Writer<java.util.Map<java.lang.String,java.util.List<T>>>']);
});

test('does not confuse an Enum generic bound with an actual enum parent', () => {
    const actual = readBytecode('public class demo.Bounded<T extends java.lang.Enum<T>> implements java.io.Serializable {\n private T value;\n}\n');
    assert.equal(actual.get('demo.Bounded').enumType, false);
    assert.deepEqual(actual.get('demo.Bounded').fields, ['value:T']);
    assert.deepEqual(actual.get('demo.Bounded').implements, ['java.io.Serializable']);
});

test('detects changed generic parent arguments and removed extended interfaces', t => {
    const {root, contracts} = fixture(t);
    contracts.types[0].extends = ['demo.Base<String>'];
    const actual = readBytecode('public class demo.Example extends demo.Base<java.lang.Integer> {\n private java.util.Map<java.lang.String, java.util.List<java.lang.String>> labels;\n public java.lang.String read(java.util.Map<java.lang.String, java.util.List<java.lang.String>>);\n}\n');
    // This fixture checks inheritance shape without a field-table traversal into an external parent.
    fs.unlinkSync(path.join(root, 'models/Example.md'));
    const errors = compareSnapshots(contracts, actual, root).errors;
    assert(errors.includes('Example parents: missing demo.Base<java.lang.String>'));
    assert(errors.includes('Example parents: unexpected demo.Base<java.lang.Integer>'));
    contracts.types[0].extends = ['java.io.Serializable'];
    actual.get('demo.Example').extends = [];
    assert(compareSnapshots(contracts, actual, root).errors.includes('Example parents: missing java.io.Serializable'));
});

test('reports a parent absent from the actual inspected SDK graph', t => {
    const {root, contracts, actual} = inheritedFixture(t);
    actual.delete('demo.Base');
    const errors = compareSnapshots(contracts, actual, root).errors;
    assert(errors.includes('SDK type missing: demo.Base'));
    assert(errors.includes('SDK inherited type missing from inspected contracts: demo.Base'));
});
