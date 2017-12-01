/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eleandro.gephi.plugin.searchpath.report;

import org.gephi.graph.api.Node;

/**
 *
 * @author eleandro
 */
public class PathReport {

    protected Node start;
    protected Node end;
    protected int minLC;
    protected int maxLC;
    protected int minNP;
    protected int maxNP;
    protected int minNodes;
    protected int maxNodes;

    public PathReport(Node start, Node end, int minLC, int maxLC, int minNP, int maxNP, int minNodes, int maxNodes) {
        this.start = start;
        this.end = end;
        this.minLC = minLC;
        this.maxLC = maxLC;
        this.minNP = minNP;
        this.maxNP = maxNP;
        this.minNodes = minNodes;
        this.maxNodes = maxNodes;
    }

    public Object[] getArray() {
        return new Object[]{
                    start.getNodeData().getId(),
                    end.getNodeData().getId(),
                    minLC,
                    maxLC,
                    minNP,
                    maxNP,
                    minNodes,
                    maxNodes
                };
    }
}