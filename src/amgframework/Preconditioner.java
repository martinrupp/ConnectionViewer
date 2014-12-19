/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package amgframework;

/**
 *
 * @author mrupp
 */
public interface Preconditioner
{
    void init(FlexSparseMatrix A);
    void apply(Vector corr, Vector defect);
    void step(Vector x, Vector c, FlexSparseMatrix A, Vector defect);
}
