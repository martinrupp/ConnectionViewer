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
 */

package edu.gcsc.connectionviewer.vrl;

import edu.gcsc.connectionviewer.ConnectionViewerPanel;
import edu.gcsc.connectionviewer.ConnectionViewerParameters;
import eu.mihosoft.vrl.annotation.TypeInfo;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.visual.Ruler;
import groovy.lang.Script;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.io.File;
import javax.swing.*;

/**
 *
 * @author Martin Rupp <martin.rupp@gcsc.uni-frankfurt.de>
 */
@TypeInfo(input = true, output = true, style="default", type=ConnectionViewerParameters.class)
public class ConnectionViewerType
        extends TypeRepresentationBase
{
    private static final long serialVersionUID = 1L;

    ConnectionViewerParameters parameters;
    long lastModifiedDate = -1;

    ConnectionViewerPanel jConnectionViewerPanel;
    public ConnectionViewerType() {

        setValueName("ConnectionViewerParameters:"); // name of the visualization

        setUpdateLayoutOnValueChange(false);

	    jConnectionViewerPanel = new ConnectionViewerPanel() {

            {
                addComponentListener(this);
                this.addDividerLocationChangeListener((c)->{
                    componentResized(null);
                });
            }

            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                ConnectionViewerType.this.setValueOptions("width=" + getWidth() + ";"
                        + "height=" + getHeight() +"; dividerLoc = " + getDividerLocation());
            }

        };
	
        jConnectionViewerPanel.setDividerLocation(0);
            
        this.setLayout(new BorderLayout());
        // Add ruler
        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(Box.createVerticalGlue());
        box.add(new Ruler(jConnectionViewerPanel));
        this.add(box,BorderLayout.EAST);
        this.add(jConnectionViewerPanel,BorderLayout.CENTER);

        setMinimumPlotPaneSize(new Dimension(300,200));
    }

    @Override
    public void setViewValue(Object o) {
        if (o instanceof ConnectionViewerParameters)
        {
            parameters = (ConnectionViewerParameters) o;
            File f = new File(parameters.filename);
            long modDate = f.lastModified();
            if(modDate != lastModifiedDate)
            jConnectionViewerPanel.readFile(parameters.filename);
            lastModifiedDate = modDate;
        }
    }

    @Override
    public Object getViewValue() {
        return parameters;
    }

    @Override
    public void emptyView() {
    }

    protected void setMinimumPlotPaneSize(Dimension plotPaneSize) {

        jConnectionViewerPanel.setPreferredSize(plotPaneSize);
        jConnectionViewerPanel.setSize(plotPaneSize);
        jConnectionViewerPanel.setMinimumSize(plotPaneSize);
        Dimension minimumPlotPaneSize = plotPaneSize;

        setValueOptions("width=" + plotPaneSize.width + ";"
                + "height=" + plotPaneSize.height);
    }

    private void setPlotPaneSizeFromValueOptions(Script script) {
        Object property = null;
        Integer w = 400;
        Integer h = 300;
        Integer dividerLoc = null;

        if (getValueOptions() != null) {

            if (getValueOptions().contains("width")) {
                property = script.getProperty("width");
            }

//            System.out.println("Property:" + property.getClass());

            if (property != null) {
                w = (Integer) property;
            }

            property = null;

            if (getValueOptions().contains("height")) {
                property = script.getProperty("height");
            }

            if (property != null) {
                h = (Integer) property;
            }
            
            property = null;

            if (getValueOptions().contains("dividerLoc")) {
                property = script.getProperty("dividerLoc");
            }

            if (property != null) {
                dividerLoc = (Integer) property;
            }

        }

        if (w != null && h != null) {
            jConnectionViewerPanel.setPreferredSize(new Dimension(w, h));
            jConnectionViewerPanel.setSize(new Dimension(w, h));
        }
        
        if(dividerLoc!=null) {
            jConnectionViewerPanel.setDividerLocation(dividerLoc);
        }
    }

    @Override
    protected void evaluationRequest(Script script) {
        setPlotPaneSizeFromValueOptions(script);
    }
}
