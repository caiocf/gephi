package com.eleandro.gephi.plugin.searchpath;

import com.eleandro.gephi.plugin.searchpath.linkcount.PathLCComparator;
import java.util.ArrayList;
import java.util.List;
import org.gephi.graph.api.Attributes;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;

/**
 *
 * @author eleandro
 */
public class PathStat implements Comparable<PathStat> {

    private String name;
    private List<Edge> edges;
    private int linkCountTotal;
    private int nodePairTotal;

    public PathStat(String name) {
        this.name = name;
        this.edges = new ArrayList<Edge>();
    }

    public PathStat(String name, PathStat old) {
        this(name);
        edges.addAll(old.getEdges());
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean add(Edge e) {
        return edges.add(e);
    }

    public boolean contains(Edge o) {
        return edges.contains(o);
    }

    public int getLinkCountTotal() {
        if (linkCountTotal == 0) {
            this.linkCountTotal = sumStats(ColumnConstantes.LINK_COUNT_NAME);
        }
        return linkCountTotal;
    }

    public int getNodePairTotal() {
        if (nodePairTotal == 0) {
            this.nodePairTotal = sumStats(ColumnConstantes.NODE_PAIR_NAME);
        }
        return nodePairTotal;
    }

    public void calculateStats() {
        this.nodePairTotal = sumStats(ColumnConstantes.NODE_PAIR_NAME);
        this.linkCountTotal = sumStats(ColumnConstantes.LINK_COUNT_NAME);
    }

    public Node getStartNode() {
        return edges.get(0).getSource();
    }

    public Node getEndNode() {
        return edges.get(edges.size()-1).getTarget();
    }

    @Override
    public int compareTo(PathStat o) {
        return PathLCComparator.getInstance().compare(this, o);
    }

    private int sumStats(String statName) {
        int stat = 0;
        for (Edge ee : this.edges) {
            Attributes attributes = ee.getAttributes();
            Integer i = (Integer) attributes.getValue(statName);
            if (i != null) {
                stat += i;
            }

        }
        return stat;
    }

    public int getNodeCount() {
        return edges.size() +1; 
    }
}
