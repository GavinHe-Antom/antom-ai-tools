// SPDX-License-Identifier: Apache-2.0
// Compare an authorized SDK with maintained API snapshots. Never download or redistribute the SDK.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const skillRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

function typeName(value) {
    return value.replace(/\b(?:[\w$]+\.)+([\w$]+)/g, '$1').replace(/\s+/g, '').replace(/\.\.\./g, '[]');
}

function parameters(value) {
    if (!value.trim()) return [];
    const result = [];
    let start = 0;
    let depth = 0;
    for (let i = 0; i < value.length; i++) {
        if (value[i] === '<') depth++;
        else if (value[i] === '>') depth--;
        else if (value[i] === ',' && depth === 0) {
            result.push(typeName(value.slice(start, i)));
            start = i + 1;
        }
    }
    result.push(typeName(value.slice(start)));
    return result;
}

/** Split inheritance lists without splitting commas inside generic arguments. */
function inheritedTypes(value) {
    if (!value.trim()) return [];
    const result = [];
    let depth = 0;
    let start = 0;
    for (let index = 0; index < value.length; index++) {
        if (value[index] === '<') depth++;
        else if (value[index] === '>') depth--;
        else if (value[index] === ',' && depth === 0) {
            result.push(value.slice(start, index).replace(/\s+/g, ''));
            start = index + 1;
        }
    }
    result.push(value.slice(start).replace(/\s+/g, ''));
    return result;
}

