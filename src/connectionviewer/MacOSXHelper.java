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

package connectionviewer;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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
	 * This function inits the Mac OS Open File Handler
	 * It is written this way (with that Class.forName...)
	 * to ensure that it is also working on systems
	 * that do not have com.apple.eawt.*
	 * Otherwise the program would not be able to start.
	 * @see MacOSHandler
	 * @param handler 
	 */
	public static void InitMacOSX(MacOSHandler handler)
	{
		if(IsMacOSX() == false || isInitialized)
			return;		
		try
		{	
			System.out.println("IsMacOSX!");
			Class appClass = Class.forName("com.apple.eawt.Application");
			Object application = appClass.newInstance();
		
			Class listClass = Class.forName("com.apple.eawt.ApplicationListener");
			Method addAppListmethod = appClass.getDeclaredMethod("addApplicationListener", listClass);
			
			Class adapterClass = Class.forName("com.apple.eawt.ApplicationAdapter");
			Object listener = ListenerProxy.newInstance(handler, adapterClass.newInstance());
			addAppListmethod.invoke(application, listener);
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
				KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
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

/**
 * emulating com.apple.eawt.ApplicationAdapter
 * @author mrupp
 */
class ListenerProxy implements InvocationHandler 
{
    private Object object;
	private MacOSXHelper.MacOSHandler handler;
	public static Object newInstance(MacOSXHelper.MacOSHandler handler, Object obj) 
	{
		return java.lang.reflect.Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(), new ListenerProxy(handler, obj));
	}
	 
	private ListenerProxy(MacOSXHelper.MacOSHandler handler, Object obj) 
	{
		this.handler = handler;
		this.object = obj;
	}
	
	// this function mostly forwards everything to the MacOSHandler
	// which is kind of platform-independent.	
	@Override
	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable 
	{
		Object result = null;
		try
		{
			boolean bHandled = false;
			if ("handleAbout".equals(m.getName())) 
			{
				bHandled = handler.handleAbout();
			}
			else if ("handleQuit".equals(m.getName())) 
			{
				bHandled = handler.handleQuit();
			}
			else if ("handleOpenApplication".equals(m.getName()) || "handleOpenFile".equals(m.getName()))
			{

			    bHandled = true;
			    Method getFilename = args[0].getClass().getDeclaredMethod("getFilename");
			    String str = (String)getFilename.invoke(args[0]);
			    System.out.println(m.getName() + " called, getFilename = " + str);
				if("handleOpenApplication".equals(m.getName()))
					bHandled = handler.handleOpenApplication(str);
				else
					bHandled = handler.handleOpenFile(str);
			}
			else
			{
				bHandled = false;
				result = m.invoke(object, args);
			}
			
			Object event = args[0];
			Method eventSetter = Class.forName("com.apple.eawt.ApplicationEvent").
			getDeclaredMethod("setHandled", Boolean.TYPE);
			eventSetter.invoke(event, bHandled);			
		}
		catch (Exception e) {     }
		return result;			 	
	}
}