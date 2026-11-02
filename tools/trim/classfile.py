"""Minimal Java class-file parser for tools/trim/reachability.py (stdlib only, dev tool, not shipped).

parse_class(bytes) -> ClassInfo with: names, access flags, annotations, fields, methods, and for every method the
references found by walking its bytecode (calls, field accesses, class constants, strings). Everything the analysis
needs and nothing more.
"""
import re
import struct

ACC_INTERFACE = 0x0200
ACC_ABSTRACT = 0x0400
ACC_STATIC = 0x0008
ACC_PRIVATE = 0x0002
ACC_SYNTHETIC = 0x1000

# operand bytes per opcode; -1 = variable (tableswitch/lookupswitch/wide handled in the walker)
_OPLEN = [0] * 256
for _op in (0x10, 0x12, 0xbc, 0xa9) + tuple(range(0x15, 0x1a)) + tuple(range(0x36, 0x3b)):
    _OPLEN[_op] = 1
for _op in (0x11, 0x13, 0x14, 0x84, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1, 0xc6, 0xc7) + tuple(range(0x99, 0xa9)):
    _OPLEN[_op] = 2
_OPLEN[0xc5] = 3
for _op in (0xb9, 0xba, 0xc8, 0xc9):
    _OPLEN[_op] = 4
_OPLEN[0xaa] = _OPLEN[0xab] = _OPLEN[0xc4] = -1

_KIND = {0xb6: "v", 0xb7: "p", 0xb8: "s", 0xb9: "i"}
_DESC_CLASS = re.compile(r"(?<![\w/$])L([\w/$]+)(?=[;<])")


def desc_classes(desc):
    """Internal class names mentioned in a descriptor or generic signature."""
    return _DESC_CLASS.findall(desc)


class MethodInfo:
    __slots__ = ("name", "desc", "access", "annots", "calls", "fields", "classes", "strings", "lines", "signature")

    def __init__(self, name, desc, access):
        self.name, self.desc, self.access = name, desc, access
        self.annots = []      # [(type_desc, {elem: value})]
        self.calls = []       # [(kind, owner, name, desc)]  kind: v virtual, i interface, s static, p special, h method handle, d indy site
        self.fields = []      # [(kind, owner, name, desc)]  kind: g get, p put, gs getstatic, ps putstatic
        self.classes = set()  # class constants (new, checkcast, instanceof, ldc, anewarray, ...)
        self.strings = set()  # ldc String
        self.lines = None     # (min, max) of LineNumberTable
        self.signature = None


class ClassInfo:
    __slots__ = ("name", "super", "interfaces", "access", "source", "fields", "methods", "annots", "inner", "nest_host",
                 "refs", "strings", "annot_strings", "signature", "path")

    def __init__(self):
        self.annots = []
        self.fields = []     # [(name, desc, access, annots)]
        self.methods = []
        self.inner = []      # [(inner, outer, simple, flags)]
        self.refs = set()    # every internal class name mentioned structurally
        self.strings = set() # ldc strings in all methods
        self.annot_strings = set()
        self.nest_host = None
        self.signature = None
        self.path = None

    @property
    def is_interface(self):
        return bool(self.access & ACC_INTERFACE)

    def annot(self, desc):
        for t, v in self.annots:
            if t == desc:
                return v
        return None


class _R:
    __slots__ = ("b", "p")

    def __init__(self, b, p=0):
        self.b, self.p = b, p

    def u1(self):
        v = self.b[self.p]
        self.p += 1
        return v

    def u2(self):
        v = (self.b[self.p] << 8) | self.b[self.p + 1]
        self.p += 2
        return v

    def u4(self):
        v = struct.unpack_from(">I", self.b, self.p)[0]
        self.p += 4
        return v


def _parse_annotation(r, cp, sink):
    t = cp[r.u2()]
    vals = {}
    for _ in range(r.u2()):
        n = cp[r.u2()]
        vals[n] = _parse_value(r, cp, sink)
    sink.add(t)
    return (t, vals)


def _parse_value(r, cp, sink):
    tag = chr(r.u1())
    if tag in "BCDFIJSZ":
        r.u2()
        return None
    if tag == "s":
        return cp[r.u2()]
    if tag == "e":
        t = cp[r.u2()]
        n = cp[r.u2()]
        sink.add(t)
        return ("enum", t, n)
    if tag == "c":
        d = cp[r.u2()]
        sink.add(d)
        return ("class", d)
    if tag == "@":
        return _parse_annotation(r, cp, sink)
    if tag == "[":
        return [_parse_value(r, cp, sink) for _ in range(r.u2())]
    raise ValueError("bad annotation tag " + tag)


