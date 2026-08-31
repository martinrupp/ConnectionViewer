/*
 * Smoke test for the DOM free parts (parser, model, renderer, tex export).
 * Run with:  node HTML/test/smoke.js
 */
"use strict";

const fs = require("fs");
const path = require("path");
const vm = require("vm");

const jsDir = path.join(__dirname, "..", "js");
const examples = path.join(__dirname, "..", "..", "resources", "examples");

const sandbox = { console, TextDecoder, module: { exports: {} } };
vm.createContext(sandbox);
for (const f of ["parser.js", "model.js", "render.js", "tex.js"]) {
	vm.runInContext(fs.readFileSync(path.join(jsDir, f), "utf8"), sandbox, { filename: f });
}
const CV = sandbox.CV;

let failed = 0;
function check(name, cond, extra) {
	if (cond) {
		console.log("  ok   " + name);
	} else {
		failed++;
		console.log("  FAIL " + name + (extra === undefined ? "" : " (" + extra + ")"));
	}
}

function read(p) { return fs.readFileSync(path.join(examples, p), "utf8"); }

/** minimal stand-in for the loading code in app.js */
function loadMat(file, isVec) {
	const text = read(file);
	const data = CV.parseMat(text, isVec);
	const m = new CV.SubMatrix(data, CV.baseName(file), 0, 1);
	for (const ref of data.markRefs) {
		const p = path.join(path.dirname(file), ref);
		if (fs.existsSync(path.join(examples, p)))
			m.addMarks(CV.parseMarks(read(p), ref, m.n));
	}
	for (const ref of data.valueRefs) {
		const p = path.join(path.dirname(file), ref);
		if (fs.existsSync(path.join(examples, p)))
			m.setValues(CV.parseValues(read(p), ref, m.n));
	}
	if (fs.existsSync(path.join(examples, file + ".indices")))
		m.setIndices(CV.parseIndices(read(file + ".indices"), m.n));
	m.initValues();
	m.calculateMinNeighborDist();
	return m;
}

function makeView(mats) {
	const view = new CV.Viewer();
	view.matrices = mats;
	view.dim = mats.reduce((d, m) => (m.dim === 3 ? 3 : d), mats[0].dim);
	view.width = 900;
	view.height = 700;
	view.calcGlobalBounds();
	view.updateVisibility();
	view.rezoom();
	return view;
}

// --------------------------------------------------------------- bodensee

console.log("bodensee.mat (2d, 3557 nodes)");
{
	const m = loadMat("bodensee.mat");
	check("node count", m.n === 3557, m.n);
	check("dimension", m.dim === 2, m.dim);
	check("positions", m.pos[0].x === -58.92 && m.pos[0].y === 12.1);
	check("connections", m.totalNrOfConnections() > 3557, m.totalNrOfConnections());
	check("bounds", m.bounds.width() > 0 && m.bounds.height() > 0);

	const view = makeView([m]);
	const p = view.translate(m.pos[0]);
	check("projection inside canvas", p.x >= 0 && p.x <= view.width && p.y >= 0 && p.y <= view.height,
		p.x + "," + p.y);

	// select a node and inspect it
	m.selectNode(17);
	const s = m.getSelectionString(false);
	check("selection string", /^node 17\n/.test(s) && /connections to:/.test(s));

	// neighborhood
	view.neighborhood = 1;
	view.updateVisibility();
	let visible = 0;
	for (let i = 0; i < m.n; i++) if (m.isVisible(i)) visible++;
	check("N1 hides most nodes", visible > 1 && visible < 50, visible);
	view.neighborhood = 0;
	view.updateVisibility();

	// tex export
	const tex = CV.exportTex(view, "bodensee.mat");
	check("tex has tikzpicture", tex.indexOf("\\begin{tikzpicture}") !== -1);
	check("tex has draw commands", (tex.match(/\\draw/g) || []).length > 100);
	check("tex has fills", tex.indexOf("\\fill") !== -1);
	check("tex defines colors", tex.indexOf("\\definecolor") !== -1);
	check("tex has no NaN", tex.indexOf("NaN") === -1);
}

