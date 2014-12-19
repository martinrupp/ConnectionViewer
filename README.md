ConnectionViewer
===============================

Webpage: http://gcsc.uni-frankfurt.de/Members/mrupp/martin-rupps-homepage/connectionviewer

ConnectionViewer uses a very easy ASCII file format for Coordinates, Matrices and Vectors, which is implementable in every programming language in a couple of minutes, a description is here: 
http://gcsc.uni-frankfurt.de/Members/mrupp/connectionviewer/mat-file-format-description
 

Here's also a sample gallery : http://gcsc.uni-frankfurt.de/Members/mrupp/connectionviewer/samples

 

# Some documentation:

- Connections: Turning this on/off will display / not display the connections between nodes in the window. sometimes displaying a lot of connections can be slow.

- Connections   as arrows: will display the connections as arrows. slower, but sometimes more clear than the normal way (since these are "directed")

- Diffusion: Will color the connections depending on the ruge/stueben strength of connection. i.e. you can see anisotropic diffusion in one direction.

- Convection: Will show an arrow pointing into the direction of "algebraic convection". That is: if you have a connection -1.9 to left, +2 to mid, and -0.1 to right, convection is to headed to the left.

- Parallel Nodes: enabling this will color each file from a pmat file differently.

- re-move: when using pmat files, you can move the nodes of one processor by holding shift, clicking the mouse and move it around. re-move will move all nodes in the original position.

- Clip… : Will open up a window so you can clip some axes (X, Y, Z clipping). Useful especially in 3d.

- Export…: Export the current view to PDF or tex (as tikzpicture). See galery.

- reopen: Reopen the current file. Automatic reload: automatic reload if file changes.

- Arrow Size, Font Size: Change arrow/font size.

- all nodes / N1 / N2 …: display all nodes or neighborhood 1 / 2 / 3 etc. of the currently selected node(s). useful in 3d.

- all comp: show different components. 

- recenter: recenter the loaded file.

- Search node: enter a node you want to see. This node is selected then. Use 2.234 to select node 234 from parallel file 2. Use to selection to zoom to the selected node.