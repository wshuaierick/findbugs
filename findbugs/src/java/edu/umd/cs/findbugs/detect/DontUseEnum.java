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

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class DontUseEnum extends BytecodeScanningDetector {

	BugReporter bugReporter;

	public DontUseEnum(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	//@Override
	public void visit(Method obj) {
		if (obj.getName().equals("enum") || obj.getName().equals("assert")) {
			BugInstance bug = new BugInstance(this, "TESTING", NORMAL_PRIORITY)
			.addClass(this).addMethod(this);
			bugReporter.reportBug(bug);
		}
	}
	
	@Override
	public void visit(Field obj) {
		if (obj.getName().equals("enum") || obj.getName().equals("assert")) {
			BugInstance bug = new BugInstance(this, "TESTING", NORMAL_PRIORITY)
			.addClass(this).addField(this);
			bugReporter.reportBug(bug);
		}
	}

	@Override
	public void visit(Code obj) {
		if (true) // do we want to dismantle the bytecode?
			super.visit(obj);
	}

	int prevOpcode;
	@Override
	public void sawOpcode(int seen) {

		if (prevOpcode == I2D && seen == INVOKESTATIC
				&& getNameConstantOperand().equals("ceil")
				&& getClassConstantOperand().equals("java.lang.Math"))
			bugReporter.reportBug(new BugInstance(this, "TESTING", HIGH_PRIORITY)
			.addClassAndMethod(this).addCalledMethod(this).addSourceLine(this));


		prevOpcode = seen;
	}
}
