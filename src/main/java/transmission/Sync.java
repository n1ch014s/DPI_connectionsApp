package transmission;

import connections.GraphUtil;
import connections.Node;

/** The Peer to Peer synchronisation.
 *
 */
public class Sync{
    private GraphUtil graph;

    /**
     * Creates a Sync Manager which controls the parsing and processing of incoming and outgoing nodes
     * @param graph the graph which contains the friend data
     */
    public Sync(GraphUtil graph){
        this.graph = graph;
    }

    /**
     * Parses and Processes incoming json data, turning them into nodes
     * @param json the incoming data, this is turned into a node with a friend list
     */
    public void processIncomingNode(String json){
        Node remote = parse(json);

        if(remote.isFriend){
            graph.update(remote);
        }
        else {
            graph.addFriend(remote); //is the addfriend method supposed to be default? isnt that an interface thing?
            graph.addConnection(getLocalNode(), remote);
        }
    }

}
