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

import java.util.*;

/**
 * Merge a saved results file (containing annotations) with a new results file.
 * This is useful when re-running FindBugs after changing the detectors
 * (e.g., to fix false positives).  All of the annotations from the original
 * run for bugs still present in the new run are preserved in the output file
 * (whose bugs are identical to the new run).  Note that some annotations
 * can be lost, if those bugs are not present in the new run.
 *
 * @author David Hovemeyer
 */
public class MergeResults {
	private static final boolean VERSION_INSENSITIVE = Boolean.getBoolean("mergeResults.vi");

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + MergeResults.class.getName() + " <orig results> <new results> <output file>");
			System.exit(1);
		}

		if (VERSION_INSENSITIVE) {
			System.out.println("Using version-insensitive bug comparator");
		}

		DetectorFactoryCollection.instance(); // as a side effect, loads detector plugins

		String origResultsFile = argv[0];
		String newResultsFile = argv[1];
		String outputFile = argv[2];

		Project project = new Project();

		SortedBugCollection origCollection = new SortedBugCollection();
		SortedBugCollection newCollection = new SortedBugCollection();

		origCollection.readXML(origResultsFile, new Project());
		newCollection.readXML(newResultsFile, project);

		SortedSet<BugInstance> origSet = createSet(origCollection);
		SortedSet<BugInstance> newSet = createSet(newCollection);

		int numPreserved = 0;
		int numLost = 0;
		int numLostWithAnnotations = 0;

		Iterator<BugInstance> i = origSet.iterator();
		while (i.hasNext()) {
			BugInstance orig = i.next();
			if (newSet.contains(orig)) {
				SortedSet<BugInstance> tailSet = newSet.tailSet(orig);
				BugInstance matching = tailSet.first();
				matching.setAnnotationText(orig.getAnnotationText());
				numPreserved++;
			} else {
				numLost++;
				if (!orig.getAnnotationText().equals("")) {
					System.out.println("Losing a bug with an annotation:");
					System.out.println(orig.getMessage());
					SourceLineAnnotation srcLine = orig.getPrimarySourceLineAnnotation();
					if (srcLine != null)
						System.out.println("\t" + srcLine.toString());
					System.out.println(orig.getAnnotationText());
					numLostWithAnnotations++;
				}
			}
		}

		System.out.println(numPreserved + " preserved, " +
			numLost + " lost (" + numLostWithAnnotations + " lost with annotations)");

		newCollection.writeXML(outputFile, project);
	}

	private static SortedSet<BugInstance> createSet(BugCollection bugCollection) {
		TreeSet<BugInstance> set = VERSION_INSENSITIVE
			? new TreeSet<BugInstance>(VersionInsensitiveBugComparator.instance())
			: new TreeSet<BugInstance>();
		set.addAll(bugCollection.getCollection());
		return set;
	}
}

// vim:ts=4
