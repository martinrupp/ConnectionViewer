/*
 Copyright (c) 2011-2013, Martin Rupp, University Frankfurt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 1. Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 3. All advertising materials mentioning features or use of this software
 must display the following acknowledgement:
 This product includes software developed by Martin Rupp, University Frankfurt
 4. Neither the name of the University nor the
 names of its contributors may be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY FRANKFURT ''AS IS'' AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY FRANKFURT BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 * @author Martin Rupp
 * @email martin.rupp@gcsc.uni-frankfurt.de
 */
package connectionviewer;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

class SubMatrix 
{
	
	////////////////////////////////////////////////////////////////////////////
	private int iNeighborhood;
	boolean bUseArrows;
	double minNeighborDist = 1;
	double maxValue=0, minValue=0;
	int icomponents;
	
	Boolean bSuccessfullyLoaded=false;
	public int iNrOfNodes;
	
	// position of nodes
	private Point3D[] pos;
	
	// bounds of nodes
	private Rectangle3D bounds;
	
	// ArrayList of connection for each node
	private ArrayList<connection>[] matrix;
	
	// marks
	private ArrayList<MarkStruct> marks;
	
	// additional values
	private ArrayList<ValueStruct> values;
	
	private amgframework.SparseGraph symmMatrix;
	
	// show true/false for each node
	private Boolean[] bShow;
	private Boolean[] bShow2;
	
	// stores how long arrows are
	double arrowScale;
	
	// our dimension (2 or 3)
	private int iDimensions;
	
	private int iConnectionViewerVersion;
	// the name of the file loaded
	private String filename;
	// last component of the filename
	private String name;
	
	private ConnectionViewerPanel cv;
	
	// our color when showing parallel nodes
	Color parallelColor;
	int nrMatrix;
	
	// for moving in parallel
	double dMoveX=0;	
	double dMoveY=0;

	double oldStatus = 0;
	
	private boolean bVec; // true if vec data (not matrix)
	
	ArrayList<Integer> selected; // selected nodes
	
	// for submatrix selection
	int rcomp = -1;
	int ccomp = -1;


	////////////////////////////////////////////////////////////////////////////
	void saveoutputJulia() {
		
	}	

	void indexNeighborhood(int i, ArrayList<Integer> [] neigh, boolean bVisited[], int now, int max)
	{
		if(now >= max) return;
		if(neigh[now] == null)
			neigh[now] = new ArrayList<Integer>();
		for(int j : neigh[now-1])
		{
			for(connection c : matrix[j])
			{				
				if(bVisited[c.to]) continue;
				neigh[now].add(c.to);
				bVisited[c.to] = true;
			}			
		}
		for(int j : neigh[now])
			indexNeighborhood(j, neigh, bVisited, now+1, max);
	}
	
	String ToJulia(String s)
	{
		return s.replace("|", ";");
	}
	
	
	
	void writeLatex(FileWriter file) throws IOException
	{
		Dimension dim = cv.getDrawingSize();
		
		double scale = Math.max(dim.height, dim.width)/15.0;
		
		tikzGraphics2D tg = new tikzGraphics2D(filename, new Rectangle(0, 0, dim.width, dim.height), file, scale);
		
/*			void paint(Graphics g, boolean bPrintEntriesInWindow,
			boolean bPrintNumbersInWindow,
			boolean bShowParallelNodes, boolean bDrawConnections, boolean bArrowConnections,
			boolean bDrawConvection, boolean bDrawDiffusion,
			int fontsize)*/

		for(SubMatrix mat : cv.matrices)
			cv.drawmatrix(mat, tg);
		
		tg.finish();

	}
	void writeJulia(FileWriter file) throws IOException
	{
		
		
		file.write("require(\"amg/io.jl\")\n");
		file.write("function f_load()\n");
				
		int depth=4;
		ArrayList<Integer> [] neigh = new ArrayList[depth];
		if(selected.size() == 0) return;
		int i=selected.get(0);
		
		neigh[0] = new ArrayList<Integer>();
		neigh[0].add(i);
		boolean bVisited[] = new boolean [iNrOfNodes];
		bVisited[i] = true;
		indexNeighborhood(i, neigh, bVisited, 1, depth);
		
		ArrayList<Integer> localToGlobal = new ArrayList<Integer>();
		int globalToLocal[] = new int[iNrOfNodes];
		
		for(int j=0; j<depth; j++)
			for(int k : neigh[j])
			{
				globalToLocal[k] = localToGlobal.size();
				localToGlobal.add(k);				
			}
		file.write("pos=Array(Pos3d,"+localToGlobal.size()+")\n");
		for(int j=0; j<localToGlobal.size(); j++)
		{
			int k=localToGlobal.get(j);
			file.write("pos["+(j+1)+"] = Pos3d("+pos[k].x+","+pos[k].y+","+pos[k].z+")\n");
		}
		
		
		file.write("A=zeros("+localToGlobal.size()+","+localToGlobal.size()+")\n");
		
		for(int j=0; j<localToGlobal.size(); j++)
		{
			for(connection c : matrix[localToGlobal.get(j)])
			{
				if(bVisited[c.to] == false) continue;
				file.write("A["+(j+1)+", "+ (globalToLocal[c.to]+1)+"]="+ToJulia(c.value)+"\n");
			}
		}
		
		
		// indices 
		int cSize=neigh[0].size()+1;
		file.write("indices = ["+cSize);
		for(int j=1; j<depth; j++)
		{
			cSize+=neigh[j].size();
			file.write(", "+cSize);
		}
		file.write("]\n");
		
		file.write("return A, Grid(pos, "+iDimensions+"), indices\n");
		file.write("end\n");
		
	}

