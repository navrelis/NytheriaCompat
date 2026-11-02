#!/usr/bin/env python3
"""Add or refresh the "Changed by the Nytheria Compat project" notice in modified upstream source files.

LGPL-2.1 section 2(a) requires modified files to carry prominent notices stating that they were changed and the
date of the change. Nytheria Compat's public source snapshot has no git history, so every source file that came from
upstream and was changed gets a dated notice in its header. This script finds those files with git, works out the
first and last date they were changed by us, and writes (or updates) the notice.

Which files count as modified upstream files:
  src/, compat/          *.java / *.kt changed or renamed since the Kilt tag v21.1.13. Git pairs files with their
                         upstream file at 30% similarity (short renamed files fall below the default 50%), and a file
                         that git still sees as added counts as upstream if a file of the same name exists in the
                         tag (Nytheria Compat -> Kilt in the name is allowed). Over-marking is safe, under-marking is not.
                         The remaining files are listed as "Nytheria Compat-only" for review; files identical to upstream
                         are skipped.
  build-logic/           *.java / *.kt like src/ and compat/ (the Gradle build logic of Kilt).
  forge/, fml/, loader/  *.java / *.kt / *.kts changed since the commit that vendored them (see VENDORED.md).
                         Files added after vendoring are skipped.

Other file types are covered as well. In src/, compat/, build-logic/, tweakers/ and the root-level files
build.gradle.kts, settings.gradle.kts, gradle.properties, README.md, .gitignore, .github/ and gradle/, every other file that git
reports as modified (M) or renamed below 100% (R) against the Kilt tag counts. Files git still sees as added do not
count, with one exception: an added file that sits in the same directory as a deleted file of the same extension
(for example a META-INF/services file whose name changed with the package rename) is its renamed upstream file and
counts. An added file also counts if it has the name of a deleted file apart from Kilt -> Nytheria Compat. In forge/, fml/
and loader/ the non-code files modified since vendoring count. Never considered are
.wakatime-project and everything under .knowledge/, testing/, tools/release/ and logs/. The notice goes into the
file in the comment syntax of its type, with the same two sentences everywhere:
  .kts, .gradle                    the /* ... */ block described above (also for *.kts under src/ and compat/)
  .properties, .toml, .yml, .yaml, two "# " lines at the very top (after a BOM), then one blank line; in a YAML file
  .gitignore, META-INF/services/*  that starts with a "---" line they go after that line instead
  .accesswidener, .classtweaker    two "# " lines directly after the mandatory header line (accessWidener ... /
                                   classTweaker ...), which has to stay the first line
  .md                              two one-line HTML comments at the very top, then one blank line
Fabric's access widener and class tweaker readers, Night Config's TOML parser (default_mods.toml) and
java.util.ServiceLoader all accept "#" comment lines.
Files without comment syntax (.json, .tiny, .txt, ...), the files in tweakers/ (ClassTweakerUpdater merges them into
src/main/resources/nytheria_compat.classtweaker, which would copy a comment into the middle of that file), binary files (NUL byte in the first 8 KB, or .png, .jar, .zip,
.jpg, .gif, .ico, .nbt) and files whose format is not recognised are never modified. They are listed with their dates
in docs/CHANGED-FILES.md instead, which the script writes (sorted by path, no timestamp, so it only changes when the
list changes).

Run from anywhere inside the repository (the script finds the repository root itself):

    python tools/licence/add-change-notices.py            # write the notices
    python tools/licence/add-change-notices.py --dry-run  # only report what would change
    python tools/licence/add-change-notices.py --check    # exit 1 if any notice or the list file is out of date

The script is idempotent: a file that already has a notice starting with "Changed by the Nytheria Compat project" gets
that notice rewritten with the current dates instead of a second one. In Java, Kotlin and Gradle Kotlin files an
existing licence/copyright comment at the top of a file is never changed; the notice is inserted directly after it
(or at the very top if the file has no such comment). Line endings, BOM and encoding of each file are preserved.

The commit that adds the notices would itself count as a change and move every "last changed" date to its own
day. To avoid that, commits whose message contains the --skip-grep text (default "change notices") are ignored
when the dates are computed, so give that commit a message such as "Add LGPL change notices".

Only the Python 3 standard library and the git command line are used.
"""

import argparse
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

MARKER = "Changed by the Nytheria Compat project"
KILT_ORIGIN = ("Kilt by KiltMC (https://github.com/KiltMC/Kilt)", "the GNU LGPL 2.1")

