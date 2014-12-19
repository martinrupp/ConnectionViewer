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

import connectionviewer.ConnectionViewer;
import eu.mihosoft.vrl.system.*;
import eu.mihosoft.vrl.visual.ActionDelelator;
import eu.mihosoft.vrl.visual.VAction;
import eu.mihosoft.vrl.visual.VActionGroup;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Martin Rupp <martin.rupp@gcsc.uni-frankfurt.de>
 */
public class CustomPluginConfigurator extends VPluginConfigurator {

    static final public String sConnectionViewerVersion = "0.5";

    public CustomPluginConfigurator() {
        // specifiy the plugin name, version
        setIdentifier(new PluginIdentifier("ConnectionViewer", sConnectionViewerVersion));

        // add dependencies
        // e.g.: addDependency(new PluginDependency("VRL-UG4", "0.1","0.2"));
        
        addDependency(new PluginDependency("VRL", "0.4.0", "0.4.x"));
    }

    @Override
    public void register(PluginAPI papi) {

        if (papi instanceof VPluginAPI) {
            VPluginAPI vApi = (VPluginAPI) papi;

            // register type representation
            vApi.addTypeRepresentation(new ConnectionViewerType());

            // register component.
            // components must:
            // a) provide a public constructor without parameters
            // b) implement serializable interface:
            //      all fields must be serializable
            //      or marked as transient
            //      (see Java documentation for detailed instructions)
            vApi.addComponent(ConnectionViewerComponent.class);


            // add connectionviewer to tool menu
            VActionGroup actionGroup = new VActionGroup("ConnectionViewer");

            actionGroup.add(new VAction("Start ConnectionViewer") {

                @Override
                public void actionPerformed(ActionEvent e, Object owner) {
                    try {
                        ConnectionViewer.main(new String[]{});
                    } catch (InterruptedException ex) {
                        Logger.getLogger(CustomPluginConfigurator.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }
                }
            });

            vApi.addAction(actionGroup, ActionDelelator.TOOL_MENU);
        }

    }

    @Override
    public void unregister(PluginAPI papi) {
        // currently not necessary
    }

    @Override
    public void init(InitPluginAPI iApi) {
        // currently not necessary
    }

    public void init()
    {
	throw new UnsupportedOperationException("Not supported yet.");
    }
}
