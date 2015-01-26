/**
 * Created by gs on 1/26/15.
 */
public class MessagePasser {

    public void MessagePasser(String configuration_filename, String local_name){

        ServerThread server = new ServerThread(this);
        new Thread(server).start();

    }

}