ROOT_PATHS = ["build.gradle.kts", "settings.gradle.kts", "gradle.properties", "README.md", ".gitignore", ".github",
              "gradle"]
# Never considered, whatever git says.
EXCLUDED_NAMES = {".wakatime-project"}
EXCLUDED_PREFIXES = (".knowledge/", "testing/", "tools/release/", "logs/")
LIST_ONLY_PREFIXES = ("tweakers/",)
BINARY_EXTENSIONS = (".png", ".jar", ".zip", ".jpg", ".gif", ".ico", ".nbt")
CHANGED_FILES_LIST = "docs/CHANGED-FILES.md"

# name -> (git paths, extensions of the code files handled by the first pass, base kind, origin, licence)
TREES = {
    "src": (["src"], (".java", ".kt"), "kilt", *KILT_ORIGIN),
    "compat": (["compat"], (".java", ".kt"), "kilt", *KILT_ORIGIN),
    # Root-level files of the Kilt repository; they hold no Java/Kotlin code, so only the other-files pass applies.
    "root": (ROOT_PATHS, (), "kilt", *KILT_ORIGIN),
    "build-logic": (["build-logic"], (".java", ".kt"), "kilt", *KILT_ORIGIN),
    # Class tweakers and access wideners that ClassTweakerUpdater merges into src/main/resources/nytheria_compat.classtweaker
    # (and partly regenerates), so a comment in them would be copied into the merged file. Listed instead.
    "tweakers": (["tweakers"], (), "kilt", *KILT_ORIGIN),
    "forge": (["forge"], (".java", ".kt", ".kts"), "vendor",
              "NeoForge by NeoForged, as ported by KiltMC (see VENDORED.md)", "the GNU LGPL 2.1"),
    "fml": (["fml"], (".java", ".kt", ".kts"), "vendor",
            "FancyModLoader by NeoForged, as ported by KiltMC (see VENDORED.md)", "the GNU LGPL 2.1"),
    "loader": (["loader"], (".java", ".kt", ".kts"), "vendor",
               "Knit Loader by KiltMC (see VENDORED.md)", "the MIT licence (loader/LICENSE)"),
}

LICENCE_WORDS = re.compile(r"copyright|licen[cs]e|spdx|permission|warranty", re.IGNORECASE)
WHITESPACE = re.compile(r"\s*")
# Existing "// TRACKED HASH" lines have to stay the first line of an inject class, so the notice goes below them.
TRACKED_HASH = re.compile(r"// TRACKED HASH: [^\r\n]*(?:\r\n|\n)")
BLANK_LINES = re.compile(r"(?:[ \t]*\r?\n)*")


