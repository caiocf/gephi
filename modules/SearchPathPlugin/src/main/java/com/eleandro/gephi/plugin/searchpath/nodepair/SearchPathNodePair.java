/*
 * Your license here
 */
package com.eleandro.gephi.plugin.searchpath.nodepair;

import com.eleandro.gephi.plugin.searchpath.*;
import java.util.*;
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
/**
 *
 * See http://wiki.gephi.org/index.php/HowTo_write_a_metric#Create_Statistics
 *
 * @author José Eleandro Custódio
 */
public class SearchPathNodePair implements Statistics, LongTask, Cancelable {

    private final String NEW_LINE = "</br>\n";
    private static final Integer ZERO = 0;
    private static final Integer ONE = 1;
    private static final Integer M_ONE = -1;
    private boolean cancel = false;
    private ProgressTicket progressTicket;
    //variáveis de sessao, limpar apos uso!!!
    private List<String> errors = new LinkedList<String>();
    private List<String> reportList = new LinkedList<String>();
    private List<String> debugMessages = new LinkedList<String>();
    private static final boolean DEBUG = true;
    private boolean isDirected;
    private int countProgress = 3;
    private float avgNodePair = 0;
    private List<Node> startNodes;
    private List<Node> endNodes;
    private Map<String, PathStat> paths;
    private int pathNumber;
    private int mergePath;
    private int pathOnReport;

    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        cancel = false;
        Graph graph = graphModel.getGraphVisible();
        graph.readLock();
        isDirected = graph instanceof DirectedGraph;

        HierarchicalDirectedGraph directedGraph = null;
        if (isDirected) {
            directedGraph = graph.getGraphModel().getHierarchicalDirectedGraphVisible();
        }

        errors.clear();
        reportList.clear();
        debugMessages.clear();

