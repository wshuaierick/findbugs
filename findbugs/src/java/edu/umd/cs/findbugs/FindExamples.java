/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 University of Maryland
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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

/**
 * Find bug instances annotated with "GOOD_EXAMPLE" or "EXCELLENT_EXAMPLE".
 */
public class FindExamples {
	private boolean brief = false;
	private String category = null;

	public static void main(String[] argv) throws Exception {
		new FindExamples().execute(argv);
	}

	public void execute(String[] argv) throws Exception {
		int start = 0;
/*
		if (argv[0].equals("-category")) {
			category = argv[1];
			start = 2;
		}
*/
		while (start < argv.length && argv[start].startsWith("-")) {
			if (argv[start].equals("-category"))
				category = argv[1];
			else if (argv[start].equals("-brief"))
				brief = true;
			else
				throw new IllegalArgumentException("Unknown option: " + argv[start]);
			++start;
		}

		if (start == argv.length) {
			System.err.println("Usage: " + FindExamples.class.getName() +
				" [-category <category>]" +
				" [-brief]" +
				" <results file>");
			System.exit(1);
		}

		DetectorFactoryCollection.instance(); // load plugins

		for (int i = start; i < argv.length; ++i)
			scan(argv[i]);
	}

	public void scan(String filename) throws Exception {

		BugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(filename, new Project());

		boolean first = false;
		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			String annotation = bugInstance.getAnnotationText();
			if (annotation.equals(""))
				continue;

			if (category != null && !bugInstance.getAbbrev().equals(category))
				continue;

			Set<String> contents = parseAnnotation(annotation);

			if (contents.contains("GOOD_EXAMPLE") || contents.contains("EXCELLENT_EXAMPLE")) {
				if (first)
					first = false;
				else
					System.out.println();
				if (brief)
					dumpBugBrief(bugInstance);
				else
					dumpBug(bugInstance, filename);
			}
		}

	}

	private void dumpBugBrief(BugInstance bugInstance) throws Exception {
		System.out.println(bugInstance.getMessage());
		SourceLineAnnotation srcLine = bugInstance.getPrimarySourceLineAnnotation();
		if (srcLine != null)
			System.out.println("\tAt " + srcLine.format("full"));
		BufferedReader r = new BufferedReader(new StringReader(bugInstance.getAnnotationText()));
		String line;
		while ((line = r.readLine()) != null) {
			line = line.trim();
			if (line.equals("") || line.equals("BUG") || line.equals("GOOD_EXAMPLE") || line.equals("EXCELLENT_EXAMPLE"))
				continue;
			System.out.println("\t" + line);
		}
	}

	private void dumpBug(BugInstance bugInstance, String filename) {
		System.out.println("In " + filename);
		System.out.println(bugInstance.getMessage());
		System.out.println("\t" + bugInstance.getAbbrev());
		MethodAnnotation method = bugInstance.getPrimaryMethod();
		if (method != null)
			System.out.println("\t" + method.toString());
		SourceLineAnnotation srcLine = bugInstance.getPrimarySourceLineAnnotation();
		if (srcLine != null)
			System.out.println("\t" + srcLine.toString());
		System.out.println(bugInstance.getAnnotationText());
	}

	private Set<String> parseAnnotation(String annotation) {
		HashSet<String> result = new HashSet<String>();
		StringTokenizer tok = new StringTokenizer(annotation, " \t\r\n\f");
		while (tok.hasMoreTokens())
			result.add(tok.nextToken());
		return result;
	}
}

// vim:ts=4
