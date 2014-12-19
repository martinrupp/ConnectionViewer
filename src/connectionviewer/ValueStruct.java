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
class ValueStruct
{	
	int icomp=-1; // -1 == all
	void set_comp(int comp)
	{
		icomp = comp;
	}
	public ValueStruct(int N)
	{
		val = new Point3D[N];
		vs = new String[N];
		for(int i=0; i<N; i++)
		{
			val[i] = new Point3D();
			vs[i] = new String();
		}
	}

	public String name;
	Point3D[] val;
	String[] vs;

	public void read(String[] res, BufferedReader f2) throws IOException
	{
		String line2;
		name = res[1];					
		while ((line2 = f2.readLine()) != null)
		{
			String[] res2 = line2.split(" ");
			int j = Integer.parseInt(res2[0]);
			String s = res2[1];
			for (int i = 2; i < res2.length; i++) {
				s += " " + res2[i];
			}
			vs[j] = s;
		}
	}
	int icomponents=0;
	
	void parse_values()
	{
		icomponents=0;
		for (int i = 0; i < vs.length; i++)
		{
			if(vs[i].length() == 0)
			{
				val[i].x = val[i].y = val[i].z = 0.0;
				continue;
			}
			String[] res2 = vs[i].split(" ");
			val[i].x = val[i].y = val[i].z;
			if (res2[0].compareTo("[") != 0) 
			{
				double d = SubMatrix.MyParseDouble(res2[0]);
				val[i].z = d;
				icomponents = Math.max(1, icomponents);
			} else {
				// 0 1 2 3 4
				// [ x y z ]
				double dx = SubMatrix.MyParseDouble(res2[1]);
				double dy = SubMatrix.MyParseDouble(res2[2]);
				double dz = 0;
				
				if (res2.length >= 5)
					dz = SubMatrix.MyParseDouble(res2[3]);

				int iii=1;
				switch(iii)
				{
					case 1:
						val[i].x = dx;
						val[i].y = dy;
						val[i].z = dz;											
						icomponents = Math.max(dz == 0.0 ? 2 : 3, icomponents);
						break;
					case 2:
						val[i].x = dx;
						icomponents = Math.max(1, icomponents);
						break;

					case 3:
						val[i].x = dy;
						icomponents = Math.max(1, icomponents);
						break;

					case 4:
						val[i].x = dz;
						icomponents = Math.max(1, icomponents);
						break;
				}
			}
		}		
	}
	
	public int get_nr_of_components()
	{
		return icomponents;
	}
	
	public double get_double_value(int i)
	{
		if(icomponents == 1)
			return val[i].z;
		else if(icomp == -1)
			return val[i].length();
		else if(icomp == 0)
			return val[i].x;
		else if(icomp == 1)
			return val[i].y;
		else if(icomp == 2)
			return val[i].z;
		return 0.0;
	}
	
	public double get_max_value()
	{
		double maxValue = 0;
		
		for (int i = 0; i < val.length; i++)
			maxValue = Math.max(maxValue, get_double_value(i));
		return maxValue;		
	}
	
	public double get_min_value()
	{
		double minValue = 1e40;
		
		for (int i = 0; i < val.length; i++)
			minValue = Math.min(minValue, get_double_value(i));
		return minValue;		
	}
	
};