        try {
            Progress.start(progressTicket, graph.getNodeCount() * 3);
            Progress.progress(progressTicket);


            if (errors.size() > 0) {
                return;
            }

            deletePath(attributeModel, ColumnConstantes.SPNP_PATH);
            deletePath(attributeModel, ColumnConstantes.SPNP_PATH_MERGE);

            createOrUpdateAttribute(attributeModel.getEdgeTable(), ColumnConstantes.NODE_PAIR_NAME);
            createOrUpdateAttribute(attributeModel.getNodeTable(), ColumnConstantes.NP_FROM_TO);
            createOrUpdateAttribute(attributeModel.getNodeTable(), ColumnConstantes.NP_TO_FROM);

            //criando o nodePair
            calculateNodePair(directedGraph);
            //progressTicket.progress("SPNP calculado", graph.getNodeCount() * 3);

            //Marcando nós inicio e nós fim
            findStartEndAnd(graph);

            if (errors.size() > 0) {
                return;
            }

            //cria todos os caminho
            PathStatFactory pathFactory = new PathStatFactory();
            pathFactory.generatePaths(graph, startNodes);
            paths = pathFactory.getPaths();

            //cria as colunas marcando os caminhos
            markPaths(attributeModel, directedGraph, paths, pathNumber);
            markMergeColumn(paths);

            //marcando inicio e fim
            createColumnsStartAndEnd(attributeModel);

            graph.readUnlockAll();
        } catch (OperationNotSupportedException e) {
            errors.add(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
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

    private void createColumnsStartAndEnd(AttributeModel attributeModel) {
        AttributeColumn ref = createOrUpdateLinkCountAtt(attributeModel.getNodeTable(), ColumnConstantes.PATH_REF);

        int index = ref.getIndex();

        for (Node n : startNodes) {
            n.getAttributes().setValue(index, M_ONE);
        }

        for (Node n : endNodes) {
            n.getAttributes().setValue(index, ONE);
        }
    }

    private AttributeColumn createOrUpdateLinkCountAtt(AttributeTable table, String colName) {
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

    private void markPaths(AttributeModel attributeModel, Graph graph, Map<String, PathStat> paths, int pathNumber) {
        PathStat[] arr = listPaths(new ArrayList<PathStat>(paths.values()), pathNumber);


        String baseCol = ColumnConstantes.SPNP_PATH;
        for (int i = 1; i <= pathNumber; i++) {
            createOrUpdateLinkCountAtt(attributeModel.getEdgeTable(), baseCol + i);
            createOrUpdateLinkCountAtt(attributeModel.getNodeTable(), baseCol + i);
        }

        for (int i = 0; i < arr.length; i++) {
            List<Edge> ee = arr[i].getEdges();
            for (Edge e : ee) {
                //marcando os nós e arestas como 1
                e.getAttributes().setValue(baseCol + (i + 1), ONE);
                e.getSource().getAttributes().setValue(baseCol + (i + 1), ONE);
                e.getTarget().getAttributes().setValue(baseCol + (i + 1), ONE);
            }

        }
    }

    private void markMergeColumn(Map<String, PathStat> paths) {
        if (mergePath > 0) {
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

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
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

    /**
     * o Node Pair de uma aresta é basicamente inDegree * outDegree
     *
     * @param attributeModel model para criar a coluna
     * @param graph grafo direcionado para calcular o grau de entrada edges
     * saida
     */
    public void calculateNodePair(HierarchicalDirectedGraph graph) throws OperationNotSupportedException {
        avgNodePair = 0.0f;
        calculateNodePairFromTo(graph);
        calculateNodePairToFrom(graph);

        //calculando a multiplicacao
        for (Edge e : graph.getEdges()) {
            //progressTicket.progress(countProgress++);
            if (cancel) {
                return;
            }
            //ni 
            Integer fromTo = (Integer) getAttribute(e.getSource(), ColumnConstantes.NP_FROM_TO);
            //mj
            Integer toFrom = (Integer) getAttribute(e.getTarget(), ColumnConstantes.NP_TO_FROM);


            int np = fromTo * toFrom;
            e.getAttributes().setValue(ColumnConstantes.NODE_PAIR_NAME, new Integer(np));
            avgNodePair += np;
        }
        avgNodePair = avgNodePair / graph.getEdgeCount();

    }

    private void calculateNodePairFromTo(HierarchicalDirectedGraph graph) throws OperationNotSupportedException {
        HashMap<Node, HashSet<Node>> successors = new HashMap<Node, HashSet<Node>>();
        HashSet<Node> completed = new HashSet();
        for (Node n : graph.getNodes()) {
            calculateNodePairFromTo(n, graph, successors, completed, 0);
        }
        for (Map.Entry<Node, HashSet<Node>> e : successors.entrySet()) {
            Node n = e.getKey();
            HashSet<Node> succ = e.getValue();
            //System.out.println("Node="+n.getNodeData().getId()+" = "+succ.size());
            setAttribute(n, ColumnConstantes.NP_FROM_TO, succ.size());
        }
    }

    private void calculateNodePairFromTo(Node n, Graph graph, HashMap<Node, HashSet<Node>> successors, HashSet complete, int depth) throws OperationNotSupportedException {
        //System.out.println(getTabs(depth) + "node=" + n.getNodeData().getId());
        if (!successors.containsKey(n)) {
            HashSet<Node> succList = new HashSet<Node>();
            successors.put(n, succList);

            //n itsef counts on NP ni
            succList.add(n);

            for (Edge e : graph.getEdges(n)) {
                Node next = e.getTarget();
                if (!e.isSelfLoop() && !n.equals(next)) {
                    if (!succList.contains(next)) {
                        //se nao estiver calculado calcule
                        calculateNodePairFromTo(next, graph, successors, complete, depth + 1);
                        succList.addAll(successors.get(next));
                    } else {
                        if (complete.contains(next)) {
                            succList.addAll(successors.get(next));
                        } else {
                            throw new OperationNotSupportedException("O grafo contém ciclos");
                        }
                    }
                }
            }
            complete.add(n);
        }
    }

    private void calculateNodePairToFrom(Graph graph) throws OperationNotSupportedException {
        HashMap<Node, HashSet<Node>> ancestors = new HashMap<Node, HashSet<Node>>();
        HashSet<Node> completed = new HashSet<Node>();

        for (Node n : graph.getNodes()) {
            calculateNodePairToFrom(n, graph, ancestors, completed, 0);
        }

        for (Map.Entry<Node, HashSet<Node>> e : ancestors.entrySet()) {
            Node n = e.getKey();
            HashSet<Node> succ = e.getValue();
            //System.out.println("Node=" + n.getNodeData().getId() + " = " + succ.size());
            setAttribute(n, ColumnConstantes.NP_TO_FROM, succ.size());
        }
    }

    private void calculateNodePairToFrom(Node n, Graph graph, HashMap<Node, HashSet<Node>> ancestors, HashSet complete, int depth) throws OperationNotSupportedException {
        // System.out.println(getTabs(depth) + "node=" + n.getNodeData().getId());
        if (!ancestors.containsKey(n)) {
            HashSet<Node> ancestorsSet = new HashSet<Node>();
            ancestors.put(n, ancestorsSet);

            //n itsef counts on NP ni
            ancestorsSet.add(n);

            for (Edge e : graph.getEdges(n)) {
                Node prev = e.getSource();
                if (!e.isSelfLoop() && !n.equals(prev)) {
                    if (!ancestorsSet.contains(prev)) {
                        //se nao estiver calculado calcule
                        calculateNodePairToFrom(prev, graph, ancestors, complete, depth + 1);
                        ancestorsSet.addAll(ancestors.get(prev));
                    } else {
                        if (complete.contains(prev)) {
                            ancestorsSet.addAll(ancestors.get(prev));
                        } else {
                            throw new OperationNotSupportedException("O grafo contém ciclos");
                        }
                    }

                }
            }
            complete.add(n);
        }
    }

    private void setAttribute(Node n, String column, Object value) {
        n.getAttributes().setValue(column, value);
    }

    private Object getAttribute(Node n, String column) {
        return n.getAttributes().getValue(column);
    }

    public int getPathNumber() {
        return pathNumber;
    }

    void setPathNumber(int pathNumber) {
        this.pathNumber = pathNumber;
    }

    private AttributeColumn createOrUpdateAttribute(AttributeTable table, String colName) {
        AttributeColumn lcCol;
        //se existir deleta
        if (table.hasColumn(colName)) {
            //deletando a coluna existente edges criando uma nova
            lcCol = table.getColumn(colName);
            table.removeColumn(lcCol);
        }

        lcCol = table.addColumn(colName, colName, AttributeType.INT, AttributeOrigin.COMPUTED, ZERO);
        return lcCol;
    }

    @Override
    public boolean isCancel() {
        return cancel;
    }

    public float getAvgNodePair() {
        return avgNodePair;
    }

    private String getTabs(int depth) {
        StringBuilder sb = new StringBuilder();
        for (; depth > 0; depth--) {
            sb.append('\t');
        }
        return sb.toString();
    }

    private PathStat[] listPaths(List<PathStat> paths, int pathCount) {
        ArrayList<PathStat> v = new ArrayList<PathStat>();
        v.addAll(paths);
        Collections.sort(v, new PathNPComparator());

        //removendo acima de 10
        while (v.size() > pathCount) {
            v.remove(v.size() - 1);
        }

        return (PathStat[]) v.toArray(new PathStat[pathCount]);
    }

    private void deletePath(AttributeModel attributeModel, String column) {
        deletePath(attributeModel.getEdgeTable(),column);
        deletePath(attributeModel.getNodeTable(),column);
    }

    private void deletePath(AttributeTable table, String column) {
        //removing all splc paths
        List<AttributeColumn> remove = new ArrayList<AttributeColumn>();
        for (AttributeColumn a : table.getColumns()) {
            if (a.getId().contains(column)) {
                remove.add(a);
            }
        }
        for (AttributeColumn a : remove) {
            table.removeColumn(a);
        }
    }

    /**
     * -----------------------------------------------------------
     */
    @Override
    public String getReport() {
        if(pathOnReport > 0){
            return null;
        }
        
        //Write the report HTML string here
        StringBuilder report = new StringBuilder();

        report.append("<html><body><h1>Search Path Node Pair</h1><hr/>");

        PathStat[] arr = listPaths(new ArrayList<PathStat>(paths.values()), pathOnReport);
        report.append("<h3>Resumo SPNP</h3>");
        report.append("<table border='1'>");
        report.append(ReportUtils.tableRow("th",
                new Object[]{
                    "<b>ID</b>",
                    "<b>SPNP</b>",
                    "<b>Relevance</b>",
                    "<b>Number<br/> of Nodes</b>",
                    "<b>Initial Node</b>",
                    "<b>End Node</b>"}));
        int i = 1;
        int relevance = 1;
        int lastStast = -1;
        for (PathStat p : arr) {
            if (p.getNodePairTotal() != lastStast) {
                lastStast = p.getNodePairTotal();
                relevance = i;
            }
            report.append(ReportUtils.tableRow("td",
                    new Object[]{
                        i++,
                        p.getNodePairTotal(),
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

        i = 0;
        for (PathStat p : arr) {
            report.append("<h3>Nodes in SPNP Path - ").append(1 + i++).append("</h3>");
            report.append("<p>").append(ReportUtils.pathToString(p)).append("</p>");
        }

        if (!errors.isEmpty()) {
            report.append("<h1>Erro no processamento</h1>");
            report.append(ReportUtils.listToHTML(errors, "ol"));
            errors.clear();
        }
        report.append("<p><b>Algorithm implemented by:</b><br/><i>J.Eleandro Cust&oacute;dio (2012)</i></p>");
        report.append("</body></html>");
        return report.toString();
    }
}