def _walk_strings(v, out):
    if isinstance(v, str):
        out.add(v)
    elif isinstance(v, list):
        for x in v:
            _walk_strings(x, out)
    elif isinstance(v, tuple) and len(v) == 2 and isinstance(v[1], dict):
        for x in v[1].values():
            _walk_strings(x, out)


def _parse_annots(r, cp, sink, strings):
    out = []
    for _ in range(r.u2()):
        a = _parse_annotation(r, cp, sink)
        if a[0] != "Lkotlin/Metadata;":
            _walk_strings(a, strings)
        out.append(a)
    return out


def parse_class(data):
    r = _R(data)
    if r.u4() != 0xCAFEBABE:
        raise ValueError("not a class file")
    r.u2(); r.u2()
    n = r.u2()
    raw = [None] * n
    i = 1
    while i < n:
        tag = r.u1()
        if tag == 1:
            ln = r.u2()
            raw[i] = (1, data[r.p:r.p + ln].decode("utf-8", "replace"))
            r.p += ln
        elif tag in (3, 4):
            r.p += 4
        elif tag in (5, 6):
            r.p += 8
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            raw[i] = (tag, r.u2())
        elif tag in (9, 10, 11, 12, 17, 18):
            raw[i] = (tag, r.u2(), r.u2())
        elif tag == 15:
            raw[i] = (15, r.u1(), r.u2())
        else:
            raise ValueError("bad cp tag %d" % tag)
        i += 1

    def utf(ix):
        return raw[ix][1]

    cp = [None] * n  # utf8 strings only, for annotation parsing
    for i in range(1, n):
        if raw[i] and raw[i][0] == 1:
            cp[i] = raw[i][1]

    def cname(ix):
        return utf(raw[ix][1])

    def nat(ix):
        e = raw[ix]
        return utf(e[1]), utf(e[2])

    def mref(ix):
        e = raw[ix]
        nm, ds = nat(e[2])
        return cname(e[1]), nm, ds

    ci = ClassInfo()
    ci.access = r.u2()
    ci.name = cname(r.u2())
    s = r.u2()
    ci.super = cname(s) if s else None
    ci.interfaces = [cname(r.u2()) for _ in range(r.u2())]
    refs = ci.refs
    for e in raw:
        if not e:
            continue
        t = e[0]
        if t == 7:
            nm = utf(e[1])
            if nm.startswith("["):
                refs.update(desc_classes(nm))
            else:
                refs.add(nm)
        elif t == 12:
            refs.update(desc_classes(utf(e[2])))
        elif t == 16:
            refs.update(desc_classes(utf(e[1])))
    annot_types = set()
    bootstrap = []
    ci.source = None

    def read_member_attrs(owner_is_method, mi_or_none, annots_out):
        for _ in range(r.u2()):
            an = utf(r.u2())
            ln = r.u4()
            end = r.p + ln
            if an in ("RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations"):
                annots_out.extend(_parse_annots(r, cp, annot_types, ci.annot_strings))
            elif an == "Signature":
                sg = utf(r.u2())
                refs.update(desc_classes(sg))
                if mi_or_none is not None:
                    mi_or_none.signature = sg
            elif an == "Code" and mi_or_none is not None:
                _parse_code(r, mi_or_none, raw, utf, cname, mref)
            elif an in ("RuntimeVisibleParameterAnnotations", "RuntimeInvisibleParameterAnnotations"):
                for _p in range(r.u1()):
                    _parse_annots(r, cp, annot_types, ci.annot_strings)
            r.p = end

    for _ in range(r.u2()):
        acc = r.u2()
        nm = utf(r.u2())
        ds = utf(r.u2())
        refs.update(desc_classes(ds))
        an = []
        read_member_attrs(False, None, an)
        ci.fields.append((nm, ds, acc, an))
    for _ in range(r.u2()):
        acc = r.u2()
        nm = utf(r.u2())
        ds = utf(r.u2())
        refs.update(desc_classes(ds))
        m = MethodInfo(nm, ds, acc)
        read_member_attrs(True, m, m.annots)
        ci.methods.append(m)
    for _ in range(r.u2()):
        an = utf(r.u2())
        ln = r.u4()
        end = r.p + ln
        if an == "SourceFile":
            ci.source = utf(r.u2())
        elif an in ("RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations"):
            ci.annots.extend(_parse_annots(r, cp, annot_types, ci.annot_strings))
        elif an == "Signature":
            ci.signature = utf(r.u2())
            refs.update(desc_classes(ci.signature))
        elif an == "InnerClasses":
            for _i in range(r.u2()):
                a, b, c, f = r.u2(), r.u2(), r.u2(), r.u2()
                ci.inner.append((cname(a), cname(b) if b else None, utf(c) if c else None, f))
        elif an == "NestHost":
            ci.nest_host = cname(r.u2())
        elif an == "BootstrapMethods":
            bootstrap = [(r.u2(), [r.u2() for _a in range(r.u2())]) for _b in range(r.u2())]
        r.p = end
    _resolve_indy(ci, bootstrap, raw, utf, cname, mref)
    for t in annot_types:
        refs.update(desc_classes(t))
    for t, v in ci.annots:
        refs.update(desc_classes(t))
    for m in ci.methods:
        ci.strings |= m.strings
        refs |= m.classes
    return ci


