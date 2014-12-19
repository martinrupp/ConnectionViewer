/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionviewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mrupp
 */
public class tikzGraphics2D extends EmptyGraphics2D
{
	double xscale, yscale;
	FileWriter file;
	String colorstring, fillstring;
	Color currentColor, setColor;
	Rectangle baseRect;
	tikzGraphics2D(Rectangle baseRect, FileWriter file, double scaling) throws IOException
	{
		this.baseRect = baseRect;
		this.file = file;
		
		setColor = currentColor = Color.BLACK;
		file.write("\\begin{tikzpicture}\n");		
		fillstring=colorstring = "";
		xscale= scaling;
		yscale = -scaling;
	}
	
	void filewrite(String s)
	{
		try {
			file.write(s);
		} catch (IOException ex) {
			Logger.getLogger(tikzGraphics2D.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	void checkColor()
	{
		if(currentColor == setColor) return;
		if(currentColor.equals(setColor))
			;
		else if(setColor.equals(Color.BLACK))
			fillstring=colorstring = "";
		else
		{
			filewrite("\\definecolor{myc}{RGB}{" + setColor.getRed() + "," + setColor.getGreen() + "," + setColor.getBlue() + "}\n");	
			colorstring = "[draw=myc]";
			fillstring = "[fill=myc]";
		}
		currentColor = setColor;		
	}
	
	 boolean clipLine(Line2D line, Rectangle2D rect) 
	 {

        double x1 = line.getX1();
        double y1 = line.getY1();
        double x2 = line.getX2();
        double y2 = line.getY2();

        double minX = rect.getMinX();
        double maxX = rect.getMaxX();
        double minY = rect.getMinY();
        double maxY = rect.getMaxY();

        int f1 = rect.outcode(x1, y1);
        int f2 = rect.outcode(x2, y2);

        while ((f1 | f2) != 0) {
            if ((f1 & f2) != 0) {
                return false;
            }
            double dx = (x2 - x1);
            double dy = (y2 - y1);
            // update (x1, y1), (x2, y2) and f1 and f2 using intersections
            // then recheck
            if (f1 != 0) {
                // first point is outside, so we update it against one of the
                // four sides then continue
                if ((f1 & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT
                        && dx != 0.0) {
                    y1 = y1 + (minX - x1) * dy / dx;
                    x1 = minX;
                }
                else if ((f1 & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT
                        && dx != 0.0) {
                    y1 = y1 + (maxX - x1) * dy / dx;
                    x1 = maxX;
                }
                else if ((f1 & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM
                        && dy != 0.0) {
                    x1 = x1 + (maxY - y1) * dx / dy;
                    y1 = maxY;
                }
                else if ((f1 & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP
                        && dy != 0.0) {
                    x1 = x1 + (minY - y1) * dx / dy;
                    y1 = minY;
                }
                f1 = rect.outcode(x1, y1);
            }
            else if (f2 != 0) {
                // second point is outside, so we update it against one of the
                // four sides then continue
                if ((f2 & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT
                        && dx != 0.0) {
                    y2 = y2 + (minX - x2) * dy / dx;
                    x2 = minX;
                }
                else if ((f2 & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT
                        && dx != 0.0) {
                    y2 = y2 + (maxX - x2) * dy / dx;
                    x2 = maxX;
                }
                else if ((f2 & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM
                        && dy != 0.0) {
                    x2 = x2 + (maxY - y2) * dx / dy;
                    y2 = maxY;
                }
                else if ((f2 & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP
                        && dy != 0.0) {
                    x2 = x2 + (minY - y2) * dx / dy;
                    y2 = minY;
                }
                f2 = rect.outcode(x2, y2);
            }
        }

        line.setLine(x1, y1, x2, y2);
        return true;  // the line is visible - if it wasn't, we'd have
                      // returned false from within the while loop above

    }
	
	Rectangle clipRectangle(int x, int y, int dx, int dy)
	{
		Rectangle r = new Rectangle(x, y, dx, dy);
		if(baseRect.intersects(r))
		{
			Rectangle result = new Rectangle();
			Rectangle.intersect(baseRect, r, result);
			return result;				
		}
		else
			return null;
	}
	
	boolean clipPoint(double x, double y)
	{
		return baseRect.contains(x, y);
	}
			
	
	@Override
	public void drawLine(int x, int y, int x2, int y2) {
	
		Line2D l = new Line2D.Double(x, y, x2, y2);
		if(clipLine(l, baseRect))
		{
			checkColor();
			filewrite("\\draw"+colorstring+"(" + l.getX1()/xscale + ", " + l.getY1()/yscale + ") -- (" 
					+ (l.getX2())/xscale + ", " + (l.getY2())/yscale + ");\n");		
		}
	}
	
	
	@Override
	public void fillRect(int x, int y, int dx, int dy) {
		Rectangle r = clipRectangle(x, y, dx, dy);
		if(r != null)
		{
			checkColor();
			filewrite("\\fill"+fillstring+"("+r.x/xscale+","+r.y/yscale+") rectangle ("+(r.x+r.width)/xscale+","+(r.y+r.height)/yscale+");\n");
		}
	}
	
	@Override
	public void drawString(String string, int x, int y) {
		this.drawString(string, (float)x, (float)y);
	}

	@Override
	public void drawString(String string, float x, float y) {
		if(clipPoint(x, y))
			filewrite("\\draw("+x/xscale+","+y/yscale+") node {\\verb@"+string+"@};\n");
	}
	
	@Override
	public void setColor(Color color) {		
		setColor = color;
	}	
	
	void finish() throws IOException
	{
		file.write("\\end{tikzpicture}\n");
	}
}
