/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

import edu.gcsc.amgframework.SparseMatrix.IConnection;

/**
 *
 * @author mrupp
 */
public class LU
{
    class DenseMatrix
    {
	double[] values;
	int rows, cols;
	void resize(int rows, int cols)
	{
	    values = new double[rows*cols];
	    this.rows = rows;
	    this.cols = cols;
	}
	void set(int r, int c, double v)
	{
	    assert(r >= 0 && r < rows && c >= 0 && c < cols );
	    values[r*cols+c] = v;
	}
	double get(int r, int c)
	{
	    assert(r >= 0 && r < rows && c >= 0 && c < cols );
	    return values[r*cols+c];
	}
	int num_rows() { return rows; }
	int num_cols() { return cols; }
    }
    DenseMatrix A;
	    
    boolean init(SparseMatrix M)
    {
	assert(M.num_rows() == M.num_cols());
	A = new DenseMatrix();
	A.resize(M.num_rows(), M.num_rows());
	for(int r=0; r<M.num_rows(); r++)
	    for(IConnection con : M.row(r))
		A.set(r, con.index(), con.value());			

	for(int i=0; i<A.num_rows(); i++)
	{
	    if(Math.abs(A.get(i,i)) < 1e-10)
		return false;
	    for(int k=0; k<i; k++)
	    {
		// add row k to row i by A(i, .) -= A(k,.)  A(i,k) / A(k,k)
		// so that A(i,k) is zero (elements A(i, <k) are already "zero")
		// safe A(i,k)/A(k,k) in A(i,k)
		double a_ik = A.get(i,k) / A.get(k,k);
		A.set(i,k, a_ik);		

		for(int j=k+1; j<A.num_rows(); j++)
		    A.set(i,j, A.get(i,j) - A.get(k,j) * a_ik);
	    }
	}
	    
	return true;
    }
    
    void solve(Vector x, Vector b)
    {
	x.assign(1.0, b);
	// LU x = b, -> U x = L^{-1} b
	// solve lower left
	double s;
	for(int i=0; i<A.num_rows(); i++)
	{
		s = x.values[i];
		for(int k=0; k<i; k++)
			s -= A.get(i, k)*x.values[k];
		x.values[i] = s;
	}
	
	// -> x = U^{-1} (L^{-1} b)
	// solve upper right
	for(int i=A.num_rows()-1; ; i--)
	{
	    s = x.values[i];
	    for(int k=i+1; k<A.num_rows(); k++)
	    	s -= A.get(i, k)*x.values[k];
	    x.values[i] = s/A.get(i,i);
	    if(i==0) break;
	}
    }
}
