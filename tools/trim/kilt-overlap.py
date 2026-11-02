#!/usr/bin/env python3
"""How much of Nytheria Compat's code is still identical to its bases, per area, at a git ref.

    py -3 tools/trim/kilt-overlap.py [ref]      (default HEAD; reads git objects, so commit first)

Bases: src/ and compat/ against Kilt at cd706a5 (src + compat), forge/ fml/ loader/ against the vendoring commit ae0303389.
Both sides map Kilt names to Nytheria Compat names first. Comments, imports and package lines are dropped.

Columns:
  lines   code lines of at least 30 characters (after stripping)
  same%   share of those lines identical to a line anywhere in the base. The target number.
  forced% share of lines that are identical AND mixin skeleton, in injects, injections and mixin: annotations and @At targets,
          @Shadow members, class heads, method heads (a handler's parameters are the target's, an extension method's whole
          head is the API a mod links to). A rewrite can only lower this by dropping the hook or member itself.
  free%   same% minus forced%: identical code that is ours to change.
  shape%  share of 12-token runs found in the base, with local names and nytheriacompat$ names blanked. Line wrapping and renames
          do not move it, so same% falling while shape% stays put means reformatting, not rewriting.
  kept    identical lines, absolute. same% is a ratio: deleting new code raises it, this column shows the real amount.

A line that occurs more often here than in the base counts by b/h per occurrence, so areas add up to the total and do not
depend on file order. Each area is compared with the whole base, so moving a line to another package does not hide it.
"""
import collections
import re
import subprocess
import sys

SMALL = ('api', 'interop', 'processor', 'remaps', 'server')
KILT_BASE = 'cd706a5'
VENDOR_BASE = 'ae0303389'
MIN_LEN = 30
SHINGLE = 12

RENAMES = (('xyz.bluspring.kilt', 'dev.nytheria.compat'), ('xyz/bluspring/kilt', 'dev/nytheria/compat'), ('kilt$', 'nytheriacompat$'),
           ('KILT', 'NYTHERIACOMPAT'), ('Kilt', 'Nytheria Compat'), ('kilt', 'nytheria_compat'))
CODE = re.compile(r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])\'|/\*.*?\*/|//[^\n]*', re.S)
SKIP = re.compile(r'^(import |package |@file:)')
TOKEN = re.compile(r'[A-Za-z_$][\w$]*|\d[\w.]*|"(?:\\.|[^"\\\n])*"|\S')
KEYWORDS = frozenset('''abstract boolean break byte case catch char class continue default do double else enum extends final
    finally float for fun if implements import in instanceof int interface is long new null object override package private
    protected public return short static super switch this throw true false try val var void when while'''.split())

ANNOTATIONS = ('Mixin', 'Pseudo', 'Inject', 'ModifyExpressionValue', 'ModifyReturnValue', 'ModifyReceiver', 'ModifyArg',
               'ModifyArgs', 'ModifyVariable', 'ModifyConstant', 'WrapOperation', 'WrapWithCondition', 'WrapMethod', 'Redirect',
               'Overwrite', 'Shadow', 'Final', 'Unique', 'Mutable', 'Dynamic', 'At', 'Slice', 'Local', 'Share', 'Accessor',
               'Invoker', 'Definition', 'Expression', 'Desc', 'Coerce')
FORCED = re.compile(r'^(@(' + '|'.join(ANNOTATIONS) + r')\b|(method|target|at|slice|from|to|value|ordinal|shift|by|id|desc) *= *)')
MEMBER_AFTER = re.compile(r'^@(Shadow|Final|Mutable|Accessor|Invoker)\b(\([^)]*\))?$')
HEAD = re.compile(r'^((public|private|protected|default|static|final|abstract|synchronized) )+[^=]*\(|^(public |abstract |final |static )*(class|interface|enum|record) ')
BODY_START = re.compile(r'^(return|throw|new|else|if|for|while|switch|try|var|this|super)\b')
INTERFACE_HEAD = re.compile(r'^[\w.<>\[\], ?]+ \w+\(.*\) *(throws [\w., ]+)?[{;]$')
SKELETON_AREAS = ('/injects/', '/injections/', '/mixin/')


def tree(ref, tops):
    out = subprocess.run(['git', 'ls-tree', '-r', ref, '--', *tops], capture_output=True, check=True).stdout.decode('utf8')
    for line in out.splitlines():
        meta, path = line.split('\t', 1)
        _, kind, sha = meta.split()
        if kind == 'blob' and path.endswith(('.java', '.kt')):
            yield sha, path


def read(items):
    items = list(items)
    run = subprocess.run(['git', 'cat-file', '--batch'], input='\n'.join(s for s, _ in items).encode() + b'\n', capture_output=True, check=True)
    out, at = run.stdout, 0
    for _, path in items:
        end = out.index(b'\n', at)
        size = int(out[at:end].split()[2])
        yield path, out[end + 1:end + 1 + size].decode('utf8', 'replace')
        at = end + 1 + size + 1


def rename(text):
    for old, new in RENAMES:
        text = text.replace(old, new)
    return text


