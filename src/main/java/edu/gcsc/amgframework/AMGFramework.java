/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

/**
 *
 * @author mrupp
 */
public class AMGFramework
{
    
    static void LinearSolver(Vector x, FlexSparseMatrix A, Vector b, Preconditioner B, int steps)
    {
		int N = A.num_rows();
		assert(b.size() == N && x.size() == N);
		Vector d = new Vector(N);
		Vector c = new Vector(N);

		// d = b-Ax
		d.set(1.0, b, -1.0, A, x);

		long start=System.currentTimeMillis();
		B.init(A);
		System.out.println("init took "+(System.currentTimeMillis()-start)+" ms");

		double defect = d.norm(), newDefect;
		double rate;

		start=System.currentTimeMillis();
		System.out.println("0: \t" + defect + " \t" + "-");
		for(int i=0; i<steps; i++)
		{
			// x = x + B^{-1}d, update d 
			B.step(x, c, A, d);

			newDefect = d.norm();
			rate = newDefect/defect;
			defect = newDefect;
			System.out.println("" + (i+1) + ": \t" + defect + " \t" + rate);
		}	
		System.out.println("apply took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    

    static void CG(Vector x, FlexSparseMatrix A, Vector b, Preconditioner B, int steps)
    {
	int N=A.num_rows();
	assert(b.size() == N && x.size() == N);
	Vector r = new Vector(N), d = new Vector(N), z = new Vector(N), t = new Vector(N);
	
	r.set(1.0, b, -1.0, A, x);
	    
	B.init(A);
	B.apply(d, r);
	z.set(1.0, d);

	double defect = r.norm(), newDefect;
	double rate;

	System.out.println("0: \t" + defect + " \t" + "-");
	double rz_old = r.prod(z);
	double rz_new;
	for(int i=0; i<steps; i++)
	{
	    // alpha = <r, z>/<d, Ad>
	    double alpha = rz_old / d.prod(A, d);

	    // x += alpha d
	    x.add(1.0, alpha, d);

	    // r -= alpha A d			
	    r.add(1.0, -alpha, A, d);

	    // z = B^{-1}r
	    B.apply(z, r);
	    rz_new = r.prod(z);

	    double beta = rz_new/rz_old;
	    // d = beta*d + z;
	    d.add(beta, 1.0, z);
	    rz_old = rz_new;

	    newDefect = r.norm();
	    rate = newDefect/defect;
	    defect = newDefect;
	    System.out.println("" + (i+1) + ": \t" + defect + " \t" + rate);
	}

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
	
		Grid grid = new Grid();
		String basepath = "/Users/mrupp/amgproblems/";
		int refinements = 4;
		String problemName = "Poisson";
		int problemNr = 1;
		String path = basepath + refinements + "/" + problemName + "P" + problemNr;

		long start = System.currentTimeMillis();

		FlexSparseMatrix A = grid.readMatrix(path + "Stiffness.mat");
		Vector x = grid.readVector(path + "u.vec");
		Vector b = grid.readVector(path + "Rhs.vec");
		long end = System.currentTimeMillis();

		System.out.println("loading took "+(System.currentTimeMillis()-start)+" ms");

		AMG amg = new AMG();
		Jacobi jac = new Jacobi();
		amg.set_output(grid, "/Users/mrupp/amgproblems/results/");

		start = System.currentTimeMillis();
		LinearSolver(x, A, b, amg, 10);
		System.out.println("complete AMG took "+(System.currentTimeMillis()-start)+" ms");

		return;
    }
    public static void main1(String[] args)
    {
		FlexSparseMatrix A = new FlexSparseMatrix(true);

		long start = System.currentTimeMillis();
		int N=100;
		A.resize(N, N);
		for(int i=1; i<N; i++)
		{

			A.set(i, i, 2.0);
			A.set(i, i-1, -1.0);
			A.set(i, i+1, -1.0);
		}
		A.defragment();

		A.print();
			System.out.println("--");
		long end = System.currentTimeMillis();
		A.defragment();
		System.out.println("took "+(end-start)+" ms");
		//A.print();
		// TODO code application logic here
    }
}