// ------------------------------------------------------------------- elder

console.log("elder.mat (2x2 blocks, comp mode)");
{
	const m = loadMat("elder.mat");
	check("node count", m.n === 297, m.n);
	check("block dimension detected", m.maxBlockDim === 2, m.maxBlockDim);

	const c = m.rows[0].find(x => x.to === 0);
	const mm = m.getM(c);
	check("block parsed 2x2", mm.length === 2 && mm[0].length === 2, JSON.stringify(mm));
	check("all comp shows matrix", m.getString(c).split("\n").length === 2, m.getString(c));
	check("all comp value = m[0][0]", m.getDoubleValue(c) === mm[0][0]);

	const view = makeView([m]);
	const list = view.componentList();
	check("component list", list.length === 5 && list[1].label === "(1, 1)"
		&& list[4].label === "(2, 2)", JSON.stringify(list.map(e => e.label)));

	view.setComp(1, 1);
	check("comp (2,2) selects entry", m.getDoubleValue(c) === mm[1][1],
		m.getDoubleValue(c) + " != " + mm[1][1]);
	check("comp (2,2) string", m.getString(c) === String(mm[1][1]), m.getString(c));
	view.setComp(-1, -1);
	check("back to all comp", m.getDoubleValue(c) === mm[0][0]);

	// diffusion/convection need finite values
	view.options.diffusion = true;
	view.options.convection = true;
	const tex = CV.exportTex(view, "elder.mat");
	check("tex with conv/diff", tex.indexOf("\\begin{tikzpicture}") !== -1 && tex.length > 1000);
	check("tex has no NaN", tex.indexOf("NaN") === -1);
}

// --------------------------------------------------------------- amg/marks

console.log("amg/Laplace0/A_L0.mat (marks)");
{
	const m = loadMat("amg/Laplace0/A_L0.mat");
	check("node count", m.n === 1089, m.n);
	check("marks files read", m.marks.length === 4, m.marks.length);
	const coarse = m.marks.find(x => x.name.indexOf("coarse") !== -1);
	check("mark color/size", coarse && coarse.blue === 1 && coarse.size === 2,
		coarse && JSON.stringify([coarse.red, coarse.green, coarse.blue, coarse.alpha, coarse.size]));
	let marked = 0;
	for (let i = 0; i < m.n; i++) if (coarse.marks[i]) marked++;
	check("marked nodes", marked > 0 && marked < m.n, marked);

	const view = makeView([m]);
	const tex = CV.exportTex(view, "A_L0.mat");
	check("tex export with marks", tex.indexOf("\\fill") !== -1);
}

// ---------------------------------------------------- parallel + vec (synth)

