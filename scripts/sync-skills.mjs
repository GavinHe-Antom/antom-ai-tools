// Mirror the source-of-truth skills/ directory into each provider plugin package.
//
// Source: skills/<skill>/...
// Targets:
//   - providers/cursor/plugin/skills/<skill>/...
//   - providers/claude/plugin/skills/<skill>/...
//   - providers/codex/plugins/antom-integration/skills/<skill>/...
//
// Usage:
//   node scripts/sync-skills.mjs
//   npm run sync-skills

import { promises as fs } from "node:fs";
import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const repoRoot = path.resolve(path.dirname(__filename), "..");

const SOURCE_DIR = path.join(repoRoot, "skills");

// Codex bundles every skill under the single antom-integration plugin, so all
// skills share one plugin's skills/ directory (one plugin, multiple skills).
const CODEX_SKILLS_DIR = path.join(
  repoRoot,
  "providers/codex/plugins/antom-integration/skills",
);

const FLAT_TARGETS = [
  path.join(repoRoot, "providers/cursor/plugin/skills"),
  path.join(repoRoot, "providers/claude/plugin/skills"),
];

async function* walk(dir, eligibleFiles, eligibleDirectories) {
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory() && eligibleDirectories.has(p)) {
      yield* walk(p, eligibleFiles, eligibleDirectories);
    } else if (entry.isFile() && eligibleFiles.has(p)) {
      yield p;
    }
  }
}

async function copyFile(src, dest) {
  await fs.mkdir(path.dirname(dest), { recursive: true });
  const content = await fs.readFile(src);
  await fs.writeFile(dest, content);
}

const run = async () => {
  const skillNames = (
    await fs.readdir(SOURCE_DIR, { withFileTypes: true })
  )
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name);

  if (skillNames.length === 0) {
    console.log("No skills found under skills/. Nothing to sync.");
    return;
  }

  // Include tracked files and new, untracked skill files, but never traverse
  // ignored build output or local SDKs. NUL delimiters preserve unusual names.
  const gitFiles = execFileSync(
    "git",
    ["ls-files", "--cached", "--others", "--exclude-standard", "-z", "--", "skills/"],
    { cwd: repoRoot, encoding: "utf8", maxBuffer: 10 * 1024 * 1024 },
  );
  const eligibleFiles = new Set();
  const eligibleDirectories = new Set();

  for (const relativeFile of gitFiles.split("\0")) {
    if (!relativeFile) continue;

    const file = path.join(repoRoot, relativeFile);
    eligibleFiles.add(file);
    let directory = path.dirname(file);
    while (directory.startsWith(`${SOURCE_DIR}${path.sep}`)) {
      eligibleDirectories.add(directory);
      directory = path.dirname(directory);
    }
  }

  let writeCount = 0;

  for (const skill of skillNames) {
    const skillSource = path.join(SOURCE_DIR, skill);
    const targets = [
      ...FLAT_TARGETS.map((base) => path.join(base, skill)),
      // Codex layout: providers/codex/plugins/antom-integration/skills/<skill>/
      path.join(CODEX_SKILLS_DIR, skill),
    ];

    for await (const file of walk(skillSource, eligibleFiles, eligibleDirectories)) {
      const rel = path.relative(skillSource, file);
      for (const target of targets) {
        const dest = path.join(target, rel);
        await copyFile(file, dest);
        console.log(`✓ ${path.relative(repoRoot, dest)}`);
        writeCount++;
      }
    }
  }

  console.log(
    `\nSynced ${skillNames.length} skill(s) — ${writeCount} file write(s).`,
  );
};

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
