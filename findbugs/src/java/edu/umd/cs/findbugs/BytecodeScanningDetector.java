/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.findbugs;

import edu.umd.cs.daveho.ba.ClassContext;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;

/**
 * Base class for Detectors which want to extend DismantleBytecode.
 * @see DismantleBytecode
 */
public class BytecodeScanningDetector extends DismantleBytecode implements Detector {
  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
  }

  public void report() { }
}