console.log("synthetic .pmat / .vec / .indices / .tarmat");
{
	// two 2-node files, each with one connection
	const mat = "1\n2\n2\n0 0\n1 0\n1\n0 0 2\n0 1 -1\n1 1 2\n";
	const names = CV.parsePmat("2\nsub0.mat\nsub1.mat\n");
	check("pmat names", names.length === 2 && names[1] === "sub1.mat");

	const mats = names.map((n, i) => new CV.SubMatrix(CV.parseMat(mat, false), n, i, 2));
	mats[1].move(2, 0);
	const view = makeView(mats);
	check("parallel colors differ",
		JSON.stringify(mats[0].parallelColor) !== JSON.stringify(mats[1].parallelColor));
	check("move shifted bounds", mats[1].bounds.xmin === 2, mats[1].bounds.xmin);
	mats[1].reMove();
	check("re-move restored", mats[1].bounds.xmin === 0, mats[1].bounds.xmin);
	view.calcGlobalBounds();
	check("global bounds", view.globalBounds.xmax === 1, view.globalBounds.xmax);

	// vector: values on the diagonal, 2d -> 3d with z = value
	const vecText = "1\n2\n3\n0 0\n1 0\n2 0\n1\n0 0 [ 1 2 ]\n1 1 [ 0 1 ]\n2 2 [ -1 0 ]\n";
	const v = new CV.SubMatrix(CV.parseMat(vecText, true), "t.vec", 0, 1);
	v.initValues();
	v.calculateMinNeighborDist();
	check("vec becomes 3d", v.dim === 3, v.dim);
	check("vec components", v.values[0].icomponents === 2, v.values[0].icomponents);
	check("vec uses arrows", v.useArrows === true);
	check("vec value length", Math.abs(v.values[0].getDoubleValue(0) - Math.sqrt(5)) < 1e-12);
	check("vec z from value", Math.abs(v.pos[0].z - Math.sqrt(5)) < 1e-12, v.pos[0].z);
	const vview = makeView([v]);
	vview.isVec = true;
	check("vec tex export", CV.exportTex(vview, "t.vec").indexOf("tikzpicture") !== -1);

	// indices file
	const idx = CV.parseIndices("indices 2\nc\np\n0\n1\n0\n", 3);
	check("indices parsed", idx && idx.numFct === 2 && idx.names[1] === "p"
		&& idx.fctIndex[1] === 1, JSON.stringify(idx));
	const mi = new CV.SubMatrix(CV.parseMat(vecText, false), "t.mat", 0, 1);
	mi.setIndices(idx);
	const iview = makeView([mi]);
	const ilist = iview.componentList();
	check("indices component list", ilist.length === 5 && ilist[1].label === "(c, c)"
		&& ilist[2].label === "(p, c)", JSON.stringify(ilist.map(e => e.label)));
	iview.setComp(0, 0);
	let shown = 0;
	for (let i = 0; i < mi.n; i++) if (mi.isVisible(i)) shown++;
	check("component filters nodes", shown === 2, shown);

	// tar archive
	function tarEntry(name, content) {
		const buf = Buffer.alloc(512 + Math.ceil(content.length / 512) * 512);
		buf.write(name, 0, "ascii");
		buf.write(content.length.toString(8).padStart(11, "0") + "\0", 124, "ascii");
		buf.write("0", 156, "ascii");
		buf.write("ustar\0" + "00", 257, "ascii");
		buf.write(content, 512, "ascii");
		return buf;
	}
	const tar = Buffer.concat([
		tarEntry("Stiffness.pmat", "1\nsub0.mat\n"),
		tarEntry("sub0.mat", mat),
		Buffer.alloc(1024)
	]);
	const members = CV.parseTar(tar.buffer.slice(tar.byteOffset, tar.byteOffset + tar.length));
	check("tar members", members.size === 2 && members.has("sub0.mat"),
		Array.from(members.keys()).join(","));
	check("tar content", new TextDecoder().decode(members.get("Stiffness.pmat")).trim()
		=== "1\nsub0.mat");
}

// ------------------------------------------------------------------- errors

console.log("error handling");
{
	let threw = false;
	try { CV.parseMat("2\n2\n0\n1\n"); } catch (e) { threw = true; }
	check("rejects wrong version", threw);
	threw = false;
	try { CV.parseMat("1\n2\n5\n0 0\n1\n"); } catch (e) { threw = true; }
	check("rejects truncated positions", threw);
	const m = CV.parseMat("1\n2\n2\n0 0\n1 1\n1\n0 1\n");
	check("accepts entries without value", m.rows[0][0].value === "");
	const sm = new CV.SubMatrix(m, "x.mat", 0, 1);
	check("empty value -> NaN", isNaN(sm.getDoubleValue(m.rows[0][0])));
	check("empty value string", sm.getString(m.rows[0][0]) === "");
}

console.log(failed === 0 ? "\nall checks passed" : "\n" + failed + " check(s) FAILED");
process.exit(failed === 0 ? 0 : 1);
