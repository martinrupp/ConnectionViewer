/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

/**
 *
 * @author mrupp
 */
public class Vector
{
    Vector()
    {	
    }
    Vector(int size)
    {
	resize(size);
    }
    void resize(int size)
    {
	values = new double[size];
    }
    
    int size()
    {
	return values.length;
    }
    
    
    void add(double alpha, double beta, Vector b)
    {
	for(int i=0; i<size(); i++)
	    values[i] = alpha*values[i]+beta*b.values[i];
    }
    
    void add(double alpha, double beta, Vector b, double gamma, Vector c)
    {
	for(int i=0; i<size(); i++)
	    values[i] = alpha*values[i]+beta*b.values[i]+gamma*c.values[i];
    }
    
    void add(double alpha, double beta, SparseMatrix A, Vector b)
    {
	for(int i=0; i<size(); i++)
	    values[i] = alpha*values[i] + beta*A.row_mult(b, i);
    }
    
    void add(double alpha, double beta, SparseMatrix A, Vector b, double gamma, Vector c)
    {
	for(int i=0; i<size(); i++)
	    values[i] = alpha*values[i] + beta*A.row_mult(b, i) + gamma*c.values[i];
    }
    
    void add(double alpha, double gamma, Vector c, double beta, SparseMatrix A, Vector b)
    {
	add(alpha, beta, A, b, gamma, c);
    }
    
    void set(double beta, Vector b)
    {
	add(0.0, beta, b);
    }
    void set(double beta, Vector b, double gamma, Vector c)
    {
	add(0.0, beta, b, gamma, c);
    }
    void set(double beta, SparseMatrix A, Vector b)
    {
	add(0.0, beta, A, b);
    }
    void set(double beta, SparseMatrix A, Vector b, double gamma, Vector c)
    {
	add(0.0, beta, A, b, gamma, c);
    }
    
    void set(double gamma, Vector c, double beta, SparseMatrix A, Vector b)
    {
	add(0.0, beta, A, b, gamma, c);
    }
    
    void assign(double alpha, Vector b)
    {
		if(size() != b.size()) resize(b.size());
		for(int i=0; i<size(); i++)
			values[i] = alpha * b.values[i];
    }
    
    double get(int i)
    {
		return values[i];	
    }
    
    double prod(Vector v)
    {
	assert(v.size() == size());
	double s=0;
	for(int i=0; i<size(); i++)
	    s += values[i]*v.values[i];
	return s;
    }
    
    double prod(FlexSparseMatrix A, Vector v)
    {
	assert(size() == A.num_rows() && A.num_cols() == v.size());
	double s=0;
	for(int i=0; i<size(); i++)
	    s += values[i] * A.row_mult(v, i);
	return s;
    }
    
    double norm()
    {
	return Math.sqrt(prod(this));
    }
    
    void set(double d)
    {
	for(int i=0; i<size(); i++)
	    values[i] = d;
    }
    
    double[] values;
}
