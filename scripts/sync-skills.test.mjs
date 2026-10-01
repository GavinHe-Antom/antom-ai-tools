import test from "node:test";
import assert from "node:assert/strict";
import { copyFile, mkdir, mkdtemp, readFile, readdir, rm, symlink, writeFile } from "node:fs/promises";
import { execFileSync } from "node:child_process";
import { tmpdir } from "node:os";
import path from "node:path";

const targetDirectories = [
  "providers/cursor/plugin/skills",
  "providers/claude/plugin/skills",
  "providers/codex/plugins/antom-integration/skills",
];

async function fixture(t) {
  const root = await mkdtemp(path.join(tmpdir(), "sync-skills-"));
  t.after(() => rm(root, { recursive: true, force: true }));
  await mkdir(path.join(root, "scripts"), { recursive: true });
  await mkdir(path.join(root, "skills"), { recursive: true });
  await copyFile(new URL("./sync-skills.mjs", import.meta.url), path.join(root, "scripts/sync-skills.mjs"));

  const git = (...args) => execFileSync("git", args, { cwd: root, stdio: "ignore" });
  git("init", "-q");

  const write = async (relativeFile, content = relativeFile) => {
    const file = path.join(root, relativeFile);
    await mkdir(path.dirname(file), { recursive: true });
    await writeFile(file, content);
  };
  const sync = () => execFileSync(process.execPath, ["scripts/sync-skills.mjs"], { cwd: root, encoding: "utf8" });
  return { root, git, write, sync };
}

async function filesUnder(directory, relativeDirectory = "") {
  const files = [];
  for (const entry of await readdir(path.join(directory, relativeDirectory), { withFileTypes: true })) {
    const relativeFile = path.join(relativeDirectory, entry.name);
    if (entry.isDirectory()) {
      files.push(...await filesUnder(directory, relativeFile));
    } else {
      files.push(relativeFile);
    }
  }
  return files.sort();
}

async function assertMirrors(root, expectedFiles) {
  for (const targetDirectory of targetDirectories) {
    const target = path.join(root, targetDirectory);
    assert.deepEqual(await filesUnder(target), Object.keys(expectedFiles).sort());
    for (const [relativeFile, content] of Object.entries(expectedFiles)) {
      assert.deepEqual(await readFile(path.join(target, relativeFile)), Buffer.from(content));
    }
  }
}

test("sync mirrors tracked and new nonignored skill files to the existing destinations", async t => {
  const { root, git, write, sync } = await fixture(t);
  const expectedFiles = {
    "existing-skill/SKILL.md": "Tracked skill\n",
    "existing-skill/scripts/empty.txt": "",
    "new skill/SKILL.md": "New skill\n",
    "new skill/scripts/nested folder/name with\nnewline.txt": "Nested file\n",
    "new skill/scripts/binary.dat": Buffer.from([0, 1, 255]),
  };
  for (const [relativeFile, content] of Object.entries(expectedFiles)) {
    await write(path.join("skills", relativeFile), content);
  }
  git("add", "--", "skills/existing-skill");
  await write("README.md", "Outside skills\n");
  await write(".codefuse/local.txt", "Local configuration\n");

  const firstOutput = sync();
  assert.match(firstOutput, /Synced 2 skill\(s\) — 15 file write\(s\)/);
  await assertMirrors(root, expectedFiles);

  // Repeated runs preserve bytes, layout, and unrelated destination content.
  for (const targetDirectory of targetDirectories) {
    await write(path.join(targetDirectory, "keep.txt"), "Keep existing destination content\n");
  }
  const secondOutput = sync();
  assert.equal(secondOutput, firstOutput);
  await assertMirrors(root, { ...expectedFiles, "keep.txt": "Keep existing destination content\n" });
});

test("sync excludes Git-ignored Maven output, separately licensed SDKs, and local exclusions", async t => {
  const { root, git, write, sync } = await fixture(t);
  await write(".gitignore", "skills/new-skill/cache/\n");
  await write(".git/info/exclude", "skills/new-skill/local-only/\n");
  const expectedFiles = {
    "existing-skill/SKILL.md": "Tracked skill\n",
    "new-skill/SKILL.md": "New skill\n",
    "new-skill/scripts/ais-cli/.gitignore": "/target/\n/sdk/\n",
    "new-skill/scripts/ais-cli/pom.xml": "<project/>\n",
    "new-skill/scripts/ais-cli/src/main/java/App.java": "class App {}\n",
  };
  for (const [relativeFile, content] of Object.entries(expectedFiles)) {
    await write(path.join("skills", relativeFile), content);
  }
  git("add", "--", ".gitignore", "skills/existing-skill");
  await write("skills/new-skill/scripts/ais-cli/target/classes/App.class", "Build output\n");
  await write("skills/new-skill/scripts/ais-cli/sdk/sdk.jar", "Separately licensed SDK\n");
  await write("skills/new-skill/cache/temp.txt", "Ignored by the repository\n");
  await write("skills/new-skill/local-only/private.txt", "Excluded locally\n");

  sync();
  await assertMirrors(root, expectedFiles);
});

test("sync skips symlink files and directories, including tracked paths below replaced directories", async t => {
  const { root, git, write, sync } = await fixture(t);
  await write("skills/test-skill/SKILL.md", "Regular skill\n");
  await write("skills/test-skill/replaced-dir/outside.txt", "Previously tracked\n");
  await write("external/outside.txt", "External content\n");
  await symlink("../../external/outside.txt", path.join(root, "skills/test-skill/tracked-link.txt"));
  git("add", "--", "skills/test-skill");
  await rm(path.join(root, "skills/test-skill/replaced-dir"), { recursive: true });
  await symlink("../../external", path.join(root, "skills/test-skill/replaced-dir"));
  await symlink("../../external", path.join(root, "skills/test-skill/new-link-dir"));
  await symlink("../external", path.join(root, "skills/linked-skill"));

  sync();
  await assertMirrors(root, { "test-skill/SKILL.md": "Regular skill\n" });
});
