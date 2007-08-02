/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.EnumValue;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * @author William Pugh
 */
public class TypeQualifierApplications {
	static final boolean DEBUG = SystemProperties.getBoolean("tqa.debug");
	
	static Map<AnnotatedObject, Collection<AnnotationValue>> objectAnnotations = new HashMap<AnnotatedObject, Collection<AnnotationValue>>();
	static DualKeyHashMap<XMethod, Integer, Collection<AnnotationValue>> parameterAnnotations 
	= new DualKeyHashMap<XMethod, Integer, Collection<AnnotationValue>>();
	
	 static Collection<AnnotationValue> getDirectAnnotation(AnnotatedObject m) {
		Collection<AnnotationValue> result = objectAnnotations.get(m);
		if (result != null) return result;
		if (m.getAnnotationDescriptors().isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getAnnotations());
		if (result.size() == 0) result = Collections.emptyList();
		objectAnnotations.put(m, result);
		return result;
	}
	 static Collection<AnnotationValue> getDirectAnnotation(XMethod m, int parameter) {
		Collection<AnnotationValue> result = parameterAnnotations.get(m, parameter);
		if (result != null) return result;
		if (m.getParameterAnnotationDescriptors(parameter).isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getParameterAnnotations(parameter));
		if (result.size() == 0) result = Collections.emptyList();
		parameterAnnotations.put(m, parameter, result);
		return result;
	}
	
	 static void getApplicableApplications(Map<TypeQualifierValue, When> result, XMethod o, int parameter) {
		 Collection<AnnotationValue> values = getDirectAnnotation(o, parameter);
		 ElementType e = ElementType.PARAMETER;
		 for(AnnotationValue v : values) {
			 Object a = v.getValue("applyTo");
			 if (a instanceof Object[]) {
				 for(Object o2 : (Object[]) a) 
					 if (o2 instanceof EnumValue) { 
						 EnumValue ev = (EnumValue)o2;
						 if (ev.desc.getClassName().equals("java/lang/annotation/ElementType") && e.toString().equals(ev.value))
						    constructTypeQualifierAnnotation(result, v);
					 }
			 } else 
				 constructTypeQualifierAnnotation(result, v);
		 }
	}
	public static void getApplicableApplications(Map<TypeQualifierValue, When> result, AnnotatedObject o, ElementType e) {
		 Collection<AnnotationValue> values = getDirectAnnotation(o);
		 for(AnnotationValue v : values) {
			 Object a = v.getValue("applyTo");
			 if (a instanceof Object[]) {
				 for(Object o2 : (Object[]) a) 
					 if (o2 instanceof EnumValue) { 
						 EnumValue ev = (EnumValue)o2;
						 if (ev.desc.getClassName().equals("java/lang/annotation/ElementType") && e.toString().equals(ev.value))
						    constructTypeQualifierAnnotation(result, v);
					 }
			 } else if (o.getElementType().equals(e))
				 constructTypeQualifierAnnotation(result, v);
		 }
	}
	/**
     * @param v
     * @return
     */
    private static void constructTypeQualifierAnnotation( Map<TypeQualifierValue, When> map, AnnotationValue v) {
    	assert map != null;
    	assert v != null;
	    EnumValue whenValue = (EnumValue) v.getValue("when");
	    When when = whenValue == null ? When.ALWAYS : When.valueOf(whenValue.value);
	    TypeQualifierValue tqv = TypeQualifierValue.getValue(v.getAnnotationClass(), v.getValue("value"));
	    if (whenValue == null) {
	    	tqv.setIsStrict();
	    }
	    map.put(tqv, when);
	    if (DEBUG && whenValue == null) {
	    	System.out.println("When value unspecified for type qualifier value " + tqv);
	    }
    }

