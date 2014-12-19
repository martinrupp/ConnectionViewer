/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package amgframework;


import java.util.*;
interface SparseMatrix
{

    interface IConnection
    {
	double value();
	int index();
    }

    int num_rows();
    int num_cols();
    Iterator<IConnection> begin_row(int r);
    
    
    class Row implements Iterable<IConnection>
    {
	int r;
	SparseMatrix A;
	Row(FlexSparseMatrix _A, int _r)
	{
	    A = _A;
	    r = _r;
	}

	@Override
	public Iterator<IConnection> iterator()
	{
	    return A.begin_row(r);
	}	
    }
    Row row(int r);
    void print();  
    
    boolean is_equal(SparseMatrix A);
    
    boolean has_connection(int r, int c);
    
    public double get(int r, int c);
    
    double row_mult(Vector b, int r);
}
