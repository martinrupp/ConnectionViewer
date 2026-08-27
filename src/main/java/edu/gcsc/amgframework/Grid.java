/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mrupp
 */
public class Grid
{
    int dimension;
    Point3D[] pos;
    int size() { return pos.length; }
    void resize(int s, int dimension)
    {
	pos = new Point3D[s];
	this.dimension = dimension;
    }

    void write_grid_header(BufferedWriter out)
    {
	try
	{
	    out.write("" + 1 + "\n" + dimension + "\n" + size() + "\n");
	    for(int i=0; i<size(); i++)
	    {
		out.write(pos[i].x + " " + pos[i].y);
		if(dimension == 3) out.write(" " + pos[i].z);
		out.write("\n");
	    }
	    out.write("1\n"); // stringsInWindow			
	}
	catch (Exception ex)
	{
	    Logger.getLogger(AMGFramework.class.getName()).log(Level.SEVERE, null, ex);
	}

    }
    
    
    static void AddMarkers(String filename, String markfilename)
    {	
	try
	{
	    BufferedWriter f = new BufferedWriter(new FileWriter(filename, true));
	    f.write("c " + markfilename + "\n");
	    f.flush();
	}
	catch (IOException ex)
	{
	    Logger.getLogger(AMGFramework.class.getName()).log(Level.SEVERE, null, ex);
	}	
    }
    
   static void WriteMarkers(String markfilename, boolean[] markers, double r, double g, double b, double alpha, int size)
    {
	try
	{
	    BufferedWriter f = new BufferedWriter(new FileWriter(markfilename));
	    f.write("" + r + " " + g + " " + b + " " + alpha + " " + size + "\n");
	    for(int i=0; i<markers.length; i++)
		if(markers[i])
		    f.write("" + i + "\n");
	    f.flush();
	}
	catch (IOException ex)
	{
	    Logger.getLogger(AMGFramework.class.getName()).log(Level.SEVERE, null, ex);
	}
    }

    
    void writeMatrix(String filename, SparseMatrix A)
    {
		BufferedWriter out;
		try
		{
			out = new BufferedWriter(new FileWriter(filename));

			write_grid_header(out);
			for(int r=0; r<A.num_rows(); r++)
				for(SparseMatrix.IConnection con : A.row(r))
				out.write("" + r + " " + con.index() + " " + con.value() + "\n");
			out.flush();
		}
		catch (IOException ex)
		{
			Logger.getLogger(AMGFramework.class.getName()).log(Level.SEVERE, null, ex);
		}
    }
    
    void writeProlongation(String filename, SparseMatrix A, int[] newIndex)
    {
	BufferedWriter out;
	try
	{
	    out = new BufferedWriter(new FileWriter(filename));
	    int[] parentIndex = new int[A.num_rows()];
	    for(int i=0; i<newIndex.length; i++)
		if(newIndex[i] != -1) 
		    parentIndex[newIndex[i]] = i;
	
	    write_grid_header(out);	    
	    for(int r=0; r<A.num_rows(); r++)
	    	for(SparseMatrix.IConnection con : A.row(r))
		    out.write("" + r + " " + parentIndex[con.index()] + " " + con.value() + "\n");
	    out.flush();
	}
	catch (IOException ex)
	{
	    Logger.getLogger(AMGFramework.class.getName()).log(Level.SEVERE, null, ex);
	}
    }
    void writeRestriction(String filename, SparseMatrix A, int[] newIndex)
    {
	BufferedWriter out;
	try
	{
	    out = new BufferedWriter(new FileWriter(filename));
	    int[] parentIndex = new int[A.num_rows()];
	    for(int i=0; i<newIndex.length; i++)
		if(newIndex[i] != -1) 
		    parentIndex[newIndex[i]] = i;
	
	    write_grid_header(out);	    
	    for(int r=0; r<A.num_rows(); r++)
	    	for(SparseMatrix.IConnection con : A.row(r))
		    out.write("" + parentIndex[r] + " " + con.index() + " " + con.value() + "\n");
	    out.flush();
	}
	catch (IOException ex)
	{
	    Logger.getLogger(AMGFramework.class.getName()).log(Level.SEVERE, null, ex);
	}
    }
    
