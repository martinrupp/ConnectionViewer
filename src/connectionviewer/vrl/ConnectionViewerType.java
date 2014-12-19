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

package connectionviewer.vrl;

import connectionviewer.ConnectionViewerPanel;
import connectionviewer.ConnectionViewerParameters;
import eu.mihosoft.vrl.reflection.RepresentationType;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.visual.Ruler;
import eu.mihosoft.vrl.visual.VGraphicsUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 *
 * @author Martin Rupp <martin.rupp@gcsc.uni-frankfurt.de>
 */
public class ConnectionViewerType
        extends TypeRepresentationBase
{
    private static final long serialVersionUID = 1L;

    ConnectionViewerParameters parameters;
    long lastModifiedDate = -1;

    ConnectionViewerPanel jConnectionViewerPanel;
    public ConnectionViewerType() {

        setType(ConnectionViewerParameters.class); // visualization for String
        setValueName("ConnectionViewerParameters:"); // name of the visualization
        setStyleName("default"); // style name: can be chosen via annotation

        // Define this type representation as input
        addSupportedRepresentationType(RepresentationType.INPUT);

        this.setLayout(new BorderLayout());
        //this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	
	
	jConnectionViewerPanel = new ConnectionViewerPanel();
	
        
        this.setMinimumSize(jConnectionViewerPanel.getMinimumSize());
        this.setPreferredSize(jConnectionViewerPanel.getPreferredSize());
	
	
	this.setBorder(VGraphicsUtil.createDebugBorder());
        this.setLayout(new BorderLayout());
        // Add ruler
	JPanel right_panel = new JPanel();
	right_panel.setLayout(new BoxLayout(right_panel,BoxLayout.Y_AXIS));
	right_panel.setOpaque(false);
	JPanel filler = new JPanel();
	filler.setOpaque(false);
	filler.setPreferredSize(new Dimension(0,200));
	Ruler xychart_panel_ruler = new Ruler(this);
	right_panel.add(filler);
	right_panel.add(xychart_panel_ruler);
	this.add(right_panel,BorderLayout.EAST);

	this.add(jConnectionViewerPanel,BorderLayout.CENTER);
	
        // define this type representation as output
        addSupportedRepresentationType(RepresentationType.OUTPUT);        
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
}
