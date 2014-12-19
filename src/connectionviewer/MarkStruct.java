/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionviewer;

import java.io.BufferedReader;
import java.io.IOException;

/**
 *
 * @author mrupp Martin Rupp <martin.rupp@gcsc.uni-frankfurt.de>
 */
class MarkStruct 
	{

	public float red;
	public float green;
	public float blue;
	public float alpha;
	public int size;
	public String name;
	public boolean[] marks;

	public void read(String[] res, BufferedReader f2, int iNrOfNodes) throws IOException
	{
		name = res[1];
		marks = new boolean[iNrOfNodes];
		String line2 = f2.readLine();
		if (line2 != null) {
			String[] res2 = line2.split(" ");
			red = Float.parseFloat(res2[0]);
			green = Float.parseFloat(res2[1]);
			blue = Float.parseFloat(res2[2]);
			alpha = Float.parseFloat(res2[3]);
			size = Integer.parseInt(res2[4]);
			while ((line2 = f2.readLine()) != null) {
				marks[Integer.parseInt(line2)] = true;
			}
		}
	}
};