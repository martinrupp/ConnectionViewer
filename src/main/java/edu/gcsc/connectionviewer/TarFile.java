/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.connectionviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mrupp
 */
public class TarFile
{
	public class LimitedCountingStreamReader extends CountingStreamReader
	{
	// variables
		private int pos;
		private int line;
		private int from, size;

	// functions
		public LimitedCountingStreamReader(InputStreamReader isr, int from, int size) throws IOException
		{	
			super(isr);
			this.from = from;
			this.size = size;			
			pos=0;
			super.skip(from);			
		}


		@Override
		public String readLine() throws IOException
		{
			if(pos > size) throw new IOException("over the top!");
			if(pos >= size) 
				return null;
			String s=super.readLine();
			if(s != null)
			{
				pos+=s.length()+1;
				line++;
			}
			return s;
		}
		@Override
		public int read(char[] chars, int i1, int i2) throws IOException
		{
			if(pos > size) throw new IOException("over the top!");
			if(pos >= size) return 0;
			pos += i2;
			return super.read(chars, i1, i2);
		}
		@Override
		public void skip(int i) throws IOException
		{
			if(pos >= size) throw new IOException("over the top!");
			pos+=i;
			super.skip(i);
		}

		@Override
		public int getPos() { return pos; }
		@Override
		public int getLine() { return line; }
	}
	
	String readString(CountingStreamReader f, int length) throws IOException
	{
		char chars[] = new char[length];
		f.read(chars, 0, length);				
		int i;
		for(i=0; i<length; i++)
			if(chars[i] == 0) break;
		return String.valueOf(chars, 0, i);
	}
	
	int readOctal(CountingStreamReader f, int length) throws IOException
	{
		String s = readString(f, length);
		s=s.substring(0, length-1);
		return Integer.parseInt(s, 8);
	}
	
	TarComponent getComponent(String filename)
	{
		TarComponent t= components.get(filename);
		return t;
	}
	
	
	TarFile(String filename) 
	{
		File file = new  File(filename);
		long fsize = file.length();
		components = new HashMap<String, TarComponent>();
		CountingStreamReader f;
		tarfilename = filename;
		try
		{
			f = new CountingStreamReader(new FileReader(filename));
		} catch (IOException e)
		{
			System.out.println("File " + filename + "not found! (" + e.toString() + ")");
			return;
		}
		
		try
		{
			while(f.ready()==true && f.getPos()<= fsize)
			{
				TarComponent t = new TarComponent();
								
				t.filename = readString(f, 100);
				if(t.filename.length() == 0) break;
				readString(f, 8);
				readString(f, 8);
				readString(f, 8);
				t.size = readOctal(f, 12);
				f.skip(12+8+1+100+6+2+32+32+8+8+155+12);
				char chars[] = new char[t.size];
				//f.read(chars, 0, t.size);
				t.startPos = f.getPos();
				f.skip(t.size);
				if(t.size%512 != 0)
					f.skip(512-t.size%512);
				components.put(t.filename, t);
			}
		}
		catch (IOException ex)
		{
			Logger.getLogger(TarFile.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public class TarComponent
	{
		LimitedCountingStreamReader getStream() throws FileNotFoundException, IOException
		{
			return new LimitedCountingStreamReader(new FileReader(tarfilename), startPos, size);	
		}
		String filename;
		int startPos;
		int size;
	}
	
	String tarfilename;	
	HashMap<String, TarComponent> components;
	
}
