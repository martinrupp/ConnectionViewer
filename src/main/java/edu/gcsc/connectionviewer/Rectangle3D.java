/*
Copyright (c) 2011-2013, Martin Rupp, University Frankfurt
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
   This product includes software developed by Martin Rupp, University Frankfurt
4. Neither the name of the University nor the
   names of its contributors may be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY FRANKFURT ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY FRANKFURT BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * @author Martin Rupp
 * @email martin.rupp@gcsc.uni-frankfurt.de
 */

package edu.gcsc.connectionviewer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

class Rectangle3D extends Rectangle2D.Double
{
    Rectangle3D()
    {
	zmin = zmax = 0;
    }
    public void setRect(double x, double y, double z, double dx, double dy, double dz)
    {
	setRect(x, y, dx, dy);
	zmin = z;
	zmax = z+dz;
    }

    public void setRect(Rectangle3D rect)
    {
	setRect((Rectangle2D.Double) rect);
	zmin = rect.zmin;
	zmax = rect.zmax;
    }

    public void add(Rectangle3D rect)
    {
	add((Rectangle2D.Double) rect);
	zmin = Math.min(zmin, rect.zmin);
	zmax = Math.max(zmax, rect.zmax);
    }

	public void add(double x, double y, double z)
    {
		add(x, y);
		zmin = Math.min(zmin, z);
		zmax = Math.max(zmax, z);
    }
    public void add(Point3D p)
    {
		add((Point2D.Double) p);
		zmin = Math.min(zmin, p.z);
		zmax = Math.max(zmax, p.z);
    }

    public double getCenterZ()
    {
	return (zmin + zmax) / 2;
    }
    public double zmin;
    public double zmax;

     @Override
    public String toString()
    {
	return "Rectangle3D(" + getX() + " .. " + (getX()+getWidth()) + ", "+ getY() +" .. " +
		(getY()+getHeight()) + ", "
		+ zmin + " .. "+ zmax + ")";
    }
}
