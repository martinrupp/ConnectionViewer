/*
 Copyright (c) 2015, Martin Rupp, University Frankfurt
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


package connectionviewer;

/**
 *
 * @author mrupp
 */
public class CommandLineHelper
{
	String args[];
	CommandLineHelper(String _args[])
	{
		args = _args;
	}
	
	int ParamPos(String param)
	{		
		for(int i=0; i<args.length; i++)
			if(args[i].compareTo(param) == 0)
				return i;
		return -1;
	}
	
	Boolean HasParam(String param)
	{		
		return ParamPos(param) != -1;
	}
		
	
	double GetParamDouble(String param, double def)
	{
		int p = ParamPos(param);
		if(p == -1) return def;
		return Double.parseDouble(args[p+1]);
	}
	
	int GetParamInt(String param, int def)
	{
		int p = ParamPos(param);
		if(p == -1) return def;
		return Integer.parseInt(args[p+1]);
	}
	
	String GetParamString(String param, String def)
	{
		int p = ParamPos(param);
		if(p == -1) return def;
		return args[p+1];			
	}
}
