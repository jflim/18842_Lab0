import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Created by gs on 1/26/15.
 */


public class MessagePasser {

	String configuration_filename;
	String local_name;
    
	public MessagePasser(String configuration_filename, String local_name){
    	this.configuration_filename = configuration_filename;
        this.local_name = local_name;
    	//ServerThread server = new ServerThread(this);
        //new Thread(server).start();
    	
    }
    
    
    
    public void parseConfig() throws FileNotFoundException{
    	InputStream input = new FileInputStream(new File(configuration_filename));
    	Yaml yaml = new Yaml();
    	Object data = yaml.load(input);
    	//System.out.println(data.toString());
    	
    }

}
