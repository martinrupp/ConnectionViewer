/*
 * ConnectionViewer HTML - drawing back end for a <canvas>.
 *
 * The renderer only uses the small "gfx" interface implemented here and in
 * tex.js (TikZ export), the same way the Java version draws through
 * java.awt.Graphics resp. tikzGraphics2D.
 *
 * interface gfx:
 *   setColor(color)   color = {r,g,b,a}
 *   setLineWidth(w)
 *   setFont(size), fontHeight(), stringWidth(s)
 *   drawLine(x1,y1,x2,y2), fillRect(x,y,w,h), fillPolygon(xs,ys,n)
 *   drawString(s,x,y)
 *   flush()
 */
var CV = CV || {};

(function (CV) {
"use strict";

function colorString(c) {
	if (c.a === undefined || c.a >= 1) return "rgb(" + c.r + "," + c.g + "," + c.b + ")";
	return "rgba(" + c.r + "," + c.g + "," + c.b + "," + c.a + ")";
}
CV.colorString = colorString;

function CanvasGfx(ctx, width, height) {
	this.ctx = ctx;
	this.width = width;
	this.height = height;
	this.color = { r: 0, g: 0, b: 0, a: 1 };
	this.fontSize = 11;
	this.lineWidth = 1;
	this.path = null;        // batched lines of the current color
	ctx.lineWidth = 1;
	ctx.font = this.fontSize + "px ui-monospace, Menlo, Consolas, monospace";
	ctx.textBaseline = "alphabetic";
}

CanvasGfx.prototype.clear = function () {
	this.ctx.clearRect(0, 0, this.width, this.height);
	this.ctx.fillStyle = "#fff";
	this.ctx.fillRect(0, 0, this.width, this.height);
};

CanvasGfx.prototype.flush = function () {
	if (this.path) {
		this.ctx.stroke(this.path);
		this.path = null;
	}
};

CanvasGfx.prototype.setColor = function (c) {
	if (c.r === this.color.r && c.g === this.color.g && c.b === this.color.b && c.a === this.color.a)
		return;
	this.flush();
	this.color = c;
	var s = colorString(c);
	this.ctx.strokeStyle = s;
	this.ctx.fillStyle = s;
};

CanvasGfx.prototype.setLineWidth = function (w) {
	if (w === this.lineWidth) return;
	this.flush();
	this.lineWidth = w;
	this.ctx.lineWidth = w;
};

CanvasGfx.prototype.setFont = function (size) {
	this.fontSize = size;
	this.ctx.font = size + "px ui-monospace, Menlo, Consolas, monospace";
};

CanvasGfx.prototype.fontHeight = function () {
	return Math.round(this.fontSize * 1.2);
};

CanvasGfx.prototype.stringWidth = function (s) {
	return this.ctx.measureText(s).width;
};

CanvasGfx.prototype.drawLine = function (x1, y1, x2, y2) {
	if (!this.path) this.path = new Path2D();
	this.path.moveTo(x1 + 0.5, y1 + 0.5);
	this.path.lineTo(x2 + 0.5, y2 + 0.5);
};

CanvasGfx.prototype.fillRect = function (x, y, w, h) {
	this.flush();
	this.ctx.fillRect(x, y, w, h);
};

CanvasGfx.prototype.fillPolygon = function (xs, ys, n) {
	this.flush();
	var ctx = this.ctx;
	ctx.beginPath();
	ctx.moveTo(xs[0], ys[0]);
	for (var i = 1; i < n; i++) ctx.lineTo(xs[i], ys[i]);
	ctx.closePath();
	ctx.fill();
};

CanvasGfx.prototype.drawString = function (s, x, y) {
	this.flush();
	this.ctx.fillText(s, x, y);
};

CV.CanvasGfx = CanvasGfx;

})(CV);