	int get_components() {
		return icomponents;
	}

	void move(double dx, double dy)
	{
		for(int i=0; i<pos.length; i++)
		{
			pos[i].x+=dx;
			pos[i].y+=dy;
			bounds.add(pos[i]);
		}
		dMoveX += dx;
		dMoveY += dy;
		
	}

	void reMove()
	{
		bounds = new Rectangle3D();
		for(int i=0; i<pos.length; i++)
		{
			pos[i].x-=dMoveX;
			pos[i].y-=dMoveY;
			if(i==0)
				bounds.setRect(pos[0].getX(), pos[0].getY(), pos[0].getZ(), 0, 0, 0);
			else
				bounds.add(pos[i]);
		}
		dMoveX = 0;
		dMoveY = 0;
	}

	
	private class connection {

		public int to;
		private String value;
		private String str;
		private double dValue;
		private double m[][];
		
		boolean valueCalculated=false;
		boolean mCalculated=false;
		
		
		void get_m()
		{			
			if(mCalculated) return;
			dValue = Double.NaN;
			m = null;
			mCalculated = true;
			try{
				String s[] = value.split(" ");
				m=null;
				if(bVec)
				{
					if(s.length == 4 && s[0].compareTo("[")==0 && s[3].compareTo("]")==0)
					{
						m = new double[2][1];
						m[0][0] = Double.parseDouble(s[1]);
						m[1][0] = Double.parseDouble(s[2]);
					}
					else if(s.length == 5 && s[0].compareTo("[")==0 && s[4].compareTo("]")==0)
					{
						m = new double[3][1];
						m[0][0] = Double.parseDouble(s[1]);
						m[1][0] = Double.parseDouble(s[2]);
						m[2][0] = Double.parseDouble(s[3]);
					}
				}
				else
				{
					if(s.length == 7 && s[0].compareTo("[")==0 && s[3].compareTo("|")==0 && s[6].compareTo("]")==0)
					{
						m = new double[2][2];
						m[0][0] = Double.parseDouble(s[1]);
						m[0][1] = Double.parseDouble(s[2]);
						m[1][0] = Double.parseDouble(s[4]);
						m[1][1] = Double.parseDouble(s[5]);			
					}
					else if(s.length == 13 && s[0].compareTo("[")==0 && s[4].compareTo("|")==0 && s[8].compareTo("|")==0 && s[12].compareTo("]")==0)
					{
						m = new double[3][3];
						m[0][0] = Double.parseDouble(s[1]);
						m[0][1] = Double.parseDouble(s[2]);
						m[0][2] = Double.parseDouble(s[3]);
						m[1][0] = Double.parseDouble(s[5]);
						m[1][1] = Double.parseDouble(s[6]);
						m[1][2] = Double.parseDouble(s[7]);
						m[2][0] = Double.parseDouble(s[9]);
						m[2][1] = Double.parseDouble(s[10]);
						m[2][2] = Double.parseDouble(s[11]);
					}
				}
				if(m==null)
				{
					m=new double[1][1];
					m[0][0]=Double.parseDouble(value);
				}				
				
			}
			catch(Exception e)
			{
				
			}		
		}
		String getString()
		{
			if(str != null) return str;
			get_m();
			
			if(m == null) 
				str = value;
			else if(rcomp == -1)
			{
				if(bVec)
					str = value;
				else
				{
					int N=m.length;

					int maxLength[] = new int[N];
					for(int c =0; c<N; c++) maxLength[c] = 0;
					for(int r =0; r<N; r++)
						for(int c =0; c<N; c++)
							maxLength[c] = Math.min(10, Math.max(myDoubleStringLength(m[r][c]), maxLength[c]));				

					String res="";
					for(int r =0; r<N; r++)
					{
						if(r!=0) res+="\n";
						res +="[";
						for(int c =0; c<N; c++)
						{
							if(c!=0) res += " | ";
							res += myDoubleToString(m[r][c], maxLength[c]);
						}
						res += "]";
					}
					str = res;			
				}
			}
			else if(rcomp < m.length && ccomp < m[0].length)
			{
				str = Double.toString(m[rcomp][ccomp]);
			}
			else 
				str = "-";
			return str;
		}
		
		double getDoubleValue()
		{
			get_m();
			if(!valueCalculated) 
			{
				valueCalculated = true;
				if(m == null) dValue = Double.NaN;
				else if(rcomp == -1 || numFct != -1)
					dValue = m[0][0];
				else if(rcomp < m.length && ccomp < m[0].length)
					dValue = m[rcomp][ccomp];				
				else
					dValue = Double.NaN;				
			}
			return dValue;
			
		}
	};

	
	public int get_total_nr_of_connections()
	{
		int TNC=0;
		for(int i=0; i<matrix.length; i++)
			TNC += matrix[i].size();
		return TNC;
	}
	
	////////////////////////////////////////////////////////////////////////////

