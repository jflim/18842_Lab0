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
    private List<String> memberNames;
    private Map<String, Node> membersNodes;
    //private ClockService groupTimeStamp;

	public Group(String groupName, List<String> memberNames,
			Map<String, Node> nodes, ClockService groupTimeStamp) {
		this.groupName = groupName;
		this.memberNames = memberNames;
		//this.groupTimeStamp = groupTimeStamp;

		this.membersNodes = new HashMap<String, Node>();
		for (String name : nodes.keySet()) {
			if (memberNames.contains(name)) {
				this.membersNodes.put(name, nodes.get(name));
			}
		}

	}
	
	
	// getters
	
	public String getName(){
		return groupName;
	}
	
	public Map<String, Node> getNodes(){
		return membersNodes;
	}
	
	public List<String> memberNames(){
		return memberNames;
		
	}
}
