/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

/**
 *
 * @author mrupp
 */
public class Jacobi implements Preconditioner
{
    double[] m_diagInv;
    double m_damp=0.7;
    
    void set_damp(double d)
    {
	m_damp = d;
    }
    public void init(FlexSparseMatrix A)
    {
	m_diagInv = new double[A.num_rows()];
	for(int i=0; i<A.num_rows(); i++)
	    m_diagInv[i] = m_damp / A.get(i,i);
    }

    public void apply(Vector corr, Vector defect)
    {
	for(int i=0; i<corr.size(); i++)
	    corr.values[i] = defect.values[i] * m_diagInv[i];
    }

    @Override
    public void step(Vector x, Vector c, FlexSparseMatrix A, Vector defect)
    {
	apply(c, defect);
	x.add(1.0, 1.0, c);
	defect.add(1.0, -1.0, A, c);
    }
    
}
