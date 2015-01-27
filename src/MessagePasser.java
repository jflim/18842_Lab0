/**
 * Created by gs on 1/26/15.
 */
public class MessagePasser {

    public MessagePasser(String configuration_filename, String local_name){

        try {
            setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setup() throws Exception {

        ServerThread serverThread = new ServerThread(this, 12345);
        new Thread(serverThread).start();
        System.out.println("Server thread on");

        Client client = new Client();
        client.send();
    }

}
