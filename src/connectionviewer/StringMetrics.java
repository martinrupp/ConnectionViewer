/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package connectionviewer;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

class StringMetrics {

  Font font;
  FontRenderContext context;

  public StringMetrics(Graphics2D g2) {

    font = g2.getFont();
    context = g2.getFontRenderContext();
  }

   public StringMetrics(Graphics g) {

    font = g.getFont();
	Graphics2D g2 = (Graphics2D) g;
    context = g2.getFontRenderContext();
  }

  Rectangle2D getBounds(String message) {

    return font.getStringBounds(message, context);
  }

  int getWidth(String message) {

    Rectangle2D bounds = getBounds(message);
    return (int)(bounds.getWidth());
  }

  int getHeight(String message) {

    Rectangle2D bounds = getBounds(message);
    return (int)(bounds.getHeight());
  }

}