	 static void getApplicableScopedApplications(Map<TypeQualifierValue, When> result, AnnotatedObject o, ElementType e) {
		AnnotatedObject outer = o.getContainingScope();
		if (outer != null) 
			getApplicableScopedApplications(result, outer, e);
		getApplicableApplications(result, o, e);
	}
	 static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(AnnotatedObject o, ElementType e) {
		Map<TypeQualifierValue, When> result = new HashMap<TypeQualifierValue, When>();
		getApplicableScopedApplications(result, o, e);
		return  TypeQualifierAnnotation.getValues(result);
	}
		
	 static void getApplicableScopedApplications(Map<TypeQualifierValue, When> result, XMethod o, int parameter) {
		 ElementType e = ElementType.PARAMETER;
		getApplicableScopedApplications(result, o, e);
		getApplicableApplications(result, o, parameter);
	}
	 static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(XMethod o, int parameter) {
		Map<TypeQualifierValue, When> result = new HashMap<TypeQualifierValue, When>();
		ElementType e = ElementType.PARAMETER;
		getApplicableScopedApplications(result, o, e);
		getApplicableApplications(result, o, parameter);
		return TypeQualifierAnnotation.getValues(result);
	}
	
	public static Collection<TypeQualifierAnnotation> getApplicableApplications(AnnotatedObject o) {
		return getApplicableScopedApplications(o, o.getElementType());
	}
	public static Collection<TypeQualifierAnnotation> getApplicableApplications(XMethod o, int parameter) {
		return getApplicableScopedApplications(o, parameter);
	}
	
	/*
	 * XXX: is there a more efficient way to do this?
	 */
	static TypeQualifierAnnotation findMatchingTypeQualifierAnnotation(
			Collection<TypeQualifierAnnotation> typeQualifierAnnotations,
			TypeQualifierValue typeQualifierValue) {
		for (TypeQualifierAnnotation typeQualifierAnnotation : typeQualifierAnnotations) {
			if (typeQualifierAnnotation.typeQualifier.equals(typeQualifierValue)) {
				return typeQualifierAnnotation;
			}
		}
		return null;
	}
	
	/**
	 * Look up the type qualifier annotation(s) on given AnnotatedObject.
	 * If the AnnotatedObject is an instance method,
	 * annotations applied to supertype methods which the method overrides
	 * are considered.
	 * 
	 * @param o                  an AnnotatedObject
	 * @param typeQualifierValue a TypeQualifierValue specifying the kind of annotation we want to look up
	 * @return TypeQualifierAnnotationLookupResult summarizing the relevant TypeQualifierAnnotation(s)
	 */
	public static TypeQualifierAnnotationLookupResult lookupTypeQualifierAnnotationConsideringSupertypes(
			AnnotatedObject o,
			TypeQualifierValue typeQualifierValue) {
		if (o instanceof XMethod && !((XMethod)o).isStatic()) {
			// Instance method: accumulate return value annotations in supertypes, if any
			XMethod xmethod = (XMethod) o;
			ReturnTypeAnnotationAccumulator accumulator = new ReturnTypeAnnotationAccumulator(typeQualifierValue, xmethod);
			return accumulateSupertypeAnnotations(xmethod, accumulator);
		} else {
			// Annotated object is not an instance method, so we don't have to check supertypes
			// for inherited annotations
			TypeQualifierAnnotationLookupResult result = new TypeQualifierAnnotationLookupResult();
			TypeQualifierAnnotation tqa = getDirectOrDefaultTypeQualifierAnnotation(o, typeQualifierValue);
			if (tqa != null) {
				result.addPartialResult(new TypeQualifierAnnotationLookupResult.PartialResult(o, tqa));
			}
			return result;
		}
	}
	
	public static TypeQualifierAnnotation getDirectOrDefaultTypeQualifierAnnotation(AnnotatedObject o,
			TypeQualifierValue typeQualifierValue) {
		return findMatchingTypeQualifierAnnotation(getApplicableApplications(o), typeQualifierValue);
	}

