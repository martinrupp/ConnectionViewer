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
 * ConnectionViewer.java
 *
 * Created on 26.07.2011, 11:34:48
 *
 *
 * 3.01 :
 *	- fixed bug with .vec files (wrong zbounds -> wrong zzoom)
 *	- z compression
 * 3.02 :
 *	- files are now closed, so that they can be overwritten again (-> reload works)
 *	- bugfix if zmin == zmax
 *	- search field now usable again, now supports matrixfile.nodeindex .
 * 3.03b :
 *	- when clicking on a point, all underlying points are selected as well
 *	- fix with "show indices"
 * 3.04 :
 *	- removed a System.out.println which slowed things down
 * 3.1:
 *	- parallel loading, parallel drawing
 *	- Draw Connections on/off
 *	- now in git !
 * 3.11:
 *	- added support for VRL
 *	- fixed bug with big files by adding VMOptions in info.plist
 *	- added progress bar
 * 3.12:
 *	- ignoring files which could'nt be opened
 *      - y direction is now mathematical (up is +, down is -)
 * 3.13
 *	- y direction now also fixed in 3d
 * 
 * 3.13b
 *	- minor fixes (compatible with vrl-0.4.x)
 *
 * 3.14 - select neighborhood fix
 * 3.15 - support for vector fields (= vecs with values [ x y ] or [ x y z ]
 * 3.16 - output to pdf
 * 3.17 - file selection restricted to .mat/.vec/.pmat/.pvec
 *      - arrow drawing for connections, also in pdf output
 * 3.18 - fontsize changeable
 * 3.19 - NaN support
 * 3.20 - automatic reloading of files
 * 3.21 - fixed progress bar, output of errors when loading, better 3d vector support
 * 3.22 - bug with values files and ./v.values
 * 3.23 - changed handling of values
 *  - better error report
 *  - window close on mac fix
 *  - z-clipplane
 * 3.25
 *  - Convection/Diffusion display
 * 3.26
 *	- latex/tikz output
 * 3.27
 * - clipping
 * 3.28
 * - components
 * - fixed a bug when a vector contains only zeros.
 * 3.29
 * - pdf window size fix (height/width mixed)
 * - neighborhood now symmetric
 * - clip: in seperate window, X, Y, Z
 * - export in seperate window, pdf name fix
 * - toselection fix
 * 3.30
 * - reload components fix
 * - axis
 * 3.31
 * - moving of parallel nodes. (hold shift)
 * - min/max for values
 * 3.32
 * - tex export for parallel fixed.
 * - fixed command line call: java -cp ConnectionViewer.jar connectionviewer.ConnectionViewer Stiffness.mat
 * - additional CommandLine parameters. example:
 *   java -cp ConnectionViewer.jar connectionviewer.ConnectionViewer Stiffness.mat 
 *     -scaleZoom 0.99 -height 700 -width 950 -drawConvection 1 -drawConnections 1 -exportPDF myfile.pdf -quit
 *   options are (D double, I integer, B 0 or 1)
 *   -width I -height I -arrowSize I -fontsize I -zcompression I
 *   -scaleZoom D
 *   -arrowConnections B -automaticReload B -drawConnections B -drawConvection B
 *   -drawDiffusion B -showParallelNodes B
 * 
 *   -exportPDF filename.pdf
 *   -exportTex filename.tex
 *   -quit (quit after exporting)
 * 3.33: - some tixz enhancements. 
 *       - fixed toselection (again?)
 * 
 * @author Martin Rupp
 * @email martin.rupp@gcsc.uni-frankfurt.de
 */
package connectionviewer;

import java.awt.Dimension;

/**
 *
 * @author mrupp
 */
public class ConnectionViewer extends javax.swing.JFrame
{	
	
	private static final long serialVersionUID = 1L;
	
////////////////////////////////////////////////////////
	static int windowpos = 40;	
	boolean fileLoaded = false;
	static ConnectionViewer cvf;
	
	
	void readFile(String str)
	{		
		fileLoaded = true;
		((ConnectionViewerPanel) jConnectionViewerPanel).readFile(str);
	}

	public ConnectionViewer()
	{		
		initComponents();

		// window close adapters
		addWindowListener(new WindowClosingAdapter(false));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		// set size and position so that futher windows are "staggered"
		setSize(1000, 900);
		setLocation(windowpos, windowpos);
		windowpos += 20;
		if (windowpos > 200)
			windowpos = 35;
		
		// set title
		setTitle("ConnectionViewer " + ConnectionViewerPanel.sConnectionViewerVersion + " - no file loaded.");

		
		if (MacOSXHelper.IsMacOSX())
			MacOSXHelper.AddCloseDisposeAction(this);

		jConnectionViewerPanel.setFrame(this);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jConnectionViewerPanel = new connectionviewer.ConnectionViewerPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                ConnectionViewer.this.windowClosing(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jConnectionViewerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1016, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jConnectionViewerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 756, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void windowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_windowClosing
    {//GEN-HEADEREND:event_windowClosing
		jConnectionViewerPanel.release();
    }//GEN-LAST:event_windowClosing

	/**
	 * @param args the command line arguments
	 */
	public static void main2(String args[])
	{
		/*
		 * Set the Nimbus look and feel
		 */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the
		 * default look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		 */
		try
		{
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
			{
				if ("Nimbus".equals(info.getName()))
				{
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex)
		{
			java.util.logging.Logger.getLogger(ConnectionViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex)
		{
			java.util.logging.Logger.getLogger(ConnectionViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex)
		{
			java.util.logging.Logger.getLogger(ConnectionViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex)
		{
			java.util.logging.Logger.getLogger(ConnectionViewer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>

		/*
		 * Create and display the form
		 */
		java.awt.EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				new ConnectionViewer().setVisible(true);
			}
		});
	}
	
	public static class MyMacOSHandler extends MacOSXHelper.StdMacOsHandler
	{
		@Override
		public Boolean openApp(String filename, Boolean bOpen)
		{
			ConnectionViewer cvf;
			if(ConnectionViewer.cvf.fileLoaded == false)
			cvf = ConnectionViewer.cvf;
			else
			cvf = new ConnectionViewer();
			if(filename != null)
				cvf.readFile(filename);
			cvf.setVisible(true);
			return true;
		}		
	}
	
	
	
	public static void main(String args[]) throws InterruptedException
	{
		System.out.println("Runtime.getRuntime().availableProcessors() = " + Runtime.getRuntime().availableProcessors());
		
		String filename = null;
		if (args.length > 0)
			filename = args[0];

		cvf = new ConnectionViewer();
		if (filename != null)
		{	
			CommandLineHelper cl = new CommandLineHelper(args);
			int height = cvf.getSize().height;
			int width = cvf.getSize().width;
			height = cl.GetParamInt("-height", height);
			width = cl.GetParamInt("-width", width);
			cvf.setSize(new Dimension(width, height));
			
			cvf.readFile(filename);
			cvf.setVisible(true);
			
			ConnectionViewerPanel p = cvf.jConnectionViewerPanel;
			p.waitForReadingDone();
			
			
			
			cvf.repaint();
						
			p.readArgs(cl);
			
			if(cl.HasParam("-quit"))
				System.exit(0);
			
		}
		else if (MacOSXHelper.IsMacOSX())
		{
			/*JFrame frame = new JFrame("ConnectionViewer");
			frame.setVisible(true);
			frame.setVisible(false);*/
			MacOSXHelper.InitMacOSX(new MyMacOSHandler());
		}
	}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private connectionviewer.ConnectionViewerPanel jConnectionViewerPanel;
    // End of variables declaration//GEN-END:variables

	
	
}
