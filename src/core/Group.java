package core;

import Clock.ClockService;
import Clock.VectorClock;
import core.MessagePasser.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gs on 2/15/15.
 */
public class Group {
    private String groupName;
    private List<String> membersName;
    private Map<String, Node> membersNodes;
    private ClockService groupTimeStamp;

    public Group(String groupName, List<String> membersName, Map<String, Node>  nodes, ClockService groupTimeStamp){
        this.groupName = groupName;
        this.membersName = membersName;
        this.groupTimeStamp = groupTimeStamp;

        this.membersNodes = new HashMap<String, Node>();
        for(String name : nodes.keySet()){
            if(membersName.contains(name)) {
                this.membersNodes.put(name, nodes.get(name));
            }
        }

    }
}