def git(*args, root):
    result = subprocess.run(["git", *args], cwd=root, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode != 0:
        sys.exit("git %s failed: %s" % (" ".join(args), result.stderr.decode("utf-8", "replace").strip()))
    return result.stdout.decode("utf-8", "surrogateescape")


def find_vendoring_commit(root):
    out = git("log", "--diff-filter=A", "--format=%H", "--", "forge/LICENSE.txt", "fml/LICENSE.txt",
              "loader/LICENSE", root=root).split()
    if not out:
        sys.exit("Could not find the vendoring commit (no commit adds forge/LICENSE.txt).")
    return out[-1]  # oldest


def changed_files(root, base, paths, extensions):
    """Return (modified paths as they are in HEAD, added paths, skipped [(path, reason)]).

    Renames are detected at a low similarity (30%), because short files whose package and class names were renamed
    fall below git's default 50%. Added files are returned separately; the caller decides whether they are really
    new or just too different from their upstream file for git to notice.
    """
    out = git("diff", "--find-renames=30%", "--name-status", "-z", base, "HEAD", "--", *paths, root=root)
    parts = out.split("\0")
    modified, added, skipped = [], [], []
    i = 0
    while i < len(parts) and parts[i]:
        status = parts[i]
        if status[0] in "RC":
            new = parts[i + 2]
            i += 3
            if not new.endswith(extensions):
                continue
            if status[0] == "R" and status[1:] == "100":
                skipped.append((new, "pure rename, identical to upstream"))
            else:
                modified.append(new)
        else:
            path = parts[i + 1]
            i += 2
            if not path.endswith(extensions):
                continue
            if status[0] == "M":
                modified.append(path)
            elif status[0] == "A":
                added.append(path)
            # D: the file no longer exists, nothing to mark
    return modified, added, skipped


def is_excluded(path):
    return path.rsplit("/", 1)[-1] in EXCLUDED_NAMES or path.startswith(EXCLUDED_PREFIXES)


def changed_other_files(root, base, paths, code_extensions, pair_added):
    """Modified files of the types the first pass does not handle.

    Returns (modified paths as they are in HEAD, paired paths, skipped [(path, reason)]). Only status M and renames
    below 100% count. With pair_added, an added file is returned as paired if it sits in the same directory as a
    deleted file with the same extension (a META-INF/services file named after a renamed package) or has the name of
    a deleted file apart from Kilt -> Nytheria Compat (kilt-plugin.gradle.kts -> nytheria-compat-plugin.gradle.kts): it is the
    upstream file under a new name, changed by too much for git to notice.
    """
    out = git("diff", "--find-renames=30%", "--name-status", "-z", base, "HEAD", "--", *paths, root=root)
    parts = out.split("\0")
    modified, added, deleted, skipped = [], [], [], []
    i = 0
    while i < len(parts) and parts[i]:
        status = parts[i]
        if status[0] in "RC":
            path = parts[i + 2]
            i += 3
            if status[0] == "R" and status[1:] == "100":
                skipped.append((path, "pure rename, identical to upstream"))
            else:
                modified.append(path)
        else:
            path = parts[i + 1]
            i += 2
            {"M": modified, "A": added, "D": deleted}.get(status[0], []).append(path)

    def key(path):
        directory, _, name = path.rpartition("/")
        return directory, name[name.rfind("."):] if "." in name[1:] else ""

    deleted_keys = {key(d) for d in deleted}
    deleted_names = {d.rsplit("/", 1)[-1] for d in deleted}
    paired = [a for a in added if pair_added and (key(a) in deleted_keys or basename_matches(a, deleted_names))]

    def keep(path):
        return not path.endswith(code_extensions) and not is_excluded(path)

    return [p for p in modified if keep(p)], [p for p in paired if keep(p)], skipped


def upstream_basenames(root, base, paths, extensions):
    """File names of all source files in the upstream tree."""
    out = git("ls-tree", "-r", "--name-only", base, "--", *paths, root=root)
    return {line.rsplit("/", 1)[-1] for line in out.splitlines() if line.endswith(extensions)}


def basename_matches(path, upstream_names):
    """True if the file has the name of an upstream file, allowing for the Kilt -> Nytheria Compat rename of the name
    (and for the earlier Kilt -> Sporran one)."""
    name = path.rsplit("/", 1)[-1]
    candidates = {name}
    for ours, theirs in (("NytheriaCompat", "Kilt"), ("nytheria_compat", "kilt"), ("nytheria-compat", "kilt"),
                         ("Sporran", "Kilt"), ("sporran", "kilt")):
        candidates.add(name.replace(ours, theirs))
        if name.startswith(ours):
            candidates.add(theirs + name[len(ours):])
    return bool(candidates & upstream_names)


def creation_info(root, rev_range, path):
    """(date, short hash, subject) of the commit that created the file at its current path, or None."""
    out = git("log", "--diff-filter=A", "--format=%ad|%h|%s", "--date=short", rev_range, "--", path, root=root)
    lines = [l for l in out.splitlines() if l]
    if not lines:
        return None
    date, short, subject = lines[-1].split("|", 2)  # oldest
    return date, short, subject


def change_dates(root, rev_range, path, skip_grep):
    args = ["log", "--follow", "--format=%ad", "--date=short"]
    if skip_grep:
        args += ["--invert-grep", "--grep=" + skip_grep]
    args += [rev_range, "--", path]
    dates = sorted(set(git(*args, root=root).split()))
    return (dates[0], dates[-1]) if dates else None


def when_text(first, last):
    return "on %s" % first if first == last else "between %s and %s" % (first, last)


def notice_sentences(first, last, origin, licence):
    """The two sentences of the notice, shared by every comment syntax."""
    return ["%s %s; see docs/CHANGES.md." % (MARKER, when_text(first, last)),
            "Based on %s. Licensed under %s." % (origin, licence)]


def notice_text(first, last, origin, licence, eol):
    one, two = notice_sentences(first, last, origin, licence)
    return eol.join(["/*", " * " + one, " * " + two, " */"])


def detect_eol(text):
    crlf = text.count("\r\n")
    lf = text.count("\n") - crlf
    return "\r\n" if crlf > lf else "\n"


def leading_blocks(text, start):
    """Block comments at the top of the file, separated only by whitespace: [(start, end)]."""
    blocks = []
    pos = start
    while True:
        pos = WHITESPACE.match(text, pos).end()
        if not text.startswith("/*", pos) or text.startswith("/**", pos):  # javadoc is never a header
            break
        end = text.find("*/", pos + 2)
        if end < 0:
            break
        blocks.append((pos, end + 2))
        pos = end + 2
    return blocks


def apply_notice(text, notice_for):
    """Return the text with the notice added or refreshed. notice_for(eol) builds it for the file's line ending."""
    eol = detect_eol(text)
    notice = notice_for(eol)
    start = 1 if text.startswith("\ufeff") else 0
    tracked = TRACKED_HASH.match(text, start)
    if tracked:
        start = tracked.end()
    blocks = leading_blocks(text, start)

    # An existing notice is rewritten in place.
    for b_start, b_end in blocks:
        if MARKER in text[b_start:b_end]:
            return text[:b_start] + notice + text[b_end:]

    # Insert directly after an existing licence/copyright header (the licence blocks at the very top).
    header_end = None
    for b_start, b_end in blocks:
        if not LICENCE_WORDS.search(text[b_start:b_end]):
            break
        header_end = b_end
    if header_end is not None:
        rest = text[header_end:]
        # Whatever follows the header stays as it is; only make sure one blank line separates it from the notice.
        blank = BLANK_LINES.match(rest).group(0).count("\n")
        pad = eol * (2 - blank) if blank < 2 else ""
        return text[:header_end] + eol + eol + notice + pad + rest

    # Otherwise at the very top.
    return text[:start] + notice + eol + eol + text[start:]


FRONT_MATTER = re.compile(r"---[ \t]*\r?\n")
TWEAKER_HEADER = re.compile(r"(?:accessWidener|classTweaker)[ \t][^\r\n]*(?:\r\n|\n|$)")


def line_notice_regex(prefix, suffix):
    """An existing two-line notice: the 'Changed by' line directly followed by the 'Based on' line."""
    return re.compile("%s[^\\r\\n]*%s(?:\\r\\n|\\n)%s[^\\r\\n]*%s" % (
        re.escape(prefix + MARKER), re.escape(suffix), re.escape(prefix + "Based on "), re.escape(suffix)))


def apply_line_notice(text, sentences, prefix, suffix, front_matter):
    """Notice as two comment lines at the very top (after a BOM), followed by one blank line.

    A YAML front matter ("---" as the first line) stays the first line; the notice goes directly below it.
    """
    eol = detect_eol(text)
    notice = eol.join(prefix + s + suffix for s in sentences)
    start = 1 if text.startswith("\ufeff") else 0
    front = FRONT_MATTER.match(text, start) if front_matter else None
    pos = front.end() if front else start
    existing = line_notice_regex(prefix, suffix).match(text, pos)
    if existing:
        return text[:existing.start()] + notice + text[existing.end():]
    if front:
        return text[:pos] + notice + eol + text[pos:]
    return text[:pos] + notice + eol + eol + text[pos:]


def apply_hash_notice(text, sentences):
    return apply_line_notice(text, sentences, "# ", "", True)


def apply_html_notice(text, sentences):
    return apply_line_notice(text, sentences, "<!-- ", " -->", False)


def apply_tweaker_notice(text, sentences):
    """Notice as two "#" lines directly below the mandatory header line of an access widener / class tweaker."""
    eol = detect_eol(text)
    notice = eol.join("# " + s for s in sentences)
    start = 1 if text.startswith("\ufeff") else 0
    header = TWEAKER_HEADER.match(text, start)
    if not header:
        raise ValueError("no accessWidener/classTweaker header on the first line")
    pos = header.end()
    existing = line_notice_regex("# ", "").match(text, pos)
    if existing:
        return text[:existing.start()] + notice + text[existing.end():]
    head = text[:pos] if text[:pos].endswith("\n") else text[:pos] + eol
    rest = text[pos:]
    gap = "" if not rest or rest.startswith(("\n", "\r\n")) else eol
    return head + notice + eol + gap + rest


APPLIERS = {"hash": apply_hash_notice, "html": apply_html_notice, "tweaker": apply_tweaker_notice}


def style_of(path):
    """Comment syntax of a file type: block, hash, html or tweaker; None if the type has none."""
    name = path.rsplit("/", 1)[-1]
    if name.endswith((".kts", ".gradle")):
        return "block"
    if name.endswith((".properties", ".toml", ".yml", ".yaml")) or name == ".gitignore" \
            or "/META-INF/services/" in "/" + path:
        return "hash"
    if name.endswith((".accesswidener", ".classtweaker")):
        return "tweaker"
    if name.endswith(".md"):
        return "html"
    return None


def resolve_style(root, path):
    """(style, reason). style is None, with the reason, if the file can only be listed in the list file."""
    data = (root / path).read_bytes()
    if path.lower().endswith(BINARY_EXTENSIONS) or b"\0" in data[:8192]:
        return None, "binary"
    style = style_of(path)
    if path.startswith(LIST_ONLY_PREFIXES):
        return None, "merged and regenerated by ClassTweakerUpdater"
    if style is None:
        return None, "no comment syntax"
    if style == "tweaker" and not TWEAKER_HEADER.match(data.decode("utf-8", "surrogateescape").lstrip("\ufeff")):
        return None, "no access widener header on the first line"
    return style, ""


def process(root, name, path, first, last, dry_run, style="block"):
    origin, licence = TREES[name][3], TREES[name][4]
    file = root / path
    text = file.read_bytes().decode("utf-8", "surrogateescape")
    if style == "block":
        new = apply_notice(text, lambda eol: notice_text(first, last, origin, licence, eol))
    else:
        new = APPLIERS[style](text, notice_sentences(first, last, origin, licence))
    if new == text:
        return False
    if not dry_run:
        file.write_bytes(new.encode("utf-8", "surrogateescape"))
    return True


def changed_files_list(entries):
    """Content of docs/CHANGED-FILES.md. entries: {path: (first, last)}; deterministic, no timestamp."""
    lines = [
        "# Changed files without an in-file notice",
        "",
        "These files came from upstream and were changed by the Nytheria Compat project, but their format (JSON, mappings, "
        "images and similar) has no comment syntax, or, for the class tweakers in tweakers/, the build copies them "
        "into a generated file, so they cannot carry the change notice themselves. Each line "
        "below states that the file was changed by the Nytheria Compat project and when. They are based on Kilt 21.1.13 "
        "(https://github.com/KiltMC/Kilt) under the GNU LGPL 2.1; files under forge/, fml/ and loader/ are based on "
        "the origin given for them in VENDORED.md. What changed is described in CHANGES.md. This list is written "
        "by tools/licence/add-change-notices.py.",
        "",
    ]
    for path in sorted(entries):
        first, last = entries[path]
        lines.append("- `%s` - changed by the Nytheria Compat project %s" % (path, when_text(first, last)))
    return "\n".join(lines) + "\n"


def code_pass(root, name, paths, exts, kind, base, rev_range, args, dry):
    """Notices in the Java/Kotlin (and, for the vendored trees, Kotlin script) files. Returns the number to write."""
    modified, added, skipped = changed_files(root, base, paths, exts)
    by_name = []
    if kind == "kilt":
        names = upstream_basenames(root, base, paths, exts)
        by_name = [p for p in added if basename_matches(p, names)]
    new_files = [p for p in added if p not in by_name]
    modified = [p for p in modified + by_name if (root / p).is_file()]
    new_files = [p for p in new_files if (root / p).is_file()]

    def work(path):
        dates = change_dates(root, rev_range, path, args.skip_grep)
        if dates is None:
            return path, None, False
        if path in by_name:  # first change = the commit that created the file
            created = creation_info(root, rev_range, path)
            if created:
                dates = (min(created[0], dates[0]), dates[1])
        return path, dates, process(root, name, path, dates[0], dates[1], dry)

    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(work, modified))
        created = list(pool.map(lambda p: (p, creation_info(root, rev_range, p)), new_files))
    no_dates = [p for p, d, _ in results if d is None]
    changed = [p for p, d, c in results if c]
    print("%-7s modified upstream files: %4d (of which matched by file name only: %d), notices %s: %4d, "
          "Nytheria Compat-only files: %d%s" % (
              name, len(modified), len(by_name), "to write" if dry else "written", len(changed),
              len(new_files), ", no change date found: %d" % len(no_dates) if no_dates else ""))
    for p in no_dates:
        print("   WARNING no change date for " + p)
    if args.verbose:
        for p, d, c in results:
            print("   %s %s %s%s" % ("*" if c else " ", d, p, "  [matched by name]" if p in by_name else ""))
        for p, why in skipped:
            print("   skipped %s (%s)" % (p, why))
    if new_files:
        print("   Treated as Nytheria Compat-only (no upstream counterpart by content or file name):")
        for p, info in created:
            print("      %s  [%s]" % (p, "%s %s %s" % (info[0], info[1], info[2]) if info else "creating commit not found"))
    return len(changed)