def _parse_code(r, mi, raw, utf, cname, mref):
    r.u2(); r.u2()
    codelen = r.u4()
    code = r.b[r.p:r.p + codelen]
    r.p += codelen
    mi_calls, mi_fields, mi_classes, mi_strings = mi.calls, mi.fields, mi.classes, mi.strings
    i = 0
    L = codelen
    while i < L:
        op = code[i]
        if op == 0xb6 or op == 0xb7 or op == 0xb8 or op == 0xb9:
            ix = (code[i + 1] << 8) | code[i + 2]
            o, nm, ds = mref(ix)
            mi_calls.append((_KIND[op], o, nm, ds))
            i += 5 if op == 0xb9 else 3
        elif op == 0xba:
            ix = (code[i + 1] << 8) | code[i + 2]
            e = raw[ix]
            nm, ds = (utf(raw[e[2]][1]), utf(raw[e[2]][2]))
            mi_calls.append(("d", "", nm, ds, e[1]))
            i += 5
        elif 0xb2 <= op <= 0xb5:
            ix = (code[i + 1] << 8) | code[i + 2]
            o, nm, ds = mref(ix)
            mi_fields.append((("gs", "ps", "g", "p")[op - 0xb2], o, nm, ds))
            mi_classes.add(o)
            i += 3
        elif op in (0xbb, 0xbd, 0xc0, 0xc1, 0xc5):
            ix = (code[i + 1] << 8) | code[i + 2]
            nm = cname(ix)
            if nm.startswith("["):
                mi_classes.update(desc_classes(nm))
            else:
                mi_classes.add(nm)
            i += 3 if op != 0xc5 else 4
        elif op == 0x12 or op == 0x13:
            ix = code[i + 1] if op == 0x12 else (code[i + 1] << 8) | code[i + 2]
            e = raw[ix]
            if e:
                if e[0] == 8:
                    mi_strings.add(utf(e[1]))
                elif e[0] == 7:
                    nm = utf(e[1])
                    if nm.startswith("["):
                        mi_classes.update(desc_classes(nm))
                    else:
                        mi_classes.add(nm)
            i += 2 if op == 0x12 else 3
        elif op == 0xaa:
            i = (i + 4) & ~3
            lo = struct.unpack_from(">i", code, i + 4)[0]
            hi = struct.unpack_from(">i", code, i + 8)[0]
            i += 12 + 4 * (hi - lo + 1)
        elif op == 0xab:
            i = (i + 4) & ~3
            npairs = struct.unpack_from(">i", code, i + 4)[0]
            i += 8 + 8 * npairs
        elif op == 0xc4:
            i += 6 if code[i + 1] == 0x84 else 4
        else:
            i += 1 + _OPLEN[op]
    for _ in range(r.u2()):  # exception table
        t = r.b[r.p + 6] << 8 | r.b[r.p + 7]
        if t:
            mi_classes.add(cname(t))
        r.p += 8
    for _ in range(r.u2()):
        an = utf(r.u2())
        ln = r.u4()
        e2 = r.p + ln
        if an == "LineNumberTable":
            ls = [(r.b[r.p + 2 + 4 * k + 2] << 8) | r.b[r.p + 2 + 4 * k + 3] for k in range((r.b[r.p] << 8 | r.b[r.p + 1]))]
            if ls:
                mi.lines = (min(ls), max(ls))
        r.p = e2


def _resolve_indy(ci, bms, raw, utf, cname, mref):
    """Attach bootstrap method handle / type arguments of invokedynamic sites as 'h' calls and class refs."""
    handles = []
    for _mh, args in bms:
        hs = []
        for a in args:
            e = raw[a]
            if e[0] == 15:
                hs.append(("h",) + mref(e[2]))
            elif e[0] == 16:
                ci.refs.update(desc_classes(utf(e[1])))
            elif e[0] == 7:
                ci.refs.add(cname(a))
        handles.append(hs)
    for m in ci.methods:
        if not any(c[0] == "d" for c in m.calls):
            continue
        out = []
        for c in m.calls:
            if c[0] == "d":
                out.extend(handles[c[4]])
                ci.refs.update(desc_classes(c[3]))
                out.append(c[:4])
            else:
                out.append(c)
        m.calls = out
