/*
 * ConnectionViewer HTML - parsers for the ConnectionViewer file format.
 *
 * See ../../FORMAT.md for the format description. This file is DOM free so it
 * can also be used from node (see test/smoke.js).
 */
var CV = CV || {};

(function (CV) {
"use strict";

/** split into lines, drop trailing whitespace, keep empty lines out of the way */
function splitLines(text) {
	return text.replace(/\r\n?/g, "\n").split("\n");
}

function tokens(line) {
	return line.trim().split(/\s+/);
}

/**
 * "nan"/"-nan" and everything unparsable becomes NaN, like SubMatrix.MyParseDouble.
 */
function parseDouble(s) {
	if (s === undefined) return NaN;
	var d = parseFloat(s);
	return isFinite(d) ? d : NaN;
}
CV.parseDouble = parseDouble;

/**
 * Parse a .mat or .vec file (format version 1).
 *
 * @param text     file content
 * @param isVec    true for .vec/.pvec data
 * @returns {{dim, n, pos, rows, markRefs, valueRefs, maxBlockDim}}
 */
CV.parseMat = function (text, isVec) {
	var lines = splitLines(text);
	var ln = 0;

	function nextLine() {
		while (ln < lines.length) {
			var l = lines[ln++];
			if (l.trim().length > 0) return l;
		}
		return null;
	}

	var l = nextLine();
	if (l === null) throw new Error("empty file");
	var version = parseInt(l, 10);
	if (version !== 1) throw new Error("unsupported ConnectionViewerVersion " + l);

	var dim = parseInt(nextLine(), 10);
	if (dim !== 2 && dim !== 3) throw new Error("unsupported number of dimensions " + dim);

	var n = parseInt(nextLine(), 10);
	if (!(n >= 0)) throw new Error("bad number of nodes");

	// positions
	var pos = new Array(n);
	for (var i = 0; i < n; i++) {
		l = nextLine();
		if (l === null) throw new Error("unexpected end in positions (node " + i + ")");
		var t = tokens(l);
		pos[i] = {
			x: parseDouble(t[0]),
			y: parseDouble(t[1]),
			z: dim === 3 ? parseDouble(t[2]) : 0
		};
	}

	// the legacy "ShowInWindow" line
	l = nextLine();
	if (l === null) throw new Error("unexpected end after positions");

	// connections
	var rows = new Array(n);
	for (i = 0; i < n; i++) rows[i] = [];

	var markRefs = [], valueRefs = [];
	var maxBlockDim = 0;

	while ((l = nextLine()) !== null) {
		var tk = tokens(l);
		if (tk[0] === "c" || tk[0] === "v") {
			// marks/values sections: "c file" / "v file" until end of file
			do {
				tk = tokens(l);
				if (tk.length >= 2) {
					if (tk[0] === "c") markRefs.push(tk[1]);
					else if (tk[0] === "v") valueRefs.push(tk[1]);
				}
			} while ((l = nextLine()) !== null);
			break;
		}

		var from = parseInt(tk[0], 10);
		var to = parseInt(tk[1], 10);
		if (!(from >= 0 && from < n)) throw new Error("connection from index out of range: " + l);
		if (!(to >= 0 && to < n)) throw new Error("connection to index out of range: " + l);

		var value = tk.length > 2 ? tk.slice(2).join(" ") : "";
		if (value.charAt(0) === "[") {
			var d = 1;
			for (var k = 0; k < value.length; k++) if (value.charAt(k) === "|") d++;
			if (d > maxBlockDim) maxBlockDim = d;
		}
		rows[from].push({ to: to, value: value, m: undefined, str: null, d: undefined });
	}

	return {
		version: version,
		dim: dim,
		n: n,
		pos: pos,
		rows: rows,
		markRefs: markRefs,
		valueRefs: valueRefs,
		isVec: !!isVec,
		maxBlockDim: maxBlockDim
	};
};

/**
 * Parse a .marks file:
 *   red green blue alpha visibleSize
 *   index...
 */
CV.parseMarks = function (text, name, n) {
	var lines = splitLines(text).filter(function (s) { return s.trim().length > 0; });
	if (lines.length === 0) return null;
	var t = tokens(lines[0]);
	var marks = new Uint8Array(n);
	for (var i = 1; i < lines.length; i++) {
		var idx = parseInt(lines[i], 10);
		if (idx >= 0 && idx < n) marks[idx] = 1;
	}
	return {
		name: name,
		red: parseDouble(t[0]),
		green: parseDouble(t[1]),
		blue: parseDouble(t[2]),
		alpha: t.length > 3 ? parseDouble(t[3]) : 1,
		size: t.length > 4 ? parseInt(t[4], 10) : 0,
		marks: marks
	};
};

/**
 * Parse a value file referenced by a "v file" line:
 *   index valueString
 */
CV.parseValues = function (text, name, n) {
	var lines = splitLines(text);
	var vs = new Array(n);
	for (var i = 0; i < n; i++) vs[i] = "";
	for (i = 0; i < lines.length; i++) {
		if (lines[i].trim().length === 0) continue;
		var t = tokens(lines[i]);
		var j = parseInt(t[0], 10);
		if (j >= 0 && j < n) vs[j] = t.slice(1).join(" ");
	}
	return { name: name, vs: vs };
};

/**
 * Parse a .indices file:
 *   <ignored> numFct / function names / one function index per node
 */
CV.parseIndices = function (text, n) {
	var lines = splitLines(text).filter(function (s) { return s.trim().length > 0; });
	if (lines.length === 0) return null;
	var t = tokens(lines[0]);
	if (t.length !== 2) return null;
	var numFct = parseInt(t[1], 10);
	if (!(numFct > 0) || lines.length < 1 + numFct + n) return null;
	var names = [];
	for (var i = 0; i < numFct; i++) names.push(lines[1 + i].trim());
	var fctIndex = new Int32Array(n);
	for (i = 0; i < n; i++) fctIndex[i] = parseInt(lines[1 + numFct + i], 10);
	return { numFct: numFct, names: names, fctIndex: fctIndex };
};

/**
 * Parse a .pmat/.pvec file: number of files, then one file name per line.
 */
CV.parsePmat = function (text) {
	var lines = splitLines(text).filter(function (s) { return s.trim().length > 0; });
	if (lines.length === 0) throw new Error("empty parallel file");
	var count = parseInt(lines[0], 10);
	if (!(count >= 0)) throw new Error("bad number of parallel files");
	var names = [];
	for (var i = 0; i < count && i + 1 < lines.length; i++) names.push(lines[i + 1].trim());
	return names;
};

/**
 * Minimal (ustar) tar reader for .tarmat files.
 * @returns Map name -> Uint8Array
 */
CV.parseTar = function (buffer) {
	var data = new Uint8Array(buffer);
	var out = new Map();
	var off = 0;

	function str(start, len) {
		var s = "";
		for (var i = start; i < start + len; i++) {
			var c = data[i];
			if (c === 0) break;
			s += String.fromCharCode(c);
		}
		return s;
	}

	while (off + 512 <= data.length) {
		var name = str(off, 100).trim();
		if (name.length === 0) break;                    // end of archive
		var prefix = str(off + 345, 155).trim();
		if (prefix.length > 0) name = prefix + "/" + name;
		var size = parseInt(str(off + 124, 12).trim(), 8) || 0;
		var type = String.fromCharCode(data[off + 156] || 48);
		off += 512;
		if (type === "0" || type === "\0" || type === "48") {
			out.set(name, data.subarray(off, off + size));
		}
		off += Math.ceil(size / 512) * 512;
	}
	return out;
};

/** file name helpers, cf. FileUtil.java */
CV.baseName = function (path) {
	var i = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
	return i === -1 ? path : path.substring(i + 1);
};

CV.dirName = function (path) {
	var i = path.lastIndexOf("/");
	return i === -1 ? "" : path.substring(0, i + 1);
};

CV.extension = function (path) {
	var i = path.lastIndexOf(".");
	return i === -1 ? "" : path.substring(i + 1).toLowerCase();
};

if (typeof module !== "undefined" && module.exports) module.exports = CV;

})(CV);
