/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eleandro.gephi.plugin.searchpath.report;

import com.eleandro.gephi.plugin.searchpath.Cancelable;
import com.eleandro.gephi.plugin.searchpath.GraphUtils;
import com.eleandro.gephi.plugin.searchpath.PathStat;
import com.eleandro.gephi.plugin.searchpath.PathStatFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.naming.OperationNotSupportedException;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.project.api.Project;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools",
id = "com.eleandro.gephi.plugin.searchpath.report.SearchPathReport")
@ActionRegistration(displayName = "#CTL_SearchPathReport")
@ActionReferences({
    @ActionReference(path = "Menu/Plugins", position = 3333)
})
@Messages("CTL_SearchPathReport=SPLC and SPNP export")
public final class SearchPathReport implements ActionListener, Cancelable {

    private boolean cancel = false;

    @Override
    public void actionPerformed(ActionEvent e) {
        ProjectController projectController = Lookup.getDefault().lookup(ProjectController.class);
    	Project project = projectController.getCurrentProject();
    	Workspace workspace = projectController.getCurrentWorkspace();
 
    	// Get the graph instance
    	GraphController graphController = workspace.getLookup().lookup(GraphController.class);
    	GraphModel graphModel = graphController.getModel();
    	Graph g = graphModel.getDirectedGraph();

        PathStatFactory factory = new PathStatFactory();
        List<Node> starts = GraphUtils.findStartNodes(g, this);
        try {
            factory.generatePaths(g, starts);
        } catch (OperationNotSupportedException ex) {
            Exceptions.printStackTrace(ex);
        }

        List<PathStat> paths = factory.getPathsAsList();
        Collections.sort(paths, new PathComparator());

        ArrayList<PathReport> reportList = new ArrayList<PathReport>();

        if (paths.size() > 2) {
            PathStat current = paths.get(0);
            Node start = current.getStartNode();
            Node end = current.getEndNode();
            int minNodes = current.getNodeCount();
            int maxNodes = minNodes;

            int minLC = current.getLinkCountTotal();
            int maxLC = minLC;
            int minNP = current.getNodePairTotal();
            int maxNP = minNP;

            for (int j = 0; j < paths.size(); j++) {
                PathStat next = paths.get(j);
                if (next.getStartNode().equals(start) && next.getEndNode().equals(end)) {
                    int t = next.getLinkCountTotal();
                    minLC = (minLC > t) ? t : minLC;
                    maxLC = (maxLC < t) ? t : maxLC;

                    t = next.getNodePairTotal();
                    minNP = (minNP > t) ? t : minNP;
                    maxNP = (maxNP < t) ? t : maxNP;

                    t = next.getNodeCount();
                    minNodes = (minNodes > t) ? t : minNodes;
                    maxNodes = (maxNodes < t) ? t : maxNodes;
                } else {
                    PathReport p = new PathReport(start, end, minLC, maxLC, minNP, maxNP, minNodes, maxNodes);
                    reportList.add(p);
                    current = next;
                    start = current.getStartNode();
                    end = current.getEndNode();
                    maxNodes = minNodes = current.getNodeCount();
                    maxLC = minLC = current.getLinkCountTotal();
                    maxNP = minNP = current.getNodePairTotal();
                }
            }

            ExcelXMLCreator creator = new ExcelXMLCreator();
            ExcelSheet sheet = creator.createSheet("Path Report");
            sheet.setHeaders(
                    new String[]{
                        "Start Node",
                        "End Node",
                        "min LC",
                        "max LC",
                        "min NP",
                        "max NP",
                        "min qtd nodes",
                        "max qtd nodes"});
            for (PathReport r : reportList) {
                sheet.add(r.getArray());
            }
            try{
                File file = getFile();

                if(file != null){
                    FileWriter out = new FileWriter(file);
                    out.write(creator.buildXML());
                    out.close();
                }
            }catch(Exception ee){
                
            }

        }

    }
    
    private File getFile(){
        //The default dir to use if no value is stored
      File home = new File (System.getProperty("user.home") + File.separator + "lib");
      //Now build a file chooser and invoke the dialog in one line of code
      //"libraries-dir" is our unique key
      File toAdd = new FileChooserBuilder ("libraries-dir").setTitle("Save report").
              setDefaultWorkingDirectory(home).setApproveText("Save").showSaveDialog();
      return toAdd;
    }

    private class PathComparator implements Comparator<PathStat> {

        @Override
        public int compare(PathStat o1, PathStat o2) {
            if (o1.getStartNode().equals(o2.getStartNode())) {
                if (o1.getEndNode().equals(o2.getEndNode())) {
                    return 0;
                } else if (o1.getEndNode().getId() > o2.getEndNode().getId()) {
                    return 1;
                }
            } else if (o1.getStartNode().getId() > o2.getStartNode().getId()) {
                return 1;
            }
            return -1;
        }
    }

    @Override
    public boolean isCancel() {
        return cancel;
    }
}
