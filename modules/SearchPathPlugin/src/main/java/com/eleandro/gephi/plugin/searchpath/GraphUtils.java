/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eleandro.gephi.plugin.searchpath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;

/**
 *
 * @author eleandro
 */
public class GraphUtils {

    /**
     * Gera um CSV com os caminhos
     *
     * @param paths
     * @param statName nome da coluna do edge que contem a estatistica
     * @return
     */
    public static String getCSV(Map<String, PathStat> paths, String statName) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, PathStat> entry : paths.entrySet()) {
            //pegando a coluna e  incrementando
            sb.append(entry.getKey()).append(";[");
            boolean first = true;
            for (Edge ee : entry.getValue().getEdges()) {
                if (first) {
                    sb.append(ee.getSource().getNodeData().getId());
                    first = false;
                }
                sb.append(',').append(ee.getTarget().getNodeData().getId());
            }
            sb.append(";").append(entry.getValue().getLinkCountTotal());
            sb.append("\r\n");

        }
        return sb.toString();
    }

    public static String getCSV(PathStat[] stats) {
        StringBuilder sb = new StringBuilder();
        for (PathStat s : stats) {
            //pegando a coluna e  incrementando
            sb.append(s.getName()).append(";[");
            boolean first = true;
            for (Edge ee : s.getEdges()) {
                if (first) {
                    sb.append(ee.getSource().getNodeData().getId());
                    first = false;
                }
                sb.append(',').append(ee.getTarget().getNodeData().getId());
            }
            sb.append(";").append(s.getLinkCountTotal());
            sb.append("\r\n");

        }
        return sb.toString();
    }
    
    public static List<Node> findStartNodes(Graph graph, Cancelable cancelable){
         ArrayList<Node> startNodes = new ArrayList<Node>();
        for (Node n : graph.getNodes()) {
            EdgeIterable es = graph.getEdges(n);

            boolean isStart = true;

            for (Edge e : es) {
                //todos os inícios nao podem ser source
                if (!e.getSource().equals(n)) {
                    isStart = false;
                    break;
                }
            }

            if (isStart) {
                startNodes.add(n);
            }
            //se o usuário clicar em sair parar o algoritmo.
            if (cancelable.isCancel()) {
                return new ArrayList<Node>();
            }
        }
        return startNodes;
    }

    public static ArrayList<Node> findEndNodes(Graph graph, Cancelable cancelable) {
        ArrayList<Node> endNodes = new ArrayList<Node>();
        for (Node n : graph.getNodes()) {
            EdgeIterable es = graph.getEdges(n);
            boolean isEnd = true;

            for (Edge e : es) {
                //todos os inicios nao pode ser target
                if (!e.getTarget().equals(n)) {
                    isEnd = false;
                    break;
                }
            }

            if (isEnd) {
                endNodes.add(n);
            }

            //se o usuário clicar em sair parar o algoritmo.
            if (cancelable.isCancel()) {
                return new ArrayList<Node>();
            }
        }
        return endNodes;
    }

}
