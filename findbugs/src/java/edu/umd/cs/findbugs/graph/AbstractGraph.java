/*
 * Generic graph library
 * Copyright (C) 2000,2003 University of Maryland
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

package edu.umd.cs.daveho.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractGraph
	<
	EdgeType extends AbstractEdge<EdgeType, VertexType>,
	VertexType extends AbstractVertex<EdgeType, VertexType>
	> implements Graph<EdgeType, VertexType> {

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * Iterator over outgoing edges.
	 */
	private static class OutgoingEdgeIterator
		<
		EdgeType extends AbstractEdge<EdgeType, VertexType>,
		VertexType extends AbstractVertex<EdgeType, VertexType>
		> implements Iterator<EdgeType> {

		private EdgeType edge;

		public OutgoingEdgeIterator(VertexType source) {
			this.edge = source.getFirstOutgoingEdge();
		}

		public boolean hasNext() { return edge != null; }

		public EdgeType next() {
			if (!hasNext())
				throw new NoSuchElementException();
			EdgeType result = edge;
			edge = edge.getNextOutgoingEdge();
			return result;
		}

		public void remove() { throw new UnsupportedOperationException(); }
	}

	/**
	 * Iterator over incoming edges.
	 */
	private static class IncomingEdgeIterator
		<
		EdgeType extends AbstractEdge<EdgeType, VertexType>,
		VertexType extends AbstractVertex<EdgeType, VertexType>
		> implements Iterator<EdgeType> {

		private EdgeType edge;

		public IncomingEdgeIterator(VertexType target) {
			this.edge = target.getFirstIncomingEdge();
		}

		public boolean hasNext() { return edge != null; }

		public EdgeType next() {
			if (!hasNext())
				throw new NoSuchElementException();
			EdgeType result = edge;
			edge = edge.getNextIncomingEdge();
			return result;
		}

		public void remove() { throw new UnsupportedOperationException(); }
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private ArrayList<VertexType> vertexList;
	private ArrayList<EdgeType> edgeList;
	private int maxVertexLabel;
	private int nextVertexId;
	private int maxEdgeLabel;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	public AbstractGraph() {
		this.vertexList = new ArrayList<VertexType>();
		this.edgeList = new ArrayList<EdgeType>();
		this.maxVertexLabel = 0;
		this.nextVertexId = 0;
		this.maxEdgeLabel = 0;
	}

	public int getNumEdges() {
		return edgeList.size();
	}

	public int getNumVertices() {
		return vertexList.size();
	}

	public Iterator<EdgeType> getEdgeIterator() {
		return edgeList.iterator();
	}

	public Iterator<VertexType> getVertexIterator() {
		return vertexList.iterator();
	}

	public VertexType addVertex() {
		VertexType v = createVertex();
		v.setId(nextVertexId++);
		v.setLabel(maxVertexLabel++);
		return v;
	}

	public EdgeType addEdge(VertexType source, VertexType target) {
		EdgeType edge = createEdge(source, target);
		edge.setLabel(maxEdgeLabel++);
		return edge;
	}

	public EdgeType lookupEdge(VertexType source, VertexType target) {
		Iterator<EdgeType> i = outgoingEdgeIterator(source);
		while (i.hasNext()) {
			EdgeType edge = i.next();
			if (edge.getTarget() == target)
				return edge;
		}
		return null;
	}

	public int getNumVertexLabels() {
		return maxVertexLabel;
	}

	public void setNumVertexLabels(int numLabels) {
		this.maxVertexLabel = numLabels;
	}

	public int getNumEdgeLabels() {
		return maxEdgeLabel;
	}

	public void setNumEdgeLabels(int numLabels) {
		maxEdgeLabel = numLabels;
	}

	public void removeEdge(EdgeType edge) {
		if (!edgeList.remove(edge))
			throw new IllegalArgumentException("removing nonexistent edge!");
		edge.getSource().removeOutgoingEdge(edge);
		edge.getTarget().removeIncomingEdge(edge);
	}

	public void removeVertex(VertexType v) {
		if (!vertexList.remove(v))
			throw new IllegalArgumentException("removing nonexistent vertex!");

		for (Iterator<EdgeType> i = incomingEdgeIterator(v); i.hasNext(); )
			removeEdge(i.next());

		for (Iterator<EdgeType> i = outgoingEdgeIterator(v); i.hasNext(); )
			removeEdge(i.next());
	}

	public Iterator<EdgeType> outgoingEdgeIterator(VertexType source) {
		return new OutgoingEdgeIterator<EdgeType, VertexType>(source);
	}

	public Iterator<EdgeType> incomingEdgeIterator(VertexType target) {
		return new IncomingEdgeIterator<EdgeType, VertexType>(target);
	}

	public Iterator<VertexType> adjacencyListIterator(final VertexType source) {
		return new Iterator<VertexType>() {
			private Iterator<EdgeType> iter = outgoingEdgeIterator(source);
			public boolean hasNext() { return iter.hasNext(); }
			public VertexType next() { return iter.next().getTarget(); }
			public void remove() { iter.remove(); }
		};
	}

	/* ----------------------------------------------------------------------
	 * Downcall methods
	 * ---------------------------------------------------------------------- */

	protected abstract VertexType createVertex();
	protected abstract EdgeType createEdge(VertexType source, VertexType target);

}

// vim:ts=4