/** Remove the class's own type parameters before reading its direct parents. */
function inheritanceHeader(value) {
    let tail = value.trim();
    if (tail.startsWith('<')) {
        let depth = 0;
        for (let index = 0; index < tail.length; index++) {
            if (tail[index] === '<') depth++;
            else if (tail[index] === '>') depth--;
            if (depth === 0) {
                tail = tail.slice(index + 1).trim();
                break;
            }
        }
    }
    tail = tail.replace(/\s*\{$/, '');
    const extended = tail.match(/\bextends\s+(.+?)(?:\s+implements\s+|$)/);
    const implemented = tail.match(/\bimplements\s+(.+)$/);
    return {extends: inheritedTypes(extended?.[1] || ''), implements: inheritedTypes(implemented?.[1] || '')};
}

/** Parse declared API shape and qualified inheritance, not implementation bodies. */
export function readBytecode(output) {
    const types = new Map();
    let current;
    for (const raw of output.split('\n')) {
        const line = raw.trim();
        const header = line.match(/^(?:public\s+)?(?:(?:final|abstract)\s+)*(class|interface)\s+([\w.$]+)(.*)$/);
        if (header) {
            current = {fields: [], enums: [], methods: [], ...inheritanceHeader(header[3])};
            current.enumType = current.extends.some(parent => parent.startsWith('java.lang.Enum<'));
            // Enum's implicit JDK parent is not a declared SDK inheritance contract.
            if (current.enumType) current.extends = [];
            current.extends = current.extends.filter(parent => parent !== 'java.lang.Object');
            types.set(header[2], current);
            continue;
        }
        if (!current) continue;
        const field = line.match(/^(?:public|protected|private)\s+(.+?)\s+([\w$]+);$/);
        if (field) {
            if (/\bstatic\b/.test(field[1])) {
                if (current.enumType && /^public static final /.test(line) && !field[2].startsWith('$')) {
                    current.enums.push(field[2]);
                }
            } else {
                const type = field[1].replace(/^(?:(?:final|volatile|transient)\s+)*/, '');
                current.fields.push(`${field[2]}:${typeName(type)}`);
            }
            continue;
        }
        const method = line.match(/^(?:public|protected|private)\s+(.+?)\s+([\w$]+)\((.*)\)(?:\s+throws\s+[^;]+)?;$/);
        if (method) {
            const returnType = method[1].replace(/^(?:(?:static|final|abstract|default|synchronized)\s+)*/, '')
                .replace(/^<[^>]+>\s*/, '');
            current.methods.push({name: method[2], defaultMethod: /\bdefault\b/.test(method[1]),
                signature: `${method[2]}(${parameters(method[3]).join(',')}):${typeName(returnType)}`});
        }
    }
    return types;
}

function compareSet(label, expected, actual, errors) {
    const wanted = new Set(expected);
    const found = new Set(actual);
    for (const value of wanted) if (!found.has(value)) errors.push(`${label}: missing ${value}`);
    for (const value of found) if (!wanted.has(value)) errors.push(`${label}: unexpected ${value}`);
}

/** Resolve documented parent names without confusing equal simple names from different packages. */
function qualifiedParent(value, type, byName) {
    return value.replace(/[\w.$]+/g, name => {
        if (name.includes('.') || ['extends', 'super'].includes(name)) return name;
        const imported = (type.imports || []).find(item => item.endsWith(`.${name}`));
        if (imported) return imported;
        const documented = byName.get(name);
        if (documented) return documented.qualifiedName;
        if (['Object', 'String', 'Number', 'Comparable', 'Enum'].includes(name)) return `java.lang.${name}`;
        return name;
    }).replace(/\s+/g, '');
}

/** Collect documented fields iteratively, keeping a child's field ahead of a shadowed parent field. */
function allFields(type, byName) {
    const pending = [type];
    const seen = new Set();
    const fields = new Map();
    for (let index = 0; index < pending.length; index++) {
        const next = pending[index];
        if (seen.has(next.name)) continue;
        seen.add(next.name);
        for (const field of next.fields) if (!fields.has(field.name)) fields.set(field.name, typeName(field.type));
        for (const parent of next.extends || []) {
            const inherited = byName.get(typeName(parent).replace(/<.*>$/, ''));
            if (!inherited) throw new Error(`Unknown documented parent ${parent} of ${next.name}`);
            pending.push(inherited);
        }
    }
    return [...fields].map(([name, type]) => `${name}:${type}`);
}

/** Read inherited fields from the SDK graph, not the documented parent graph. */
function sdkFields(qualifiedName, actual, errors) {
    const pending = [qualifiedName];
    const seen = new Set();
    const fields = new Map();
    for (let index = 0; index < pending.length; index++) {
        const name = pending[index];
        if (seen.has(name)) continue;
        seen.add(name);
        const next = actual.get(name);
        if (!next) {
            errors.push(`SDK inherited type missing from inspected contracts: ${name}`);
            continue;
        }
        for (const field of next.fields) {
            const separator = field.indexOf(':');
            const fieldName = field.slice(0, separator);
            if (!fields.has(fieldName)) fields.set(fieldName, field);
        }
        for (const parent of next.extends) {
            const parentName = parent.replace(/<.*>$/, '');
            // External JDK parents carry no documented SDK business fields.
            if (!parentName.startsWith('java.')) pending.push(parentName);
        }
    }
    return [...fields.values()];
}

function markdownFields(text) {
    const fields = [];
    for (const line of text.split('\n')) {
        const row = line.match(/^\|\s*`([\w]+)`\s*\|\s*`([^`]+)`/);
        if (row) fields.push(`${row[1]}:${typeName(row[2])}`);
    }
    return fields;
}

export function compareSnapshots(contracts, actual, referenceRoot) {
    const errors = [];
    const byName = new Map(contracts.types.map(type => [type.name, type]));
    let markdownTables = 0;
    for (const type of contracts.types) {
        const found = actual.get(type.qualifiedName);
        if (!found) {
            errors.push(`SDK type missing: ${type.qualifiedName}`);
            continue;
        }
        compareSet(`${type.name} parents`, (type.extends || []).map(parent => qualifiedParent(parent, type, byName)), found.extends, errors);
        compareSet(`${type.name} interfaces`, (type.implements || []).map(parent => qualifiedParent(parent, type, byName)), found.implements, errors);
        compareSet(`${type.name} fields`, type.fields.map(field => `${field.name}:${typeName(field.type)}`), found.fields, errors);
        compareSet(`${type.name} enum constants`, type.enumValues.map(value => value.name), found.enums, errors);
        for (const method of type.methods) {
            const signature = `${method.name}(${method.parameters.map(item => typeName(item.type)).join(',')}):${typeName(method.returnType)}`;
            const match = found.methods.find(item => item.signature === signature);
            if (!match) errors.push(`${type.name} method missing: ${signature}`);
            else if (Boolean(method.defaultMethod) !== match.defaultMethod) errors.push(`${type.name}.${method.name}: default-method mismatch`);
        }
        if (/\.spi\.(?:payment|refund|notify)\.[^.]+Service$/.test(type.qualifiedName)) {
            compareSet(`${type.name} SPI methods`, type.methods.map(method => method.name), found.methods.map(method => method.name), errors);
        }
        const modelFile = path.join(referenceRoot, 'models', `${type.name}.md`);
        if (fs.existsSync(modelFile) && allFields(type, byName).length) {
            compareSet(`${type.name} Markdown`, allFields(type, byName), markdownFields(fs.readFileSync(modelFile, 'utf8')), errors);
            compareSet(`${type.name} SDK inherited fields`, allFields(type, byName), sdkFields(type.qualifiedName, actual, errors), errors);
            markdownTables++;
        }
    }
    for (const spi of contracts.spi) {
        const type = byName.get(spi.spi);
        const method = type?.methods.find(item => item.name === spi.method);
        if (!method || typeName(method.returnType) !== spi.responseType
                || typeName(method.parameters[0]?.type || '') !== spi.requestType) {
            errors.push(`SPI index signature mismatch: ${spi.spi}.${spi.method}`);
        }
        const page = fs.readFileSync(path.join(referenceRoot, spi.documentation), 'utf8');
        const requestSection = page.split('## Request')[1]?.split('## Response')[0] || '';
        const responseSection = page.split('## Response')[1]?.split('\n## ')[0] || '';
        for (const [name, section] of [[spi.requestType, requestSection], [spi.responseType, responseSection]]) {
            const valueType = byName.get(name);
            if (!valueType) errors.push(`SPI ${spi.method}: unknown type ${name}`);
            else {
                compareSet(`${spi.method} ${name} Markdown`, allFields(valueType, byName), markdownFields(section), errors);
                compareSet(`${spi.method} ${name} SDK inherited fields`, allFields(valueType, byName), sdkFields(valueType.qualifiedName, actual, errors), errors);
            }
            markdownTables++;
        }
    }
    return {errors, types: contracts.types.length, spiMethods: contracts.spi.length, markdownTables};
}

function main() {
    const jar = path.resolve(process.argv[2] || path.join(skillRoot, 'scripts/ais-cli/sdk/common-sdk-1.5.2.jar'));
    if (!fs.existsSync(jar)) throw new Error(`Obtain the authorized platform SDK separately and place it at ${jar}`);
    const referenceRoot = path.join(skillRoot, 'references/guide/reference');
    const contracts = JSON.parse(fs.readFileSync(path.join(referenceRoot, 'contracts.json'), 'utf8'));
    const javap = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/javap') : 'javap';
    const result = spawnSync(javap, ['-private', '-classpath', jar, ...contracts.types.map(type => type.qualifiedName)],
        {encoding: 'utf8', timeout: 60000, maxBuffer: 16 * 1024 * 1024});
    if (result.error || result.status !== 0) throw new Error(`SDK inspection failed: ${result.error || result.stderr}`);
    const report = compareSnapshots(contracts, readBytecode(result.stdout), referenceRoot);
    if (report.errors.length) throw new Error(report.errors.join('\n'));
    console.log(`PASS SDK contracts: ${report.types} types, ${report.spiMethods} SPI methods, ${report.markdownTables} Markdown field tables`);
    console.log('This checks API shape, not business semantics, protocol correctness or runtime exception bodies.');
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try { main(); } catch (error) { console.error(error.message); process.exitCode = 1; }
}
