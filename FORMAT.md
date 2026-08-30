ConnectionViewer File Format
===============================

ConnectionViewer data format is mostly used in the simulation framework ug4 [[Heppner et al., 2013]](#references).
It is a simple ASCII representation, that is similar to e.g the Matrix Market Coordinate Format [[Boisvert et al., 1996]](#references).

**Indexing into arrays and matrices is 0-based.**

All lines are plain ASCII, fields within a line are separated by single spaces,
and every record occupies exactly one line. There are no comment lines.

## File extensions

| Extension | Content |
| --------- | ------- |
| `.mat`    | serial matrix |
| `.vec`    | serial vector (same format, see [Vector files](#vector-files)) |
| `.pmat`   | parallel matrix — a list of `.mat` files |
| `.pvec`   | parallel vector — a list of `.vec` files |
| `.tarmat` | tar archive containing a `Stiffness.pmat` plus its `.mat` files |
| `.marks`  | mark file, referenced from a `.mat`/`.vec` file |
| `.indices`| optional component/function names, found next to a `.mat` file |

## ConnectionViewer .mat file format

`#` are comments, not actual part of the format

    ConnectionViewerVersion        # (1, integer)
    Dimensions                     # (integer)
    NrOfNodes                      # (integer)
    Positions[0]                   # (PositionType, see below)
    ...                            # (PositionType)
    Positions[NrOfNodes-1]         # (PositionType)
    1
    Connection[0]                  # (ConnectionType)
    Connection[1]                  # (ConnectionType)
    ...                            # (ConnectionType)
    c MarksFilename0
    c MarksFilename1
    ...

First comes a line giving the version of the file format. In the current version
this is always a `1`.

Next follows the number of dimensions: `2` or `3`.

The next line holds the number of nodes, followed by a list of their
coordinates. The coordinates are given in the following format:

ConnectionViewer PositionType for `Dimensions=2`:

    x y                            # (float)

ConnectionViewer PositionType for `Dimensions=3`:

    x y z                          # (float)

After the coordinates follows a single line containing a `1`, then the matrix
entries (i.e. graph edges) in the style of the Matrix Market Coordinate Format:

ConnectionViewer ConnectionType:

    iFrom iTo strValue             # (integer, integer, string)

`iFrom` and `iTo` are node indices ∈ [0, …, NrOfNodes-1].

So a matrix entry *A(3,9) = 1.88* — counting rows and columns from 1, as usual in
mathematical notation — corresponds to the line

    2 8 1.88

The third field `strValue` is read as a string and may therefore also carry other information. It can be left empty, which allows graphs without edge weights to be represented as well. All entries are displayed, including those with `strValue` = `0.0` or empty.

Entries do not have to be sorted, and rows may be given in any order.

### Block matrix entries

If `strValue` is bracketed, ConnectionViewer interprets it as a small dense
block instead of a scalar, and can then display single components of the block:

    0 0 [ a11 a12 | a21 a22 ]                            # (2x2 block)
    0 0 [ a11 a12 a13 | a21 a22 a23 | a31 a32 a33 ]       # (3x3 block)

Anything that parses as neither a scalar nor one of these block forms is kept and shown verbatim as a string.

### The line containing `1`

The single `1` between the positions and the connections is a legacy
*ShowInWindow* field. It is read but currently unused — always write `1`.

## Mark files

To mark nodes, for example for algorithms such as graph colouring, an external
mark file can be given. This is done by adding an arbitrary number of lines of the
form

    c MarksFilename

at the end of the file, that is, after the last matrix entry.

Mark files have the format

    red green blue alpha visibleSize
    markIndex1                     # (integer)
    markIndex2                     # (integer)
    ..
    markIndexN                     # (integer)

Here the size (`visibleSize`) and the colour of the mark can be given. Colour
components and alpha are floats in [0, 1], `visibleSize` is an integer in pixels.
Then follow the indices that are to be marked with this marker.

Relative mark filenames are resolved relative to the directory of the `.mat`
file, not the current working directory. A mark file that cannot be opened is
skipped with a warning; loading continues.

Example (`_coarse_L0.marks`, blue markers of size 2):

    0 0 1 1 2
    128
    130
    132

## Value files

Analogous to `c`, a line

    v ValuesFilename

attaches per-node values, which are displayed as colours and, for vector-valued data, as arrows. A value file contains one line per node that has a value:

    index valueString              # (integer, string)

`valueString` is either a scalar (`3.0`) or a bracketed vector (`[ 1.5 2.0 ]`,
`[ 1.5 2.0 0.5 ]`). Nodes not listed keep the value 0. Only the last `v` line of
a file takes effect; unlike marks, values do not accumulate.

## Vector files

`.vec` files use exactly the same format 1 as `.mat` files. The vector entry for
node *i* is stored as the diagonal connection

    i i valueString

with `valueString` scalar or bracketed as above. For 2D vector data,
ConnectionViewer lifts the drawing into 3D by using the value as the z
coordinate.

## Index files

If a file `<name>.mat.indices` exists next to `<name>.mat`, it is read
automatically and provides names for the components (functions) each node belongs
to:

    <ignored> numFct               # (two tokens; the second is the number of functions)
    fctName1
    ..
    fctNameNumFct
    fctIndexOfNode0                # (integer)
    ..
    fctIndexOfNode(NrOfNodes-1)    # (integer)

The file is optional; if it is missing or malformed it is silently ignored.

## Parallel files

Normal (serial) matrices are stored with the extension `.mat`. Support for parallel computations is provided in ConnectionViewer through `.pmat` files. These have the structure

    NumberOfParallelFiles         # (integer)
    File1                         # (.mat file)
    File2
    ..
    FileN

Each of the listed `.mat` files is again an ordinary (serial) `.mat` file, and
relative names are resolved relative to the `.pmat` file's directory. Vectors work the same way with `.pvec` listing `.vec` files. Missing entries are tolerated: the corresponding sub-matrix is simply not displayed.

Reading and display in ConnectionViewer are parallelised, i.e. a separate thread is started for each `.mat` file. To tell the graphs of the individual processes apart, nodes can be coloured by processor. Furthermore the parallel files can be moved individually: select a node — either by clicking it directly or via the search function, where you can search using the pattern `FileNr.Index` — and then hold the Shift key to move only that file.

With these functions, parallel discretisations and AMG methods can be debugged very comfortably. In particular the overlaps required for parallel AMG methods, or agglomerations, can be inspected.

### Tar archives

A `.tarmat` file is a tar archive whose member `Stiffness.pmat` is a `.pmat` file
as above; the `.mat` files it lists are read from within the same archive.

## Complete example

A 3-node 2D matrix with one mark file:

`example.mat`

    1
    2
    3
    0.0 0.0
    1.0 0.0
    0.0 1.0
    1
    0 0 2
    0 1 -1
    1 0 -1
    1 1 2
    2 2 1
    c example.marks

`example.marks` — one red marker of size 3 on node 2:

    1 0 0 1 3
    2

## References

- **[Boisvert et al., 1996]** Boisvert, Ronald F., R. Pozo and K. Remington
  (1996). *The Matrix Market Exchange Formats: Initial Design.* National
  Institute of Standards and Technology Internal Report, NISTIR 5935.
  <http://math.nist.gov/MatrixMarket/formats.html>
- **[Heppner et al., 2013]** Heppner, Ingo, Michael Lampe, Arne Nägel, Sebastian
  Reiter, Martin Rupp, Andreas Vogel and Gabriel Wittum (2013). *Software
  Framework ug4: Parallel Multigrid on the Hermit Supercomputer.* In: Nagel,
  Wolfgang E., Dietmar H. Kröner and Michael M. Resch (eds.), *High Performance
  Computing in Science and Engineering 2012*, pp. 435–449. Springer Berlin
  Heidelberg. doi:[10.1007/978-3-642-33374-3_32](http://dx.doi.org/10.1007/978-3-642-33374-3_32)
