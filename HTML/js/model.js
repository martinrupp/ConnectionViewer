/*
 * ConnectionViewer HTML - data model.
 *
 * CV.SubMatrix mirrors SubMatrix.java (one loaded .mat/.vec file),
 * CV.Viewer mirrors the view state of ConnectionViewerPanel.java
 * (global bounds, zoom, translation, rotation, options).
 *
 * DOM free, so it can be used from node as well.
 */
var CV = CV || {};

(function (CV) {
"use strict";

// ------------------------------------------------------------------ bounds

function Bounds() {
	this.empty = true;
	this.xmin = 0; this.xmax = 0;
	this.ymin = 0; this.ymax = 0;
	this.zmin = 0; this.zmax = 0;
}

Bounds.prototype.add = function (x, y, z) {
	if (this.empty) {
		this.empty = false;
		this.xmin = this.xmax = x;
		this.ymin = this.ymax = y;
		this.zmin = this.zmax = z;
		return;
	}
	if (x < this.xmin) this.xmin = x; else if (x > this.xmax) this.xmax = x;
	if (y < this.ymin) this.ymin = y; else if (y > this.ymax) this.ymax = y;
	if (z < this.zmin) this.zmin = z; else if (z > this.zmax) this.zmax = z;
};

Bounds.prototype.addBounds = function (b) {
	if (b.empty) return;
	this.add(b.xmin, b.ymin, b.zmin);
	this.add(b.xmax, b.ymax, b.zmax);
};

Bounds.prototype.width = function () { return this.xmax - this.xmin; };
Bounds.prototype.height = function () { return this.ymax - this.ymin; };
Bounds.prototype.depth = function () { return this.zmax - this.zmin; };
Bounds.prototype.centerX = function () { return (this.xmin + this.xmax) / 2; };
Bounds.prototype.centerY = function () { return (this.ymin + this.ymax) / 2; };
Bounds.prototype.centerZ = function () { return (this.zmin + this.zmax) / 2; };

CV.Bounds = Bounds;

// ------------------------------------------------------------ number output

function numLength(d) { return String(d).length; }

/** cf. SubMatrix.myDoubleToString */
function numToString(d, maxLength) {
	var s = String(d);
	if (s.length <= maxLength) {
		while (s.length < maxLength) s = " " + s;
		return s;
	}
	var e = Math.abs(d).toExponential(3);
	return (d >= 0 ? "+" : "-") + e;
}

// ------------------------------------------------------------- ValueStruct

/** per-node values, cf. ValueStruct.java */
function ValueStruct(n, name) {
	this.name = name || "";
	this.n = n;
	this.vs = new Array(n);
	this.val = new Array(n);
	for (var i = 0; i < n; i++) {
		this.vs[i] = "";
		this.val[i] = { x: 0, y: 0, z: 0 };
	}
	this.icomponents = 0;
	this.icomp = -1;
}

ValueStruct.prototype.parseValues = function () {
	this.icomponents = 0;
	for (var i = 0; i < this.n; i++) {
		var v = this.val[i];
		var s = this.vs[i].trim();
		if (s.length === 0) { v.x = v.y = v.z = 0; continue; }
		var t = s.split(/\s+/);
		v.x = v.y = v.z = 0;
		if (t[0] !== "[") {
			v.z = CV.parseDouble(t[0]);
			this.icomponents = Math.max(1, this.icomponents);
		} else {
			// [ x y ] or [ x y z ]
			v.x = CV.parseDouble(t[1]);
			v.y = CV.parseDouble(t[2]);
			v.z = t.length >= 5 ? CV.parseDouble(t[3]) : 0;
			this.icomponents = Math.max(v.z === 0 ? 2 : 3, this.icomponents);
		}
	}
};

ValueStruct.prototype.getDoubleValue = function (i) {
	var v = this.val[i];
	if (this.icomponents === 1) return v.z;
	if (this.icomp === -1) return Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
	if (this.icomp === 0) return v.x;
	if (this.icomp === 1) return v.y;
	if (this.icomp === 2) return v.z;
	return 0;
};

ValueStruct.prototype.getMaxValue = function () {
	var m = -Infinity;
	for (var i = 0; i < this.n; i++) m = Math.max(m, this.getDoubleValue(i));
	return m === -Infinity ? 0 : m;
};

ValueStruct.prototype.getMinValue = function () {
	var m = Infinity;
	for (var i = 0; i < this.n; i++) m = Math.min(m, this.getDoubleValue(i));
	return m === Infinity ? 0 : m;
};

CV.ValueStruct = ValueStruct;

// --------------------------------------------------------------- SubMatrix

/**
 * One loaded file of a (possibly parallel) matrix/vector.
 *
 * @param data  result of CV.parseMat
 * @param name  file name (for display)
 */
function SubMatrix(data, name, nrMatrix, ofMatrices) {
	this.name = name;
	this.nrMatrix = nrMatrix || 0;
	this.n = data.n;
	this.dim = data.dim;
	this.pos = data.pos;
	this.rows = data.rows;
	this.isVec = !!data.isVec;
	this.maxBlockDim = data.maxBlockDim;

	this.marks = [];
	this.values = [];
	this.numFct = -1;
	this.fctNames = null;
	this.fctIndex = null;

	this.selected = [];
	this.rcomp = -1;
	this.ccomp = -1;
	this.useArrows = false;
	this.icomponents = 0;
	this.minNeighborDist = 1;
	this.maxValue = 0;
	this.minValue = 0;
	this.arrowScale = 0;
	this.dMoveX = 0;
	this.dMoveY = 0;

	this.bShow = new Uint8Array(this.n);
	this.bShow2 = this.bShow;
	for (var i = 0; i < this.n; i++) this.bShow[i] = 1;

	// hue like SubMatrix(): nrMatrix/ofMatrices*0.75
	this.parallelColor = CV.hsb((nrMatrix || 0) / Math.max(1, ofMatrices || 1) * 0.75, 1, 1);

	this.buildSymmetric();
	this.computeBounds();
}

SubMatrix.prototype.buildSymmetric = function () {
	var n = this.n, i, j;
	var count = new Int32Array(n + 1);
	for (i = 0; i < n; i++) {
		var r = this.rows[i];
		for (j = 0; j < r.length; j++) {
			if (r[j].to === i) continue;
			count[i]++;
			count[r[j].to]++;
		}
	}
	var start = new Int32Array(n + 1);
	var total = 0;
	for (i = 0; i < n; i++) { start[i] = total; total += count[i]; }
	start[n] = total;
	var fill = Int32Array.from(start.subarray(0, n));
	var adj = new Int32Array(total);
	for (i = 0; i < n; i++) {
		var row = this.rows[i];
		for (j = 0; j < row.length; j++) {
			var to = row[j].to;
			if (to === i) continue;
			adj[fill[i]++] = to;
			adj[fill[to]++] = i;
		}
	}
	this.symStart = start;
	this.symAdj = adj;
};

SubMatrix.prototype.computeBounds = function () {
	var b = new Bounds();
	for (var i = 0; i < this.n; i++) {
		var p = this.pos[i];
		b.add(p.x, p.y, isNaN(p.z) ? 0 : p.z);
	}
	this.bounds = b;
};

/** move all nodes of this file, cf. SubMatrix.move (shift+drag on a .pmat) */
SubMatrix.prototype.move = function (dx, dy) {
	for (var i = 0; i < this.n; i++) {
		this.pos[i].x += dx;
		this.pos[i].y += dy;
	}
	this.dMoveX += dx;
	this.dMoveY += dy;
	this.computeBounds();
};

/** undo all move() calls, cf. SubMatrix.reMove */
SubMatrix.prototype.reMove = function () {
	this.move(-this.dMoveX, -this.dMoveY);
	this.dMoveX = 0;
	this.dMoveY = 0;
};

SubMatrix.prototype.totalNrOfConnections = function () {
	var t = 0;
	for (var i = 0; i < this.n; i++) t += this.rows[i].length;
	return t;
};

// ---- connection values ----------------------------------------------------

/** parse the value string into a small dense matrix, cf. connection.get_m */
SubMatrix.prototype.getM = function (c) {
	if (c.m !== undefined) return c.m;
	var s = c.value.trim();
	var m = null;
	if (s.length === 0) {
		m = null;
	} else if (s.charAt(0) === "[" && s.charAt(s.length - 1) === "]") {
		var inner = s.substring(1, s.length - 1);
		var rowStrings = inner.split("|");
		var mm = [];
		var ok = true;
		for (var r = 0; r < rowStrings.length; r++) {
			var t = rowStrings[r].trim();
			var vals = t.length === 0 ? [] : t.split(/\s+/).map(CV.parseDouble);
			if (vals.length === 0) { ok = false; break; }
			mm.push(vals);
		}
		if (ok && mm.length > 0) {
			if (this.isVec && mm.length === 1) {
				// vectors are stored as column vectors, cf. connection.get_m
				m = mm[0].map(function (v) { return [v]; });
			} else {
				m = mm;
			}
		}
	}
	if (m === null && s.length > 0 && s.charAt(0) !== "[") {
		var d = CV.parseDouble(s);
		if (!isNaN(d)) m = [[d]];
	}
	c.m = m;
	return m;
};

/** cf. connection.getDoubleValue */
SubMatrix.prototype.getDoubleValue = function (c) {
	if (c.d !== undefined) return c.d;
	var m = this.getM(c);
	var d;
	if (m === null) d = NaN;
	else if (this.rcomp === -1 || this.numFct !== -1) d = m[0][0];
	else if (this.rcomp < m.length && this.ccomp < m[0].length) d = m[this.rcomp][this.ccomp];
	else d = NaN;
	c.d = d;
	return d;
};

/** cf. connection.getString - the text shown for an entry */
SubMatrix.prototype.getString = function (c) {
	if (c.str !== null) return c.str;
	var m = this.getM(c);
	var s;
	if (m === null) {
		s = c.value;
	} else if (this.rcomp === -1) {
		if (this.isVec || (m.length === 1 && m[0].length === 1)) {
			s = c.value;
		} else {
			var widths = [];
			var r, cc;
			for (r = 0; r < m.length; r++)
				for (cc = 0; cc < m[r].length; cc++)
					widths[cc] = Math.min(10, Math.max(numLength(m[r][cc]), widths[cc] || 0));
			var lines = [];
			for (r = 0; r < m.length; r++) {
				var parts = [];
				for (cc = 0; cc < m[r].length; cc++) parts.push(numToString(m[r][cc], widths[cc]));
				lines.push("[" + parts.join(" | ") + "]");
			}
			s = lines.join("\n");
		}
	} else if (this.rcomp < m.length && this.ccomp < m[0].length) {
		s = String(m[this.rcomp][this.ccomp]);
	} else {
		s = "-";
	}
	c.str = s;
	return s;
};

SubMatrix.prototype.resetValueCache = function () {
	for (var i = 0; i < this.n; i++) {
		var r = this.rows[i];
		for (var j = 0; j < r.length; j++) {
			r[j].str = null;
			r[j].d = undefined;
		}
	}
};

// ---- marks / values / indices --------------------------------------------

SubMatrix.prototype.addMarks = function (mark) {
	if (mark) this.marks.push(mark);
};

SubMatrix.prototype.setValues = function (valueFile) {
	var v = new ValueStruct(this.n, valueFile.name);
	v.vs = valueFile.vs;
	this.values = [v];
	this.isVec = true;
};

SubMatrix.prototype.setIndices = function (idx) {
	if (!idx) return;
	this.numFct = idx.numFct;
	this.fctNames = idx.names;
	this.fctIndex = idx.fctIndex;
};

/** cf. SubMatrix.init_values */
SubMatrix.prototype.initValues = function () {
	if (this.values.length === 0 && this.isVec && (this.dim === 2 || this.dim === 3)) {
		var v = new ValueStruct(this.n, "");
		for (var i = 0; i < this.n; i++) {
			var r = this.rows[i];
			for (var j = 0; j < r.length; j++)
				if (r[j].to === i) v.vs[i] = r[j].value;
		}
		this.values = [v];
	}
	if (this.values.length === 0) return;
	this.values[0].parseValues();
	this.calculateValues();
	this.icomponents = this.values[0].icomponents;
	this.useArrows = this.icomponents > 1 && this.rcomp === -1;
};

/** cf. SubMatrix.calculate_values - for 2d vectors the value becomes z */
SubMatrix.prototype.calculateValues = function () {
	if (this.values.length === 0) return;
	var v = this.values[0];
	v.icomp = this.rcomp;
	this.maxValue = v.getMaxValue();
	this.minValue = v.getMinValue();

	var b = new Bounds();
	for (var i = 0; i < this.n; i++) {
		if (this.dim === 2) this.pos[i].z = v.getDoubleValue(i);
		var z = this.pos[i].z;
		if (isNaN(z)) z = 0;
		b.add(this.pos[i].x, this.pos[i].y, z);
	}
	this.bounds = b;
	if (this.dim === 2) this.dim = 3;
};

/** cf. SubMatrix.calculate_min_neighbor_dist */
SubMatrix.prototype.calculateMinNeighborDist = function () {
	var d = 10000;
	var n = this.n;
	function dist(a, b) {
		var dx = a.x - b.x, dy = a.y - b.y, dz = (a.z || 0) - (b.z || 0);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	for (var i = 0; i < n; i++) {
		if (isNaN(this.pos[i].z)) continue;
		var row = this.rows[i];
		if (row.length <= 1) {
			var lim = Math.min(n, 1000);
			for (var j = 0; j < lim; j++) {
				if (i === j || isNaN(this.pos[j].z)) continue;
				var dd = dist(this.pos[i], this.pos[j]);
				if (dd !== 0) d = Math.min(dd, d);
			}
		}
		for (var k = 0; k < row.length; k++) {
			var to = row[k].to;
			if (isNaN(this.pos[to].z)) continue;
			var d2 = dist(this.pos[i], this.pos[to]);
			if (d2 !== 0) d = Math.min(d2, d);
		}
	}
	this.minNeighborDist = d;
};

/** cf. SubMatrix.postprocess_NaNs */
SubMatrix.prototype.postprocessNaNs = function (globalZmax) {
	var hasNaN = false;
	for (var i = 0; i < this.n; i++) {
		if (isNaN(this.pos[i].z)) {
			this.pos[i].z = 2 * globalZmax;
			hasNaN = true;
		}
	}
	if (hasNaN) this.computeBounds();
	return hasNaN;
};

SubMatrix.prototype.hasValues = function () {
	return this.values.length >= 1;
};

// ---- selection / visibility ----------------------------------------------

SubMatrix.prototype.clearSelection = function () { this.selected = []; };

SubMatrix.prototype.selectNode = function (i) {
	if (i < 0 || i >= this.n) return false;
	if (this.selected.indexOf(i) === -1) this.selected.push(i);
	return true;
};

/**
 * bShow: which nodes are drawn (neighborhood + row component filter),
 * bShow2: which nodes are valid connection targets (column component filter).
 * cf. selectNeighborhood() and set_comp()
 */
SubMatrix.prototype.updateVisibility = function (neighborhood) {
	var i, n = this.n;
	if (!neighborhood || this.selected.length === 0) {
		for (i = 0; i < n; i++) this.bShow[i] = 1;
	} else {
		this.bShow.fill(0);
		// breadth first search up to depth `neighborhood` on the symmetrized graph
		var queue = this.selected.slice();
		var depth = new Int32Array(n).fill(-1);
		for (i = 0; i < queue.length; i++) depth[queue[i]] = 0;
		for (i = 0; i < queue.length; i++) {
			var node = queue[i];
			this.bShow[node] = 1;
			if (depth[node] >= neighborhood) continue;
			for (var k = this.symStart[node]; k < this.symStart[node + 1]; k++) {
				var to = this.symAdj[k];
				if (depth[to] !== -1) continue;
				depth[to] = depth[node] + 1;
				queue.push(to);
			}
		}
	}

	if (this.numFct !== -1 && this.rcomp !== -1) {
		this.bShow2 = this.bShow.slice();
		var v = this.values.length ? this.values[0] : null;
		this.maxValue = -Infinity;
		this.minValue = Infinity;
		for (i = 0; i < n; i++) {
			if (this.fctIndex[i] !== this.rcomp) this.bShow[i] = 0;
			else if (v) {
				this.maxValue = Math.max(this.maxValue, v.getDoubleValue(i));
				this.minValue = Math.min(this.minValue, v.getDoubleValue(i));
			}
			if (this.fctIndex[i] !== this.ccomp) this.bShow2[i] = 0;
		}
		if (!isFinite(this.maxValue)) { this.maxValue = 0; this.minValue = 0; }
	} else {
		this.bShow2 = this.bShow;
	}
};

SubMatrix.prototype.isVisible = function (i) { return this.bShow[i] === 1; };

/** cf. SubMatrix.set_comp */
SubMatrix.prototype.setComp = function (rcomp, ccomp) {
	if (this.numFct === -1 && (this.rcomp !== rcomp || this.ccomp !== ccomp)) {
		this.rcomp = rcomp;
		this.ccomp = ccomp;
		this.resetValueCache();
		this.calculateValues();
		this.calculateMinNeighborDist();
	} else {
		this.rcomp = rcomp;
		this.ccomp = ccomp;
	}
	this.useArrows = this.icomponents > 1 && rcomp === -1;
};

/** the text shown in the info panel, cf. getSelectionString */
SubMatrix.prototype.getSelectionString = function (withName) {
	var s = "";
	for (var k = 0; k < this.selected.length; k++) {
		var i = this.selected[k];
		if (withName) s += this.name + "\n";
		s += "node " + i + "\npos: [ " + this.pos[i].x + " | " + this.pos[i].y;
		if (this.dim === 3) s += " | " + this.pos[i].z;
		s += " ]\n";
		if (this.numFct !== -1 && this.fctIndex) s += "fct: " + this.fctNames[this.fctIndex[i]] + "\n";
		var row = this.rows[i];
		s += row.length + " connections to:\n";
		for (var j = 0; j < row.length; j++) {
			var str = this.getString(row[j]).replace(/\n/g, " ");
			s += row[j].to + ": " + str + "\n";
		}
	}
	return s;
};

CV.SubMatrix = SubMatrix;

// ------------------------------------------------------------------- color

/** java.awt.Color.getHSBColor, h/s/b in [0,1] -> {r,g,b,a} in [0,255] */
CV.hsb = function (h, s, v) {
	h = h - Math.floor(h);
	var i = Math.floor(h * 6);
	var f = h * 6 - i;
	var p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
	var r, g, b;
	switch (i % 6) {
		case 0: r = v; g = t; b = p; break;
		case 1: r = q; g = v; b = p; break;
		case 2: r = p; g = v; b = t; break;
		case 3: r = p; g = q; b = v; break;
		case 4: r = t; g = p; b = v; break;
		default: r = v; g = p; b = q; break;
	}
	return { r: Math.round(r * 255), g: Math.round(g * 255), b: Math.round(b * 255), a: 1 };
};

CV.rgb = function (r, g, b, a) {
	return { r: r, g: g, b: b, a: a === undefined ? 1 : a };
};

CV.colors = {
	black: CV.rgb(0, 0, 0),
	blue: CV.rgb(0, 0, 255),
	red: CV.rgb(255, 0, 0),
	lightGray: CV.rgb(192, 192, 192)
};

// ------------------------------------------------------------------ Viewer

/**
 * View state: all loaded matrices plus zoom/translation/rotation and options.
 */
function Viewer() {
	this.matrices = [];
	this.filename = "";
	this.dim = 2;
	this.isVec = false;
	this.globalBounds = new Bounds();
	this.globalMinValue = 0;
	this.globalMaxValue = 0;
	this.hasValues = false;

	this.width = 800;
	this.height = 600;

	this.scaleZoom = 0.9;
	this.translateDx = 0;
	this.translateDy = 0;
	this.alpha = 0;
	this.beta = 0;
	this.zZoom = 1;

	this.selectedMatrix = -1;
	this.neighborhood = 0;
	this.rcomp = -1;
	this.ccomp = -1;

	this.options = {
		connections: true,
		arrows: false,
		convection: false,
		diffusion: false,
		printEntries: true,
		printIndices: false,
		parallelNodes: false,
		zCompression: false,
		zCompressionValue: 100,
		fontSize: 11,
		arrowSize: 1.0
	};
}

Viewer.prototype.calcGlobalBounds = function () {
	var b = new Bounds();
	this.hasValues = false;
	for (var i = 0; i < this.matrices.length; i++) {
		var m = this.matrices[i];
		b.addBounds(m.bounds);
		if (m.hasValues()) {
			if (!this.hasValues) {
				this.globalMaxValue = m.maxValue;
				this.globalMinValue = m.minValue;
				this.hasValues = true;
			} else {
				this.globalMaxValue = Math.max(this.globalMaxValue, m.maxValue);
				this.globalMinValue = Math.min(this.globalMinValue, m.minValue);
			}
		}
	}
	this.globalBounds = b;
};

Viewer.prototype.rezoom = function () {
	this.alpha = 0;
	this.beta = 0;
	this.translateDx = 0;
	this.translateDy = 0;
	this.scaleZoom = 0.9;
};

/** cf. calcZzoom() */
Viewer.prototype.calcZZoom = function () {
	var gb = this.globalBounds;
	if (this.options.zCompression && this.isVec && gb.depth() !== 0)
		this.zZoom = (gb.width() / 2) / gb.depth() * (this.options.zCompressionValue / 100);
};

/** the base zoom that fits globalBounds into the drawing area */
Viewer.prototype.dzoom = function () {
	var gb = this.globalBounds;
	var w = gb.width() || 1e-10;
	var h = gb.height() || 1e-10;
	return Math.min(this.width / w, this.height / h);
};

/** cf. ConnectionViewerPanel.TranslatePoint */
Viewer.prototype.translate = function (p, out) {
	var gb = this.globalBounds;
	var dz = this.dzoom();
	var s = this.scaleZoom * dz;
	out = out || { x: 0, y: 0 };
	if (this.dim === 2) {
		out.x = (p.x + this.translateDx - gb.centerX()) * s + this.width / 2;
		out.y = this.height / 2 - (p.y + this.translateDy - gb.centerY()) * s;
	} else {
		var x = p.x - gb.centerX();
		var y = p.y - gb.centerY();
		var z = (p.z - gb.centerZ()) * this.zZoom;
		var ca = Math.cos(this.alpha), sa = Math.sin(this.alpha);
		var py = ca * y - sa * z;
		var pz = sa * y + ca * z;
		var px = Math.cos(this.beta) * x - Math.sin(this.beta) * pz;
		out.x = (px + this.translateDx) * s + this.width / 2;
		out.y = this.height / 2 - (py + this.translateDy) * s;
	}
	return out;
};

/**
 * Zoom by `factor`, keeping the point (sx, sy) of the drawing area fixed.
 * Both projections are affine in (translateDx, translateDy), so the same
 * correction works in 2d and 3d.
 */
Viewer.prototype.zoomAt = function (factor, sx, sy) {
	var dz = this.dzoom();
	var s0 = this.scaleZoom * dz;
	this.scaleZoom *= factor;
	var s1 = this.scaleZoom * dz;
	if (sx === undefined) return;
	var A = sx - this.width / 2;
	var B = this.height / 2 - sy;
	this.translateDx += A / s1 - A / s0;
	this.translateDy += B / s1 - B / s0;
};

Viewer.prototype.updateVisibility = function () {
	for (var i = 0; i < this.matrices.length; i++)
		this.matrices[i].updateVisibility(this.neighborhood);
};

Viewer.prototype.setComp = function (rcomp, ccomp) {
	this.rcomp = rcomp;
	this.ccomp = ccomp;
	for (var i = 0; i < this.matrices.length; i++) this.matrices[i].setComp(rcomp, ccomp);
	this.updateVisibility();
	this.calcGlobalBounds();
};

/** select all nodes within 9 pixels of (sx, sy); cf. SubMatrix.select */
Viewer.prototype.selectAt = function (sx, sy) {
	var found = false;
	var p = { x: 0, y: 0 };
	for (var k = 0; k < this.matrices.length; k++) {
		var m = this.matrices[k];
		var hit = false;
		for (var i = 0; i < m.n; i++) {
			if (!m.isVisible(i)) continue;
			this.translate(m.pos[i], p);
			var dx = p.x - sx, dy = p.y - sy;
			if (dx * dx + dy * dy < 81) {
				m.selectNode(i);
				hit = true;
			}
		}
		if (hit) {
			found = true;
			this.selectedMatrix = k;
		}
	}
	return found;
};

Viewer.prototype.clearSelection = function () {
	for (var i = 0; i < this.matrices.length; i++) this.matrices[i].clearSelection();
};

Viewer.prototype.getSelectionString = function () {
	var withName = this.matrices.length > 1;
	var s = "";
	for (var i = 0; i < this.matrices.length; i++) {
		if (this.matrices[i].selected.length === 0) continue;
		s += this.matrices[i].getSelectionString(withName) + "\n";
	}
	return s;
};

/** cf. zoomToSelection() */
Viewer.prototype.zoomToSelection = function () {
	for (var i = 0; i < this.matrices.length; i++) {
		var m = this.matrices[i];
		for (var k = 0; k < m.selected.length; k++) {
			var p = m.pos[m.selected[k]];
			this.translateDx = -p.x + this.globalBounds.centerX();
			this.translateDy = -p.y + this.globalBounds.centerY();
		}
	}
};

/**
 * Component list entries, cf. fileReadingDone(): either the functions from the
 * .indices file or the entries of the small dense blocks.
 */
Viewer.prototype.componentList = function () {
	var list = [{ label: "all comp", rcomp: -1, ccomp: -1 }];
	if (this.matrices.length === 0) return list;
	var m0 = this.matrices[0];
	var r, c;
	if (m0.numFct !== -1) {
		for (c = 0; c < m0.numFct; c++)
			for (r = 0; r < m0.numFct; r++)
				list.push({
					label: "(" + m0.fctNames[r] + ", " + m0.fctNames[c] + ")",
					rcomp: r, ccomp: c
				});
	} else {
		var nc = 0;
		for (var i = 0; i < this.matrices.length; i++)
			nc = Math.max(nc, this.matrices[i].maxBlockDim);
		if (nc <= 1) return list;
		for (c = 0; c < nc; c++)
			for (r = 0; r < nc; r++)
				list.push({ label: "(" + (r + 1) + ", " + (c + 1) + ")", rcomp: r, ccomp: c });
	}
	return list;
};

CV.Viewer = Viewer;

if (typeof module !== "undefined" && module.exports) module.exports = CV;

})(CV);