	/**
	 * Look up the type qualifier annotation(s) on given method parameter.
	 * If the method is an instance method,
	 * annotations applied to supertype methods which the method overrides
	 * are considered.
	 * 
	 * @param xmethod            a method
	 * @param parameter          parameter (0 == first parameter)
	 * @param typeQualifierValue a TypeQualifierValue specifying the kind of annotation we want to look up
	 * @return TypeQualifierAnnotationLookupResult summarizing the relevant TypeQualifierAnnotation(s)
	 */
	public static TypeQualifierAnnotationLookupResult lookupTypeQualifierAnnotationConsideringSupertypes(
			XMethod o,
			int parameter,
			TypeQualifierValue typeQualifierValue) {
		
		if (o instanceof XMethod && !((XMethod) o).isStatic()) {
			XMethod xmethod = (XMethod) o;
			ParameterAnnotationAccumulator accumulator = new ParameterAnnotationAccumulator(typeQualifierValue, xmethod, parameter);
			return accumulateSupertypeAnnotations(xmethod, accumulator);
		} else {
			TypeQualifierAnnotationLookupResult result = new TypeQualifierAnnotationLookupResult();
			TypeQualifierAnnotation tqa = getDirectOrDefaultTypeQualifierAnnotation(o, parameter, typeQualifierValue);
			if (tqa != null) {
				result.addPartialResult(new TypeQualifierAnnotationLookupResult.PartialResult(o, tqa));
			}
			return result;
		}
	}
	
	public static TypeQualifierAnnotation getDirectOrDefaultTypeQualifierAnnotation(XMethod o, int parameter,
			TypeQualifierValue typeQualifierValue) {
		return findMatchingTypeQualifierAnnotation(getApplicableApplications(o, parameter), typeQualifierValue);
	}

	private static TypeQualifierAnnotationLookupResult accumulateSupertypeAnnotations(
			XMethod xmethod,
			AbstractMethodAnnotationAccumulator accumulator) {
		try {
			AnalysisContext.currentAnalysisContext().getSubtypes2().traverseSupertypes(xmethod.getClassDescriptor(), accumulator);
		} catch (ClassNotFoundException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
		}
		return accumulator.getResult();
	}

	/**
	 * Get the applicable TypeQualifierAnnotation matching given
	 * TypeQualifierValue for given AnnotatedObject.
	 * Returns null if there is no applicable annotation of the
	 * given TypeQualifierValue for this object.
	 * 
	 * @param o                  an AnnotatedObject
	 * @param typeQualifierValue a TypeQualifierValue 
	 * @return the TypeQualifierAnnotation matching the AnnotatedObject/TypeQualifierValue,
	 *         or null if there is no matching TypeQualifierAnnotation
	 */
	public static @CheckForNull TypeQualifierAnnotation getApplicableApplicationConsideringSupertypes(AnnotatedObject o, TypeQualifierValue typeQualifierValue) {
		TypeQualifierAnnotationLookupResult lookupResult = lookupTypeQualifierAnnotationConsideringSupertypes(o, typeQualifierValue);
		return lookupResult.getEffectiveTypeQualifierAnnotation();
	}
	
	/**
	 * Get the applicable TypeQualifierAnnotation matching given
	 * TypeQualifierValue for given method parameter.
	 * Returns null if there is no applicable annotation of the
	 * given TypeQualifierValue for this parameter.
	 * 
	 * @param o                  an XMethod
	 * @param parameter          parameter number (0 for first declared parameter)
	 * @param typeQualifierValue a TypeQualifierValue 
	 * @return the TypeQualifierAnnotation matching the parameter,
	 *         or null if there is no matching TypeQualifierAnnotation
	 */
	public static @CheckForNull TypeQualifierAnnotation getApplicableApplicationConsideringSupertypes(XMethod o, int parameter, TypeQualifierValue typeQualifierValue) {
		TypeQualifierAnnotationLookupResult lookupResult = lookupTypeQualifierAnnotationConsideringSupertypes(o, parameter, typeQualifierValue);
		return lookupResult.getEffectiveTypeQualifierAnnotation();
	}
}
