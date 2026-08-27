/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

import java.util.*;

/**
 *
 * @author mrupp
 */
class Lambda2
{
    int values[];
    ArrayList<SortedSet<Integer> > list;
    
    Lambda2(int size)
    {
	values = new int[size];
	list = new ArrayList<SortedSet<Integer> >();
    }

    void set(int i, int v)
    {
	values[i] = v;
	Set<Integer> s;
	
	if(list.size() <= v)
	{
	    while(list.size() <= v)
		list.add(new TreeSet<Integer>());
	}
	s = list.get(v);
	s.add(i);	
    }
    void remove(int i)
    {
	if(values[i] < 0) return;

	list.get(values[i]).remove(i);
	values[i] = -1;
    }
    void unset(int i)
    {
	values[i] = -1;
    }
    
    int remove_max()
    {
	int i= get_max();
	remove(i);
	return i;
    }
    int get_max()
    {
	for(int i=list.size()-1; i>=0; i--)
	{
	    if(list.get(i).isEmpty()) continue;
	    return list.get(i).first();	    
	}
	return -1;
    }

    boolean assigned(int i)
    {
	return values[i] < 0;
    }

    void increase(int k)
    {
	int v = values[k];
	if(v < 0) return;
	remove(k);
	set(k, v+1);	
    }

    void decrease(int k)
    {
	int v = values[k];
	if(v <= 0) return;
	remove(k);
	set(k, v-1);
    }
}
