/* * FindBugs Eclipse Plug-in. * Copyright (C) 2003, Peter Friese * * This library is free software; you can redistribute it and/or * modify it under the terms of the GNU Lesser General Public * License as published by the Free Software Foundation; either * version 2.1 of the License, or (at your option) any later version. * * This library is distributed in the hope that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU * Lesser General Public License for more details. * * You should have received a copy of the GNU Lesser General Public * License along with this library; if not, write to the Free Software * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */ package de.tobject.findbugs.builder;import java.io.IOException;import java.util.Collection;import java.util.Iterator;import java.util.List;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.launching.JavaRuntime;import de.tobject.findbugs.FindbugsPlugin;import de.tobject.findbugs.marker.FindBugsMarker;import de.tobject.findbugs.reporter.Reporter;import edu.umd.cs.findbugs.BugReporter;import edu.umd.cs.findbugs.Detector;import edu.umd.cs.findbugs.DetectorFactory;import edu.umd.cs.findbugs.DetectorFactoryCollection;import edu.umd.cs.findbugs.FindBugs;/** * TODO Enter a comment for . * @author U402101 * @version 1.0 * @since 26.09.2003 */public class FindBugsWorker {		private IProgressMonitor monitor;		private List selectedDetectorFactories;		private IProject project;		public FindBugsWorker(IProject project, IProgressMonitor monitor) {		super();		this.project = project;		this.monitor = monitor;		try {			selectedDetectorFactories =				FindbugsPlugin.readDetectorFactories(project);		}		catch (CoreException e) {			// TODO Auto-generated catch block			e.printStackTrace();		}	}		/**	 * Controls debugging.	 */	public static boolean DEBUG;		public void work(Collection files) throws CoreException {		int count = 0;		if (files != null) {			if (this.monitor != null) {				this.monitor.beginTask("FindBugs", files.size());			}			String findBugsHome = FindbugsPlugin.getFindBugsEnginePluginLocation();			if (DEBUG) {				System.out.println("Looking for detecors in: " + findBugsHome); //$NON-NLS-1$			}			System.setProperty("findbugs.home", findBugsHome); //$NON-NLS-1$			BugReporter bugReporter = new Reporter(this.project, this.monitor);			bugReporter.setPriorityThreshold(Detector.LOW_PRIORITY);			edu.umd.cs.findbugs.Project findBugsProject =				new edu.umd.cs.findbugs.Project();			FindBugs findBugs = new FindBugs(bugReporter, findBugsProject);			Iterator iter = files.iterator();			while (iter.hasNext()) {				// get the resource				IResource res = (IResource) iter.next();				if (isJavaArtefact(res)) {					res.deleteMarkers(						FindBugsMarker.NAME,						true,						IResource.DEPTH_INFINITE);				}				if (isClassFile(res)) {					// add this file to the work list:					String fileName = res.getLocation().toOSString();					res.refreshLocal(IResource.DEPTH_INFINITE, null);					if (DEBUG) {						System.out.println(							"Resource: "								+ fileName								+ ": in synch: "								+ res.isSynchronized(IResource.DEPTH_INFINITE));					}					findBugsProject.addJar(fileName);				}			}			String[] classPathEntries = createClassPathEntries();			// add to findbugs classpath			for (int i = 0; i < classPathEntries.length; i++) {				findBugsProject.addAuxClasspathEntry(classPathEntries[i]);			}			// configure detectors.			// XXX currently detector factories are shared between different projects!!!			// cause detector factories list is a singleton!!!			// if multiple workers are working (Eclipse 3.0 allows background build),			// there is a big problem!!!			if (selectedDetectorFactories != null) {				Iterator iterator =					DetectorFactoryCollection.instance().factoryIterator();				while (iterator.hasNext()) {					DetectorFactory factory = (DetectorFactory) iterator.next();					factory.setEnabled(selectedDetectorFactories.contains(factory));				}			}			try {				findBugs.execute();			}			catch (IOException e) {				e.printStackTrace();			}			catch (InterruptedException e) {				e.printStackTrace();			}			//			Iterator iter = files.iterator();			//			while (iter.hasNext()) {			//				// get the resource			//				IResource res = (IResource) iter.next();			//			//				// advance progress monitor			//				if (monitor != null && count % MONITOR_INTERVAL == 0) {			//					monitor.worked(MONITOR_INTERVAL);			//					monitor.subTask("Performing bug check on: " + res.getName());			//					if (monitor.isCanceled())			//						break;			//				}			//			//				// visit resource			//				res.accept(new FindBugsVisitor());			//				count++;			//			}		}		else {			if (DEBUG) {				System.out.println("No files to build"); //$NON-NLS-1$			}		}	}		private boolean isJavaArtefact(IResource resource) {		if (resource != null) {			if ((resource.getName().endsWith(".java")) //$NON-NLS-1$			|| (resource.getName().endsWith(".class"))) { //$NON-NLS-1$				return true;			}		}		return false;	}		private String[] createClassPathEntries() {		IJavaProject javaProject = JavaCore.create(project);		try {			return JavaRuntime.computeDefaultRuntimeClassPath(javaProject);		}		catch (CoreException e) {			if (DEBUG) {				// TODO Auto-generated catch block				e.printStackTrace();			}		}		return new String[0];	}		private boolean isClassFile(IResource resource) {		if (resource != null) {			if (resource.getName().endsWith(".class")) { //$NON-NLS-1$				return true;			}		}		return false;	}	}