def code_lines(text):
    """The file's stripped lines with comments gone, imports and package lines dropped, whitespace runs collapsed."""
    text = CODE.sub(lambda m: m[0] if m[0][0] in '"\'' else '\n' * m[0].count('\n'), text)
    return [ln for ln in (' '.join(raw.split()) for raw in text.splitlines()) if ln and not SKIP.match(ln)]


def forced_flags(lines, path):
    """For each line: is it mixin skeleton, so that only dropping the hook or member would change it?"""
    if not any(a in path for a in SKELETON_AREAS):
        return [False] * len(lines)
    flags, after_member = [], False
    for ln in lines:
        flag = after_member or bool(FORCED.match(ln)) or bool(HEAD.match(ln))
        if '/injections/' in path and INTERFACE_HEAD.match(ln) and not BODY_START.match(ln):
            flag = True
        after_member = bool(MEMBER_AFTER.match(ln))
        flags.append(flag)
    return flags


def shingles(lines):
    tokens = TOKEN.findall(' '.join(lines))
    blank = []
    for i, tok in enumerate(tokens):
        name = tok[0].isalpha() or tok[0] in '_$'
        call = i + 1 < len(tokens) and tokens[i + 1] == '('
        local = name and tok not in KEYWORDS and (tok.startswith('nytheriacompat$') or (tok[0].islower() and not call))
        blank.append('v' if local else tok)
    return {hash(tuple(blank[i:i + SHINGLE])) for i in range(len(blank) - SHINGLE + 1)}


def area(path):
    if not path.startswith('src/'):
        top = path.split('/')[0]
        return 'compat' if top == 'compat' else top
    if path.startswith('src/test/'):
        return 'src/test'
    java = path.startswith('src/main/java/dev/nytheria/compat/')
    kotlin = path.startswith('src/main/kotlin/dev/nytheria/compat/')
    parts = path.split('/')[5:]
    if (not java and not kotlin) or (parts[0] in SMALL) or len(parts) == 1 and java:
        return 'other main (cpw, shims, small)'
    if not java:
        return 'kt ' + (parts[0] if len(parts) > 1 else 'top (NytheriaCompat.kt)')
    if parts[0] != 'injects':
        return parts[0]
    if parts[1] == 'world' and len(parts) > 3:
        return 'injects world/' + (parts[2] if parts[2] in ('entity', 'item', 'level') else 'other')
    return 'injects ' + (parts[1] if parts[1] in ('client', 'server', 'network') else 'other')


def base_pool(ref, tops):
    lines, shapes = collections.Counter(), set()
    for path, text in read(tree(ref, tops)):
        kept = code_lines(rename(text))
        lines.update(ln for ln in kept if len(ln) >= MIN_LEN)
        shapes |= shingles(kept)
    return lines, shapes


def main():
    ref = sys.argv[1] if len(sys.argv) > 1 else 'HEAD'
    pools = {'kilt': base_pool(KILT_BASE, ['src', 'compat']), 'vendor': base_pool(VENDOR_BASE, ['forge', 'fml', 'loader'])}
    files = []
    for path, text in read(tree(ref, ['src', 'compat', 'forge', 'fml', 'loader'])):
        lines = code_lines(rename(text))
        files.append((path, lines, forced_flags(lines, path)))
    head = {'kilt': collections.Counter(), 'vendor': collections.Counter()}
    for path, lines, _ in files:
        head['kilt' if path.startswith(('src/', 'compat/')) else 'vendor'].update(ln for ln in lines if len(ln) >= MIN_LEN)

    rows = collections.defaultdict(lambda: collections.Counter())
    for path, lines, flags in files:
        pool = 'kilt' if path.startswith(('src/', 'compat/')) else 'vendor'
        base_lines, base_shapes = pools[pool]
        row = rows[area(path)]
        row['files'] += 1
        for ln, forced in zip(lines, flags):
            if len(ln) < MIN_LEN:
                continue
            share = min(1.0, base_lines[ln] / head[pool][ln])
            row['lines'] += 1
            row['same'] += share
            row['forced'] += share if forced else 0
        shapes = shingles(lines)
        row['shapes'] += len(shapes)
        row['shapes_same'] += len(shapes & base_shapes)

    def show(name, row):
        n = max(1, row['lines'])
        print(f"{name:30}{row['files']:6}{row['lines']:7}{100 * row['same'] / n:7.0f}{100 * row['forced'] / n:8.0f}"
              f"{100 * (row['same'] - row['forced']) / n:6.0f}{100 * row['shapes_same'] / max(1, row['shapes']):7.0f}{row['same']:7.0f}")

    print(f"{'area @ ' + ref[:20]:30}{'files':>6}{'lines':>7}{'same%':>7}{'forced%':>8}{'free%':>6}{'shape%':>7}{'kept':>7}")
    main_total = sum((rows[a] for a in rows if a not in ('src/test', 'compat', 'forge', 'fml', 'loader')), collections.Counter())
    for name in sorted(rows, key=lambda a: (a in ('src/test', 'compat', 'forge', 'fml', 'loader'), a)):
        show(name, rows[name])
    show('TOTAL src/main', main_total)


if __name__ == '__main__':
    main()