	SubMatrix(ConnectionViewerPanel c, int nrMatrix, int ofMatrices) 
	{
		symmMatrix = new amgframework.SparseGraph();
		cv = c;
		selected = new ArrayList<Integer>();
		float f = (float) nrMatrix / (float) ofMatrices * 0.75f;
		parallelColor = Color.getHSBColor(f, 1.0f, 1.0f);
	}
	
	void release() 
	{
		if (matrix != null) {
			for (int i = 0; i < matrix.length; i++) {
				if(matrix[i] != null)
					matrix[i].clear();
				matrix[i] = null;
			}
		}
		if (marks != null) {
			marks.clear();
		}
		if (values != null) {
			values.clear();
		}
		matrix = null;
		marks = null;
		values = null;
		if (pos != null) {
			for (int i = 0; i < pos.length; i++) {
				pos[i] = null;
			}
		}
		pos = null;
	}

	int getDimension() {
		return iDimensions;
	}

	Rectangle3D getBounds() {
		return bounds;
	}
	////////////////////////////////////////////////////////////////////////////
	// selection
	
	public void setNeighborhood(int neigh) {
		if(!bSuccessfullyLoaded) return;
		if (iNeighborhood != neigh) {
			iNeighborhood = neigh;
			selectNeighborhood();
		}
	}

	void examineNeighborhood(int i, int untilDepth) {
		if(!bSuccessfullyLoaded) return;
		if (untilDepth <= 0) {
			return;
		}

		for(amgframework.SparseGraph.IConnection con : symmMatrix.row(i))
		{		
			bShow[con.index()] = true;
			examineNeighborhood(con.index(), untilDepth - 1);
		}
	}

	void selectNeighborhood(int iSelectedNode) {
		if(!bSuccessfullyLoaded) return;
		if (iSelectedNode != -1) {
			examineNeighborhood(iSelectedNode, iNeighborhood);
		}
	}

	void selectNeighborhood() {
		if(!bSuccessfullyLoaded) return;
		if (iNeighborhood == 0) {
			for (int i = 0; i < iNrOfNodes; i++) {
				bShow[i] = true;
			}
		} else {
			for (int i = 0; i < iNrOfNodes; i++) {
				bShow[i] = false;
			}
		}
		for (int i = 0; i < selected.size(); i++) {
			selectNeighborhood(selected.get(i));
		}
	}
	
	boolean isVisible(int i)
	{
		double xmin = (bounds.width)*cv.xminFactor*0.99 + bounds.x;
		double xmax = (bounds.width)*cv.xmaxFactor*1.01 + bounds.x;
		
		double ymin = (bounds.height)*cv.yminFactor*0.99 + bounds.y;
		double ymax = (bounds.height)*cv.ymaxFactor*1.01 + bounds.y;
		
		double zmin = (bounds.zmax-bounds.zmin)*cv.zminFactor*0.99 + bounds.zmin;
		double zmax = (bounds.zmax-bounds.zmin)*cv.zmaxFactor*1.01 + bounds.zmin;
			
		return bShow[i] == true && 
				xmin <= pos[i].x && xmax >= pos[i].x &&
				ymin <= pos[i].y && ymax >= pos[i].y &&
				zmin <= pos[i].z && zmax >= pos[i].z;
	}

	boolean select(Point p) {
		if(!bSuccessfullyLoaded) return false;
		double dmin = 9;
		boolean bSelected = false;
		if(!selected.isEmpty()) bSelected = true;
		//clearSelection();
		for (int i = 0; i < iNrOfNodes; i++) 
		{
			if(!isVisible(i)) continue;
			Point p2 = cv.TranslatePoint(pos[i]);
			double d = p2.distance(p);
			if (d < 9) {
				selectNode(i);
				bSelected = true;
			}
		}

		return bSelected;
	}
	public ArrayList<Integer> get_selected()
	{
		if(!bSuccessfullyLoaded) return new ArrayList<Integer>();
		return selected;
	}
	public void set_selected(ArrayList<Integer> s)
	{
		if(!bSuccessfullyLoaded) return;
		selected = s;
	}
	
	public void clearSelection() {
		selected.clear();
	}

	public boolean selectNode(int i) {
		if(!bSuccessfullyLoaded) return false;
		if (i < 0 || i > iNrOfNodes) {
			i = -1;
		} else {
			selected.add(new Integer(i));
			selectNeighborhood(i);
		}
		return i != -1;
	}

	public String getSelectionString() {
		if(!bSuccessfullyLoaded) return "";
		String s = "";
		for (int i = 0; i < selected.size(); i++) {
			int iSelectedNode = selected.get(i).intValue();

			if (cv.matrices.length > 1) {
				s = s + name + "\n";
			}
			s = s + "node " + iSelectedNode + "\npos: [ " + pos[iSelectedNode].x + " | " + pos[iSelectedNode].y;
			if (iDimensions == 3) {
				s = s + " | " + pos[iSelectedNode].z;
			}
			s = s + " ]\n" + matrix[iSelectedNode].size() + " connections to:\n";
			for (int j = 0; j < matrix[iSelectedNode].size(); j++) {
				connection c = matrix[iSelectedNode].get(j);
				String s2 = "" + c.to + ": " + c.value + "\n";
				s += s2;
			}
		}
		return s;
	}

