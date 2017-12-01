package com.eleandro.gephi.plugin.searchpath.linkcount;

import com.eleandro.gephi.plugin.searchpath.*;
import java.util.*;
import java.util.Map.Entry;
import javax.naming.OperationNotSupportedException;
import org.gephi.data.attributes.api.*;
import org.gephi.graph.api.*;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

/**
 *
 * See http://wiki.gephi.org/index.php/HowTo_write_a_metric#Create_Statistics
 *
 * @author José Eleandro Custódio
 */
public class SearchPathLinkCount implements Statistics, LongTask, Cancelable {
    private final String NEW_LINE = "</br>\n";
    private static final Integer M_ONE = -1;
    private static final Integer ZERO = 0;
    private static final Integer ONE = 1;
    private static final Integer TWO = 2;
    
    
    private boolean cancel = false;
    private ProgressTicket progressTicket;
    
    
    //variáveis de sessao, limpar apos uso!!!
    private List<String> errors = new LinkedList<String>();
    private List<Node> startNodes = new ArrayList<Node>();
    private List<Node> endNodes = new ArrayList<Node>();
    private List<String> reportList = new LinkedList<String>();
    private List<String> debugMessages = new LinkedList<String>();
    private static final boolean DEBUG = true;
    
    private Map<String, PathStat> paths;
    private int pathNumber;
    private int mergePath;
    private int pathOnReport;
    
    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        cancel = false;
        Graph graph = graphModel.getGraphVisible();
        graph.readLock();
        boolean isDirected = graph instanceof DirectedGraph;

         HierarchicalDirectedGraph directedGraph = null;
        if(isDirected) {
            directedGraph = graph.getGraphModel().getHierarchicalDirectedGraphVisible();
        }

        errors.clear();
        reportList.clear();
        debugMessages.clear();
        PathStatFactory pathFactory = new PathStatFactory();

