/*
 * ConnectionViewer HTML - TikZ (.tex) export.
 *
 * Implements the same gfx interface as CV.CanvasGfx, so CV.render draws the
 * current view into a tikzpicture. Port of tikzGraphics2D.java (with its
 * fillRect coordinate glitch and the undefined-color case fixed).
 */
var CV = CV || {};

(function (CV) {
"use strict";

function fmt(d) {
	if (!isFinite(d)) d = 0;
	var s = d.toFixed(4);
	s = s.replace(/0+$/, "").replace(/\.$/, "");
	return s === "-0" ? "0" : s;
}

function escapeTex(s) {
	return String(s)
		.replace(/\\/g, "\\textbackslash{}")
		.replace(/([&%$#_{}])/g, "\\$1")
		.replace(/\^/g, "\\textasciicircum{}")
		.replace(/~/g, "\\textasciitilde{}");
}

/**
 * @param name   file name, written as a comment
 * @param width  drawing width in pixels
 * @param height drawing height in pixels
 */
function TikzGfx(name, width, height) {
	this.name = name;
	this.width = width;
	this.height = height;
	this.out = [];
	this.color = { r: 0, g: 0, b: 0, a: 1 };
	this.definedColor = null;
	this.fontSize = 11;
	this.lineWidth = 1;

	var scaling = Math.max(width, height) / 15.0;
	this.xscale = scaling;
	this.yscale = -scaling;

	this.out.push(
		"% ConnectionViewer export of " + name,
		"% needs \\usepackage{tikz}",
		"\\begin{figure}",
		"\\centering",
		"\\begin{tikzpicture}",
		"\\def\\dy{0.0}");
}

TikzGfx.prototype.finish = function () {
	this.out.push(
		"\\end{tikzpicture}",
		"% from file " + this.name,
		"\\end{figure}",
		"");
	return this.out.join("\n");
};

TikzGfx.prototype.setColor = function (c) { this.color = c; };
TikzGfx.prototype.setLineWidth = function (w) { this.lineWidth = w; };
TikzGfx.prototype.setFont = function (size) { this.fontSize = size; };
TikzGfx.prototype.fontHeight = function () { return Math.round(this.fontSize * 1.2); };
TikzGfx.prototype.stringWidth = function (s) { return String(s).length * this.fontSize * 0.6; };
TikzGfx.prototype.flush = function () {};

/** emit a \definecolor when the color changed; returns the option string */
TikzGfx.prototype.checkColor = function () {
	var c = this.color;
	var key = c.r + "," + c.g + "," + c.b;
	if (this.definedColor !== key) {
		this.out.push("\\definecolor{myc}{RGB}{" + key + "}");
		this.definedColor = key;
	}
	var opt = "draw=myc, fill=myc";
	if (c.a !== undefined && c.a < 1) opt += ", opacity=" + fmt(c.a);
	return opt;
};

TikzGfx.prototype.x = function (v) { return fmt(v / this.xscale); };
TikzGfx.prototype.y = function (v) { return fmt(v / this.yscale); };

// ---- clipping (cf. tikzGraphics2D.clipLine / clipRectangle) ---------------

var OUT_LEFT = 1, OUT_TOP = 2, OUT_RIGHT = 4, OUT_BOTTOM = 8;

TikzGfx.prototype.outcode = function (x, y) {
	var c = 0;
	if (x < 0) c |= OUT_LEFT; else if (x > this.width) c |= OUT_RIGHT;
	if (y < 0) c |= OUT_TOP; else if (y > this.height) c |= OUT_BOTTOM;
	return c;
};

TikzGfx.prototype.clipLine = function (l) {
	var f1 = this.outcode(l.x1, l.y1), f2 = this.outcode(l.x2, l.y2);
	var guard = 0;
	while ((f1 | f2) !== 0) {
		if ((f1 & f2) !== 0) return false;
		if (++guard > 8) return false;
		var dx = l.x2 - l.x1, dy = l.y2 - l.y1;
		if (f1 !== 0) {
			if ((f1 & OUT_LEFT) && dx !== 0) { l.y1 += (0 - l.x1) * dy / dx; l.x1 = 0; }
			else if ((f1 & OUT_RIGHT) && dx !== 0) { l.y1 += (this.width - l.x1) * dy / dx; l.x1 = this.width; }
			else if ((f1 & OUT_BOTTOM) && dy !== 0) { l.x1 += (this.height - l.y1) * dx / dy; l.y1 = this.height; }
			else if ((f1 & OUT_TOP) && dy !== 0) { l.x1 += (0 - l.y1) * dx / dy; l.y1 = 0; }
			else return false;
			f1 = this.outcode(l.x1, l.y1);
		} else {
			if ((f2 & OUT_LEFT) && dx !== 0) { l.y2 += (0 - l.x2) * dy / dx; l.x2 = 0; }
			else if ((f2 & OUT_RIGHT) && dx !== 0) { l.y2 += (this.width - l.x2) * dy / dx; l.x2 = this.width; }
			else if ((f2 & OUT_BOTTOM) && dy !== 0) { l.x2 += (this.height - l.y2) * dx / dy; l.y2 = this.height; }
			else if ((f2 & OUT_TOP) && dy !== 0) { l.x2 += (0 - l.y2) * dx / dy; l.y2 = 0; }
			else return false;
			f2 = this.outcode(l.x2, l.y2);
		}
	}
	return true;
};

// ---- drawing -------------------------------------------------------------

TikzGfx.prototype.drawLine = function (x1, y1, x2, y2) {
	var l = { x1: x1, y1: y1, x2: x2, y2: y2 };
	if (!this.clipLine(l)) return;
	var opt = this.checkColor();
	this.out.push("\\draw[" + opt + "](" + this.x(l.x1) + ", " + this.y(l.y1) + ") -- ("
		+ this.x(l.x2) + ", " + this.y(l.y2) + "+\\dy);");
};

TikzGfx.prototype.fillRect = function (x, y, w, h) {
	var x1 = Math.max(0, x), y1 = Math.max(0, y);
	var x2 = Math.min(this.width, x + w), y2 = Math.min(this.height, y + h);
	if (x2 <= x1 || y2 <= y1) return;
	var opt = this.checkColor();
	this.out.push("\\fill[" + opt + "](" + this.x(x1) + "," + this.y(y1) + ") rectangle ("
		+ this.x(x2) + "," + this.y(y2) + ");");
};

TikzGfx.prototype.fillPolygon = function (xs, ys, n) {
	var inside = false;
	for (var i = 0; i < n; i++) if (this.outcode(xs[i], ys[i]) === 0) { inside = true; break; }
	if (!inside) return;
	var opt = this.checkColor();
	var parts = [];
	for (i = 0; i < n; i++) parts.push("(" + this.x(xs[i]) + "," + this.y(ys[i]) + ")");
	parts.push("(" + this.x(xs[0]) + "," + this.y(ys[0]) + ")");
	this.out.push("\\draw[" + opt + "] " + parts.join(" -- ") + ";");
};

TikzGfx.prototype.drawString = function (s, x, y) {
	if (this.outcode(x, y) !== 0) return;
	if (String(s).length === 0) return;
	var opt = this.checkColor();
	this.out.push("\\draw[" + opt + "](" + this.x(x + 4) + "," + this.y(y + 4)
		+ ") node {\\texttt{\\tiny " + escapeTex(s) + "}};");
};

CV.TikzGfx = TikzGfx;

/**
 * Render the current view into a standalone tikzpicture.
 * @returns the .tex source as a string
 */
CV.exportTex = function (view, name) {
	var gfx = new TikzGfx(name || view.filename || "connectionviewer", view.width, view.height);
	CV.render(gfx, view);
	return gfx.finish();
};

if (typeof module !== "undefined" && module.exports) module.exports = CV;

})(CV);