	void zoomToSelection() {
		for (int i = 0; i < selected.size(); i++) 
		{
			int iSelectedNode = selected.get(i).intValue();				
			cv.TranslateDx = -pos[iSelectedNode].x + cv.globalBounds.getCenterX();
			cv.TranslateDy = -pos[iSelectedNode].y  + cv.globalBounds.getCenterY();
		}
		cv.repaint();
	}
	
	////////////////////////////////////////////////////////////////////////////
	// painting

	void drawArrow(Graphics g, int x1, int y1, int x2, int y2, int ARR_SIZE, double scale) {
		//Graphics2D g = (Graphics2D) g1.create();

		double dx = x2 - x1, dy = y2 - y1;
		double angle = Math.atan2(dy, dx);
		double len = Math.sqrt(dx * dx + dy * dy)*scale;
		if(len < 1) return;
		AffineTransform at = AffineTransform.getTranslateInstance(x1, y1);
		at.concatenate(AffineTransform.getRotateInstance(angle));
		Point2D p0 = new Point2D.Double(0, 0);
		Point2D p1 = new Point2D.Double(len, 0);
		Point2D p2 = new Point2D.Double(len - ARR_SIZE, -ARR_SIZE);
		Point2D p3 = new Point2D.Double(len - ARR_SIZE, +ARR_SIZE);

		at.transform(p0, p0);
		at.transform(p1, p1);
		at.transform(p2, p2);
		at.transform(p3, p3);

		//g.transform(at);

		// Draw horizontal arrow starting in (0, 0)
		//g.drawLine(0, 0, len, 0);
		//g.fillPolygon(new int[] {len, len-ARR_SIZE, len-ARR_SIZE, len},
		//              new int[] {0, -ARR_SIZE, ARR_SIZE, 0}, 4);
		g.drawLine((int) p0.getX(), (int) p0.getY(), (int) p1.getX(), (int) p1.getY());
		g.fillPolygon(new int[]{(int) p1.getX(), (int) p2.getX(), (int) p3.getX(), (int) p1.getX()},
				new int[]{(int) p1.getY(), (int) p2.getY(), (int) p3.getY(), (int) p1.getY()}, 4);
	}

	/*void draw_connections(Graphics g, Boolean bArrowConnections, Point[] tpos)
	{
		
		Point p1, p2;
		g.setColor(Color.lightGray);
		for (int i = 0; i < iNrOfNodes; i++) {
			if (!isVisible(i)) {
				continue;
			}
			p1 = tpos[i];
			for (int j = 0; j < matrix[i].size(); j++) {
				connection c = matrix[i].get(j);
				if(i == c.to) continue;

				//if(bDrawZeroConnections == false && c.value.compareTo("0") == 0)
				//  continue;
				p2 = tpos[c.to];
				if (bArrowConnections) {
					drawArrow(g, p1.x, p1.y, p2.x, p2.y, 4, 0.4);
				} else {
					g.drawLine(p1.x, p1.y, p2.x, p2.y);
				}
			}
		}
	}*/
	