        try {
            //avoid 
            Progress.start(progressTicket, 5);
            Progress.progress(progressTicket);

            //Marcando nós inicio e nós fim
            findStartEndAnd(graph);
            
            if (errors.size() > 0) {
                return;
            }
            
            reportList.add("N&oacute;s inicio:"+startNodes.size());
            reportList.add("N&oacute;s fim:"+endNodes.size());
            
            deletePath(attributeModel, ColumnConstantes.SPLC_PATH);
            deletePath(attributeModel, ColumnConstantes.SPLC_PATH_MERGE);
            
            createAttribute(attributeModel.getNodeTable(), ColumnConstantes.SPLC_PATH_MERGE);
            createAttribute(attributeModel.getEdgeTable(), ColumnConstantes.SPLC_PATH_MERGE);
                      
            //cria todos os caminhos
            pathFactory.generatePaths(graph, startNodes);
            paths=pathFactory.getPaths();
            //progressTicket.progress("Caminhos gerados", 1);
            
            //executa a marcacao do LC para todos os edges
            runLinkCount(attributeModel,directedGraph, paths);
            //progressTicket.progress("SPLC calculado", 2);
            
             //cria as colunas marcando os caminhos
            markPaths(attributeModel, paths,pathNumber);
            markMergeColumn(paths);
            //progressTicket.progress("Caminhos marcados", 4);
            
            //marcando inicio e fim
            createColumnsStartAndEnd(attributeModel);
           // progressTicket.progress("Inicio e Fim marcados", 5);
            
            graph.readUnlockAll();
        } catch (OperationNotSupportedException e) {
            errors.add(e.getMessage());
        } catch (Exception e) {
            errors.add("Erro desconhecido:" + e.getMessage());
        } finally {
            //Unlock graph
            graph.readUnlockAll();
        }
    }


    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }

    private void findStartEndAnd(Graph graph) {
        startNodes = GraphUtils.findStartNodes(graph, this);
        endNodes = GraphUtils.findEndNodes(graph, this);

        if (startNodes.isEmpty()) {
            errors.add("O grafo não possui nos inicio (que referenciam mas não são referenciados).");
        }

        if (endNodes.isEmpty()) {
            errors.add("O grafo não possui nós fim (que sao referenciados mas não referenciam).");
        }
    }
   
    
    private void runLinkCount(AttributeModel attributeModel,Graph graph, Map<String, PathStat> paths) {
        AttributeColumn lcCol = createAttribute(attributeModel.getEdgeTable(), ColumnConstantes.LINK_COUNT_NAME);
        
        if(DEBUG){
            debugMessages.add("Iniciando link count paths="+paths.size());
        }
        int index = lcCol.getIndex();

        for (Entry<String, PathStat> entry : paths.entrySet()) {
            //pegando a coluna e  incrementando
            for (Edge ee : entry.getValue().getEdges()) {
                Attributes attributes = ee.getAttributes();
                Integer i = (Integer) attributes.getValue(index);

                if (i == null) {
                    attributes.setValue(index, new Integer(1));
                } else {
                    attributes.setValue(index, new Integer(i.intValue() + 1));
                }
                
                 if (cancel) {
                    return;
                }
            }
        }
    }

    private AttributeColumn createAttribute(AttributeTable table, String colName) {
        AttributeColumn lcCol;
        //se existir deleta
        if (table.hasColumn(colName)) {
            //deletando a coluna existente e criando uma nova
            lcCol = table.getColumn(colName);
            table.removeColumn(lcCol);
        }

        lcCol = table.addColumn(colName, colName, AttributeType.INT, AttributeOrigin.COMPUTED, ZERO);
        return lcCol;
    }

    private PathStat[] listPaths(List<PathStat> paths, int pathNumber) {
        ArrayList<PathStat> v = new ArrayList<PathStat>();
        v.addAll(paths);
        Collections.sort(v, new PathLCComparator());
        
        //removendo acima de 10
        while(v.size()> pathNumber){
            v.remove(v.size()-1);
        }

        return (PathStat[])v.toArray(new PathStat[pathNumber]);
    }
    
    private void deletePath(AttributeModel attributeModel, String colName){
        deletePath(attributeModel.getEdgeTable(), colName);
        deletePath(attributeModel.getNodeTable(), colName);
    }
    
    private void deletePath(AttributeTable table, String colName){
        //removing all splc paths
        List<AttributeColumn> remove = new ArrayList<AttributeColumn>();
        for( AttributeColumn a: table.getColumns()){
            if(a.getId().contains(colName)){
                remove.add(a);
            }
        }
        for(AttributeColumn a:remove){
            table.removeColumn(a);
        }  
    }

    private void markPaths(AttributeModel attributeModel, Map<String, PathStat> paths,int pathNumber) {
        PathStat[] arr = listPaths(new ArrayList<PathStat>(paths.values()), pathNumber);


        String baseCol = ColumnConstantes.SPLC_PATH;
        for (int i = 1; i <= pathNumber; i++) {
            createAttribute(attributeModel.getEdgeTable(), baseCol+i);
            createAttribute(attributeModel.getNodeTable(), baseCol+i);
        }

        for (int i = 0; i < arr.length; i++) {
            List<Edge> ee = arr[i].getEdges();
            for (Edge e : ee) {
                //marcando os nós e arestas como 1
                e.getAttributes().setValue(baseCol+(i+1), ONE);
                e.getSource().getAttributes().setValue(baseCol+(i+1), ONE);
                e.getTarget().getAttributes().setValue(baseCol+(i+1), ONE);
            }
        }
    }
    
    private void markMergeColumn(Map<String, PathStat> paths){
        if(mergePath > 0){
            PathStat[] arr = listPaths(new ArrayList<PathStat>(paths.values()), mergePath);
            String baseCol = ColumnConstantes.SPLC_PATH_MERGE;
            for (int i = 0; i < arr.length; i++) {
                List<Edge> ee = arr[i].getEdges();
                for (Edge e : ee) {
                    //marcando os nós e arestas como 1
                    e.getAttributes().setValue(baseCol, ONE);
                    e.getSource().getAttributes().setValue(baseCol, ONE);
                    e.getTarget().getAttributes().setValue(baseCol, ONE);
                }
            }
        }
    }
    
    private void createColumnsStartAndEnd(AttributeModel attributeModel){
        AttributeColumn ref = createAttribute(attributeModel.getNodeTable(), ColumnConstantes.PATH_REF);
        
        int index = ref.getIndex();
        
        for(Node n:startNodes){
           n.getAttributes().setValue(index, M_ONE);
        }
        
        for(Node n:endNodes){
           n.getAttributes().setValue(index, ONE);
        }
    }
    
    @Override
    public boolean isCancel() {
        return cancel;
    }

    public int getPathNumber() {
        return pathNumber;
    }

    void setPathNumber(int pathNumber) {
        this.pathNumber = pathNumber;
    }

    public int getMergePath() {
        return mergePath;
    }

    public void setMergePath(int mergePath) {
        this.mergePath = mergePath;
    }

    public int getPathOnReport() {
        return pathOnReport;
    }

    public void setPathOnReport(int pathOnReport) {
        this.pathOnReport = pathOnReport;
    }
    
    
    
    
    /**
     * -----------------------------------------------------------
     */
    @Override
    public String getReport() {
        if(pathOnReport == 0){
            return null;
        }
        
        //Write the report HTML string here
        StringBuilder report = new StringBuilder();
        
        report.append("<html><body><h1>Search Path Link Count</h1><hr/>");
        PathStat[] arr = listPaths(new ArrayList<PathStat>(paths.values()),pathOnReport);
         report.append("<h3>Resumo SPLC</h3>");
        report.append("<table border='1'>");
        report.append(ReportUtils.tableRow("th",
                new Object[]{
                    "<b>ID</b>",
                    "<b>SPLC</b>",
                    "<b>Relevance</b>",
                    "<b>Number<br/> of Nodes</b>",
                    "<b>Initial Node</b>",
                    "<b>End Node</b>"}));
        int i= 1;
        int relevance = 1;
        int lastStast = -1;
        for(PathStat p:arr){
            if(p.getLinkCountTotal() != lastStast){
                lastStast = p.getLinkCountTotal();
                relevance = i;
            }
            report.append(ReportUtils.tableRow("td",
                    new Object[]{
                        i++,
                        p.getLinkCountTotal(),
                        relevance,
                        p.getNodeCount(),
                        p.getStartNode().getNodeData().getId(),
                        p.getEndNode().getNodeData().getId()
                    }));
        }
        report.append("</table>");
        report.append("<hr>");
        report.append("<h3>Inicital Nodes:").append(startNodes.size()).append(" Node(s) </h3>");
        report.append("<p>");
        report.append(ReportUtils.listNodeToCsv(startNodes));
        report.append("</p>");

        report.append("<h3>End Nodes:").append(endNodes.size()).append(" Node(s)</h3>");
        report.append("<p>");
        report.append(ReportUtils.listNodeToCsv(endNodes));
        report.append("</p>");
        report.append("<hr>");
        i=0;
        for(PathStat p:arr){
            report.append("<h3>Nodes in SPLC Path - ").append(1+i++).append("</h3>");
            report.append("<p>").append(ReportUtils.pathToString(p)).append("</p>");
        }
        
        if(!errors.isEmpty()){
            report.append("<h1>Erro no processamento</h1>");
            report.append(ReportUtils.listToHTML(errors, "ol"));
            errors.clear();
        }
        report.append("<p><b>Algorithm implemented by:</b><br/><i>J.Eleandro Cust&oacute;dio (2012)</i></p>");
        report.append("</body></html>");
        return report.toString() ;
    }

}