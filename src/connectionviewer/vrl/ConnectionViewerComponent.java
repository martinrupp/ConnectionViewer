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
 *
 * 
 * @author Martin Rupp <martin.rupp@gcsc.uni-frankfurt.de>
 */
package connectionviewer.vrl;

/**
 * This class is for connection of ConnectionViewer with
 * the VRL application. This is also the reason why connectionviewer is
 * seperated into ConnectionViewerPanel and ConnectionViewer.
 */

import connectionviewer.ConnectionViewerParameters;
import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.MethodInfo;
import eu.mihosoft.vrl.annotation.ObjectInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import java.io.File;
import java.io.Serializable;


// ComponentInfo defines the menu entry for this component.
@ComponentInfo(name="ConnectionViewer Component", category="VRL")


// ObjectInfo defines the window title of this component.
@ObjectInfo(name="ConnectionViewer Component")
public class ConnectionViewerComponent implements Serializable
{
    private static final long serialVersionUID=1L;
    
    @MethodInfo(valueStyle="sample")
    public ConnectionViewerParameters loadFile(
            // param info defines the name and the style of the parameter
            // and an option string (supported file extension in this case)
            @ParamInfo(
					name="File",
					style="load-dialog",
                    options=
						"endings=[\"mat\",\"pmat\",\"vec\",\"pvec\"]; "
						+"description=\"ConnectionViewer Files (Vectors, Matrices)\"; "
						+"invokeOnChange=true"
                    )
                    File f
                    )
    {
        // this is just a sample, we return the file path
        return new ConnectionViewerParameters(f.getPath());
    }
}