	void draw_connections(Graphics g, boolean bDrawConnections, boolean bArrowConnections, boolean bDrawConvection, 
			boolean bDrawDiffusion, Point[] tpos)
	{
		if(bDrawConnections == false && bDrawConvection == false) return;
		
		Point p1, p2;
		g.setColor(Color.lightGray);
		for (int i = 0; i < iNrOfNodes; i++) {
			if (!isVisible(i)) {
				continue;
			}
			p1 = tpos[i];
			double x=0;
			double y=0;
			double ddsum=0;
			double dmax=0; 
			g.setColor(Color.lightGray);
			double diag=1;
						
			if(bDrawConvection || bDrawDiffusion) 
			{
				for (int j = 0; j < matrix[i].size(); j++)				
				{
					connection c = matrix[i].get(j);
					
					if(!bShow2[c.to]) continue;
					double dd = c.getDoubleValue();
					if(Double.isNaN(dd))
					{
						bDrawConvection = bDrawDiffusion = false;
						break;
					}
					if(i == c.to) { diag = dd; continue; }
					if(dd > 0) continue;
					x -= dd*(tpos[c.to].x-p1.x);
					y -= dd*(tpos[c.to].y-p1.y);
					ddsum -= dd;
					dmax = Math.max(Math.abs(dd), Math.abs(dmax));
				}
			}
				
			if(bDrawConnections)
			{
				for (int j = 0; j < matrix[i].size(); j++) {
					connection c = matrix[i].get(j);
					if(i == c.to) continue;
					if(!bShow2[c.to]) continue;

					if(bDrawDiffusion)
					{
						double dd = c.getDoubleValue();					
						float f = 1-Math.abs((float)(dd/dmax));
						if(f >= 0 && f <= 1)
							g.setColor(new Color(f, f, f));
						else
							g.setColor(Color.lightGray);
					}
					p2 = tpos[c.to];
					if (bArrowConnections) {
						drawArrow(g, p1.x, p1.y, p2.x, p2.y, 4, 0.4);
					} else {
						g.drawLine(p1.x, p1.y, p2.x, p2.y);
					}
				}
			}

			if(bDrawConvection && Math.abs(ddsum/dmax)>0.01)
			{
				g.setColor(Color.red);
				ddsum/=cv.getArrowSize();
				x = x/ddsum*2;
				y = y/ddsum*2;
//				x/=diag*30;
//				y/=diag*30;
				drawArrow(g, p1.x, p1.y, (int)(p1.x+x), (int)(p1.y+y), 4, 1.0);
			}
		}
		g.setColor(Color.lightGray);
	}
	
	
	void draw_markers(Graphics g, Point[] tpos)
	{
		Point p1;
		// draw markes
		for (int k = 0; k < marks.size(); k++) {
			MarkStruct m = marks.get(k);
			g.setColor(new Color(m.red, m.green, m.blue, m.alpha));
			int s = m.size;
			for (int i = 0; i < iNrOfNodes; i++) {
				if (isVisible(i) == false || marks.get(k).marks[i] == false) {
					continue;
				}
				p1 = tpos[i];
				int w = 5+s;
				g.fillRect(p1.x -w, p1.y-w, 2*w, 2*w);
			}
		}
	}
	
	
	public boolean has_values()
	{
		return values.size() >= 1 && values.get(0).val != null;
	}
		
	
	void draw_visible_nodes(Graphics g, int fh, boolean bShowParallelNodes, boolean bDrawEmptyNodes, boolean bPrintNumbersInWindow, Point[] tpos)
	{
		Point p1, p2;
		Point3D pa = new Point3D();
		ValueStruct v = null;
		if (values.size() >= 1) {
			v = values.get(0);
		}

		boolean bDrawColor = bVec && maxValue-minValue != 0.0;
		g.setColor(Color.black);
		for (int i = 0; i < iNrOfNodes; i++) {
			if (!isVisible(i))	continue;
			p1 = tpos[i];
			
			if (bDrawColor) {
				double d = (v != null && v.val != null) ? 
						(v.get_double_value(i)-cv.globalMinValue)  / (cv.globalMaxValue-cv.globalMinValue)
						: (pos[i].z - cv.globalBounds.zmin) / (cv.globalBounds.zmax - cv.globalBounds.zmin);				
				g.setColor(Color.getHSBColor((float) (d * 0.8f), 1.0f, 1.0f));
			}

			if (bShowParallelNodes) {
				g.setColor(parallelColor);
			}

			if (matrix[i].isEmpty()) {
				if (bDrawEmptyNodes == false) {
					continue;
				}
				g.fillRect(p1.x - 1, p1.y - 1, 2, 2);
			} else {
				g.fillRect(p1.x - 2, p1.y - 2, 5, 5);
			}

			if (bPrintNumbersInWindow) {
				g.drawString(Integer.toString(i), p1.x, p1.y + fh);
			}

			if (bUseArrows && v != null && v.val != null)
			{
				pa.x = pos[i].x + v.val[i].x *arrowScale;
				pa.y = pos[i].y + v.val[i].y * arrowScale;
				pa.z = pos[i].z + v.val[i].z * arrowScale;
				p2 = cv.TranslatePoint(pa);
				drawArrow(g, p1.x, p1.y, p2.x, p2.y, 4, 0.9);
//				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}
	}
	
	public static String repeat(char ch, int size) 
	{
		char[] c = new char[size];
		for (int i = 0; i < size; i++) {
			c[i] = ch;
		}
		return new String(c);
	}
	
	
	int myDoubleStringLength(double d)
	{
		String s = Double.toString(d);
		return s.length();
	}
	
	String myDoubleToString(double d, int maxLength)
	{
		// 10
		String s = Double.toString(d);
		if(s.length() <= maxLength)
			return repeat(' ', maxLength-s.length()) + s;
		else
			return String.format(d >= 0 ? "+%6.3e" : "%6.3e",d);
	}
	
			
	
	void draw_selected_node(Graphics g, Graphics2D g2, int fh, boolean bArrowConnections,
			boolean bPrintEntriesInWindow, boolean bPrintNumbersInWindow, Point[] tpos)
	{
		Point p1,p2;
		for (int i = 0; i < selected.size(); i++)
		{
			int iSelectedNode = selected.get(i).intValue();
			g2.setStroke(ConnectionViewerPanel.wideStroke);

			g.setColor(Color.blue);
			p1 = tpos[iSelectedNode];
			//if(bDrawConnections)
			{
				for (int j = 0; j < matrix[iSelectedNode].size(); j++) {
					connection c = matrix[iSelectedNode].get(j);
					p2 = tpos[c.to];
					if(!bShow2[c.to]) continue;

					if (bArrowConnections) {
						drawArrow(g, p1.x, p1.y, p2.x, p2.y, 4, 0.6);
					} else {
						g.drawLine(p1.x, p1.y, p2.x, p2.y);
					}

					g.fillRect(p2.x - 1, p2.y - 1, 3, 3);
					if (bPrintEntriesInWindow) {
						//g.drawString(c.value, p2.x - 5, p2.y - 7);
						String s = c.getString();
						String[] bla = s.split("\n");
						for (int k = 0; k < bla.length; k++) {
							g.drawString(bla[k], p2.x - 5,
									p2.y - 5 - (bla.length - 1) * fh + fh * k);
						}
					}
				}
			}
			if (bPrintNumbersInWindow) {
				g.drawString(Integer.toString(iSelectedNode), p1.x, p1.y + fh);
			}
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(p1.x - 4, p1.y - 4, 8, 8);
		}
	}
	
	
	void paint(Graphics g, boolean bPrintEntriesInWindow,
			boolean bPrintNumbersInWindow,
			boolean bShowParallelNodes, boolean bDrawConnections, boolean bArrowConnections,
			boolean bDrawConvection, boolean bDrawDiffusion,
			int fontsize)
	{
		
		arrowScale = maxValue !=0 ? cv.getArrowSize()* minNeighborDist / maxValue * 1.0
				: maxValue;
				
		if (!cv.fileLoaded || !bSuccessfullyLoaded) {
			return;
		}
		if(bShow2==null) bShow2=bShow;
		
		Point[] tpos = new Point[iNrOfNodes];
		for(int i=0; i<iNrOfNodes; i++)
			tpos[i] = cv.TranslatePoint(pos[i]);
		
				
		Font f = new Font(Font.MONOSPACED, Font.PLAIN, fontsize);
		g.setFont(f);
		Graphics2D g2 = (Graphics2D) g;
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(ConnectionViewerPanel.stroke);

		FontMetrics fm = g.getFontMetrics();
		int ascent = fm == null ? 0 : fm.getAscent();
		int fh = fm == null ? 10 : ascent + fm.getDescent();
		int space = fm == null ? 0 : fm.stringWidth(" ");
		// draw Connections
		
		draw_connections(g, bDrawConnections, bArrowConnections, bDrawConvection, bDrawDiffusion, tpos);

		if (!bShowParallelNodes) draw_markers(g, tpos);

		
		// draw visible nodes
		draw_visible_nodes(g, fh, bShowParallelNodes, true, bPrintNumbersInWindow, tpos);
		
		// draw selected node/connections
		draw_selected_node(g, g2, fh, bArrowConnections, bPrintEntriesInWindow, bPrintNumbersInWindow, tpos);
		
	}

	public boolean isVec() {
		return bVec;
	}

	///////////////////////////////////////////////////////////////////////////
	// read file
	
	public class ReadFileThreadClass implements Runnable 
	{

		CountingStreamReader f;
		String filename;
		long fsize;

		public ReadFileThreadClass(CountingStreamReader f, String filename, long fsize) {
			this.f = f;
			this.filename = filename;
			this.fsize = fsize;
		}

		public void run() {
			readFile(f, filename, fsize);
		}
	}
	
	Thread readFileThread =null;
	
	public void waitForReadingDone()
	{
		if(readFileThread != null)
			try
			{				
				readFileThread.join();
			}
			catch (InterruptedException ex)
			{
				Logger.getLogger(SubMatrix.class.getName()).log(Level.SEVERE, null, ex);
			}
		readFileThread = null;
	}

	public boolean readFilePar(TarFile.TarComponent component) throws FileNotFoundException, IOException
	{
		filename = component.filename;
		long fsize = component.size;
		CountingStreamReader f = component.getStream();
		ReadFileThreadClass t = new ReadFileThreadClass(f, filename, fsize);
		readFileThread = new Thread(t);		
		readFileThread.start();
		//readFile(f, filename, fsize);
		return true;
	}
	
	public boolean readFilePar(File file) {
		filename = file.getPath();
		long fsize = file.length();
		CountingStreamReader f;
			try {
			f = new CountingStreamReader(new FileReader(filename));
		} catch (IOException e) {
			System.out.println("File " + filename + "not found! (" + e.toString() + ")");
			return false;
		}
		if(true)
		{
			ReadFileThreadClass t = new ReadFileThreadClass(f, filename, fsize);
			readFileThread = new Thread(t);
			readFileThread.start();
		}
		else
			readFile(f, filename, fsize);
		return true;
	}

	void updateStatus(long a, long b) {
		double d = (double) a / (double) b;
		if (d == 0 || d == 1) {
			cv.updateStatus(this, d);
			oldStatus = d;
		} else if (d > oldStatus + 0.2) {
			cv.updateStatus(this, d);
			oldStatus = d;
		}
	}

	public static double MyParseDouble(String s) 
	{
		if (s.compareTo("nan") == 0 || s.compareTo("-nan") == 0) {
			return Double.NaN;
		}
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}

	}
	
	
	int numFct = -1;
	String[] fctNames;
	int[] fctIndex;
	public boolean readIndices(String filename)
	{
		
		numFct = -1;
		try{
			filename = filename + ".indices";
			if(new File(filename).exists() == false) return false;
			CountingStreamReader f;
			f = new CountingStreamReader(new FileReader(filename));

			String line = f.readLine();
			String s[] = line.split(" ");
			if(s.length != 2) return false;
			numFct = Integer.parseInt(s[1]);
			fctNames = new String[numFct];

			for(int i=0; i<numFct; i++)
				fctNames[i] = f.readLine();
			
			fctIndex = new int[iNrOfNodes];

			for(int i=0; i<iNrOfNodes; i++)
				fctIndex[i] = Integer.parseInt(f.readLine());
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * 
	 * @param file
	 * @return 
	 */
	public boolean readFile(CountingStreamReader f, String filename, long fsize)
	{		
		bVec = filename.substring(filename.lastIndexOf('.') + 1).equals("vec");
		String line;
		bUseArrows = false;
		Pattern ptn = Pattern.compile("\\n");
		
		

		try {
			if ((line = f.readLine()) == null) {
				return false;
			}
			iConnectionViewerVersion = Integer.parseInt(line);
			System.out.println("ConnectionViewerVersion: " + cv.sConnectionViewerVersion + "(" + iConnectionViewerVersion + ")");
			if (iConnectionViewerVersion != 1) {
				return false;
			}
			if ((line = f.readLine()) == null) {
				return false;
			}			
			iDimensions = Integer.parseInt(line);
			System.out.println("Dimensions: " + iDimensions);
			if ((line = f.readLine()) == null) {
				return false;
			}			
			updateStatus(f.getPos(), fsize);
			iNrOfNodes = Integer.parseInt(line);
			System.out.println("Nr of Nodes: " + iNrOfNodes);
			pos = new Point3D[iNrOfNodes];
			bounds = new Rectangle3D();
			matrix = new ArrayList[iNrOfNodes];
			marks = new ArrayList<MarkStruct>();
			values = new ArrayList<ValueStruct>();
			bShow = new Boolean[iNrOfNodes];
			symmMatrix.resize(iNrOfNodes);


			bUseArrows = false;
			double dNrOfNodes = iNrOfNodes;
			
			////////////////////////////////////////////////////////////////////
			
			
			// read positions
			for (int i = 0; i < iNrOfNodes; i++) {
				bShow[i] = true;
				if ((line = f.readLine()) == null) {
					cv.report("Error while loading " + filename + ", in line "+f.getLine() +": unexpected end in positions");
					return false;
				}				
				updateStatus(f.getPos(), fsize);
//				System.out.println(f.getLine());
				String[] res = line.split(" ");
				pos[i] = new Point3D();
				pos[i].setLocation(MyParseDouble(res[0]), MyParseDouble(res[1]));
				if (iDimensions == 3) {
					pos[i].z = MyParseDouble(res[2]);
				} else {
					//pos[i].z = i % 2;
					pos[i].z = 0;
				}				
				if (i == 0) {
					bounds.setRect(pos[0].getX(), pos[0].getY(), pos[0].getZ(), 0, 0, 0);
				} else {
					bounds.add(pos[i]);
				}
				matrix[i] = new ArrayList<connection>();
			}
			//iDimensions=3;

			/*for(int i=0; i<iNrOfNodes; i++)
			 System.out.println("Pos[" + i + "] = { " + pos[i].x + ", " + pos[i].y + " }");
			 */
			if ((line = f.readLine()) == null) {
				cv.report("Error while loading " + filename + ", in line "+f.getLine() +": unexpected end after positions");
				return false;
			}
			updateStatus(f.getPos(), fsize);

			// ShowInWindow
			cv.iShowInWindow = Integer.parseInt(line);

			////////////////////////////////////////////////////////////////////
			// read matrix values
			
			while ((line = f.readLine()) != null) {
				updateStatus(f.getPos(), fsize);
				String[] res = line.split(" ");
				if (res[0].equals("c") || res[0].equals("v")) {
					break;
				}
				connection c = new connection();
				int from = Integer.parseInt(res[0]);
				c.to = Integer.parseInt(res[1]);
				//c.value = //ptn.matcher(res[2]).replaceAll("\n");
				//	res[2].replaceAll("\\\\n", "\n");
				c.value = res[2];
				for (int i = 3; i < res.length; i++) {
					c.value += " " + res[i];
				}
				matrix[from].add(c);
				symmMatrix.set_connection(from, c.to);
				symmMatrix.set_connection(c.to, from);
			}
			

			////////////////////////////////////////////////////////////////////
			// read marks/values

			while (line != null) {
				String[] res = line.split(" ");
				line = f.readLine();
				//filePos += line.length(); updateStatus(filePos, fsize);
				BufferedReader f2;
				try {
					res[1] = FileUtil.GetFileInSamePath(filename, res[1]);
					f2 = new BufferedReader(new FileReader(res[1]));
				} catch (IOException ex) {
					System.out.println("Could not fine file " + res[1]);
					continue;
				}
				String line2;
				if (res[0].equals("c")) {
					MarkStruct m = new MarkStruct();
					m.read(res, f2, iNrOfNodes);					
					marks.add(m);					
				}
				else if (res[0].equals("v"))
				{
					ValueStruct v2 = new ValueStruct(iNrOfNodes);
					v2.read(res, f2);
					
					values.clear();
					values.add(v2);
					bVec = true;
				}
				f2.close();
			}

			init_values();
			calculate_min_neighbor_dist();

			int slash = filename.lastIndexOf("/");
			if (slash == -1)
				name = filename;
			else
				name = filename.substring(slash + 1);

			if (bVec) iDimensions = 3;
			f.close();
			
			readIndices(filename);

			System.out.println(filename + " read.");
			cv.report(filename + " read.");
			bSuccessfullyLoaded = true;
			cv.updateStatus(this, 1.0);
			
			
			
		} 
		catch (Exception e) {
//		catch (IOException e) {
			System.out.println(e.toString());
			cv.report("Error while loading " + filename + ", in line "+f.getLine() +": " + e.toString());
			//cv.updateStatus(this, 1.0);
			return false;
		}
		return true;
	}
	
	void reset_matrix_values()
	{
		for (int i = 0; i < iNrOfNodes; i++)
		{
			for (int j = 0; j < matrix[i].size(); j++)
			{
				connection c = matrix[i].get(j);
				c.mCalculated=false;
				c.valueCalculated=false;
				c.str = null;
			}
		}
	}
	
	public boolean get_conv_diff_drawable()
	{
		
		return true;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// value handling
	
	
	void init_values()
	{
		//bUseArrows = true;
		if(values.size() == 0 && bVec && (iDimensions == 2 || iDimensions == 3))
		{
			ValueStruct v=new ValueStruct(iNrOfNodes);
			v.name = "";
			
			for (int i = 0; i < iNrOfNodes; i++)
				for (int j = 0; j < matrix[i].size(); j++)
					if(i == matrix[i].get(j).to)
						v.vs[i] = matrix[i].get(j).value;
			values.clear();
			values.add(v);
		}
		if(values.size() == 0) return;
		

		bounds = null;
		ValueStruct v = values.get(0);
		
		v.parse_values();
		calculate_values();
		
		icomponents = v.get_nr_of_components();
		if(icomponents > 1 && rcomp == -1) bUseArrows = true;
		else bUseArrows = false;
	}
	/**
	 * Each node can have a associated value string.
	 * this function calculates the used value out of the value string.
	 * examples:
	 * vs[i] = "3.0" -> val[i].x = 3.0; val[i].y = val[i].z = 0.0
	 * vs[i] = "[1.5 2.0]" -> val[i].x = 1.5; val[i].y = 2.0; val[i].z = 0.0
	 * 
	 * if dimension = 2, we create a 3d vector by setting pos[i].z = val[i].length()
	 * 
	 * @todo we might want to be able to display only component 1 or only component 2
	 * part of this is implemented, we just need functionality to switch that
	 */
	void calculate_values()
	{
		if(values.size() <= 0) return;
		ValueStruct v = values.get(0);
		v.set_comp(rcomp);
		
		maxValue = v.get_max_value();
		minValue = v.get_min_value();
		
		for (int i = 0; i < iNrOfNodes; i++)
		{			
			if(iDimensions == 2)
				pos[i].z = v.get_double_value(i);			
			double z=pos[i].z;
			if (Double.isNaN(z))
				z=0;
			if (bounds == null)
			{
				bounds = new Rectangle3D();
				bounds.setRect(pos[i].getX(), pos[i].getY(), z, 0, 0, 0);
			} else {
				bounds.add(pos[i].getX(), pos[i].getY(), z);
			}
		}
		if(iDimensions == 2) iDimensions=3;
		

		System.out.println("maxValue = " + maxValue);		
	}
	
	int num_fct()
	{
		return numFct;
	}
	
	String function_name(int i)
	{
		return fctNames[i];
	}

	void set_comp(int rcomp, int ccomp)
	{
		if(numFct != -1)
		{
			selectNeighborhood();
			bShow2 = bShow.clone();
			maxValue = 1e-40;
			minValue = 1e40;
			if(rcomp != -1)
			{				
				ValueStruct v = values.isEmpty() ? null : values.get(0);
				for(int i=0; i<bShow.length; i++)
				{
					if(fctIndex[i] != rcomp) bShow[i] = false;
					else if(v != null)
					{
						maxValue = Math.max(maxValue, v.get_double_value(i));
						minValue = Math.min(maxValue, v.get_double_value(i));
					}
					if(fctIndex[i] != ccomp) bShow2[i] = false;
				}
			}
		}
		else
		{			
			if(this.rcomp != rcomp || this.ccomp != ccomp)
			{
				this.rcomp = rcomp;
				this.ccomp = ccomp;
				calculate_values();
				calculate_min_neighbor_dist();
				reset_matrix_values();				
				// recalc matrix information
			}
			
		}
		
		if(icomponents > 1 && rcomp == -1) bUseArrows = true;
		else bUseArrows = false;
	}
	/**
	 * for arrow display, we need the min_neighbor dist.
	 */
	void calculate_min_neighbor_dist()
	{		
		minNeighborDist = 10000;
		for (int i = 0; i < iNrOfNodes; i++)
		{
			if (Double.isNaN(pos[i].z)) {
				continue;
			}
			if(matrix[i].size() <= 1)
			{
				for(int j=0; j<Math.min(iNrOfNodes, 1000); j++) // don't search all
				{
					if (Double.isNaN(pos[j].z) || i == j)
						continue;

					double d = pos[i].distance(pos[j]);
					if (d != 0.0) 
						minNeighborDist = Math.min(d, minNeighborDist);
				}
			}

			for (int j = 0; j < matrix[i].size(); j++)
			{
				connection c = (connection) matrix[i].get(j);
				if (Double.isNaN(pos[c.to].z))
					continue;
				double d = pos[i].distance(pos[c.to]);
				if (d != 0.0) 
					minNeighborDist = Math.min(d, minNeighborDist);
			}
		}
	}

	/**
	 * if pos[i].z is NaN, we set it to 2*globalBounds.max
	 * That can happen if a value is NaN, and we create
	 * a 3d-vector out of a 2d-vector with z = value.
	 * This way we can easily detect NaNs in 2d vectors.
	 * @return true if we have NaNs
	 */
	public Boolean postprocess_NaNs()
	{		
		Boolean hasNaN=false;
	    for(int i=0; i<iNrOfNodes; i++)
		{
			if(Double.isNaN(pos[i].z))
			{
				pos[i].z = 2*cv.globalBounds.zmax;
				hasNaN=true;
			}
		}
		return hasNaN;			
		
	}
}
