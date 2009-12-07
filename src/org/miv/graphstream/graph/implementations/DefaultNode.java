/*
 * This file is part of GraphStream.
 * 
 * GraphStream is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GraphStream is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GraphStream.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2006 - 2009
 * 	Julien Baudry
 * 	Antoine Dutot
 * 	Yoann Pigné
 * 	Guilhelm Savin
 */

package org.miv.graphstream.graph.implementations;

import org.miv.graphstream.graph.BreadthFirstIterator;
import org.miv.graphstream.graph.DepthFirstIterator;
import org.miv.graphstream.graph.Edge;
import org.miv.graphstream.graph.Element;
import org.miv.graphstream.graph.Graph;
import org.miv.graphstream.graph.Node;
import org.miv.graphstream.io2.InputBase;
import org.miv.util.*;

import java.util.*;

/**
 * Base implementation of a {@link org.miv.graphstream.graph.Node} for the {@link DefaultGraph}.
 *
 * <p>
 * This node ensures the consistency of the graph. Such a node is able to
 * give informations about all leaving edges but also entering edges (when
 * directed), at the price however of a larger memory footprint.
 * </p>
 * 
 * <p>
 * This is a base implementation that is refined in two classes {@link SingleNode}
 * and {@link MultiNode}. The first allows only one edge between two nodes, the other
 * allows multiple edges between two nodes (or several loops).
 * </p>
 *
 * @since 20020709
 */
public abstract class DefaultNode extends AbstractElement implements Node
{
// Constant

	/**
	 * Property used to store labels.
	 */
	public static final String ATTRIBUTE_LABEL = "label";

// Attribute

	/**
	 * Parent graph.
	 */
	protected DefaultGraph G;

	/**
	 * List of all entering or leaving edges the node knows.
	 */
	protected ArrayList<Edge> edges = new ArrayList<Edge>();

// Construction
	
	/**
	 * New unconnected node.
	 * @param graph The graph containing the node.
	 * @param id Tag of the node.
	 */
	public DefaultNode( Graph graph, String id )
	{
		super( id );
		G = (DefaultGraph) graph;
	}

	
// Access

	public Graph getGraph()
	{
		return G;
	}

	public int getDegree()
	{
		return edges.size();
	}

	public abstract int getOutDegree();

	public abstract int getInDegree();

	public abstract boolean hasEdgeToward( String id );
	
	public abstract boolean hasEdgeFrom( String id );

	public abstract Edge getEdgeToward( String id );

	public abstract Edge getEdgeFrom( String id );

	public Iterator<Edge> getEdgeIterator()
	{
		return new ElementIterator<Edge>( edges );
	}
	
	public abstract Iterator<Edge> getEnteringEdgeIterator();
	
	public abstract Iterator<Edge> getLeavingEdgeIterator();

	public Iterator<Node> getNeighborNodeIterator()
	{
		return new NeighborNodeIterator( this );
	}
	
	public Iterator<Edge> iterator()
	{
		return getEdgeIterator();
	}
	
	public Edge getEdge( int i )
	{
		return edges.get( i );
	}
	
	/**
	 * @complexity of the breath first iterator O(n+m) with n the number of
	 *             nodes and m the number of edges.
	 */
	public Iterator<Node> getBreadthFirstIterator()
	{
		return new BreadthFirstIterator( this );
	}
	
	/**
	 * @complexity of the breath first iterator O(n+m) with n the number of
	 *             nodes and m the number of edges.
	 */
	public Iterator<Node> getBreadthFirstIterator( boolean directed )
	{
		return new BreadthFirstIterator( this );
	}
	
	/**
	 * @complexity of the depth first iterator O(n+m) with n the number of nodes
	 *             and m the number of edges.
	 */
	public Iterator<Node> getDepthFirstIterator()
	{
		return new DepthFirstIterator( this );
	}
	
	/**
	 * @complexity of the depth first iterator O(n+m) with n the number of nodes
	 *             and m the number of edges.
	 */
	public Iterator<Node> getDepthFirstIterator( boolean directed )
	{
		return new DepthFirstIterator( this );
	}

// Access -- Not in Node interface

	@Override
	protected String getMyGraphId()
	{
		if( G != null )
//			return G.getId();
			return G.time.newEvent();
	
		throw new RuntimeException( "WTF ?" );
	}
	
	public Iterable<? extends Edge> getEdgeSet()
	{
		return edges;
	}
	
	public abstract Iterable<? extends Edge> getLeavingEdgeSet();

	public abstract Iterable<? extends Edge> getEnteringEdgeSet();

// Command

	/**
	 * Add an edge between this node and the given target.
	 * @param tag Tag of the edge.
	 * @param target Target node.
	 * @param directed If the edge is directed only from this node to the target.
	 */
	protected abstract Edge addEdgeToward( String tag, DefaultNode target, boolean directed )
		throws IllegalArgumentException;

	/**
	 * Called by an edge to bind it.
	 */
	protected abstract void registerEdge( Edge edge )
		throws IllegalArgumentException, SingletonException;

	protected abstract void unregisterEdge( Edge edge );
	
	/**
	 * When a node is unregistered from a graph, it must not keep edges
	 * connected to nodes still in the graph. This methods untie all edges
	 * connected to this node (this also unregister them from the graph).
	 */
	protected abstract void disconnectAllEdges()
		throws IllegalStateException;

	@Override
	protected void attributeChanged( String sourceId, String attribute, AttributeChangeEvent event, Object oldValue, Object newValue )
	{
		if( G != null )
			G.listeners.sendAttributeChangedEvent( sourceId, getId(),
					InputBase.ElementType.NODE, attribute, event, oldValue, newValue );
	}
	
	@Override
	public String toString()
	{
		return String.format( "[node %s (%d edges)]", getId(), edges.size() );
	}

// Nested classes

protected class NeighborNodeIterator
	implements Iterator<Node>
{
	protected int i;

	protected Node n;

	protected NeighborNodeIterator( Node node )
	{
		i = 0;
		n = node;
	}

	public boolean hasNext()
	{
		return( i < edges.size() );
	}

	public Node next()
		throws NoSuchElementException
	{
		Edge e = edges.get( i++ );
		return e.getOpposite( n );
	}

	public void remove()
		throws UnsupportedOperationException, IllegalStateException
	{
		throw new UnsupportedOperationException( "this iterator does not allow removing" );
	}
	
}

static class ElementIterator<T extends Element> implements Iterator<T>
{
	Iterator<? extends T> iterator;
	
	ElementIterator( ArrayList<T> elements )
	{
		iterator = elements.iterator();
	}
	
	ElementIterator( HashMap<String,? extends T> elements )
	{
		iterator = elements.values().iterator();
	}

	public boolean hasNext()
	{
		return iterator.hasNext();
	}

	public T next()
	{
		return iterator.next();
	}

	public void remove()
		throws UnsupportedOperationException, IllegalStateException
	{
		throw new UnsupportedOperationException( "this iterator does not allow removing" );
	}
}
}