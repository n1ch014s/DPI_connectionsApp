package com.unibas.socialconnections.GUI;

import java.security.PublicKey;
import java.util.*;

import connections.GraphUtil;
import connections.Node;

public class PathGraphBuilder {

    static int lastID;
    static HashMap<PublicKey, String> idMap;
    public static class GraphNode {
        public final String id;    // full encoded key, used for lookups
        public final String label; // short display text
        public final int layer;    // hop distance from user (0 = user)

        public GraphNode(String id, String label, int layer) {
            this.id = id;
            this.label = label;
            this.layer = layer;
        }
    }

    public static class GraphEdge {
        public final String fromId;
        public final String toId;

        public GraphEdge(String fromId, String toId) {
            this.fromId = fromId;
            this.toId = toId;
        }
    }

    public static class GraphData {
        public final List<GraphNode> nodes = new ArrayList<>();
        public final List<GraphEdge> edges = new ArrayList<>();
    }

    public static GraphData build(LinkedList<PublicKey[]> paths, GraphUtil graph, String otherUserName) {
        GraphData data = new GraphData();
        Map<String, GraphNode> nodeMap = new LinkedHashMap<>();
        String userName = graph.getUserNode().getName();

        nodeMap.put(userName, new GraphNode(graph.getUserNode().getPublicKey().toString(), userName, 0));
        if(paths.get(0).length == 1) {
            nodeMap.put(otherUserName, new GraphNode(paths.get(0)[0].toString(), otherUserName, 1));
            data.nodes.addAll(nodeMap.values());
            return data;
        }
        lastID = 0;
        idMap = new HashMap<>();
        for(PublicKey[] path:paths) {
            String previous = userName;
            for(int i = 0; i < path.length; i++) {
                PublicKey pub = path[i];
                if(pub == null) {
                    continue;
                }
                if(i == path.length -1) {
                    nodeMap.putIfAbsent(otherUserName, new GraphNode(pub.toString(), otherUserName, path.length));
                    continue;
                }
                String id = getID(pub, graph);
                nodeMap.putIfAbsent(id, new GraphNode(pub.toString(), id, i+1));
                data.edges.add(new GraphEdge(previous, id));
                previous = id;
            }
        }
        data.nodes.addAll(nodeMap.values());
        return data;
    }

    private static String getID(PublicKey pub, GraphUtil graph){
        if(idMap.containsKey(pub)) {
            return idMap.get(pub);
        }
        if(graph.nodeList.containsKey(pub)) {
            if(graph.nodeList.get(pub).isFriend) {
                String name = graph.nodeList.get(pub).getName();
                idMap.put(pub, name);
                return name;
            }
            else {
                String id = String.valueOf(++lastID);
                idMap.put(pub, id);
                return id;
            }
        }
        else {
            String id = String.valueOf(++lastID);
            idMap.put(pub, id);
            return id;
        }
    }
}
