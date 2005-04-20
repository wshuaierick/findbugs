/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005, Tom Truscott <trt@unx.sas.com>
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

import java.text.NumberFormat;
import java.util.Set;
import java.util.HashSet;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindBadCast extends BytecodeScanningDetector implements Constants2, StatelessDetector {


	private HashSet<String> castTo = new HashSet<String>();

	BugReporter bugReporter;

	final static boolean DEBUG = false;

	public FindBadCast(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		abstractCollectionClasses.add("java/util/Collection");
		abstractCollectionClasses.add("java/util/List");
		abstractCollectionClasses.add("java/util/Set");
		abstractCollectionClasses.add("java/util/Map");
		concreteCollectionClasses.add("java/util/LinkedHashMap");
		concreteCollectionClasses.add("java/util/HashMap");
		concreteCollectionClasses.add("java/util/HashSet");
		concreteCollectionClasses.add("java/util/ArrayList");
		concreteCollectionClasses.add("java/util/LinkedList");
		concreteCollectionClasses.add("java/util/Hashtable");
		concreteCollectionClasses.add("java/util/Vector");
	}

	private Set<String> concreteCollectionClasses = new HashSet<String>();
	private Set<String> abstractCollectionClasses = new HashSet<String>();

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visit(JavaClass obj) {
	}

	public void visit(Method obj) {
	}

	private int parameters;
	OpcodeStack stack = new OpcodeStack();
	public void visit(Code obj) {
		if (DEBUG)  {
			System.out.println(getFullyQualifiedMethodName());
			}
		parameters = stack.resetForMethodEntry(this);
		castTo.clear();
		super.visit(obj);
	}


	public void sawOpcode(int seen) {
		if (DEBUG)  {
			System.out.println(stack);
			printOpCode(seen);
			}


		if (stack.getStackDepth() > 0) {
		if (seen == CHECKCAST) {
		OpcodeStack.Item it = stack.getStackItem(0);
		String signature = it.getSignature();
		if (signature.length() > 0 && signature.charAt(0) == 'L')
			signature = signature.substring(1,signature.length()-1);
		String signatureDot = signature.replace('/','.');
		String to = getClassConstantOperand();
		if (to.length() > 0 && to.charAt(0) == 'L')
			to = to.substring(1,to.length()-1);
		String toDot = to.replace('/','.');
		if (signature.length() > 0 
			&& !signature.equals("java/lang/Object") 
			&& !signature.equals(to)) {
		   if (concreteCollectionClasses.contains(to)
					&& !castTo.contains(to)) 
  bugReporter.reportBug(new BugInstance(this, "BC_BAD_CAST_TO_CONCRETE_COLLECTION", NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this)
                                .addClass(signatureDot)
                                .addClass(toDot)
				);
		   if (abstractCollectionClasses.contains(to)
			&& (signature.equals("java/util/Collection") 
			   ||  signature.equals("java/lang/Iterable") )
					&& !castTo.contains(to)) 
  bugReporter.reportBug(new BugInstance(this, "BC_BAD_CAST_TO_ABSTRACT_COLLECTION", NORMAL_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this)
                                .addClass(signatureDot)
                                .addClass(toDot)
				);

		     try {
			JavaClass toClass = Repository.lookupClass(toDot);
			JavaClass signatureClass 
				= Repository.lookupClass(signatureDot);
		     if  ( !castTo.contains(to)
				 && !Repository.instanceOf( signatureClass, toClass)) {
			if (
				 !Repository.instanceOf( toClass, signatureClass)
			&& (
			(!toClass.isInterface() && !signatureClass.isInterface())
			 || signatureClass.isFinal()
			 || toClass.isFinal()
			))
  bugReporter.reportBug(new BugInstance(this, "BC_IMPOSSIBLE_CAST", HIGH_PRIORITY)
                                .addClassAndMethod(this)
                                .addSourceLine(this)
                                .addClass(signatureDot)
                                .addClass(toDot));
		     else {
			int priority = NORMAL_PRIORITY;
			if (DEBUG)  {
			System.out.println("Checking BC in " + getFullyQualifiedMethodName());
			System.out.println("to class: " + toClass);
			System.out.println("from class: " + signatureClass);
			System.out.println("instanceof : " + 
				Repository.instanceOf( toClass, signatureClass)) ;
			}
			if (Repository.instanceOf( toClass, signatureClass)) 
				priority+=2;
			if (getThisClass().equals(toClass) || getThisClass().equals(signatureClass))
				priority+=1;
			if (DEBUG)
				System.out.println(" priority: " + priority);
			if (toClass.isInterface()) priority++;
			if (DEBUG)
				System.out.println(" priority: " + priority);
			if (priority <= LOW_PRIORITY && (signatureClass.isInterface()
				|| signatureClass.isAbstract())) priority++;
			if (DEBUG)
				System.out.println(" priority: " + priority);
			 if (abstractCollectionClasses.contains(to))
				priority--;
			if (DEBUG)
				System.out.println(" priority: " + priority);
			int reg = it.getRegisterNumber();
			if (reg >= 0 && reg < parameters
				&& it.isInitialParameter()
				&& getMethod().isPublic()) {
				priority--;
				if (getPC() < 4 && priority > LOW_PRIORITY)
					priority--;
				}
			if (DEBUG)
				System.out.println(" priority: " + priority);
			if (getMethodName().equals("compareTo"))
				priority++;
			if (DEBUG)
				System.out.println(" priority: " + priority);
			if (priority < HIGH_PRIORITY)
				priority = HIGH_PRIORITY;
			if (priority <= LOW_PRIORITY)
  bugReporter.reportBug(new BugInstance(this, "BC_UNCONFIRMED_CAST", priority)
                                .addClassAndMethod(this)
                                .addSourceLine(this)
                                .addClass(signatureDot)
                                .addClass(toDot));
			}

		     }


		     } catch (Exception e) {
				}
			}
		}
		else if (seen == INSTANCEOF) {
			String to = getClassConstantOperand();
			castTo.add(to);
			}
		}
		stack.sawOpcode(this,seen);

	}



	private void printOpCode(int seen) {
		System.out.print("  FindBadCast: [" + getPC() + "]  " + OPCODE_NAMES[seen]);
		if ((seen == INVOKEVIRTUAL) || (seen == INVOKESPECIAL) || (seen == INVOKEINTERFACE) || (seen == INVOKESTATIC))
			System.out.print("   " + getClassConstantOperand() + "." + getNameConstantOperand() + " " + getSigConstantOperand());
		else if (seen == LDC || seen == LDC_W || seen == LDC2_W) {
			Constant c = getConstantRefOperand();
			if (c instanceof ConstantString)
				System.out.print("   \"" + getStringConstantOperand() + "\"");
			else if (c instanceof ConstantClass)
				System.out.print("   " + getClassConstantOperand());
			else
				System.out.print("   " + c);
		} else if ((seen == ALOAD) || (seen == ASTORE))
			System.out.print("   " + getRegisterOperand());
		else if ((seen == GOTO) || (seen == GOTO_W)
		||       (seen == IF_ACMPEQ) || (seen == IF_ACMPNE)
		||       (seen == IF_ICMPEQ) || (seen == IF_ICMPGE)
		||       (seen == IF_ICMPGT) || (seen == IF_ICMPLE)
		||       (seen == IF_ICMPLT) || (seen == IF_ICMPNE)
		||       (seen == IFEQ) 	|| (seen == IFGE)
		||       (seen == IFGT) 	|| (seen == IFLE)
		||       (seen == IFLT) 	|| (seen == IFNE)
		||       (seen == IFNONNULL) || (seen == IFNULL))
			System.out.print("   " + getBranchTarget());
		else if ((seen == NEW) || (seen == INSTANCEOF))
			System.out.print("   " + getClassConstantOperand());
		else if ((seen == TABLESWITCH) || (seen == LOOKUPSWITCH)) {
			System.out.print("    [");
			int switchPC = getPC();
			int[] offsets = getSwitchOffsets();
			for (int i = 0; i < offsets.length; i++) {
				System.out.print((switchPC + offsets[i]) + ",");
			}
			System.out.print((switchPC + getDefaultSwitchOffset()) + "]");
		}

		System.out.println();
	}
}
