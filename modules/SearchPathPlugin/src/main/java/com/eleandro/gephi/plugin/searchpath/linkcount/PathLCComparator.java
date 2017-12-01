/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eleandro.gephi.plugin.searchpath.linkcount;

import com.eleandro.gephi.plugin.searchpath.PathStat;
import java.util.Comparator;

/**
 *
 * @author eleandro
 */
public class PathLCComparator implements Comparator<PathStat>{
    private static final PathLCComparator INSTANCE = new PathLCComparator();
    
    public static final PathLCComparator getInstance(){
        return INSTANCE;
    }
    
    public int compare(PathStat o1, PathStat o) {
        o1.calculateStats();
        o.calculateStats();
        if (o1.getLinkCountTotal() > o.getLinkCountTotal()) {
            return -1;
        } else if (o1.getLinkCountTotal() < o.getLinkCountTotal()) {
            return 1;
        } else {
            //desempatando
            if(o1.getNodePairTotal() > o.getNodePairTotal()){
                return -1;
            }else if (o1.getEdges().size() > o.getEdges().size()) {
                return -1;
            } else if (o1.getStartNode().getId() > o.getStartNode().getId()) {
                return -1;
            }
        }
        return 0;
    }
    
}