    boolean readHeader(BufferedReader f) throws IOException
    {
		String line;
		int iNrOfNodes;

		if ((line = f.readLine()) == null)
			return false;
		int iConnectionViewerVersion = Integer.parseInt(line);
		if (iConnectionViewerVersion != 1)
			return false;
		if ((line = f.readLine()) == null)
			return false;
		int dim = Integer.parseInt(line);
		//System.out.println("Dimensions: " + dimension);
		if ((line = f.readLine()) == null)
			return false;
		iNrOfNodes = Integer.parseInt(line);
		//System.out.println("Nr of Nodes: " + iNrOfNodes);

		boolean bNewGrid=false;
		if(pos == null)
		{
			bNewGrid=true;
			resize(iNrOfNodes, dim);
		}
		else assert(size() == iNrOfNodes && dimension == dim);

		////////////////////////////////////////////////////////////////////
		// read positions
		for (int i = 0; i < iNrOfNodes; i++)
		{
			if ((line = f.readLine()) == null)
			return false;
			if(!bNewGrid) continue;
			String[] res = line.split(" ");
			pos[i] = new Point3D();
			pos[i].setLocation(Double.parseDouble(res[0]), Double.parseDouble(res[1]));
			if (dimension == 3)
			pos[i].z = Double.parseDouble(res[2]);
			else
			pos[i].z = 0;		
		}

		if ((line = f.readLine()) == null)
			return false;
		return true;
    }
    
    FlexSparseMatrix readMatrix(String filename)
    {
		BufferedReader f;
		FlexSparseMatrix A = new FlexSparseMatrix(true);
		try
		{
			f = new BufferedReader(new FileReader(filename));
		}
		catch (IOException e)
		{
			return null;
		}
		try
		{
			if(readHeader(f) == false) return null;
			String line;
				A.resize(size(), size());

				while ((line = f.readLine()) != null)
			{
			//filePos += line.length(); updateStatus(filePos, fsize);
			String[] res = line.split(" ");
			if (res[0].equals("c") || res[0].equals("v"))
				break;
			int from = Integer.parseInt(res[0]);
			int to = Integer.parseInt(res[1]);
			double value = Double.parseDouble(res[2]);
			if(value != 0.0)
				A.set(from, to, value);
			}


		}
		catch (IOException e)
		{
			System.out.println(e.toString());
			return null;
		}
		return A;
    }
    
    
    Vector readVector(String filename)
    {
	int filePos=0;
	BufferedReader f;
	FlexSparseMatrix A = new FlexSparseMatrix(true);
	Vector x = new Vector();
	try
	{
	    f = new BufferedReader(new FileReader(filename));
	}
	catch (IOException e)
	{
	    return null;
	}
	try
	{
	    if(readHeader(f) == false) return null;
	    String line;	    
            x.resize(size());
            
            while ((line = f.readLine()) != null)
	    {
		//filePos += line.length(); updateStatus(filePos, fsize);
		String[] res = line.split(" ");
		if (res[0].equals("c") || res[0].equals("v"))
		    break;
		int from = Integer.parseInt(res[0]);
		int to = Integer.parseInt(res[1]);
		double value = Double.parseDouble(res[2]);
		if(from == to)
		    x.values[from] = value;
	    }
	    
            
	}
	catch (IOException e)
	{
	    System.out.println(e.toString());
	    return null;
	}
	return x;
    }
    
    Grid calc_next(int[] newIndex, int nCoarse)
    {
	Grid g = new Grid();
	g.resize(nCoarse, dimension);
	for(int i=0; i<size(); i++)
	    if(newIndex[i] != -1)
	    	g.pos[newIndex[i]] = pos[i];
	return g;
    }
}
