ConnectionViewer in HTML + JavaScript
=====================================

A browser port of ConnectionViewer: it reads the ConnectionViewer file format
(see [../FORMAT.md](../FORMAT.md)) and draws matrices, graphs and vectors on a
`<canvas>`. No build step, no dependencies — plain HTML, CSS and JavaScript.

## Running

Open `HTML/index.html` in a browser and pick a file with **open files…** /
**open folder…**, or drop files onto the drawing area.

To use the **example** dropdown (which loads from `../resources/examples/`) the
page has to be served over http, because browsers do not allow `fetch` from
`file://`:

    cd path/to/ConnectionViewer
    python3 -m http.server 8000
    # then open http://localhost:8000/HTML/index.html

A file can also be passed in the URL: `index.html?file=../resources/examples/elder.mat`.

### Opening files that reference other files

A browser cannot read a file the user did not select, so a `.mat` file that
refers to `.marks`/value files, and a `.pmat` that lists `.mat` files, only work
if those files are part of the selection. Either

- select all of them in **open files…** (multi-select), or
- use **open folder…** / drop the containing folder onto the drawing area.

Referenced names are resolved relative to the referencing file first and by base
name second, so a whole directory tree works.

If files are missing, the viewer still shows what it could read, notes the
number of missing files in the status bar and opens a dialog listing all of them
(which file referenced them and as what), with a hint to use **open folder…**
instead.

## Supported files

| Extension | Notes |
| --------- | ----- |
| `.mat`    | matrix/graph, format version 1 |
| `.vec`    | vector — values on the diagonal, drawn as colours and arrows |
| `.pmat`, `.pvec` | parallel files; every listed file becomes one sub-matrix |
| `.tarmat` | tar archive containing a `.pmat` plus its `.mat` files |
| `.marks`  | referenced by `c <file>` lines |
| value files | referenced by `v <file>` lines |
| `<name>.mat.indices` | picked up automatically, gives the component (function) names |

Block entries (`[ a b | c d ]`, 3x3, …) are parsed and can be inspected
component-wise with the **comp** dropdown.

## Interaction

| | |
| --- | --- |
| mouse wheel | zoom in/out (towards the mouse position) |
| drag | move the view |
| right-drag | rotate (3d files) |
| click a node | select it; index, position and all connections are listed below |
| shift+click | add to the selection |
| shift+drag | move the nodes of the selected parallel file only (**re-move** undoes it) |
| `r` / `+` / `-` | recenter / zoom in / zoom out |
| search field | node index, or `fileNr.index` for parallel files |

Options: **Connections** on/off, **as Arrows**, **Convection** (arrow into the
direction of algebraic convection), **Diffusion** (connections coloured by
strength of connection), **Print Entries in Window**, **Print Indices in
Window**, **Parallel Nodes** (colour each file of a `.pmat` differently),
**z-compression** for vectors, plus font size, arrow size and the neighborhood
filter (`all nodes`, `N1`, `N2`, …).

## Export

**export .tex (tikz)** writes the current view as a `tikzpicture`, like the Java
version's tex export. The generated file needs `\usepackage{tikz}` and contains
one `figure` environment. There is deliberately no PDF and no Julia export.

## Source layout

| File | Contents |
| ---- | -------- |
| `js/parser.js` | the file format: `.mat`/`.vec`, `.pmat`, `.marks`, value files, `.indices`, tar |
| `js/model.js`  | `CV.SubMatrix` (one file) and `CV.Viewer` (view state, projection, selection) |
| `js/gfx.js`    | canvas drawing back end |
| `js/render.js` | the drawing itself, against the small gfx interface |
| `js/tex.js`    | second gfx back end, emitting TikZ |
| `js/app.js`    | UI: file loading, mouse handling, options, export |

`parser.js`, `model.js`, `render.js` and `tex.js` do not touch the DOM, which is
what makes the renderer reusable for the tex export and testable outside a
browser. `window.CVApp` exposes `{view, redraw, load, loadFiles, exportTex}` for
debugging or for driving the viewer from another script.

The port follows the Java implementation closely (projection, zoom factors,
colours, diffusion/convection computation, marker sizes), so both versions show
the same picture for the same file.

## Tests

    node HTML/test/smoke.js

parses the examples in `../resources/examples`, exercises the model (selection,
neighborhood, component switching, parallel move, vector values, tar reading)
and renders through the TikZ back end. `test/data/` holds small generated files
for the cases the example directory does not cover: a `.pmat` with two blocks
and mark files, a `.vec` with a rotational field, and a `.mat` with an
`.indices` file (functions `u`, `p`).
