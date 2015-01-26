/*
 Copyright (c) 2011-2015, Martin Rupp, University Frankfurt
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
 */

/*
 * ConnectionViewerPanel.java
 *
 * todo: toselection fix
 *
 * @author Martin Rupp
 * @email martin.rupp@gcsc.uni-frankfurt.de
 */
package connectionviewer;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import de.erichseifert.vectorgraphics2d.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarFile;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

/**
 * /**
 *
 * @author mrupp Martin Rupp <martin.rupp@gcsc.uni-frankfurt.de>
 */
public class ConnectionViewerPanel extends javax.swing.JPanel
{
	static final public String sConnectionViewerVersion = 
			"3.32";
			//"3.32b built " + getClassBuildTime();

	private static final long serialVersionUID = 1L;

	JTextArea jTextArea;
	ConnectionViewer cvf;
	private JDialog aboutBox;
	
	public int iShowInWindow;	
	public int iSelectedMatrix;
	public Point lastPoint;
	public boolean bDragged = false;	
	
	public double xminFactor = 0.0, xmaxFactor = 1.0, 
			yminFactor = 0.0, ymaxFactor = 1.0,
			zminFactor = 0.0, zmaxFactor = 1.0;
	
	
	// file information
	public SubMatrix matrices[];
	private int dimension;
	public int iTotalNrOfNodes;
	public int iTotalNrOfConnections;
	public Rectangle3D globalBounds;
	
	// file loading / reloading
	private Timer fileChangeTimer;
	private Boolean reload=false;
	private double[] loadStatus;
	private int oldTotalStatus;	
	private int iDoneReading;	
	private long lastModified;	
	private String sError = "no file loaded.";
	public boolean fileLoaded = false;
	public String filename;
	
	
		
	// for transformation
	double alpha = 0;
	double beta = 0;
	public double scaleZoom;
	public double TranslateDx;
	public double TranslateDy;
	public double zZoom;
	
	// min/max value of values
	double globalMinValue, globalMaxValue;	
	boolean bHasValues;
	
	private int oldWidth = -1;
	private int oldHeight = -1;
	
	// double buffer
	BufferedImage img[];	
	
	final static BasicStroke stroke = new BasicStroke(1.5f);
	final static BasicStroke wideStroke = new BasicStroke(3.0f);	
	
