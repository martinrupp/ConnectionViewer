/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionviewer;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author mrupp Martin Rupp <martin.rupp@gcsc.uni-frankfurt.de>
 */
public class FileUtil {

	static public String GetDirectory(String filename)
	{
		int slash = filename.lastIndexOf('/');
		if (slash == -1)
		{
			return "";
		} else
		{
			return filename.substring(0, slash + 1);
		}

	}
	static public String GetFilename(String filename)
	{
		int slash = filename.lastIndexOf('/');
		if (slash == -1)
		{
			return filename;
		} else
		{
			return filename.substring(slash + 1, filename.length());
		}

	}
	static public String GetFileInSamePath(String pathTemplate, String filename)
	{
		if (filename.indexOf('/') == 0)
		{
			return filename;
		} else
		{
			return GetDirectory(pathTemplate) + filename;
		}
	}
	
	public static class OnlyEndsWithFilenameFilter implements FilenameFilter
	{

		String[] ext;

		public OnlyEndsWithFilenameFilter(String[] ext)
		{
			this.ext = ext;
		}

		public boolean accept(File dir, String name)
		{
			for (int i = 0; i < ext.length; i++)
			{
				if (name.endsWith(ext[i]))
				{
					return true;
				}
			}
			return false;
		}
	}
}
