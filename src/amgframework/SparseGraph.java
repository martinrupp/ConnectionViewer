/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package amgframework;

/**
 *
 * @author mrupp
 */
public class SparseGraph extends FlexSparseMatrix
{
	public SparseGraph()
	{
		super(false);		
	}
	
	
	public void resize(int rows)
	{
		super.resize(rows, rows);
	}
}
