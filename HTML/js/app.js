/*
 * ConnectionViewer HTML - UI: file loading, mouse/keyboard interaction,
 * option handling and .tex export.
 */
(function () {
"use strict";

var view = new CV.Viewer();

var canvas = document.getElementById("display");
var canvasWrap = document.getElementById("canvasWrap");
var ctx = canvas.getContext("2d");
var gfx = null;
var elStatus = document.getElementById("status");
var elInfo = document.getElementById("info");
var elDropHint = document.getElementById("dropHint");
var elFileList = document.getElementById("fileList");
var elComponents = document.getElementById("components");

var source = null;      // where files are read from (local selection or http)
var mainPath = null;    // path of the file currently displayed

// ---------------------------------------------------------------- utilities

function status(s) { elStatus.textContent = s; }

function resolvePath(base, name) {
	name = name.replace(/^\.\//, "");
	if (name.charAt(0) === "/") return name;
	return CV.dirName(base) + name;
}

function readFileText(file) {
	if (file.text) return file.text();
	return new Promise(function (resolve, reject) {
		var r = new FileReader();
		r.onload = function () { resolve(r.result); };
		r.onerror = function () { reject(r.error); };
		r.readAsText(file);
	});
}

function readFileBuffer(file) {
	if (file.arrayBuffer) return file.arrayBuffer();
	return new Promise(function (resolve, reject) {
		var r = new FileReader();
		r.onload = function () { resolve(r.result); };
		r.onerror = function () { reject(r.error); };
		r.readAsArrayBuffer(file);
	});
}

var LOADABLE = ["mat", "pmat", "vec", "pvec", "tarmat"];

function isLoadable(name) {
	return LOADABLE.indexOf(CV.extension(name)) !== -1;
}

// ------------------------------------------------------------------ sources

/** files picked by the user (or dropped); resolved by path and by base name */
function FileSource(files) {
	this.map = new Map();
	this.names = [];
	for (var i = 0; i < files.length; i++) {
		var f = files[i];
		var rel = f.webkitRelativePath || f.relativePath || f.name;
		this.map.set(rel, f);
		if (!this.map.has(CV.baseName(rel))) this.map.set(CV.baseName(rel), f);
		this.names.push(rel);
	}
}

FileSource.prototype.get = function (path) {
	return this.map.get(path) || this.map.get(CV.baseName(path)) || null;
};

FileSource.prototype.read = function (path) {
	var f = this.get(path);
	if (!f) return Promise.reject(new Error("file not found: " + path));
	return readFileText(f);
};

FileSource.prototype.readBinary = function (path) {
	var f = this.get(path);
	if (!f) return Promise.reject(new Error("file not found: " + path));
	return readFileBuffer(f);
};

/** files served over http, relative to the page */
function UrlSource(names) {
	this.names = names || [];
}

UrlSource.prototype.read = function (path) {
	return fetch(path).then(function (r) {
		if (!r.ok) throw new Error(r.status + " " + r.statusText + ": " + path);
		return r.text();
	});
};

UrlSource.prototype.readBinary = function (path) {
	return fetch(path).then(function (r) {
		if (!r.ok) throw new Error(r.status + " " + r.statusText + ": " + path);
		return r.arrayBuffer();
	});
};

/** members of a .tarmat archive */
function TarSource(map) {
	this.map = map;
	this.names = Array.from(map.keys());
	var self = this;
	this.decoder = new TextDecoder("utf-8");
	this.get = function (path) {
		if (self.map.has(path)) return self.map.get(path);
		var base = CV.baseName(path);
		var found = null;
		self.map.forEach(function (v, k) { if (CV.baseName(k) === base) found = v; });
		return found;
	};
}

TarSource.prototype.read = function (path) {
	var d = this.get(path);
	if (!d) return Promise.reject(new Error("not in archive: " + path));
	return Promise.resolve(this.decoder.decode(d));
};

TarSource.prototype.readBinary = function (path) {
	var d = this.get(path);
	if (!d) return Promise.reject(new Error("not in archive: " + path));
	return Promise.resolve(d.buffer);
};

function tryRead(src, path) {
	return src.read(path).catch(function () { return null; });
}

// ------------------------------------------------------- missing file report

/**
 * Files that the loaded file refers to but that could not be read. The browser
 * can only read files the user selected, so this is the common case when a
 * single .mat file was opened instead of its whole folder.
 */
var missing = [];

function noteMissing(path, referrer, kind) {
	for (var i = 0; i < missing.length; i++) if (missing[i].path === path) return;
	missing.push({ path: path, referrer: referrer, kind: kind });
}

/** read a referenced file, remembering it if it is not there */
function readReferenced(src, path, referrer, kind) {
	return src.read(path).catch(function (err) {
		console.warn("could not read " + kind + " " + path + ": " + err.message);
		noteMissing(path, referrer, kind);
		return null;
	});
}

function reportMissing() {
	if (missing.length === 0) return;

	var lines = missing.slice(0, 20).map(function (e) {
		return e.path + "   (" + e.kind + " of " + e.referrer + ")";
	});
	if (missing.length > lines.length) lines.push("… and " + (missing.length - lines.length) + " more");

	document.getElementById("warnText").textContent = missing.length === 1
		? "1 file referenced by the loaded file could not be read:"
		: missing.length + " files referenced by the loaded file could not be read:";
	document.getElementById("warnList").textContent = lines.join("\n");
	document.getElementById("warnHint").textContent = source instanceof FileSource
		? "A browser can only read files you selected yourself. Use “open folder…” "
			+ "instead of “open files…” (or drop the whole folder onto the drawing area), "
			+ "so that all referenced files are available."
		: "Check that the files exist next to the file you opened.";

	var dlg = document.getElementById("warnDialog");
	if (dlg.showModal) dlg.showModal();
	else dlg.setAttribute("open", "");
}

document.getElementById("warnOk").addEventListener("click", function () {
	var dlg = document.getElementById("warnDialog");
	if (dlg.close) dlg.close(); else dlg.removeAttribute("open");
});

// ------------------------------------------------------------------ loading

/**
 * Load one .mat/.vec file including its marks, values and .indices file.
 */
function loadSubMatrix(src, path, isVec, nr, count, referrer) {
	return src.read(path).then(function (text) {
		var data = CV.parseMat(text, isVec);
		var m = new CV.SubMatrix(data, CV.baseName(path), nr, count);
		var self = CV.baseName(path);

		var jobs = [];
		data.markRefs.forEach(function (ref) {
			jobs.push(readReferenced(src, resolvePath(path, ref), self, "marks file").then(function (t) {
				if (t === null) return;
				m.addMarks(CV.parseMarks(t, ref, m.n));
			}));
		});
		data.valueRefs.forEach(function (ref) {
			jobs.push(readReferenced(src, resolvePath(path, ref), self, "value file").then(function (t) {
				if (t === null) return;
				m.setValues(CV.parseValues(t, ref, m.n));
			}));
		});
		jobs.push(tryRead(src, path + ".indices").then(function (t) {
			if (t !== null) m.setIndices(CV.parseIndices(t, m.n));
		}));

		return Promise.all(jobs).then(function () {
			m.initValues();
			m.calculateMinNeighborDist();
			return m;
		});
	}).catch(function (err) {
		console.warn("could not load " + path + ": " + err.message);
		if (referrer) noteMissing(path, referrer, "matrix file");
		return null;
	});
}

/**
 * Load a .mat/.vec/.pmat/.pvec/.tarmat file, cf. ConnectionViewerPanel.readFile.
 */
function load(src, path) {
	status("loading " + path + " …");
	missing = [];
	var ext = CV.extension(path);
	var chain = Promise.resolve({ src: src, path: path, ext: ext });

	if (ext === "tarmat") {
		chain = src.readBinary(path).then(function (buf) {
			var tar = new TarSource(CV.parseTar(buf));
			var inner = tar.names.filter(function (n) { return CV.extension(n) === "pmat"; })[0]
				|| "Stiffness.pmat";
			return { src: tar, path: inner, ext: "pmat" };
		});
	}

	return chain.then(function (cur) {
		var isVec = cur.ext === "vec" || cur.ext === "pvec";
		var namesPromise;
		if (cur.ext === "pmat" || cur.ext === "pvec") {
			namesPromise = cur.src.read(cur.path).then(function (t) {
				return CV.parsePmat(t).map(function (n) { return resolvePath(cur.path, n); });
			});
		} else {
			namesPromise = Promise.resolve([cur.path]);
		}
		// only for parallel files is a missing .mat a missing *referenced* file
		var parallel = cur.ext === "pmat" || cur.ext === "pvec";
		return namesPromise.then(function (names) {
			return Promise.all(names.map(function (n, i) {
				return loadSubMatrix(cur.src, n, isVec, i, names.length,
					parallel ? CV.baseName(cur.path) : null);
			}));
		}).then(function (mats) {
			return { mats: mats.filter(function (m) { return m !== null; }), isVec: isVec, path: path };
		});
	}).then(function (res) {
		if (res.mats.length === 0) throw new Error("no matrix could be read");
		finishLoading(res.mats, res.isVec, res.path);
	}).catch(function (err) {
		status("error: " + err.message);
		console.error(err);
	}).then(reportMissing);
}

/** cf. ConnectionViewerPanel.fileReadingDone */
function finishLoading(mats, isVec, path) {
	view.matrices = mats;
	view.filename = path;
	view.isVec = isVec;
	view.selectedMatrix = mats.length === 1 ? 0 : -1;

	var nodes = 0, conns = 0, dim = mats[0].dim, i;
	for (i = 0; i < mats.length; i++) {
		if (mats[i].isVec) view.isVec = true;
		if (mats[i].dim === 3) dim = 3;
		nodes += mats[i].n;
		conns += mats[i].totalNrOfConnections();
	}
	view.dim = dim;

	view.calcGlobalBounds();

	// NaN values are drawn above everything else, cf. postprocess_NaNs
	var hasNaN = false;
	for (i = 0; i < mats.length; i++)
		if (mats[i].postprocessNaNs(view.globalBounds.zmax)) hasNaN = true;
	if (hasNaN) {
		view.calcGlobalBounds();
		view.globalBounds.zmax *= 2;
	}

	if (nodes > 100000 || conns > 100000) {
		document.getElementById("optConnections").checked = false;
		view.options.connections = false;
	}

	var gb = view.globalBounds;
	view.zZoom = (view.isVec && gb.depth() !== 0) ? (gb.width() / 2) / gb.depth() : 1.0;

	view.neighborhood = parseInt(document.getElementById("neighborhood").value, 10);
	view.rcomp = -1;
	view.ccomp = -1;
	view.updateVisibility();
	view.rezoom();

	buildComponentList();
	elInfo.textContent = "";
	elDropHint.classList.add("hidden");
	status(CV.baseName(path) + " — " + mats.length + " file" + (mats.length > 1 ? "s" : "")
		+ ", " + nodes + " nodes, " + conns + " connections, " + dim + "d"
		+ (view.isVec ? ", vector data" : "")
		+ (missing.length ? " — " + missing.length + " referenced file(s) missing" : ""));
	redraw();
}

function buildComponentList() {
	var list = view.componentList();
	elComponents.innerHTML = "";
	list.forEach(function (e, i) {
		var o = document.createElement("option");
		o.value = String(i);
		o.textContent = e.label;
		elComponents.appendChild(o);
	});
	elComponents.dataset.list = JSON.stringify(list);
	elComponents.value = "0";
}

function setSource(src, preferred) {
	source = src;
	elFileList.innerHTML = "";
	var loadable = src.names.filter(isLoadable);
	loadable.forEach(function (n) {
		var o = document.createElement("option");
		o.value = n;
		o.textContent = n;
		elFileList.appendChild(o);
	});
	if (loadable.length === 0) {
		status("no .mat/.pmat/.vec/.pvec/.tarmat file in the selection");
		return;
	}
	var main = preferred && loadable.indexOf(preferred) !== -1 ? preferred : pickMain(loadable);
	elFileList.value = main;
	mainPath = main;
	load(source, main);
}

var PRIORITY = ["tarmat", "pmat", "pvec", "mat", "vec"];

function pickMain(names) {
	for (var e = 0; e < PRIORITY.length; e++) {
		for (var i = 0; i < names.length; i++)
			if (CV.extension(names[i]) === PRIORITY[e]) return names[i];
	}
	return names[0];
}

// ----------------------------------------------------------------- drawing

var scheduled = false;

function redraw() {
	if (scheduled) return;
	scheduled = true;
	requestAnimationFrame(draw);
}

function draw() {
	scheduled = false;
	var r = canvasWrap.getBoundingClientRect();
	var dpr = window.devicePixelRatio || 1;
	var w = Math.max(1, Math.round(r.width)), h = Math.max(1, Math.round(r.height));
	if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
		canvas.width = w * dpr;
		canvas.height = h * dpr;
	}
	ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
	view.width = w;
	view.height = h;
	gfx = new CV.CanvasGfx(ctx, w, h);
	gfx.clear();
	if (view.matrices.length) CV.render(gfx, view);
}

window.addEventListener("resize", redraw);

// -------------------------------------------------------------------- mouse

var lastPoint = null;
var dragButton = -1;
var dragged = false;

function mousePos(ev) {
	var r = canvas.getBoundingClientRect();
	return { x: ev.clientX - r.left, y: ev.clientY - r.top };
}

canvas.addEventListener("wheel", function (ev) {
	if (!view.matrices.length) return;
	ev.preventDefault();
	var p = mousePos(ev);
	view.zoomAt(ev.deltaY < 0 ? 1 / 0.91 : 0.91, p.x, p.y);
	redraw();
}, { passive: false });

canvas.addEventListener("contextmenu", function (ev) { ev.preventDefault(); });

canvas.addEventListener("mousedown", function (ev) {
	lastPoint = mousePos(ev);
	dragButton = ev.button;
	dragged = false;
});

window.addEventListener("mousemove", function (ev) {
	if (dragButton === -1 || !view.matrices.length || !lastPoint) return;
	var p = mousePos(ev);
	var dzoom = view.dzoom();
	var s = view.scaleZoom * dzoom;
	var dx = lastPoint.x - p.x;
	var dy = p.y - lastPoint.y;
	if (dx === 0 && dy === 0) return;
	dragged = true;

	if (ev.shiftKey && dragButton === 0) {
		// move the nodes of one parallel file
		if (view.selectedMatrix !== -1) {
			view.matrices[view.selectedMatrix].move(-dx / s, -dy / s);
			view.calcGlobalBounds();
		}
	} else if (dragButton === 2) {
		// rotate (3d)
		view.beta -= (p.x - lastPoint.x) * 0.01;
		view.alpha += (p.y - lastPoint.y) * 0.01;
	} else {
		view.translateDx -= dx / s;
		view.translateDy -= dy / s;
	}
	lastPoint = p;
	redraw();
});

window.addEventListener("mouseup", function (ev) {
	var button = dragButton;
	dragButton = -1;
	if (button !== 0 || dragged || !view.matrices.length) { dragged = false; return; }

	var p = mousePos(ev);
	if (p.x < 0 || p.y < 0 || p.x > view.width || p.y > view.height) return;
	if (!ev.shiftKey) view.clearSelection();
	if (!view.selectAt(p.x, p.y)) {
		if (!ev.shiftKey) {
			view.updateVisibility();
			elInfo.textContent = "";
			redraw();
		}
		return;
	}
	view.updateVisibility();
	elInfo.textContent = view.getSelectionString();
	redraw();
});

// --------------------------------------------------------------------- keys

document.addEventListener("keydown", function (ev) {
	if (ev.target.tagName === "INPUT" || ev.target.tagName === "SELECT") return;
	if (ev.key === "r") { view.rezoom(); redraw(); }
	if (ev.key === "+") { view.zoomAt(1 / 0.91); redraw(); }
	if (ev.key === "-") { view.zoomAt(0.91); redraw(); }
});

// ------------------------------------------------------------- file loading

document.getElementById("btnOpenFiles").addEventListener("click", function () {
	document.getElementById("fileInput").click();
});

document.getElementById("btnOpenDir").addEventListener("click", function () {
	document.getElementById("dirInput").click();
});

function filesSelected(ev) {
	var files = Array.prototype.slice.call(ev.target.files);
	if (files.length === 0) return;
	setSource(new FileSource(files));
}

document.getElementById("fileInput").addEventListener("change", filesSelected);
document.getElementById("dirInput").addEventListener("change", filesSelected);

elFileList.addEventListener("change", function () {
	if (!source) return;
	mainPath = elFileList.value;
	load(source, mainPath);
});

document.getElementById("btnReload").addEventListener("click", function () {
	if (source && mainPath) load(source, mainPath);
});

// drag & drop, including whole directories

function entryFiles(entry, path) {
	if (entry.isFile) {
		return new Promise(function (resolve) {
			entry.file(function (f) {
				f.relativePath = path + f.name;
				resolve([f]);
			}, function () { resolve([]); });
		});
	}
	return new Promise(function (resolve) {
		var reader = entry.createReader();
		var all = [];
		(function readMore() {
			reader.readEntries(function (entries) {
				if (entries.length === 0) {
					Promise.all(all).then(function (lists) {
						resolve([].concat.apply([], lists));
					});
					return;
				}
				entries.forEach(function (e) { all.push(entryFiles(e, path + entry.name + "/")); });
				readMore();
			}, function () { resolve([]); });
		})();
	});
}

canvasWrap.addEventListener("dragover", function (ev) {
	ev.preventDefault();
	canvasWrap.classList.add("dragover");
});

canvasWrap.addEventListener("dragleave", function () {
	canvasWrap.classList.remove("dragover");
});

canvasWrap.addEventListener("drop", function (ev) {
	ev.preventDefault();
	canvasWrap.classList.remove("dragover");
	var items = ev.dataTransfer.items;
	var jobs = [];
	if (items && items.length && items[0].webkitGetAsEntry) {
		for (var i = 0; i < items.length; i++) {
			var entry = items[i].webkitGetAsEntry();
			if (entry) jobs.push(entryFiles(entry, ""));
		}
	} else {
		jobs.push(Promise.resolve(Array.prototype.slice.call(ev.dataTransfer.files)));
	}
	Promise.all(jobs).then(function (lists) {
		var files = [].concat.apply([], lists);
		if (files.length) setSource(new FileSource(files));
	});
});

// ------------------------------------------------------------------ options

function bindCheckbox(id, key) {
	var el = document.getElementById(id);
	view.options[key] = el.checked;
	el.addEventListener("change", function () {
		view.options[key] = el.checked;
		redraw();
	});
}

bindCheckbox("optConnections", "connections");
bindCheckbox("optArrows", "arrows");
bindCheckbox("optConvection", "convection");
bindCheckbox("optDiffusion", "diffusion");
bindCheckbox("optEntries", "printEntries");
bindCheckbox("optIndices", "printIndices");
bindCheckbox("optParallel", "parallelNodes");
bindCheckbox("optZCompression", "zCompression");

document.getElementById("fontSize").addEventListener("input", function () {
	view.options.fontSize = parseInt(this.value, 10);
	redraw();
});

document.getElementById("arrowSize").addEventListener("input", function () {
	view.options.arrowSize = parseInt(this.value, 10) * 0.01;
	redraw();
});

document.getElementById("zCompressionSlider").addEventListener("input", function () {
	view.options.zCompressionValue = parseInt(this.value, 10);
	redraw();
});

document.getElementById("neighborhood").addEventListener("change", function () {
	view.neighborhood = parseInt(this.value, 10);
	view.updateVisibility();
	redraw();
});

elComponents.addEventListener("change", function () {
	var list = JSON.parse(elComponents.dataset.list || "[]");
	var e = list[parseInt(this.value, 10)] || { rcomp: -1, ccomp: -1 };
	view.setComp(e.rcomp, e.ccomp);
	elInfo.textContent = view.getSelectionString();
	redraw();
});

document.getElementById("btnRecenter").addEventListener("click", function () {
	view.rezoom();
	redraw();
});

document.getElementById("btnZoomSel").addEventListener("click", function () {
	view.zoomToSelection();
	redraw();
});

document.getElementById("btnReMove").addEventListener("click", function () {
	for (var i = 0; i < view.matrices.length; i++) view.matrices[i].reMove();
	view.calcGlobalBounds();
	redraw();
});

// -------------------------------------------------------------------- search

document.getElementById("search").addEventListener("input", function () {
	if (!view.matrices.length) return;
	var s = this.value.trim();
	view.clearSelection();
	if (s.length === 0) {
		view.updateVisibility();
		elInfo.textContent = "";
		redraw();
		return;
	}
	var dot = s.lastIndexOf(".");
	if (dot === -1) {
		var idx = parseInt(s, 10);
		if (!isNaN(idx))
			for (var i = 0; i < view.matrices.length; i++) {
				if (view.matrices[i].selectNode(idx)) view.selectedMatrix = i;
			}
	} else {
		var mi = parseInt(s.substring(0, dot), 10);
		var ni = parseInt(s.substring(dot + 1), 10);
		if (!isNaN(mi) && !isNaN(ni) && mi >= 0 && mi < view.matrices.length) {
			if (view.matrices[mi].selectNode(ni)) view.selectedMatrix = mi;
		}
	}
	view.updateVisibility();
	elInfo.textContent = view.getSelectionString();
	redraw();
});

// -------------------------------------------------------------------- export

function download(name, text, mime) {
	var blob = new Blob([text], { type: mime || "text/plain" });
	var url = URL.createObjectURL(blob);
	var a = document.createElement("a");
	a.href = url;
	a.download = name;
	document.body.appendChild(a);
	a.click();
	document.body.removeChild(a);
	setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
}

document.getElementById("btnExportTex").addEventListener("click", function () {
	if (!view.matrices.length) { status("nothing to export"); return; }
	var base = CV.baseName(view.filename).replace(/\.[^.]*$/, "");
	download(base + ".tex", CV.exportTex(view, CV.baseName(view.filename)), "text/x-tex");
	status("exported " + base + ".tex");
});

// ------------------------------------------------------------------ examples

var EXAMPLES = [
	"bodensee.mat",
	"elder.mat",
	"blackscholes.mat",
	"Hedgehog.mat",
	"RecircStiffness.mat",
	"gitarre3d.mat",
	"amg/Laplace0/A_L0.mat",
	"amg/Laplace0/P_L1.mat",
	"amg/LaplaceStrechted/A_L0.mat",
	"amg/UnstructuredGuitar/A_L0.mat"
];

var elExamples = document.getElementById("examples");
EXAMPLES.forEach(function (n) {
	var o = document.createElement("option");
	o.value = "../resources/examples/" + n;
	o.textContent = n;
	elExamples.appendChild(o);
});

elExamples.addEventListener("change", function () {
	if (!this.value) return;
	var path = this.value;
	setSource(new UrlSource([path]), path);
});

// ---------------------------------------------------------------------- init

/** handle for debugging and for embedding the viewer in another page */
window.CVApp = {
	view: view,
	redraw: redraw,
	load: function (path) { setSource(new UrlSource([path]), path); },
	loadFiles: function (files) { setSource(new FileSource(files)); },
	exportTex: function () { return CV.exportTex(view, CV.baseName(view.filename)); }
};

status("no file loaded. open a file, or drop one onto the drawing area.");
redraw();

// load a file given as ?file=... (works when served over http)
var qs = new URLSearchParams(window.location.search);
if (qs.get("file")) {
	var p = qs.get("file");
	setSource(new UrlSource([p]), p);
}

})();
