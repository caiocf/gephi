/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eleandro.gephi.plugin.searchpath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.OperationNotSupportedException;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;

/**
 *
 * @author eleandro
 */
public class PathStatFactory {

    private HashMap<String, PathStat> paths = new HashMap<String, PathStat>();
    private boolean cancel = false;

    public PathStatFactory() {
    }

    public Map<String, PathStat> getPaths() {
        return paths;
    }
    
    public List<PathStat> getPathsAsList(){
        ArrayList<PathStat> list = new ArrayList<PathStat>(paths.size());
        list.addAll(paths.values());
        return list;
    }

    public void generatePaths(Graph graph, List<Node> startNodes) throws OperationNotSupportedException {
        for (Node n : startNodes) {
            String pathName = "" + n.getId();
            paths.put(pathName, new PathStat(pathName));
            generatePath(graph, n, pathName);
        }
    }

    public void filterPathByNodeCount(int minSize) {
        ArrayList<String> remover = new ArrayList<String>();
        for (Map.Entry<String, PathStat> e : paths.entrySet()) {
            if (e.getValue().getNodeCount() < minSize) {
                remover.add(e.getKey());
            }
        }
        while (remover.size() > 0) {
            paths.remove(remover.remove(0));
        }
    }

    private void generatePath(Graph graph, Node currentNode, String pathName) throws OperationNotSupportedException {
        PathStat original;
        Edge[] edges = graph.getEdges(currentNode).toArray();

        if (edges.length == 1 && edges[0].getSource().equals(currentNode)) {
            Node next = edges[0].getTarget();
            original = paths.get(pathName);

            if (original.contains(edges[0])) {
                throw new OperationNotSupportedException("Grafos com ciclos não podem ser analisados. Edge=" + edges[0]);
            }

            original.add(edges[0]);
            generatePath(graph, next, pathName);
        } else {
            original = paths.remove(pathName);
            int i = 1;
            for (Edge e : edges) {
                if (e.getSource().equals(currentNode) && !e.isSelfLoop()) {
                    if (original.contains(e)) {
                        throw new OperationNotSupportedException("Grafos com ciclos não podem ser analisados. Edge=" + e);
                    }

                    String newPath = pathName + i;
                    PathStat nova = new PathStat(newPath, original);
                    nova.add(e);
                    paths.put(newPath, nova);
                    generatePath(graph, e.getTarget(), newPath);
                    i++;
                    if (cancel) {
                        return;
                    }
                }
            }

            if (i == 1) {
                //nenhum subcaminho elegível foi encontrado
                paths.put(pathName, original);
            }
        }

    }
}
