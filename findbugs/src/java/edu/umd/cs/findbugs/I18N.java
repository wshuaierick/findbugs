package edu.umd.cs.findbugs;

import java.util.*;

/**
 * Singleton responsible for returning localized strings for information
 * returned to the user.
 *
 * @author David Hovemeyer
 */
public class I18N {

	private final ResourceBundle messageBundle;
	private final ResourceBundle shortMessageBundle;
	private final ResourceBundle annotationDescriptionBundle;

	private I18N() {
		messageBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsMessages");
		shortMessageBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsShortMessages");
		annotationDescriptionBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsAnnotationDescriptions");
	}

	private static I18N theInstance = new I18N();

	/**
	 * Get the single object instance.
	 */
	public static I18N instance() {
		return theInstance;
	}

	/**
	 * Get a message string.
	 * This is a format pattern for describing an entire bug instance in a single line.
	 * @param key which message to retrieve
	 */
	public String getMessage(String key) {
		return messageBundle.getString(key);
	}

	/**
	 * Get a short message string.
	 * This is a concrete string (not a format pattern) which briefly describes
	 * the type of bug, without mentioning particular a particular class/method/field.
	 * @param key which short message to retrieve
	 */
	public String getShortMessage(String key) {
		return shortMessageBundle.getString(key);
	}

	/**
	 * Get an annotation description string.
	 * This is a format pattern which will describe a BugAnnotation in the
	 * context of a particular bug instance.  Its single format argument
	 * is the BugAnnotation.
	 * @param key the annotation description to retrieve
	 */
	public String getAnnotationDescription(String key) {
		return annotationDescriptionBundle.getString(key);
	}

}

// vim:ts=4
