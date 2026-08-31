/*
 * ConnectionViewer HTML - renderer.
 *
 * Mirrors SubMatrix.paint() / ConnectionViewerPanel.paint(): it draws through
 * the small gfx interface (canvas or TikZ), so the .tex export produces the
 * same picture as the screen.
 */
var CV = CV || {};

(function (CV) {
"use strict";

var C = CV.colors;

var STROKE = 1.5;
var WIDE_STROKE = 3.0;

/** cf. SubMatrix.drawArrow */
function drawArrow(gfx, x1, y1, x2, y2, arrSize, scale) {
	var dx = x2 - x1, dy = y2 - y1;
	var angle = Math.atan2(dy, dx);
	var len = Math.sqrt(dx * dx + dy * dy) * scale;
	if (len < 1) return;
	var ca = Math.cos(angle), sa = Math.sin(angle);
	function tx(px, py) { return x1 + ca * px - sa * py; }
	function ty(px, py) { return y1 + sa * px + ca * py; }

	var p1x = tx(len, 0), p1y = ty(len, 0);
	gfx.drawLine(Math.round(x1), Math.round(y1), Math.round(p1x), Math.round(p1y));
	gfx.fillPolygon(
		[Math.round(p1x), Math.round(tx(len - arrSize, -arrSize)), Math.round(tx(len - arrSize, arrSize)), Math.round(p1x)],
		[Math.round(p1y), Math.round(ty(len - arrSize, -arrSize)), Math.round(ty(len - arrSize, arrSize)), Math.round(p1y)],
		4);
}
CV.drawArrow = drawArrow;

// ------------------------------------------------------------- connections

/** cf. SubMatrix.draw_connections */
function drawConnections(gfx, view, m, tpos) {
	var opt = view.options;
	if (!opt.connections && !opt.convection) return;

	var drawConvection = opt.convection;
	var drawDiffusion = opt.diffusion;

	gfx.setColor(C.lightGray);
	for (var i = 0; i < m.n; i++) {
		if (!m.isVisible(i)) continue;
		var p1 = tpos[i];
		var row = m.rows[i];
		var x = 0, y = 0, ddsum = 0, dmax = 0;
		var j, c, dd;

		gfx.setColor(C.lightGray);

		if (drawConvection || drawDiffusion) {
			for (j = 0; j < row.length; j++) {
				c = row[j];
				if (!m.bShow2[c.to]) continue;
				dd = m.getDoubleValue(c);
				if (isNaN(dd)) { drawConvection = drawDiffusion = false; break; }
				if (i === c.to) continue;
				if (dd > 0) continue;
				x -= dd * (tpos[c.to].x - p1.x);
				y -= dd * (tpos[c.to].y - p1.y);
				ddsum -= dd;
				dmax = Math.max(Math.abs(dd), Math.abs(dmax));
			}
		}

		if (opt.connections) {
			for (j = 0; j < row.length; j++) {
				c = row[j];
				if (i === c.to) continue;
				if (!m.bShow2[c.to]) continue;

				if (drawDiffusion) {
					dd = m.getDoubleValue(c);
					var f = 1 - Math.abs(dd / dmax);
					if (f >= 0 && f <= 1) {
						var g = Math.round(f * 255);
						gfx.setColor(CV.rgb(g, g, g));
					} else {
						gfx.setColor(C.lightGray);
					}
				}
				var p2 = tpos[c.to];
				if (opt.arrows) drawArrow(gfx, p1.x, p1.y, p2.x, p2.y, 4, 0.4);
				else gfx.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}

		if (drawConvection && dmax !== 0 && Math.abs(ddsum / dmax) > 0.01) {
			gfx.setColor(C.red);
			var sum = ddsum / view.options.arrowSize;
			drawArrow(gfx, p1.x, p1.y, p1.x + x / sum * 2, p1.y + y / sum * 2, 4, 1.0);
		}
	}
	gfx.setColor(C.lightGray);
}

// ------------------------------------------------------------------- marks

/** cf. SubMatrix.draw_markers */
function drawMarkers(gfx, m, tpos) {
	for (var k = 0; k < m.marks.length; k++) {
		var mk = m.marks[k];
		gfx.setColor({
			r: Math.round(mk.red * 255), g: Math.round(mk.green * 255),
			b: Math.round(mk.blue * 255), a: mk.alpha
		});
		var w = 5 + mk.size;
		for (var i = 0; i < m.n; i++) {
			if (!m.isVisible(i) || !mk.marks[i]) continue;
			gfx.fillRect(tpos[i].x - w, tpos[i].y - w, 2 * w, 2 * w);
		}
	}
}

// ------------------------------------------------------------------- nodes

/** cf. SubMatrix.draw_visible_nodes */
function drawNodes(gfx, view, m, tpos, fh) {
	var opt = view.options;
	var v = m.values.length ? m.values[0] : null;
	var drawColor = m.isVec && view.globalMaxValue - view.globalMinValue !== 0;
	var gb = view.globalBounds;

	gfx.setColor(C.black);
	for (var i = 0; i < m.n; i++) {
		if (!m.isVisible(i)) continue;
		var p1 = tpos[i];

		if (drawColor) {
			var d = v
				? (v.getDoubleValue(i) - view.globalMinValue) / (view.globalMaxValue - view.globalMinValue)
				: (m.pos[i].z - gb.zmin) / (gb.depth() || 1);
			if (!isFinite(d)) d = 0;
			gfx.setColor(CV.hsb(d * 0.8, 1, 1));
		}
		if (opt.parallelNodes) gfx.setColor(m.parallelColor);

		if (m.rows[i].length === 0) gfx.fillRect(p1.x - 1, p1.y - 1, 2, 2);
		else gfx.fillRect(p1.x - 2, p1.y - 2, 5, 5);

		if (opt.printIndices) gfx.drawString(String(i), p1.x, p1.y + fh);

		if (m.useArrows && v) {
			var val = v.val[i];
			var pa = view.translate({
				x: m.pos[i].x + val.x * m.arrowScale,
				y: m.pos[i].y + val.y * m.arrowScale,
				z: m.pos[i].z + val.z * m.arrowScale
			});
			drawArrow(gfx, p1.x, p1.y, Math.round(pa.x), Math.round(pa.y), 4, 0.9);
		}
	}
}

/** cf. SubMatrix.draw_selected_node */
function drawSelected(gfx, view, m, tpos, fh) {
	var opt = view.options;
	for (var k = 0; k < m.selected.length; k++) {
		var i = m.selected[k];
		gfx.setLineWidth(WIDE_STROKE);
		gfx.setColor(C.blue);
		var p1 = tpos[i];
		var row = m.rows[i];
		for (var j = 0; j < row.length; j++) {
			var c = row[j];
			if (!m.bShow2[c.to]) continue;
			var p2 = tpos[c.to];
			if (opt.arrows) drawArrow(gfx, p1.x, p1.y, p2.x, p2.y, 4, 0.6);
			else gfx.drawLine(p1.x, p1.y, p2.x, p2.y);
			gfx.fillRect(p2.x - 1, p2.y - 1, 3, 3);
			if (opt.printEntries) {
				var lines = m.getString(c).split("\n");
				for (var l = 0; l < lines.length; l++)
					gfx.drawString(lines[l], p2.x - 5, p2.y - 5 - (lines.length - 1) * fh + fh * l);
			}
		}
		if (opt.printIndices) gfx.drawString(String(i), p1.x, p1.y + fh);
		gfx.setColor(C.lightGray);
		gfx.fillRect(p1.x - 4, p1.y - 4, 8, 8);
		gfx.setLineWidth(STROKE);
	}
}

// ------------------------------------------------------------- decorations

/** cf. ConnectionViewerPanel.drawMinMax */
function drawMinMax(gfx, view, fh) {
	if (!view.hasValues) return;
	var barHeight = 15, barLength = 100;
	var maxStr = String(view.globalMaxValue), minStr = String(view.globalMinValue);
	var move = view.width - barLength - 10 - gfx.stringWidth(maxStr);
	var h = view.height - 3;
	for (var i = 0; i < barLength; i++) {
		gfx.setColor(CV.hsb(i / barLength * 0.8, 1, 1));
		gfx.drawLine(move + i, h, move + i, h - barHeight);
	}
	gfx.setColor(C.black);
	gfx.drawString(minStr, move - 5 - gfx.stringWidth(minStr), h);
	gfx.drawString(maxStr, move + barLength + 5, h);
}

/** cf. ConnectionViewerPanel.drawAxis */
function drawAxis(gfx, view) {
	gfx.setColor(C.black);
	var axisLength = view.dim === 2 ? 15 : 50;
	var ox = axisLength, oy = view.height - axisLength;

	function axis(x, y, z, label) {
		var d = view.dim === 2 ? { x: x, y: y } : rot(x, y, z);
		var ix = Math.round(ox + d.x), iy = Math.round(oy - d.y);
		gfx.drawLine(ox, oy, ix, iy);
		gfx.drawString(label, ix, iy);
	}
	function rot(x, y, z) {
		var ca = Math.cos(view.alpha), sa = Math.sin(view.alpha);
		var py = ca * y - sa * z;
		var pz = sa * y + ca * z;
		return { x: Math.cos(view.beta) * x - Math.sin(view.beta) * pz, y: py };
	}

	if (view.dim === 2) {
		axis(30, 0, 0, "x");
		axis(0, 30, 0, "y");
		return;
	}
	var l = axisLength * 0.9;
	axis(l, 0, 0, "x");
	axis(0, 0, l, "z");
	axis(0, l, 0, "y");
}

// ------------------------------------------------------------------- paint

/** cf. SubMatrix.paint */
function paintMatrix(gfx, view, m, fh) {
	m.arrowScale = m.maxValue !== 0
		? view.options.arrowSize * m.minNeighborDist / m.maxValue
		: m.maxValue;

	var tpos = new Array(m.n);
	for (var i = 0; i < m.n; i++) {
		var p = view.translate(m.pos[i]);
		tpos[i] = { x: Math.round(p.x), y: Math.round(p.y) };
	}

	gfx.setLineWidth(STROKE);
	drawConnections(gfx, view, m, tpos);
	if (!view.options.parallelNodes) drawMarkers(gfx, m, tpos);
	drawNodes(gfx, view, m, tpos, fh);
	drawSelected(gfx, view, m, tpos, fh);
}

/**
 * Draw the whole scene.
 */
CV.render = function (gfx, view) {
	view.calcZZoom();
	gfx.setFont(view.options.fontSize);
	var fh = gfx.fontHeight();

	drawAxis(gfx, view);
	drawMinMax(gfx, view, fh);

	for (var i = 0; i < view.matrices.length; i++)
		paintMatrix(gfx, view, view.matrices[i], fh);

	gfx.flush();
};

})(CV);