def other_pass(root, name, paths, exts, kind, base, rev_range, args, dry, listed):
    """Notices in the other modified files (properties, YAML, Markdown, ...). Files that cannot hold a comment are
    added to `listed` instead. Returns the number of notices to write."""
    modified, paired, skipped = changed_other_files(root, base, paths, exts, kind == "kilt")
    candidates = [p for p in modified + paired if (root / p).is_file()]
    styles = {p: resolve_style(root, p) for p in candidates}

    def work(path):
        dates = change_dates(root, rev_range, path, args.skip_grep)
        if dates is None:
            return path, None, False
        if path in paired:  # first change = the commit that created the file
            created = creation_info(root, rev_range, path)
            if created:
                dates = (min(created[0], dates[0]), dates[1])
        style = styles[path][0]
        return path, dates, style is not None and process(root, name, path, dates[0], dates[1], dry, style)

    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(work, candidates))
    no_dates = [p for p, d, _ in results if d is None]
    changed = [p for p, d, c in results if c]
    in_list = [p for p, d, _ in results if d is not None and styles[p][0] is None]
    for p, d, _ in results:
        if p in in_list:
            listed[p] = d
    print("%-7s other modified files: %4d (renamed with other content: %d), notices %s: %4d, listed in %s: %d%s" % (
        name, len(candidates), len(paired), "to write" if dry else "written", len(changed), CHANGED_FILES_LIST,
        len(in_list), ", no change date found: %d" % len(no_dates) if no_dates else ""))
    for p in no_dates:
        print("   WARNING no change date for " + p)
    if args.verbose:
        for p, d, c in results:
            style, reason = styles[p]
            print("   %s %s %s  [%s%s]" % ("*" if c else " ", d, p, style or "listed: " + reason,
                                           ", renamed with other content" if p in paired else ""))
        for p, why in skipped:
            print("   skipped %s (%s)" % (p, why))
    return len(changed)


