/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.daveho.ba;

import java.util.*;

/**
 * A "value number" is a value produced somewhere in a methods.
 * We use value numbers as dataflow values in Frames.  When two frame
 * slots have the same value number, then the same value is in both
 * of those slots.
 *
 * <p> Instances of ValueNumbers produced by the same
 * {@link ValueNumberFactory ValueNumberFactory} are unique, so reference equality may
 * be used to determine whether or not two value numbers are the same.
 * In general, ValueNumbers from different factories cannot be compared.
 *
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class ValueNumber {
	/** The value number. */
	private int number;

	/**
	 * Constructor.
	 * @param number the value number
	 */
	ValueNumber(int number) {
		this.number = number;
	}

	public String toString() {
		return "(" + number + ")";
	}

	public int hashCode() {
		return System.identityHashCode(this);
	}

	public boolean equals(Object o) {
		return this == o;
	}

}

// vim:ts=4
