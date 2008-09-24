/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FirstPassDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassSummary;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.util.ClassName;

import org.apache.bcel.classfile.Code;

import java.util.HashSet;
import java.util.Set;

public class EqualsOperandShouldHaveClassCompatibleWithThis extends OpcodeStackDetector implements FirstPassDetector {

	
	final BugReporter bugReporter;
	final BugAccumulator bugAccumulator;

	final ClassSummary classSummary = new ClassSummary();
	Set<ClassDescriptor> classWithFunkyEqualsMethods = new HashSet<ClassDescriptor>();
	
		
	public EqualsOperandShouldHaveClassCompatibleWithThis(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.bugAccumulator = new BugAccumulator(bugReporter);
		AnalysisContext context = AnalysisContext.currentAnalysisContext();
		context.setClassSummary(classSummary);
		}

    
    @Override
    public void visit(Code obj) {
    	if (getMethodName().equals("equals") && getMethodSig().equals("(Ljava/lang/Object;)Z") ) {
    		super.visit(obj);
    		bugAccumulator.reportAccumulatedBugs();
    	}
    	
    }
 
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     */
    @Override
    public void sawOpcode(int seen) {
    	if (seen == INVOKEVIRTUAL) {
    		if (getNameConstantOperand().equals("equals") && getSigConstantOperand().equals("(Ljava/lang/Object;)Z")) {
    			OpcodeStack.Item item = stack.getStackItem(1);
    			ClassDescriptor c = DescriptorFactory.createClassDescriptorFromSignature(item.getSignature());
    			check(c);
				 
    		} else if (getClassConstantOperand().equals("java/lang/Class") 
    					&& (getNameConstantOperand().equals("isInstance") || getNameConstantOperand().equals("cast"))
    				) {
    			 OpcodeStack.Item item = stack.getStackItem(1);
    			 if (item.getSignature().equals("Ljava/lang/Class;")) {
    				 Object value = item.getConstant();
    				 if (value instanceof String) {
    					 ClassDescriptor c = DescriptorFactory.createClassDescriptor((String)value);
    					 check(c);
    				 }
    			 }
    			    
    		}
    		
    	}
    	else if (seen == INSTANCEOF || seen == CHECKCAST) {
	    	check(getClassDescriptorOperand());
	    }
	    
    }


	/**
     * 
     */
    private void check(ClassDescriptor c) {
	    OpcodeStack.Item item = stack.getStackItem(0);
	    if (item.isInitialParameter() && item.getRegisterNumber() == 1) {
	    	ClassDescriptor thisClassDescriptor = getClassDescriptor();
	    	if (c.equals(thisClassDescriptor)) return;
	    	Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
	    	try {
	            if (subtypes2.isSubtype(c, thisClassDescriptor)) return;
	        
	            if (subtypes2.isSubtype(thisClassDescriptor,c)) return;
	            IncompatibleTypes check = IncompatibleTypes.getPriorityForAssumingCompatible(false, thisClassDescriptor, c);
	            int priority = check.getPriority();
	            if ("java/lang/Object".equals(getSuperclassName()) && ClassName.isAnonymous(getClassName()))
	            		priority++;
	            bugAccumulator.accumulateBug(new BugInstance(this, "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS", priority).addClassAndMethod(this)
	            		.addType(c).describe(TypeAnnotation.FOUND_ROLE), this);
	            classSummary.checksForEqualTo(thisClassDescriptor, c);
	            
	    		
	    	} catch (ClassNotFoundException e) {
	            bugReporter.reportMissingClass(e);
	        } catch (CheckedAnalysisException e) {
	            bugReporter.logError("error", e);
	        }
	    	
	    	
	    	
	    	
	    }
    }

	
}