def main():
    ap = argparse.ArgumentParser(description="Add or refresh the Nytheria Compat change notice in modified upstream files.")
    ap.add_argument("--dry-run", action="store_true", help="report what would change, write nothing")
    ap.add_argument("--check", action="store_true", help="like --dry-run, exit status 1 if anything is out of date")
    ap.add_argument("--kilt-base", default="v21.1.13", help="upstream Kilt tag or commit (default v21.1.13)")
    ap.add_argument("--vendor-base", help="vendoring commit for forge/, fml/, loader/ (default: found with git log)")
    ap.add_argument("--skip-grep", default="change notices",
                    help="ignore commits whose message contains this text when computing dates")
    ap.add_argument("--verbose", action="store_true", help="list every file")
    args = ap.parse_args()

    root = Path(git("rev-parse", "--show-toplevel", root=Path(__file__).resolve().parent).strip())
    dry = args.dry_run or args.check
    vendor_base = args.vendor_base or find_vendoring_commit(root)
    bases = {"kilt": args.kilt_base, "vendor": vendor_base}
    print("Kilt base: %s, vendoring commit: %s" % (args.kilt_base, vendor_base[:10]))

    total_changed = 0
    listed = {}  # path -> (first, last) of the modified files that cannot hold a notice
    for name, (paths, exts, kind, _origin, _licence) in TREES.items():
        base = bases[kind]
        rev_range = "%s..HEAD" % base
        if exts:
            total_changed += code_pass(root, name, paths, exts, kind, base, rev_range, args, dry)
        total_changed += other_pass(root, name, paths, exts, kind, base, rev_range, args, dry, listed)

    # The list of files that cannot hold a notice.
    list_path = root / CHANGED_FILES_LIST
    if listed or list_path.is_file():
        expected = changed_files_list(listed).encode("utf-8")
        current = list_path.read_bytes() if list_path.is_file() else None
        stale = current != expected
        print("%s: %d file(s) listed, %s" % (CHANGED_FILES_LIST, len(listed),
                                              ("to write" if dry else "written") if stale else "up to date"))
        if stale:
            total_changed += 1
            if not dry:
                list_path.parent.mkdir(parents=True, exist_ok=True)
                list_path.write_bytes(expected)

    print("Notices and list %s: %d" % ("to write" if dry else "written", total_changed))
    if args.check and total_changed:
        print("%d file(s) need a notice or a date refresh." % total_changed)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
