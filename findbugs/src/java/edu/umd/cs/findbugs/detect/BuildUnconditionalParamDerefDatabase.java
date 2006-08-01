/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessProperty;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefDataflow;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefSet;

/**
 * Build database of unconditionally dereferenced parameters.
 * 
 * @author David Hovemeyer
 */
public class BuildUnconditionalParamDerefDatabase {
	private static final boolean VERBOSE_DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg.verbose");
	private static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg") || VERBOSE_DEBUG;
	
	public void visitClassContext(ClassContext classContext) {
		ParameterNullnessPropertyDatabase database =
			AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase();
		
		if (VERBOSE_DEBUG) System.out.println("Visiting class " + classContext.getJavaClass().getClassName());
		Method[] methodList = classContext.getJavaClass().getMethods();
		for (Method method : methodList) {
			boolean hasReferenceParameters = false;
			for (Type argument : method.getArgumentTypes())
				if (argument instanceof ReferenceType)
					hasReferenceParameters = true;

			if (!hasReferenceParameters) continue;

			if (classContext.getMethodGen(method) == null)
				continue; // no code

			if (VERBOSE_DEBUG) System.out.println("Check " + method);
			analyzeMethod(classContext, method);
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) {
		try {
			CFG cfg = classContext.getCFG(method);
			UnconditionalDerefDataflow dataflow = classContext.getUnconditionalDerefDataflow(method);
			UnconditionalDerefSet unconditionalDerefSet = dataflow.getResultFact(cfg.getEntry());
			
			if (!unconditionalDerefSet.isValid()) {
				if (VERBOSE_DEBUG) {
					System.out.println("\tResult is not valid: " + unconditionalDerefSet.toString());
				}
				return;
			}
			
			if (unconditionalDerefSet.isEmpty()) {
				if (VERBOSE_DEBUG) {
					System.out.println("\tResult is empty");
				}
				return;
			}
			
			if (VERBOSE_DEBUG) {
				System.out.println("\tAdding result " + unconditionalDerefSet.toString() + " to database");
			}

			ParameterNullnessProperty property = new ParameterNullnessProperty();
			property.setNonNullParamSet(unconditionalDerefSet);
			
			XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
			AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase().setProperty(xmethod, property);
			if (DEBUG) {
				System.out.println("Unconditional deref: " + xmethod + "=" + property);
			}
		} catch (CFGBuilderException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().logError(
					"Error analyzing " + method + " for unconditional deref training", e);
		} catch (DataflowAnalysisException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().logError(
					"Error analyzing " + method + " for unconditional deref training", e);
		}
	}

}
