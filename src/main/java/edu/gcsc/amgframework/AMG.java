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
public class AMG implements Preconditioner
{

	static boolean[] Coarsen(FlexSparseMatrix S, FlexSparseMatrix ST, boolean[] dirichlet)
	{
		Lambda2 lambda = new Lambda2(S.num_rows());
		//Lambda lambda = new Lambda(S.num_rows());
		boolean coarse[] = new boolean[S.num_rows()];
		// set dirichlet nodes fine and calculate lambda ratings
		int nUnassigned = S.num_rows();
		for (int i = 0; i < S.num_rows(); i++)
		{
			if (dirichlet[i])
			{
				nUnassigned--;
				lambda.unset(i);
			}
			else
				lambda.set(i, ST.num_connections(i));
		}

		while (nUnassigned > 0)
		{
			int i = lambda.remove_max();
			coarse[i] = true;
			nUnassigned--;

			for (IConnection con : ST.row(i))
			{
				int j = con.index();
				if (lambda.assigned(j))
					continue;
				lambda.remove(j);
				nUnassigned--;
				for (IConnection con2 : S.row(j))
				{
					int k = con2.index();
					if (!lambda.assigned(k))
						lambda.increase(k);
				}
			}
			for (IConnection con : S.row(i))
			{
				int j = con.index();
				if (lambda.assigned(j))
					continue;
				lambda.decrease(j);
			}
		}
		return coarse;
	}

	static FlexSparseMatrix GetRSProlongation(SparseMatrix A, boolean[] coarse, boolean[] dirichlet, int[] newIndex)
	{
		FlexSparseMatrix P = new FlexSparseMatrix(true);
		// calc new indices	
		int nCoarse = 0;
		for (int i = 0; i < A.num_rows(); i++)
		{
			if (coarse[i])
				newIndex[i] = nCoarse++;
			else
				newIndex[i] = -1;
		}


		// resize
		P.resize(A.num_rows(), nCoarse);
		// calc prolongation
		for (int i = 0; i < A.num_rows(); i++)
		{
			if (coarse[i])
				P.set(i, newIndex[i], 1.0);
			else
			{
				double sumNeighbors = 0, diag = 0, sumInterpolate = 0;
				for (IConnection con : A.row(i))
				{
					int j = con.index();
					double v = con.value();
					if (i == j)
						diag = v;
					else
						sumNeighbors += v;
					
					if (coarse[j] || dirichlet[i])
						sumInterpolate += v;
				}

				double alpha = sumNeighbors / sumInterpolate;

				for (IConnection con : A.row(i))
				{
					int j = con.index();
					double v = con.value();
					if (coarse[j])
						P.set(i, newIndex[j], -alpha * v / diag);
				}
			}
		}
		return P;
	}

	static double MaxValue(SparseMatrix A, int r)
	{
		double m = 0;
		for (IConnection c : A.row(r))
			if (c.index() != r)
				m = Math.max(c.value(), m);
		return m;
	}

	static FlexSparseMatrix CalculateStrongCouplings(SparseMatrix A, double alpha)
	{
		FlexSparseMatrix S = new FlexSparseMatrix(false);
		S.resize(A.num_rows(), A.num_rows());

		for (int i = 0; i < A.num_rows(); i++)
		{
			double m = MaxValue(A, i);
			for (IConnection c : A.row(i))
			{
				int j = c.index();
				if (i != j && Math.abs(c.value()) >= alpha * m)
					S.set_connection(i, j);
			}
		}
		S.defragment();
		return S;
	}

	static boolean[] GetDirichlet(FlexSparseMatrix S)
	{
		boolean dirichlet[] = new boolean[S.num_rows()];
		for (int i = 0; i < S.num_rows(); i++)
			dirichlet[i] = S.num_connections(i) == 0 ? true : false;

		return dirichlet;
	}

