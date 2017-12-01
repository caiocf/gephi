/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eleandro.gephi.plugin.searchpath;

import java.util.List;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;

/**
 *
 * @author eleandro
 */
public class ReportUtils {

    public static String listToHTML(List<String> messages, String tag) {
        if (messages.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<").append(tag).append(">\r\n");
            for (String m : messages) {
                sb.append("<li>").append(m).append("</li>\r\n");
            }
            sb.append("</").append(tag).append(">\r\n");
            return sb.toString();
        }
        return "";
    }
    
    public static String listNodeToCsv(List<Node> list){
        StringBuilder sb = new StringBuilder();
         for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).getNodeData().getId());
            if (i < list.size() - 1) {
                sb.append(';');
            }
        }
        return sb.toString();
    }

    public static String pathToString(PathStat p) {
        StringBuilder sb = new StringBuilder();
        Edge[] arr = (Edge[]) p.getEdges().toArray(new Edge[p.getEdges().size()]);

        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i].getSource().getNodeData().getId());
            sb.append(';');
        }
        sb.append(arr[arr.length - 1].getTarget().getNodeData().getId());
        return sb.toString();
    }

    public static String tableRow(String tag, Object[] a) {
        return tableRow(tag, null, a);
    }
    
    public static String tableRow(String tag,String attributes, Object[] a) {
        StringBuilder sb =new StringBuilder();
        if(attributes!= null && attributes.length()>0){
            sb.append("<tr ")
            .append(attributes)
            .append(">");
        }else{
            sb.append("<tr>");
        }
        for (int i = 0; i < a.length; i++) {
            sb
                .append("<")
                .append(tag)
                .append(">")
                .append(a[i].toString())
                .append("</")
                .append(tag)
                .append(">");
        }
        sb.append("</tr>\r\n");
        return sb.toString();
    }
}
