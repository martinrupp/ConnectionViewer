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
public class GaussSeidel implements Preconditioner
{
    FlexSparseMatrix L;
    double[] m_diagInv;
    
    @Override
    public void init(FlexSparseMatrix A)
    {
		L = new FlexSparseMatrix(true);
		L.resize(A.num_rows(), A.num_cols());
		m_diagInv = new double[L.num_rows()];
		for(int r=0;r<A.num_rows(); r++)
			for(IConnection con : A.row(r))
			{
			if(con.index() < r)
				L.set(r, con.index(), con.value());
			else if(con.index() == r)
				m_diagInv[r] = 1.0/con.value();
			}
		L.defragment();
    }

    @Override
    public void apply(Vector corr, Vector defect)
    {
		for(int r=0; r<corr.size(); r++)
		{
			double v = 0;
			for(IConnection con : L.row(r))
				v += con.value() * corr.values[con.index()];
			corr.values[r] = (defect.values[r] - v) * m_diagInv[r];
		}
    }

    @Override
    public void step(Vector x, Vector c, FlexSparseMatrix A, Vector defect)
    {
	apply(c, defect);
	x.add(1.0, 1.0, c);
	defect.add(1.0, -1.0, A, c);
    }
    
}
