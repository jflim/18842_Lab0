import java.net.Socket;

/**
 * Created by gs on 1/26/15.
 */
public class WorkThread implements Runnable{
    private Socket socket;

    public WorkThread(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {



    }
}
