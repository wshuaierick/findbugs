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

package edu.umd.cs.findbugs.bcel;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

/**
 * Base class for Detectors that want to scan the bytecode
 * of a method and use an opcode stack.
 *
 * @see BytecodeScanningDetector
 */
abstract public class OpcodeStackDetector extends BytecodeScanningDetector {

	protected OpcodeStack stack = new OpcodeStack();
	
	@Override
	public void beforeOpcode(int seen) {
		stack.mergeJumps(this);
	}
	
	@Override
	public void afterOpcode(int seen) {
		stack.sawOpcode(this, seen);
	}
	
	@Override
	abstract public void sawOpcode(int seen);
}
