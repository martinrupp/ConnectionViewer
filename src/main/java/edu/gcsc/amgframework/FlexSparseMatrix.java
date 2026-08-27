/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.amgframework;

import java.util.Iterator;

/**
 *
 * @author mrupp
 */
public class FlexSparseMatrix
		implements SparseMatrix
{

	FlexSparseMatrix(boolean _bNeedsValues)
	{
		bNeedsValues = _bNeedsValues;
	}

	void resize(int rows, int _cols)
	{
		rowStart = new int[rows];
		rowEnd = new int[rows];
		rowMax = new int[rows];
		for (int i = 0; i < rows; i++)
			rowStart[i] = -1;
		m_numCols = _cols;
		if (bNeedsValues)
			values = new double[rows];
		cols = new int[rows];
		maxValues = 0;
	}

	@Override
	public int num_rows()
	{
		return rowStart.length;
	}

	@Override
	public int num_cols()
	{
		return m_numCols;
	}

	void copyToNewSize(int newSize)
	{
		double v[] = new double[newSize];
		int c[] = new int[newSize];
		int j = 0;
		for (int r = 0; r < num_rows(); r++)
		{
			if (rowStart[r] == -1) continue;			
			int start = j;
			for (int k = rowStart[r]; k < rowEnd[r]; k++, j++)
			{
				if (bNeedsValues)
					v[j] = values[k];
				c[j] = cols[k];
			}
			rowStart[r] = start;
			rowEnd[r] = rowMax[r] = j;
		}
		fragmented = 0;
		maxValues = j;
		if (bNeedsValues)
			values = v;
		cols = c;
	}

	void defragment()
	{
		copyToNewSize(nnz);
	}

	void assureValuesSize(int s)
	{
		if (s < cols.length)
			return;
		int newSize = nnz * 2;
		if (newSize < s)
			newSize = s;
		copyToNewSize(newSize);

	}

	public boolean has_connection(int r, int c)
	{
		return get_index(r, c, false) != -1;
	}

	private int get_index_internal(int row, int col)
	{
		assert (rowStart[row] != -1);
		int l = rowStart[row], r = rowEnd[row], mid = 0;
		while (l < r)
		{
			mid = (l + r) / 2;
			if (cols[mid] < col)
				l = mid + 1;
			else
			{
				if (cols[mid] > col)
					r = mid - 1;
				else
					return mid;				
			}
		}
		mid = (l + r) / 2;
		if (mid < rowStart[row])
			return rowStart[row];
		else if (mid == rowEnd[row] || col <= cols[mid])
			return mid;
		else
			return mid + 1;
	}

	int get_index(int r, int c, boolean bCreate)
	{
		if (rowStart[r] == -1)
		{
			if (!bCreate)
				return -1;
			assureValuesSize(maxValues + 1);
			rowStart[r] = maxValues;
			rowEnd[r] = maxValues + 1;
			rowMax[r] = maxValues + 1;
			if (bNeedsValues)
				values[maxValues] = 0.0;
			cols[maxValues] = c;
			maxValues++;
			nnz++;
			return maxValues - 1;
		}

		/*    for(int i=rowStart[r]; i<rowEnd[r]; i++)
		 if(cols[i] == c)
		 return i;*/
		int index = get_index_internal(r, c);
		if (index < rowEnd[r] && index < maxValues && cols[index] == c)
			return index;
		assert (index == rowEnd[r] || cols[index] > c);

		if (rowEnd[r] == rowMax[r])
		{
			int newSize = (rowEnd[r] - rowStart[r]) * 2;
			if (maxValues + newSize > cols.length)
			{
				assureValuesSize(maxValues + newSize);
				index = get_index_internal(r, c);
			}
			fragmented += rowEnd[r] - rowStart[r];
			index = index - rowStart[r] + maxValues;
			int j = rowEnd[r] - rowStart[r] + maxValues;
			for (int i = rowEnd[r] - 1; i >= rowStart[r]; i--, j--)
			{
				if (j == index)
					j--;
				if (bNeedsValues)
					values[j] = values[i];
				cols[j] = cols[i];
			}
			rowEnd[r] = maxValues + rowEnd[r] - rowStart[r] + 1;
			rowStart[r] = maxValues;
			rowMax[r] = maxValues + newSize;
			maxValues += newSize;
		}
		else
		{
			for (int i = rowEnd[r] - 1; i >= index; i--)
			{
				if (bNeedsValues)
					values[i + 1] = values[i];
				cols[i + 1] = cols[i];
			}
			rowEnd[r]++;
		}
		if (bNeedsValues)
			values[index] = 0.0;
		cols[index] = c;
		assert (index >= rowStart[r] && index < rowEnd[r]);
		nnz++;
		return index;

	}

	void set(int r, int c, double v)
	{
		int j = get_index(r, c, true);
		if (bNeedsValues)
			values[j] = v;
	}

	public double get(int r, int c)
	{
		int j = get_index(r, c, false);
		if (j == -1)
			return 0.0;
		else if(bNeedsValues)
			return values[j];
		else 
			return  1.0;
	}

	public void set_connection(int r, int c)
	{
		get_index(r, c, true);
	}

	@Override
	public Iterator<IConnection> begin_row(int r)
	{
		return new rowIterator(this, r);
	}

	FlexSparseMatrix mult(FlexSparseMatrix A)
	{
		return FlexSparseMatrix.Mult(this, A);
	}

	private static FlexSparseMatrix Mult(FlexSparseMatrix A, FlexSparseMatrix B)
	{
		FlexSparseMatrix R = new FlexSparseMatrix(true);

		R.resize(A.num_rows(), B.num_cols());
		assert (A.num_cols() == B.num_rows());
		for (int i = 0; i < A.num_rows(); i++)
		{
			for (IConnection itAik : A.row(i))
			{
				double Aik = itAik.value();
				if (Aik == 0.0) continue;
				int k = itAik.index();
				for (IConnection itBkj : B.row(k))
				{
					double Bkj = itBkj.value();
					if (Bkj == 0.0) continue;
					int j = itBkj.index();
					int index = R.get_index(i, j, true);
					R.values[index] += Aik * Bkj;
				}
			}
		}
		R.defragment();
		return R;

	}

	FlexSparseMatrix mult(FlexSparseMatrix B, FlexSparseMatrix C)
	{
		return FlexSparseMatrix.Mult(this, B, C);
	}

	private static FlexSparseMatrix Mult(FlexSparseMatrix A, FlexSparseMatrix B, FlexSparseMatrix C)
	{
		FlexSparseMatrix R = new FlexSparseMatrix(true);

		R.resize(A.num_rows(), C.num_cols());
		assert (A.num_cols() == B.num_rows() && B.num_cols() == C.num_rows());
		for (int i = 0; i < A.num_rows(); i++)
		{
			for (IConnection itAik : A.row(i))
			{
				double Aik = itAik.value();
				if (Aik == 0.0) continue;
				
				int k = itAik.index();
				for (IConnection itBkj : B.row(k))
				{
					double Bkj = itBkj.value();
					if (Bkj == 0.0) continue;
					int j = itBkj.index();
					for (IConnection itCjl : C.row(j))
					{
						double Cjl = itCjl.value();
						if (Cjl == 0.0)
						{
							continue;
						}
						int l = itCjl.index();
						int index = R.get_index(i, l, true);
						R.values[index] += Aik * Bkj * Cjl;
					}
				}
			}

		}
		R.defragment();
		return R;

	}

	class connection implements IConnection
	{

		FlexSparseMatrix A;

		connection(FlexSparseMatrix _A, int _i, double _v)
		{
			A = _A;
			i = _i;
			v = _v;
		}

		void set(double d)
		{
			A.values[i] = d;
		}

		void add(double d)
		{
			A.values[i] += d;
		}

		void sub(double d)
		{
			A.values[i] -= d;
		}

		@Override
		public double value()
		{
			return v;
		}

		@Override
		public int index()
		{
			return i;
		}
		int i;
		double v;
	}

	class rowIterator implements Iterator<IConnection>
	{

		FlexSparseMatrix A;
		int row;
		int end;
		int i;

		rowIterator(FlexSparseMatrix _A, int r)
		{
			A = _A;
			i = A.rowStart[r];
			if (i == -1)	end = -2;
			else			end = A.rowEnd[r];
			
		}

		rowIterator(FlexSparseMatrix _A, int r, int c)
		{
			A = _A;
			i = A.get_index(r, c, true);
		}

		@Override
		public boolean hasNext()
		{
			return i < end;
		}

		@Override
		public IConnection next()
		{
			if (A.bNeedsValues)
				return new connection(A, A.cols[i], A.values[i++]);
			else
				return new connection(A, A.cols[i++], 1.0);
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	@Override
	public Row row(int r)
	{
		return new Row(this, r);
	}

	public int num_connections()
	{
		return nnz;
	}

	public int num_connections(int r)
	{
		if (rowStart[r] == -1) return 0;
		return rowEnd[r] - rowStart[r];
	}

	public void print_statistic()
	{
		System.out.println("Matrix " + num_rows() + "x" + num_cols() + ". NNZ: " + nnz + ". Cols Length: " + cols.length + ". MaxValues=" + maxValues + ". Fragmented: " + fragmented
				+ " values surplus: " + (maxValues - nnz));
	}

	@Override
	public void print()
	{
		for (int r = 0; r < num_rows(); r++)
			for (IConnection c : row(r))
				System.out.println("A(" + r + ", " + c.index() + ") = " + c.value());		
	}

	FlexSparseMatrix transpose()
	{
		FlexSparseMatrix A = new FlexSparseMatrix(bNeedsValues);
		/*A.resize(num_cols(), num_rows());
		 for(int r=0; r<num_rows(); r++)
		 for(IConnection c : row(r))
		 A.set(c.index(), r, c.value());
		 A.defragment();*/

		A.rowStart = new int[num_cols()];
		A.rowMax = new int[num_cols()];
		A.rowEnd = new int[num_cols()];
		if (bNeedsValues)	A.values = new double[nnz];
		A.cols = new int[nnz];
		A.nnz = nnz;
		A.maxValues = maxValues;
		A.fragmented = 0;
		A.m_numCols = num_rows();
		int r, c;
		for (r = 0; r < num_rows(); r++)
			for (IConnection con : row(r))
				A.rowMax[con.index()]++;
			
		A.rowEnd[0] = A.rowMax[0];
		A.rowMax[0] = 0;
		for (c = 1; c < num_cols(); c++)
		{
			A.rowStart[c] = A.rowEnd[c - 1];
			A.rowEnd[c] = A.rowStart[c] + A.rowMax[c];
			A.rowMax[c] = A.rowStart[c];
		}
		// ?????
		for (r = 0; r < num_rows(); r++)
		{
			for (IConnection con : row(r))
			{
				c = con.index();
				A.cols[A.rowMax[c]] = r;
				if (bNeedsValues)
					A.values[A.rowMax[c]] = con.value();
				A.rowMax[c]++;
			}
		}
		// todo: sort rows
		return A;
	}

	@Override
	public double row_mult(Vector b, int r)
	{
		if(rowStart[r] == -1) return 0.0;
		double s = 0;		
		/*for (IConnection con : row(r))
			s += b.get(con.index()) * con.value();*/
		for(int i=rowStart[r]; i<rowEnd[r]; i++)
			s += b.get(cols[i]) * values[i];
		return s;
	}

	public boolean is_equal(SparseMatrix A)
	{
		for (int r = 0; r < num_rows(); r++)
			for (IConnection con : row(r))
				if (con.value() != A.get(r, con.index()))
					return false;
		return true;
	}
	private int rowStart[];
	private int rowEnd[];
	private int rowMax[];
	private int cols[];
	int fragmented = 0;
	int nnz = 0;
	boolean bNeedsValues;
	private double[] values;
	int maxValues;
	private int m_numCols;
}
