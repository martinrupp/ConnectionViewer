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
 */

package edu.gcsc.connectionviewer;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;



public class MacOSXHelper 
{
	/**
	 * interface to the MacOSHandler
	 */
	public static interface MacOSHandler
	{
		/**
		 * handle the request to open a file
		 * @param filename
		 * @return true if successfully opened
		 */
		Boolean handleOpenApplication(String filename);
		/**
		 * handle the request to start the application with
		 * opening the file
		 * @param filename
		 * @return true if handled
		 */
		Boolean handleOpenFile(String filename);
		/**
		 * handle the request to show about box
		 * @return true if handled
		 */
		Boolean handleAbout();
		/**
		 * handle the request to quit
		 * @return true if handled
		 */
		Boolean handleQuit();
	}

	/**
	 * A standard MacOSHandler so you don't have to implement
	 * everything from the interface MacOSHandler
	 */
	public static class StdMacOsHandler implements MacOSHandler
	{	
		// std behaviour: do nothing
		public Boolean openApp(String filename, Boolean bOpen) 
		{
			return true;
		}
		// std behaviour: forward to openApp
		@Override
		public Boolean handleOpenApplication(String filename) 
		{
			return openApp(filename, false);
		}

		// std behaviour: forward to openApp
		@Override
		public Boolean  handleOpenFile(String filename) 
		{
			return openApp(filename, true);
		}

		// std behaviour: forward to openApp
		@Override
		public Boolean  handleAbout() 
		{
			return true;
		}

		// std behaviour: System.exit(0)
		@Override
		public Boolean handleQuit()
		{
			System.exit(0);
			return true;
		}
	}
	
	
	static boolean isInitialized = false;
	/**
	 * This function inits the Mac OS About / Open File / Quit handlers.
	 *
	 * Up to Java 8 this used com.apple.eawt.Application via reflection. Since
	 * Java 9 com.apple.eawt is an internal package of the java.desktop module
	 * and is no longer exported, so that code fails with
	 * "cannot access class com.apple.eawt.Application ... does not export".
	 * The public, cross-platform replacement is java.awt.Desktop together with
	 * the handler interfaces in java.awt.desktop. Every handler is guarded by
	 * isSupported(), so this stays a no-op on platforms without the feature.
	 *
	 * Note: eawt's "open application" event has no equivalent in the public
	 * API - the caller is responsible for showing its main window at startup.
	 * MacOSHandler.handleOpenApplication is therefore no longer invoked here.
	 *
	 * @see MacOSHandler
	 * @param handler
	 */
	public static void InitMacOSX(final MacOSHandler handler)
	{
		if(IsMacOSX() == false || isInitialized)
			return;
		if(Desktop.isDesktopSupported() == false)
		{
			System.out.println("java.awt.Desktop unsupported, skipping macOS integration.");
			return;
		}
		try
		{
			System.out.println("IsMacOSX!");
			Desktop desktop = Desktop.getDesktop();

			if(desktop.isSupported(Desktop.Action.APP_OPEN_FILE))
			{
				desktop.setOpenFileHandler(new OpenFilesHandler()
				{
					@Override
					public void openFiles(OpenFilesEvent e)
					{
						for(File file : e.getFiles())
						{
							String str = file.getAbsolutePath();
							System.out.println("openFiles called, getFilename = " + str);
							handler.handleOpenFile(str);
						}
					}
				});
			}

			if(desktop.isSupported(Desktop.Action.APP_ABOUT))
			{
				desktop.setAboutHandler(new AboutHandler()
				{
					@Override
					public void handleAbout(AboutEvent e)
					{
						handler.handleAbout();
					}
				});
			}

			if(desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER))
			{
				desktop.setQuitHandler(new QuitHandler()
				{
					@Override
					public void handleQuitRequestWith(QuitEvent e, QuitResponse response)
					{
						if(Boolean.TRUE.equals(handler.handleQuit()))
							response.performQuit();
						else
							response.cancelQuit();
					}
				});
			}

			isInitialized = true;
		}
		catch (Exception e)
		{
			System.out.println("Error when trying to set up MacOS handler:");
			System.out.println(e);
		}
	}
	
	/**
	 * @return true if on MacOS, otherwise false
	 */
	public static boolean IsMacOSX()
	{
	    boolean b = System.getProperty("os.name").equals("Mac OS X");
	    if(b) System.out.println("Is macOS!");
	    return b;
	}
	
	/**
	 * adds the apple+w = close behaviour to the JFrame frame.
	 * @param frame 
	 */
	public static void AddCloseDisposeAction(javax.swing.JFrame frame)
	{
		if(IsMacOSX() == false) return;
		KeyStroke closeKey = KeyStroke.getKeyStroke(
				KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
		javax.swing.JPanel content = (JPanel) frame.getContentPane();
		// use WHEN_IN_FOCUSED_WINDOW here!
		content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(closeKey, "closeWindow");
		content.getActionMap().put("closeWindow", new MacOSXHelper.CloseDisposeWindowAction(frame));					
	}

		
	/**
	 * an action which closes a given frame
	 * @see AddCloseDisposeAction
	 */
	public static class CloseDisposeWindowAction extends AbstractAction
	{
		javax.swing.JFrame frame;
		
		public CloseDisposeWindowAction(javax.swing.JFrame frame)
		{
			this.frame = frame;
		}
				
		@Override
		public void actionPerformed(ActionEvent ae) 
		{
			frame.setVisible(false);
			frame.dispose();
		}
		
	}
}