	void write(FlexSparseMatrix S, FlexSparseMatrix ST,
			int[] newIndex, FlexSparseMatrix A, FlexSparseMatrix AH,
			boolean[] coarse, boolean[] dirichlet)
	{
		grid.writeMatrix(path + "L" + level + "S.mat", S);
		grid.writeMatrix(path + "L" + level + "ST.mat", ST);
		grid.writeProlongation(path + "L" + level + "P.mat", P, newIndex);
		grid.writeRestriction(path + "L" + level + "R.mat", R, newIndex);
		Grid.AddMarkers(path + "L" + level + "P.mat", path + "L" + level + "coarse.marks");
		Grid.AddMarkers(path + "L" + level + "P.mat", path + "L" + level + "dirichlet.marks");

		grid.writeMatrix(path + "L" + level + "A.mat", A);
		Grid.AddMarkers(path + "L" + level + "A.mat", path + "L" + level + "coarse.marks");
		Grid.AddMarkers(path + "L" + level + "A.mat", path + "L" + level + "dirichlet.marks");

		Grid.WriteMarkers(path + "L" + level + "dirichlet.marks", dirichlet, 1, 0, 0, 1, 0);
		Grid.WriteMarkers(path + "L" + level + "coarse.marks", coarse, 0, 0, 1, 1, 0);

		nextGrid.writeMatrix(path + "L" + level + "AH.mat", AH);
	}

	void set_output(Grid grid, String path)
	{
		this.grid = grid;
		this.path = path;
	}

	@Override
	public void init(FlexSparseMatrix A)
	{
		System.out.print("AMG level " + level + ": " + A.num_rows() + ": ");

		long start = System.currentTimeMillis();
		/*jac = new Jacobi();
		 jac.set_damp(0.6);
		 */
		jac = new GaussSeidel();
		jac.init(A);

		this.A = A;

		FlexSparseMatrix S, ST;
		S = CalculateStrongCouplings(A, 0.25);
		System.out.print("S");
		ST = S.transpose();
		System.out.print("T");
		if (S.is_equal(ST))
		{
			ST = S;
		}

		boolean[] dirichlet = GetDirichlet(S);
		System.out.print("D");
		coarse = Coarsen(S, ST, dirichlet);
		System.out.print("C");
		int[] newIndex = new int[A.num_rows()];
		P = GetRSProlongation(A, coarse, dirichlet, newIndex);
		System.out.print("P");

		R = P.transpose();
		System.out.print("R");
		AH = R.mult(A, P);
		System.out.print("H");

		dH = new Vector(AH.num_rows());
		eH = new Vector(AH.num_rows());

		long end = System.currentTimeMillis();
		System.out.print(". took " + (end - start) + "ms . \n");

		if (grid != null)
		{
			nextGrid = grid.calc_next(newIndex, AH.num_rows());
			write(S, ST, newIndex, A, AH, coarse, dirichlet);
		}
		if (AH.num_rows() > 100)
		{
			nextAMG = new AMG(level + 1);
			nextAMG.set_output(nextGrid, path);
			nextAMG.init(AH);
		} else
		{
			lu = new LU();
			lu.init(AH);
		}
	}

	@Override
	public void apply(Vector x, Vector _d)
	{
		if (c == null)
		{
			c = new Vector(A.num_rows());
		}
		if (d == null)
		{
			d = new Vector(A.num_rows());
		}
		d.assign(1.0, _d);
		x.set(0.0);
		step(x, c, A, d);
	}

	@Override
	public void step(Vector x, Vector c, FlexSparseMatrix A, Vector d)
	{
		for (int i = 0; i < nu1; i++)
		{
			jac.step(x, c, A, d);
		}

		dH.add(0.0, 1.0, R, d);
		if (nextAMG != null)
		{
			nextAMG.apply(eH, dH);
		} else
		{
			lu.solve(eH, dH);
		}

		c.add(0.0, 1.0, P, eH);
		x.add(1.0, 1.0, c);
		d.add(1.0, -1.0, A, c);

		for (int i = 0; i < nu2; i++)
		{
			jac.step(x, c, A, d);
		}
	}

	AMG()
	{
		level = 0;
	}

	AMG(int level)
	{
		this.level = level;
	}
	// variables
	FlexSparseMatrix A, P, R, AH;
	boolean[] coarse;
	int level = 0;
	AMG nextAMG = null;
	LU lu = null;
	Preconditioner jac;
	Vector dH, eH;
	Vector c, d;
	int nu1 = 2, nu2 = 2;
	// debug
	Grid grid;
	Grid nextGrid;
	String path;
}
