/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

/**
 *
 * @author mrupp
 */
class Lambda
{
    int values[];

    Lambda(int s)
    {
	values = new int[s];
    }


    int remove_max()
    {
	int max=0;
	for(int i=1; i<values.length; i++)
	    if(values[max] < values[i])
		max = i;		
	values[max] = -1;
	return max;
    }

    void remove(int i)
    {
	values[i] = -1;
    }
    
    void unset(int i)
    {
	values[i] = -1;
    }

    boolean assigned(int i)
    {
	return values[i] < 0;
    }

    void set(int i, int val)
    {
	values[i] = val;
    }


    void increase(int k)
    {
	values[k]++;
    }

    void decrease(int k)
    {
	values[k]--;
    }

}