	boolean bVec = false;
	
	
	/**
	 * Creates new form ConnectionViewerPanel
	 */
	public ConnectionViewerPanel()
	{
		initComponents();
			
		jTextArea = new JTextArea(16, 16);
		jTextArea.setEditable(false); // set textArea non-editable
		JScrollPane scroll = new JScrollPane(jTextArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		jSplitPane2.setBottomComponent(scroll);
		

		jPrintNumbersInWindowBox.setSelected(false);
		jPrintEntriesInWindowBox.setSelected(true);
		jDrawConvectionBox.setSelected(false);
		jZcompression.setSelected(true);
		jDrawConnections.setSelected(true);

		jProgressBar.setMaximum(100);
	}
	


	
	////////////////////////////////////////////////////////////////////////////
	// selection save/restore
	ArrayList<ArrayList<Integer>> selection=null;
	void save_selection(int iNrOfMatrices)
	{
		if(reload && matrices != null && iNrOfMatrices == matrices.length)
		{
			selection = new ArrayList<ArrayList<Integer>>();
			
			for(int i=0; i<matrices.length; i++)
			{
				if(matrices[i] != null)
					selection.add(matrices[i].get_selected());
				else
					selection.add(null);
			}
		}
		else selection = null;
	}
	private void restore_selection() 
	{
		if(selection == null) return;
		for(int i=0; i<matrices.length; i++)
			matrices[i].set_selected(selection.get(i));
		selection = null;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// file loading

	
	void report(String s)
	{
		sError = sError + s + "\n";
		repaint();
	}

	private void calcZzoom() {
		if(globalBounds==null) return;
		if (jZcompression.isSelected() && bVec && globalBounds.zmax != globalBounds.zmin)
		{
			zZoom = (globalBounds.getWidth() / 2) / (globalBounds.zmax - globalBounds.zmin) * jZCompressionSlider.getValue()/100.0;
		} else
		{
			zZoom = 1.0;
		}
	}

	
	////////////////////////////////
	/// FILE READING
	
	// openDialog
	/**
	 * Opens up a FileDialog to select .mat, .pmat, .vec, .pvec or .tarmat files,
	 * and then reads it in via readFile
	 * @see  readFile
	 */
	void openDialog()
	{
		if (cvf == null)
		{
			return;
		}
		//Create a file chooser
		FileDialog filediag = new FileDialog(cvf, "Open .mat File");
		filediag.setFile("*.mat;*.pmat;*.tarmat;*.vec;*.pvec");
		filediag.setFilenameFilter(new FileUtil.OnlyEndsWithFilenameFilter(new String[]
		{
			".mat", ".pmat", ".vec", ".pvec", ".tarmat"
		}));
		if (filename != null)
		{
			filediag.setDirectory(FileUtil.GetDirectory(filename));
		}
		filediag.setVisible(true);
		if (filediag.getFile() != null)
		{
			if (fileLoaded == true)
			{
				ConnectionViewer vc = new ConnectionViewer();
				vc.readFile(filediag.getDirectory() + filediag.getFile());
				vc.setVisible(true);
				vc.requestFocus();
			} else
			{
				readFile(filediag.getDirectory() + filediag.getFile());
			}
		}

		filediag.dispose();
		//final JFileChooser fc = new JFileChooser();
		//if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION )
		//  readFile(fc.getSelectedFile());
	}
	
	public void readFile(String str)
	{
		File f = new File(str);
		if(f != null)	readFile(f);
	}
	
	/**
	 * Opens up a file
	 * @param file the file to read
	 * This function will read in the files
	 * - mat and vec:
	 *  create on SubMatrix, and read data in
	 * - pmat and pvec
	 *  read pmat/pvec file in parallel
	 * format is
	 *       #parallelFiles
	 *       parallelFileName1
	 *       parallelFileName2
	 *       parallelFileNameN
	 */
	public boolean readFile(File file)
	{
		sError="loading ..."; repaint();
		
		if(fileChangeTimer != null) fileChangeTimer.cancel();
		System.out.println("loading " + file.getPath());
		fileLoaded = false;
		setTitle("ConnectionViewer " + sConnectionViewerVersion + " - no file loaded.");

		filename = file.getPath();
		lastModified = file.lastModified();		

		int dot = filename.lastIndexOf('.');
		String extension = filename.substring(dot + 1);

		bVec = false;
		iDoneReading = 0;
		jProgressBar.setVisible(true);
		
		if (extension.equals("pmat") || extension.equals("pvec"))
		{
			// file is a parallel file. format is
			/* format is
				*       #parallelFiles
				*       parallelFileName1
				*       parallelFileName2
				*       parallelFileNameN
				*/
			
			iTotalNrOfNodes = 0;

			BufferedReader f;
			try
			{
				f = new BufferedReader(new FileReader(filename));
			} catch (IOException e)
			{
				System.out.println("File " + filename + "not found! (" + e.toString() + ")");
				return false;
			}

			String line;
			try
			{
				// read #parallelFiles
				if ((line = f.readLine()) == null)
				{
					return false;
				}
				
				int iNrOfMatrices = Integer.parseInt(line);
				
				save_selection(iNrOfMatrices);
				
				matrices = new SubMatrix[iNrOfMatrices];
				loadStatus = new double[iNrOfMatrices];
				
				
				setTitle("ConnectionViewer " + sConnectionViewerVersion + " - loading... 0 %");
				jProgressBar.setValue(0);
				
				dimension = -1;
//				for (int i = 0; i < iNrOfMatrices; i++)
//					matrices[i] = new SubMatrix(this, i, iNrOfMatrices);
				for (int i = 0; i < iNrOfMatrices; i++)
				{
					// read paralleFileName[i]
					String name = f.readLine();
					name = FileUtil.GetFileInSamePath(filename, name);
					File submatrixFile = new File(name);
					loadStatus[i] = 0;
					if (submatrixFile.exists())
					{
						matrices[i] = new SubMatrix(this, i, iNrOfMatrices);
						matrices[i].readFilePar(submatrixFile);
					}
					else
					{
						matrices[i] = null;
						loadStatus[i] = 1;
					}
				}
				f.close();
				checkIfParallelReadingDone();
			} catch (Exception ex)
			{
				Logger.getLogger(ConnectionViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
			}
		} 
		else if (extension.equals("tarmat"))
		{
			TarFile t = new TarFile(filename);
			
			iTotalNrOfNodes = 0;

			BufferedReader f = null;
			try
			{
				f = t.getComponent("Stiffness.pmat").getStream();
			}
			catch (FileNotFoundException ex)
			{
				Logger.getLogger(ConnectionViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
			}
			catch (IOException ex)
			{
				Logger.getLogger(ConnectionViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
			}
			

			String line;
			try
			{
				if ((line = f.readLine()) == null)
				{
					return false;
				}
				
				int iNrOfMatrices = Integer.parseInt(line);
				
				save_selection(iNrOfMatrices);
				
				matrices = new SubMatrix[iNrOfMatrices];
				loadStatus = new double[iNrOfMatrices];
				
				
				setTitle("ConnectionViewer " + sConnectionViewerVersion + " - loading... 0 %");
				jProgressBar.setValue(0);
				
				dimension = -1;
				for (int i = 0; i < iNrOfMatrices; i++)
				{
					String name = f.readLine();
					matrices[i] = new SubMatrix(this, i, iNrOfMatrices);
					matrices[i].readFilePar(t.getComponent(name));				
				}
				f.close();
			} catch (IOException ex)
			{
				Logger.getLogger(ConnectionViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
			}
		} 
		
		else
		{
			iTotalNrOfNodes=0;
			save_selection(1);
			matrices = new SubMatrix[1];
			loadStatus = new double[1];
			loadStatus[0] = 0;
			setTitle("ConnectionViewer " + sConnectionViewerVersion + " -  loading... 0 %");
			jProgressBar.setValue(0);
			matrices[0] = new SubMatrix(this, 0, 1);
			matrices[0].readFilePar(new File(filename));

		}
	
		return true;
	}

	void setCVSize(int width, int height)
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
    
	void setCheckBox(CommandLineHelper cl, String param, javax.swing.JCheckBox cb)
	{
		if(cl.HasParam(param))
		{
			int b = cl.GetParamInt(param, 0);
			cb.setSelected(b != 0);
		}
	}
	
	void readArgs(CommandLineHelper cl)
	{
		scaleZoom = cl.GetParamDouble("-scaleZoom", scaleZoom);
		setCheckBox(cl, "-arrowConnections", jArrowConnectionsBox1);
		setCheckBox(cl, "-automaticReload", jAutomaticReloadBox);
		setCheckBox(cl, "-drawConnections", jDrawConnections);
		setCheckBox(cl, "-drawConvection", jDrawConvectionBox);
		setCheckBox(cl, "-drawDiffusion", jDrawDiffusionBox);
		setCheckBox(cl, "-showParallelNodes", jShowParallelNodes);
		
		jArrowSizeSlider.setValue( cl.GetParamInt("-arrowSize", jArrowSizeSlider.getValue()));
		jFontsizeSlider.setValue( cl.GetParamInt("-fontsize", jFontsizeSlider.getValue()));
		jZCompressionSlider.setValue( cl.GetParamInt("-zcompression", jZCompressionSlider.getValue()));
		
		if(cl.HasParam("-exportPDF"))
			ExportToPDF(cl.GetParamString("-exportPDF", "file.pdf"));
		if(cl.HasParam("-exportTex"))
			ExportToTex(cl.GetParamString("-exportTex", "file.tex"));		
			
		repaint();
	}

	private class FileChangeTask extends TimerTask
	{
		public void run()
		{
			File f = new File(filename);
			if(f != null && f.lastModified() != lastModified)
			{
				fileChangeTimer.cancel();
				lastModified = f.lastModified();
				reload=true;
				readFile(f);
			}
		}
	}
	/**
	 * 
	 * @param m
	 * @param d 
	 */
	synchronized void updateStatus(SubMatrix m, double d)
	{
		{
			int i;
			for (i = 0; i < matrices.length; i++)
			{
				if (m == matrices[i])
					break;
			}
			loadStatus[i] = d;
		}

		iDoneReading = 0;
		double totalStatus = 0;
		for (int i = 0; i < loadStatus.length; i++)
		{
			if (loadStatus[i] == 1.0)
				iDoneReading++;
			totalStatus += loadStatus[i];
		}
		totalStatus /= loadStatus.length;
		int iStatus = (int) (totalStatus * 100);
		if (iStatus == 0 || iStatus == 100 || iStatus > jProgressBar.getValue() + 5 || iStatus < jProgressBar.getValue() - 5)
		{
			jProgressBar.setValue(iStatus);
			setTitle("ConnectionViewer " + sConnectionViewerVersion + " - "
					+ " loading... " + iStatus + " % ");
		}
		checkIfParallelReadingDone();
	}
	
	public void waitForReadingDone()
	{
		for (int i = 0; i < matrices.length; i++)
			if (matrices[i] != null)
				matrices[i].waitForReadingDone();		
	}
	synchronized void checkIfParallelReadingDone()
	{
		int toread = 0;
		for (int i = 0; i < matrices.length; i++)
			if (matrices[i] != null)
				toread++;
		if (iDoneReading == toread)
			fileReadingDone();
	}
	
	void calcGlobalBounds()
	{
		globalBounds = new Rectangle3D();
		
		bHasValues = false;
		for (int i = 0; i < matrices.length; i++)
		{
			if (i == 0)
				globalBounds.setRect(matrices[i].getBounds());
			else
				globalBounds.add(matrices[i].getBounds());
						
			if(matrices[i].has_values())
			{
				if(bHasValues==false)
				{
					globalMaxValue = matrices[i].maxValue;
					globalMinValue = matrices[i].minValue;
					bHasValues = true;
				}
				else
				{
					globalMaxValue = Math.max(matrices[i].maxValue, globalMaxValue);
					globalMinValue = Math.min(matrices[i].minValue, globalMinValue);
				}
			}
		}
	}
	
	void fileReadingDone()
	{
		// matrices2 can be smaller than matrices if some of the matrix files were not found
		SubMatrix matrices2[] = new SubMatrix[iDoneReading];
		for (int i = 0, j = 0; i < matrices.length; i++)
		{
			if (matrices[i] != null)
				matrices2[j++] = matrices[i];
		}
		matrices = matrices2;

		jProgressBar.setVisible(false);
		for (int i = 0; i < matrices.length; i++)
		{
			if (!bVec)
			{
				bVec = matrices[i].isVec();
			}
			
			if(i==0)
				dimension = matrices[i].getDimension();
			else if (dimension != matrices[i].getDimension())
			{
				if (matrices[i].getDimension() == 3)
				{
					dimension = 3;
				}
				System.out.println("Matrix " + filename + " has wrong dimension " + matrices[i].getDimension() + "!");
			}

			iTotalNrOfNodes += matrices[i].iNrOfNodes;
			iTotalNrOfConnections += matrices[i].get_total_nr_of_connections();
		}
		calcGlobalBounds();
		
		// NANs
		Boolean hasNaN=false;
		for (int i = 0; i < matrices.length; i++)
				if(matrices[i].postprocess_NaNs()) hasNaN=true;
		if(hasNaN) globalBounds.zmax*=2;
		
		// connections off if too much nodes/connections
		if (iTotalNrOfNodes > 100000 || iTotalNrOfConnections > 100000)
			jDrawConnections.setSelected(false);

		//
		jShowParallelNodes.setEnabled(matrices.length > 1);

		jZcompression.setEnabled(bVec);
		if(!reload)
		{
			if (bVec && globalBounds.zmax != globalBounds.zmin)
			{
				zZoom = (globalBounds.getWidth() / 2) / (globalBounds.zmax - globalBounds.zmin);
			} else
			{
				zZoom = 1.0;
			}
			iSelectedMatrix = -1;

			System.out.println("globalBounds = " + globalBounds);
			rezoom();

		}
		else 
			restore_selection();
		reload = false;


		fileLoaded = true;
		setTitle("ConnectionViewer " + sConnectionViewerVersion + " - "
				+ filename + " (" + matrices.length + " files, total " + iTotalNrOfNodes + " nodes, " + iTotalNrOfConnections + " Connections)");
		
		if(jAutomaticReloadBox.isSelected())
		{
			fileChangeTimer = new Timer();
			fileChangeTimer.schedule(new FileChangeTask(), 5000, 1000);
		}
		else fileChangeTimer = null;
		
		boolean bConvDiffDrawable = true;
		for(int i=0; i<matrices.length && bConvDiffDrawable; i++)
			bConvDiffDrawable = matrices[i].get_conv_diff_drawable();
		if(!bConvDiffDrawable)
		{
			jDrawDiffusionBox.setSelected(false);
			jDrawDiffusionBox.setEnabled(false);
			jDrawConvectionBox.setSelected(false);
			jDrawConvectionBox.setEnabled(false);
		}
		int icomponents = matrices[0].get_components();
		for(int i=1; i<matrices.length; i++)
			icomponents = Math.min(matrices[i].get_components(), icomponents);
		
		
		String arr[];
		if(matrices[0].num_fct() != -1)
		{
			icomponents = matrices[0].num_fct();
			arr = new String[icomponents*icomponents+1];
			arr[0] = "all comp";
			for(int r=0; r<icomponents; r++)
				for(int c=0; c<icomponents; c++)
					arr[r+icomponents*c+1] = "("+matrices[0].function_name(r)+", "+matrices[0].function_name(c)+")";
		}
		else
		{
			icomponents = 3;
			arr = new String[icomponents*icomponents+1];
			arr[0] = "all comp";
			for(int r=0; r<icomponents; r++)
				for(int c=0; c<icomponents; c++)
					arr[r+icomponents*c+1] = "("+(r+1)+", "+(c+1)+")";
		}
		
		int oldIndex=jComponentList.getSelectedIndex();
		int oldLength=jComponentList.getModel().getSize();
		jComponentList.setModel(new javax.swing.DefaultComboBoxModel(arr));
		if(oldLength == jComponentList.getModel().getSize())
			jComponentList.setSelectedIndex(oldIndex);		
		repaint();
	}
	
	

	////////////////////////////////////////////////////////////////////////////
	// translating/zoom
			
	void zoomToSelection()
	{
		if (iSelectedMatrix != -1)
		{
			matrices[iSelectedMatrix].zoomToSelection();
		}
	}

	void rezoom()
	{
		alpha = 0;
		beta = 0;
		Dimension d = jConnectionDisplay.getSize();
		TranslateDx = 0;
		TranslateDy = 0;
		System.out.println("Translate: " + TranslateDx + ", " + TranslateDy);

		scaleZoom = 0.9;
		System.out.println("TranslateDx = " + TranslateDx + " TranslateDy=" + TranslateDy + " Zoom = " + scaleZoom);
		jConnectionDisplay.repaint();
	}
	
	Dimension getDrawingSize() {
		return jConnectionDisplay.getSize();
	}
	
	/*private void selectNode(int i)
	 {
	 /* if(!fileLoaded || i < 0 || i >= iTotalNrOfNodes) return;
	 iSelectedNode = i;

	 String s = "node " + i + "\npos: [ " + pos[i].x + " | " + pos[i].y + " | " + 0 + " ]\n"
	 + matrix[i].size() + " connections to:\n";

	 for(int j=0; j<matrix[i].size(); j++)
	 {
	 connection c= (connection) matrix[i].get(j);
	 String s2 = "" + c.to + ": " + c.value + "\n";
	 s += s2;
	 }
	 jTextArea.setText(s);
	 if(jNeighborhoodList.getSelectedIndex()!=0) selectNeighborhood();
	 jConnectionDisplay.repaint();
	 }*/

	public Point TranslatePoint(Point3D p)
	{
		Dimension d = jConnectionDisplay.getSize();
		double dzoom = Math.min(d.width / globalBounds.getWidth(), d.height / globalBounds.getHeight());
		if (dimension == 2)
		{

			double x, y;
			x = p.x + TranslateDx - globalBounds.getCenterX();
			x *= scaleZoom * dzoom;
			x += d.width / 2;
			y = p.y + TranslateDy - globalBounds.getCenterY();
			y *= scaleZoom * dzoom;
			y = d.height / 2 - y;

			Point np = new Point((int) x, (int) y);
			return np;
		} else
		{
			double x = p.x - globalBounds.getCenterX();
			double y = p.y - globalBounds.getCenterY();
			double z = (p.z - globalBounds.getCenterZ()) * zZoom;

			Point3D p2 = new Point3D();
			p2.x = x;
			p2.y = Math.cos(alpha) * y - Math.sin(alpha) * z;
			p2.z = Math.sin(alpha) * y + Math.cos(alpha) * z;

			p2.x = Math.cos(beta) * p2.x - Math.sin(beta) * p2.z;
			//p2.z = Math.sin(beta) * p.z + Math.cos(beta) * p.z;

			p2.x += TranslateDx;
			p2.y += TranslateDy;

			Point np = new Point(
					(int) (p2.x * scaleZoom * dzoom + d.width / 2),
					(int) (d.height / 2 - p2.y * scaleZoom * dzoom));
			return np;
		}

	}
	
	////////////////////////////////////////////////////////////////////////////
	// drawing
	
	void drawmatrix(SubMatrix m, Graphics g)
	{
		m.paint(g,                    
			jPrintEntriesInWindowBox.isSelected(),
			jPrintNumbersInWindowBox.isSelected(),                    
			jShowParallelNodes.isSelected(),
			jDrawConnections.isSelected(),
			jArrowConnectionsBox1.isSelected(),
			jDrawConvectionBox.isSelected(),
			jDrawDiffusionBox.isSelected(),
			jFontsizeSlider.getValue());
	}
	
	void drawmatrix(int i, Graphics g)
	{		
		drawmatrix(matrices[i], g);						
	}
	void drawMinMax(Graphics g)
	{
		if(!bHasValues) return;
		int barHeight=15, barLength=100;
		StringMetrics sm = new StringMetrics(g);
		int move = jConnectionDisplay.getWidth()-barLength-10-sm.getWidth(""+globalMaxValue);
		int h = jConnectionDisplay.getHeight()-3;
		for(int i=0; i<barLength; i++)
		{
			double d = i/((double)barLength);
			g.setColor(Color.getHSBColor((float) (d * 0.8f), 1.0f, 1.0f));
			g.drawLine(move+i, h, move+i, h-barHeight);		
		}
		g.setColor(Color.black);
		
		g.drawString(""+globalMinValue, move-5-sm.getWidth(""+globalMinValue), h);
		g.drawString(""+globalMaxValue, move+barLength+5, h);
	}
	
	void drawAxis(Graphics g)
	{
		Point3D p2 = new Point3D();
		int ix, iy;
		int axisLength=50;
		double factor = 0.9;
		double x, y, z;
		if(dimension == 2) 
		{
			axisLength=15;
			p2.x=30;
			p2.y=0;
			ix = (int)(axisLength+p2.x);
			iy = (int)(getHeight()-axisLength-p2.y);			
			g.drawLine(axisLength, getHeight()-axisLength, ix, iy);
			g.drawString("x", ix, iy);

			p2.x=0;
			p2.y=30;
			ix = (int)(axisLength+p2.x);
			iy = (int)(getHeight()-axisLength-p2.y);			
			g.drawLine(axisLength, getHeight()-axisLength, ix, iy);
			g.drawString("y", ix, iy);
			return;
		}
		
		x=axisLength*factor; y=0; z=0;
		p2.x = x;
		p2.y = Math.cos(alpha) * y - Math.sin(alpha) * z;
		p2.z = Math.sin(alpha) * y + Math.cos(alpha) * z;
		p2.x = Math.cos(beta) * p2.x - Math.sin(beta) * p2.z;
		ix = (int)(axisLength+p2.x);
		iy = (int)(getHeight()-axisLength-p2.y);
		g.drawLine(axisLength, getHeight()-axisLength, ix, iy);
		g.drawString("x", ix, iy);

		x=0; y=0; z=axisLength*factor;
		p2.x = x;
		p2.y = Math.cos(alpha) * y - Math.sin(alpha) * z;
		p2.z = Math.sin(alpha) * y + Math.cos(alpha) * z;
		p2.x = Math.cos(beta) * p2.x - Math.sin(beta) * p2.z;
		ix = (int)(axisLength+p2.x);
		iy = (int)(getHeight()-axisLength-p2.y);
		g.drawLine(axisLength, getHeight()-axisLength, ix, iy);
		g.drawString("z", ix, iy);


		x=0; y=axisLength*factor; z=0;
		p2.x = x;
		p2.y = Math.cos(alpha) * y - Math.sin(alpha) * z;
		p2.z = Math.sin(alpha) * y + Math.cos(alpha) * z;
		p2.x = Math.cos(beta) * p2.x - Math.sin(beta) * p2.z;
		ix = (int)(axisLength+p2.x);
		iy = (int)(getHeight()-axisLength-p2.y);
		g.drawLine(axisLength, getHeight()-axisLength, ix, iy);
		g.drawString("y", ix, iy);
	}
	
	class MyDrawing extends JPanel
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void paint(Graphics g) //paintComponent(Graphics g)
		{
			calcZzoom();
			super.paint(g);
			if (!fileLoaded)
			{
				g.drawString(sError, 10, 10);
				return;
			}	
			
			
			drawAxis(g);
			drawMinMax(g);

			if (matrices == null)
			{
				return;
			}
			Graphics2D g2d = (Graphics2D) g;

			int threads = Math.min(Runtime.getRuntime().availableProcessors() * 2, matrices.length);
			Thread t[] = new Thread[threads];

			if (img == null)
			{
				img = new BufferedImage[matrices.length];
			}
			if (getWidth() != oldWidth || getHeight() != oldHeight)
			{
				for (int i = 0; i < t.length; i++)
				{
					img[i] = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
				}
				oldWidth = getWidth();
				oldHeight = getHeight();
			}


			class myWorker implements Runnable
			{

				int i1, i2, id;

				myWorker(int _id, int _i1, int _i2)
				{
					id = _id;
					i1 = _i1;
					i2 = _i2;
				}

				public void run()
				{
					Graphics2D gc = img[id].createGraphics();
					gc.setBackground(new Color(1.0f, 1.0f, 1.0f, 0.0f));
					gc.clearRect(0, 0, getWidth(), getHeight());
					for (int i = i1; i < i2; i++)
						drawmatrix(i, gc);
						
				}
			}
			int k = 0;
			for (int i = 0; i < t.length - 1; i++)
			{
				t[i] = new Thread(new myWorker(i, k, k + matrices.length / t.length));
				k += matrices.length / t.length;
			}
			t[t.length - 1] = new Thread(new myWorker(t.length - 1, k, matrices.length));

			for (int i = 0; i < t.length; i++)
			{
				t[i].start();
			}

			for (int i = 0; i < t.length; i++)
			{
				try
				{
					t[i].join();
				} catch (InterruptedException ex)
				{
					Logger.getLogger(ConnectionViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			for (int i = 0; i < t.length; i++)
			{
				g.drawImage(img[i], 0, 0, this);
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
		

	void setTitle(String str)
	{
		if (cvf != null)
		{
			cvf.setTitle(str);
		}
	}

	void setFrame(ConnectionViewer cvf)
	{
		this.cvf = cvf;
	}
	
	ConnectionViewer getFrame()
	{
		return cvf;
	}

	/**
	 * This method is called from within the constructor to initialize the form. WARNING:
	 * Do NOT modify this code. The content of this method is always regenerated by the
	 * Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jSplitPane1 = new javax.swing.JSplitPane();
        jConnectionDisplay = new MyDrawing();
        jProgressBar = new javax.swing.JProgressBar();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jDrawConvectionBox = new javax.swing.JCheckBox();
        jPrintEntriesInWindowBox = new javax.swing.JCheckBox();
        jPrintNumbersInWindowBox = new javax.swing.JCheckBox();
        jSearchTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jNeighborhoodList = new javax.swing.JComboBox();
        jRezoomButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        reopenButton = new javax.swing.JButton();
        toSelectionButton = new javax.swing.JButton();
        jShowParallelNodes = new javax.swing.JCheckBox();
        jZcompression = new javax.swing.JCheckBox();
        jDrawConnections = new javax.swing.JCheckBox();
        jAutomaticReloadBox = new javax.swing.JCheckBox();
        label1 = new java.awt.Label();
        jFontsizeSlider = new javax.swing.JSlider();
        jArrowConnectionsBox1 = new javax.swing.JCheckBox();
        jZCompressionSlider = new javax.swing.JSlider();
        jArrowSizeSlider = new javax.swing.JSlider();
        jLabel3 = new javax.swing.JLabel();
        jDrawDiffusionBox = new javax.swing.JCheckBox();
        jComponentList = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButtonParallelReMove = new javax.swing.JButton();

        jSplitPane1.setMinimumSize(new java.awt.Dimension(30, 30));

        jConnectionDisplay.setBackground(new java.awt.Color(255, 255, 255));
        jConnectionDisplay.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jConnectionDisplay.setEnabled(false);
        jConnectionDisplay.addMouseWheelListener(new java.awt.event.MouseWheelListener()
        {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt)
            {
                jConnectionDisplayMouseWheelMoved(evt);
            }
        });
        jConnectionDisplay.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                jConnectionDisplayMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                jConnectionDisplayMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                jConnectionDisplayMouseClicked(evt);
            }
        });
        jConnectionDisplay.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            public void mouseDragged(java.awt.event.MouseEvent evt)
            {
                jConnectionDisplayMouseDragged(evt);
            }
        });

        jProgressBar.setMinimumSize(new java.awt.Dimension(40, 20));
        jProgressBar.setPreferredSize(new java.awt.Dimension(406, 20));
        jProgressBar.setStringPainted(true);
        jConnectionDisplay.add(jProgressBar);

        jSplitPane1.setRightComponent(jConnectionDisplay);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        jSplitPane2.setLeftComponent(jPanel3);

        jPanel2.setPreferredSize(new java.awt.Dimension(120, 427));

        jDrawConvectionBox.setText("Convection");
        jDrawConvectionBox.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jDrawConvectionBoxStateChanged(evt);
            }
        });
        jDrawConvectionBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jDrawConvectionBoxActionPerformed(evt);
            }
        });

        jPrintEntriesInWindowBox.setText("Print Entries in Window");
        jPrintEntriesInWindowBox.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jPrintEntriesInWindowBoxStateChanged(evt);
            }
        });
        jPrintEntriesInWindowBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jPrintEntriesInWindowBoxActionPerformed(evt);
            }
        });

        jPrintNumbersInWindowBox.setText("Print Indices in Window");
        jPrintNumbersInWindowBox.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jPrintNumbersInWindowBoxStateChanged(evt);
            }
        });
        jPrintNumbersInWindowBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jPrintNumbersInWindowBoxActionPerformed(evt);
            }
        });

        jSearchTextField.setToolTipText("enter serach node");
        jSearchTextField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyReleased(java.awt.event.KeyEvent evt)
            {
                jSearchTextFieldKeyReleased(evt);
            }
        });

        jLabel1.setText("Search node:");

        jNeighborhoodList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "all nodes", "Neigh 1", "Neigh 2", "Neigh 3", "Neigh 4" }));
        jNeighborhoodList.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                jNeighborhoodListMouseClicked(evt);
            }
        });
        jNeighborhoodList.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jNeighborhoodListItemStateChanged(evt);
            }
        });
        jNeighborhoodList.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jNeighborhoodListActionPerformed(evt);
            }
        });

        jRezoomButton.setText("recenter");
        jRezoomButton.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                jRezoomButtonMouseClicked(evt);
            }
        });

        openButton.setText("open new");
        openButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                openButtonActionPerformed(evt);
            }
        });

        reopenButton.setText("reopen");
        reopenButton.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                reopenButtonMouseClicked(evt);
            }
        });

        toSelectionButton.setText("to selection");
        toSelectionButton.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                toSelectionButtonMouseClicked(evt);
            }
        });
        toSelectionButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                toSelectionButtonActionPerformed(evt);
            }
        });

        jShowParallelNodes.setText("Parallel Nodes");
        jShowParallelNodes.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jShowParallelNodesStateChanged(evt);
            }
        });
        jShowParallelNodes.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jShowParallelNodesActionPerformed(evt);
            }
        });

        jZcompression.setText("Z compression");
        jZcompression.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jZcompressionStateChanged(evt);
            }
        });

        jDrawConnections.setText("Connections");
        jDrawConnections.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jDrawConnectionsStateChanged(evt);
            }
        });

        jAutomaticReloadBox.setText("Automatic Reload");
        jAutomaticReloadBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jAutomaticReloadBoxActionPerformed(evt);
            }
        });

        label1.setText("Fontsize:");

        jFontsizeSlider.setMajorTickSpacing(5);
        jFontsizeSlider.setMaximum(40);
        jFontsizeSlider.setMinimum(5);
        jFontsizeSlider.setMinorTickSpacing(1);
        jFontsizeSlider.setValue(12);
        jFontsizeSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jFontsizeSliderStateChanged(evt);
            }
        });

        jArrowConnectionsBox1.setText("as Arrows");
        jArrowConnectionsBox1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jArrowConnectionsBox1ActionPerformed(evt);
            }
        });

        jZCompressionSlider.setMaximum(200);
        jZCompressionSlider.setValue(100);
        jZCompressionSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jZCompressionSliderStateChanged(evt);
            }
        });

        jArrowSizeSlider.setMaximum(1000);
        jArrowSizeSlider.setValue(100);
        jArrowSizeSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jArrowSizeSliderStateChanged(evt);
            }
        });

        jLabel3.setText("Arrow Size");

        jDrawDiffusionBox.setText("Diffusion");
        jDrawDiffusionBox.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                jDrawDiffusionBoxStateChanged(evt);
            }
        });
        jDrawDiffusionBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jDrawDiffusionBoxActionPerformed(evt);
            }
        });

        jComponentList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "all comp"}));
        jComponentList.addMouseListener(new java.awt.event.MouseAdapter()
        {
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                jComponentListMouseClicked(evt);
            }
        });
        jComponentList.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(java.awt.event.ItemEvent evt)
            {
                jComponentListItemStateChanged(evt);
            }
        });
        jComponentList.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jComponentListActionPerformed(evt);
            }
        });

        jLabel5.setText("(c) Martin Rupp 2013-2014");

        jLabel6.setText("University Frankfurt");

        jButton1.setText("Clip...");
        jButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Export...");
        jButton2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton2ActionPerformed(evt);
            }
        });

        jButtonParallelReMove.setText("re-move");
        jButtonParallelReMove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButtonParallelReMoveActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jRezoomButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, toSelectionButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jNeighborhoodList, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(jLabel1)))
                .add(8, 8, 8)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(openButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jComponentList, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jSearchTextField)
                    .add(reopenButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(5, 5, 5))
            .add(jPanel2Layout.createSequentialGroup()
                .add(10, 10, 10)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jZCompressionSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jDrawConvectionBox)
                            .add(jDrawConnections, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jArrowConnectionsBox1)
                            .add(jDrawDiffusionBox))
                        .add(8, 8, 8))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPrintNumbersInWindowBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jZcompression, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(label1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel3))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jArrowSizeSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .add(jFontsizeSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addContainerGap())
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 105, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 117, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jAutomaticReloadBox)
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(jShowParallelNodes)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jButtonParallelReMove))))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(46, 46, 46)
                        .add(jLabel6))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(32, 32, 32)
                        .add(jLabel5))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(jPrintEntriesInWindowBox)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(8, 8, 8)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jShowParallelNodes)
                    .add(jButtonParallelReMove))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jDrawConnections)
                    .add(jArrowConnectionsBox1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jDrawDiffusionBox)
                    .add(jDrawConvectionBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPrintEntriesInWindowBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPrintNumbersInWindowBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jZcompression)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jZCompressionSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton1)
                    .add(jButton2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jAutomaticReloadBox)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(18, 18, 18)
                        .add(jLabel3))
                    .add(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jArrowSizeSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(label1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jFontsizeSlider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jSearchTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jNeighborhoodList, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jComponentList, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jRezoomButton)
                    .add(openButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(reopenButton)
                    .add(toSelectionButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel6)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jSplitPane2.setTopComponent(jPanel2);

        jSplitPane1.setLeftComponent(jSplitPane2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1122, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 796, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jConnectionDisplayMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_jConnectionDisplayMouseWheelMoved
		if (!fileLoaded)
			return;
		bDragged = false;
		int i = evt.getWheelRotation();
		double factor;

		if (i < 0)
		{
			factor = 1 / 0.91;
			i = -i;
		} else
		{
			factor = 0.91;
		}
		//while(i-- > 0)
		scaleZoom *= factor;
		jConnectionDisplay.repaint();
    }//GEN-LAST:event_jConnectionDisplayMouseWheelMoved

    private void jConnectionDisplayMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jConnectionDisplayMouseDragged
		if (!fileLoaded)
			return;
		if (lastPoint == null)
		{
			lastPoint = new Point(evt.getX(), evt.getY());
			return;
		}
		Dimension d = jConnectionDisplay.getSize();
		double dzoom = Math.min(d.width / globalBounds.getWidth(), d.height / globalBounds.getHeight());
		bDragged = true;
		int dx = lastPoint.x - evt.getX();
		int dy = evt.getY() - lastPoint.y;

		if ((evt.getModifiers() & MouseEvent.SHIFT_MASK) == MouseEvent.SHIFT_MASK)
		{
			// parallel matrix move
			if(iSelectedMatrix != -1)
			{
				matrices[iSelectedMatrix].move(-dx / (scaleZoom * dzoom), -dy / (scaleZoom * dzoom));
				globalBounds.add(matrices[iSelectedMatrix].getBounds());
			}
		} else
		{			
			if ((evt.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK)
			{
				// rotate
				beta -= (evt.getX() - lastPoint.x) * 0.01;
				alpha += (evt.getY() - lastPoint.y) * 0.01;				
			} else
			{
				// move viewpoint
				TranslateDx -= dx / (scaleZoom * dzoom);
				TranslateDy -= dy / (scaleZoom * dzoom);
			}			
		}
		lastPoint.x = evt.getX();
		lastPoint.y = evt.getY();
		jConnectionDisplay.repaint();
    }//GEN-LAST:event_jConnectionDisplayMouseDragged

    private void jConnectionDisplayMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jConnectionDisplayMouseClicked
		lastPoint = new Point(evt.getX(), evt.getY());
    }//GEN-LAST:event_jConnectionDisplayMouseClicked

    private void jConnectionDisplayMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jConnectionDisplayMouseReleased
		if ((evt.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK)
		{
			return;
		}

		if (bDragged)
		{
			bDragged = false;
			return;
		}
		bDragged = false;
		if (!fileLoaded)
		{
			return;
		}
		lastPoint = null;
		Point p = new Point(evt.getX(), evt.getY());

		if ((evt.getModifiers() & MouseEvent.SHIFT_MASK) != MouseEvent.SHIFT_MASK)
		{
			// if shift is not pressed, clear selection
			for (int i = 0; i < matrices.length; i++)
				matrices[i].clearSelection();
		}
		

		boolean bSelected[] = new boolean[matrices.length];
		String s = "";
		boolean bFound = false;
		for (int i = 0; i < matrices.length; i++)
		{
			if (matrices[i].select(p) == true)
			{
				bFound = true;
				bSelected[i] = true;
				s = s + matrices[i].getSelectionString() + "\n";
				iSelectedMatrix = i;
			} else
			{
				bSelected[i] = false;
			}
		}

		if (!bFound)
		{
			return;
		}

		jTextArea.setText(s);

		jConnectionDisplay.repaint();
    }//GEN-LAST:event_jConnectionDisplayMouseReleased

    private void jConnectionDisplayMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jConnectionDisplayMousePressed
		lastPoint = new Point(evt.getX(), evt.getY());
    }//GEN-LAST:event_jConnectionDisplayMousePressed

    private void jComponentListItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jComponentListItemStateChanged
    {//GEN-HEADEREND:event_jComponentListItemStateChanged
        if (!fileLoaded)
        return;
        int c=(int) Math.sqrt(jComponentList.getItemCount()-1);
        int i=jComponentList.getSelectedIndex()-1;
        int rcomp=-1, ccomp=-1;
        if(i != -1)
        {
            rcomp = i%c;
            ccomp = i/c;
        }

        for (int j = 0; j < matrices.length; j++)
			 matrices[j].set_comp(rcomp, ccomp);
        calcGlobalBounds();
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jComponentListItemStateChanged

    private void jComponentListMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jComponentListMouseClicked
    {//GEN-HEADEREND:event_jComponentListMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jComponentListMouseClicked

	void ExportToTex(String filename)
	{
		FileWriter file;
		try
		{
			file = new FileWriter(filename);
			try
			{
				matrices[0].writeLatex(file);
				file.close();
			} catch (IOException ex)
			{
			}
		} catch (FileNotFoundException ex)
		{
		}
		catch (IOException ex)
		{
			Logger.getLogger(ConnectionViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	public void ExportToTex()
	{
		 if (cvf == null || filename == null || matrices == null || matrices.length == 0)
        {
            return;
        }

        //Create a file chooser
        FileDialog filediag = new FileDialog(cvf, "Save .tex file");
        filediag.setFile("*.tex");
        filediag.setMode(filediag.SAVE);
        filediag.setDirectory(FileUtil.GetDirectory(filename));
        filediag.setFile(FileUtil.GetFilename(filename)+".tex");
        filediag.setVisible(true);

        if (filediag.getFile() != null)
        {
           ExportToTex(filediag.getDirectory() + filediag.getFile());
        }

        filediag.dispose();
	}
    private void jDrawDiffusionBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jDrawDiffusionBoxActionPerformed
    {//GEN-HEADEREND:event_jDrawDiffusionBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jDrawDiffusionBoxActionPerformed

    private void jDrawDiffusionBoxStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jDrawDiffusionBoxStateChanged
    {//GEN-HEADEREND:event_jDrawDiffusionBoxStateChanged
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jDrawDiffusionBoxStateChanged

	public void exportToJulia()
	{
        if (cvf == null || filename == null || matrices == null || matrices.length == 0)
        {
            return;
        }

        //Create a file chooser
        FileDialog filediag = new FileDialog(cvf, "Save .jl file");
        filediag.setFile("*.jl");
        filediag.setMode(filediag.SAVE);
        filediag.setDirectory(FileUtil.GetDirectory(filename));
        filediag.setFile(FileUtil.GetFilename(filename)+".jl");
        filediag.setVisible(true);

        if (filediag.getFile() != null)
        {
            FileWriter file;
            try
            {
                file = new FileWriter(filediag.getDirectory() + filediag.getFile());
                try
                {
                    matrices[0].writeJulia(file);
                    file.close();
                } catch (IOException ex)
                {
                }
            } catch (FileNotFoundException ex)
            {
            }
            catch (IOException ex)
            {
                Logger.getLogger(ConnectionViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        filediag.dispose();
	}
    private void jArrowSizeSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jArrowSizeSliderStateChanged
    {//GEN-HEADEREND:event_jArrowSizeSliderStateChanged
        repaint();
    }//GEN-LAST:event_jArrowSizeSliderStateChanged

    private void jZCompressionSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jZCompressionSliderStateChanged
    {//GEN-HEADEREND:event_jZCompressionSliderStateChanged
        // TODO add your handling code here:
        jZcompressionStateChanged(evt);
    }//GEN-LAST:event_jZCompressionSliderStateChanged

    private void jArrowConnectionsBox1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jArrowConnectionsBox1ActionPerformed
    {//GEN-HEADEREND:event_jArrowConnectionsBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jArrowConnectionsBox1ActionPerformed

    private void jFontsizeSliderStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jFontsizeSliderStateChanged
    {//GEN-HEADEREND:event_jFontsizeSliderStateChanged
        jConnectionDisplay.repaint();
        // TODO add your handling code here:
    }//GEN-LAST:event_jFontsizeSliderStateChanged

    private void jAutomaticReloadBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jAutomaticReloadBoxActionPerformed
    {//GEN-HEADEREND:event_jAutomaticReloadBoxActionPerformed
        if(jAutomaticReloadBox.isSelected() && fileChangeTimer == null)
        {
            fileChangeTimer = new Timer();
            fileChangeTimer.schedule(new FileChangeTask(), 500, 1000);
        }
        else if(jAutomaticReloadBox.isSelected() == false && fileChangeTimer != null)
        {
            fileChangeTimer.cancel();
            fileChangeTimer = null;
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jAutomaticReloadBoxActionPerformed

	void ExportToPDF(String filename)
	{
		PDFGraphics2D g = new PDFGraphics2D(0.0, 0.0, jConnectionDisplay.getWidth(), jConnectionDisplay.getHeight());
		for (int i = 0; i < matrices.length; i++)
			drawmatrix(i, g);
		// Write the PDF output to a file
		FileOutputStream file;
		try
		{
			file = new FileOutputStream(filename);
			try
			{
				file.write(g.getBytes());
			} catch (IOException ex)
			{
			}
		} catch (FileNotFoundException ex)
		{
		}
	}
	public void ExportToPDF()
	{
		if (cvf == null || filename == null || matrices == null || matrices.length == 0)
        {
            return;
        }

        //Create a file chooser
        FileDialog filediag = new FileDialog(cvf, "Save .pdf file");
        filediag.setFile("*.pdf");
        filediag.setMode(filediag.SAVE);
        filediag.setDirectory(FileUtil.GetDirectory(filename));
        filediag.setFile(FileUtil.GetFilename(filename)+".pdf");
        filediag.setVisible(true);

        if (filediag.getFile() != null)
        {
			ExportToPDF(filediag.getDirectory() + filediag.getFile());
            
        }

        filediag.dispose();
	}
    private void jDrawConnectionsStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jDrawConnectionsStateChanged
    {//GEN-HEADEREND:event_jDrawConnectionsStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_jDrawConnectionsStateChanged

    private void jZcompressionStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jZcompressionStateChanged
    {//GEN-HEADEREND:event_jZcompressionStateChanged
        calcZzoom();
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jZcompressionStateChanged

    private void jShowParallelNodesStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jShowParallelNodesStateChanged
    {//GEN-HEADEREND:event_jShowParallelNodesStateChanged
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jShowParallelNodesStateChanged

    private void toSelectionButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_toSelectionButtonActionPerformed
    {//GEN-HEADEREND:event_toSelectionButtonActionPerformed

    }//GEN-LAST:event_toSelectionButtonActionPerformed

    private void toSelectionButtonMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_toSelectionButtonMouseClicked
    {//GEN-HEADEREND:event_toSelectionButtonMouseClicked
        zoomToSelection();
    }//GEN-LAST:event_toSelectionButtonMouseClicked

    private void reopenButtonMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_reopenButtonMouseClicked
    {//GEN-HEADEREND:event_reopenButtonMouseClicked
        reload=true;
        readFile(filename);
    }//GEN-LAST:event_reopenButtonMouseClicked

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_openButtonActionPerformed
    {//GEN-HEADEREND:event_openButtonActionPerformed
        // TODO add your handling code here:
        openDialog();
    }//GEN-LAST:event_openButtonActionPerformed

    private void jRezoomButtonMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jRezoomButtonMouseClicked
    {//GEN-HEADEREND:event_jRezoomButtonMouseClicked
        rezoom();
    }//GEN-LAST:event_jRezoomButtonMouseClicked

    private void jNeighborhoodListActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jNeighborhoodListActionPerformed
    {//GEN-HEADEREND:event_jNeighborhoodListActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jNeighborhoodListActionPerformed

    private void jNeighborhoodListItemStateChanged(java.awt.event.ItemEvent evt)//GEN-FIRST:event_jNeighborhoodListItemStateChanged
    {//GEN-HEADEREND:event_jNeighborhoodListItemStateChanged
        if (!fileLoaded)
        {
            return;
        }
        for (int i = 0; i < matrices.length; i++)
        {
            matrices[i].setNeighborhood(jNeighborhoodList.getSelectedIndex());
            matrices[i].selectNeighborhood();
        }
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jNeighborhoodListItemStateChanged

    private void jNeighborhoodListMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jNeighborhoodListMouseClicked
    {//GEN-HEADEREND:event_jNeighborhoodListMouseClicked

    }//GEN-LAST:event_jNeighborhoodListMouseClicked

    private void jSearchTextFieldKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_jSearchTextFieldKeyReleased
    {//GEN-HEADEREND:event_jSearchTextFieldKeyReleased
        if (!fileLoaded)
        {
            return;
        }
        try
        {
            for (int i = 0; i < matrices.length; i++)
            {
                matrices[i].clearSelection();
            }

            String s1 = jSearchTextField.getText();
            int point = s1.lastIndexOf(".");
            if (point == -1)
            {
                String s = "";
                for (int i = 0; i < matrices.length; i++)
                {
                    if (matrices[i].selectNode(Integer.valueOf(s1).intValue()))
                    {
                        s = s + matrices[i].getSelectionString() + "\n";
                    }
                }
                jTextArea.setText(s);
            } else
            {
                for (int i = 0; i < matrices.length; i++)
                {
                    matrices[i].selectNode(-1);
                }
                int iSelMat = Integer.valueOf(s1.substring(0, point));
                matrices[iSelMat].selectNode(Integer.valueOf(s1.substring(point + 1)));

                jTextArea.setText(matrices[iSelMat].getSelectionString());
            }
            jConnectionDisplay.repaint();
        } catch (NumberFormatException ex)
        {
        }
    }//GEN-LAST:event_jSearchTextFieldKeyReleased

    private void jPrintNumbersInWindowBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jPrintNumbersInWindowBoxActionPerformed
    {//GEN-HEADEREND:event_jPrintNumbersInWindowBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jPrintNumbersInWindowBoxActionPerformed

    private void jPrintNumbersInWindowBoxStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jPrintNumbersInWindowBoxStateChanged
    {//GEN-HEADEREND:event_jPrintNumbersInWindowBoxStateChanged
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jPrintNumbersInWindowBoxStateChanged

    private void jPrintEntriesInWindowBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jPrintEntriesInWindowBoxActionPerformed
    {//GEN-HEADEREND:event_jPrintEntriesInWindowBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jPrintEntriesInWindowBoxActionPerformed

    private void jPrintEntriesInWindowBoxStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jPrintEntriesInWindowBoxStateChanged
    {//GEN-HEADEREND:event_jPrintEntriesInWindowBoxStateChanged
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jPrintEntriesInWindowBoxStateChanged

    private void jDrawConvectionBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jDrawConvectionBoxActionPerformed
    {//GEN-HEADEREND:event_jDrawConvectionBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jDrawConvectionBoxActionPerformed

    private void jDrawConvectionBoxStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_jDrawConvectionBoxStateChanged
    {//GEN-HEADEREND:event_jDrawConvectionBoxStateChanged
        jConnectionDisplay.repaint();
    }//GEN-LAST:event_jDrawConvectionBoxStateChanged

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
    {//GEN-HEADEREND:event_jButton2ActionPerformed
        new Export(this).show();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jShowParallelNodesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jShowParallelNodesActionPerformed
    {//GEN-HEADEREND:event_jShowParallelNodesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jShowParallelNodesActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
    {//GEN-HEADEREND:event_jButton1ActionPerformed
        new Clip(this).show();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButtonParallelReMoveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButtonParallelReMoveActionPerformed
    {//GEN-HEADEREND:event_jButtonParallelReMoveActionPerformed
        for(SubMatrix s : matrices)
			s.reMove();
		calcGlobalBounds();
		jConnectionDisplay.repaint();
    }//GEN-LAST:event_jButtonParallelReMoveActionPerformed

    private void jComponentListActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jComponentListActionPerformed
    {//GEN-HEADEREND:event_jComponentListActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComponentListActionPerformed

	
		/**
	 * @param args the command line arguments
	 * @throws InterruptedException
	 */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jArrowConnectionsBox1;
    private javax.swing.JSlider jArrowSizeSlider;
    private javax.swing.JCheckBox jAutomaticReloadBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButtonParallelReMove;
    private javax.swing.JComboBox jComponentList;
    private javax.swing.JPanel jConnectionDisplay;
    private javax.swing.JCheckBox jDrawConnections;
    private javax.swing.JCheckBox jDrawConvectionBox;
    private javax.swing.JCheckBox jDrawDiffusionBox;
    private javax.swing.JSlider jFontsizeSlider;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JComboBox jNeighborhoodList;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JCheckBox jPrintEntriesInWindowBox;
    private javax.swing.JCheckBox jPrintNumbersInWindowBox;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JButton jRezoomButton;
    private javax.swing.JTextField jSearchTextField;
    private javax.swing.JCheckBox jShowParallelNodes;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSlider jZCompressionSlider;
    private javax.swing.JCheckBox jZcompression;
    private java.awt.Label label1;
    private javax.swing.JButton openButton;
    private javax.swing.JButton reopenButton;
    private javax.swing.JButton toSelectionButton;
    // End of variables declaration//GEN-END:variables
	
	

	public void windowClosing(WindowEvent event)
	{
		release();
		//event.getWindow().setVisible(false);
		event.getWindow().dispose();
	}
	
	/// try to release some memory
	public void release()
	{
		if (matrices != null)
		{
			for (int i = 0; i < matrices.length; i++)
			{
				if (matrices[i] != null)
				{
					matrices[i].release();
				}
				matrices[i] = null;
			}
		}
		System.gc();
	}
	
	double getArrowSize()
	{
		return jArrowSizeSlider.getValue()*0.01;
	}
	
	
	/**
	 * Handles files, jar entries, and deployed jar entries in a zip file (EAR).
	 * @return The date if it can be determined, or null if not.
	 */
	private static Date getClassBuildTime() {
		Date d = null;
		Class<?> currentClass = new Object() {}.getClass().getEnclosingClass();
		URL resource = currentClass.getResource(currentClass.getSimpleName() + ".class");
		if (resource != null) {
			if (resource.getProtocol().equals("file")) {
				try {
					d = new Date(new File(resource.toURI()).lastModified());
				} catch (URISyntaxException ignored) { }
			} else if (resource.getProtocol().equals("jar")) {
				String path = resource.getPath();
				d = new Date( new File(path.substring(5, path.indexOf("!"))).lastModified() );    
			}
		}
		return d;
	}
			private static final Date buildDate = getClassBuildTime();
	//*/


}
