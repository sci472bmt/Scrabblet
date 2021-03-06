
import java.net.*;
import java.io.*;
import java.util.*;
import java.net.SocketPermission;
public class Server implements Runnable {
  private int port = 5432;
  private Hashtable idcon = new Hashtable();
  private static int id = 0;
  static final String CRLF = "\r\n";
  synchronized void addConnection(Socket s) {
    ClientConnection con = new ClientConnection(this, s, id);
    // we will wait for the ClientConnection to do a clean
    // handshake setting up its "name" before calling
    // set() below, which makes this connection "live."
    id++;
  }
  synchronized void set(String the_id, ClientConnection con) {
    idcon.remove(the_id) ;  // make sure we're not in there twice.
    con.setBusy(false);
    // tell this one about the other clients.
    Enumeration e = idcon.keys();
    while (e.hasMoreElements()) {
      String id = (String)e.nextElement();
      ClientConnection other = (ClientConnection) idcon.get(id);
      if (!other.isBusy())
        con.write("add " + other + CRLF);
    }
    idcon.put(the_id, con);
    broadcast(the_id, "add " + con);
  }
  synchronized void sendto(String dest, String body) {
    ClientConnection con = (ClientConnection)idcon.get(dest);
    if (con != null) {
      con.write(body + CRLF);
    }
  }
  synchronized void broadcast(String exclude, String body) {
    Enumeration e = idcon.keys();
    while (e.hasMoreElements()) {
      String id = (String)e.nextElement();
      if (!exclude.equals(id)) {
        ClientConnection con = (ClientConnection) idcon.get(id);
        con.write(body + CRLF);
      }
    }
  }
  synchronized void delete(String the_id) {
     broadcast(the_id, "delete " + the_id);
  }
  synchronized void kill(ClientConnection c) {
    if (idcon.remove(c.getId()) == c) {
      delete(c.getId());
    }
  }
  public void run() {
    try {
SocketPermission p = new SocketPermission("127.0.0.1:5432","accept,connect,listen");
      ServerSocket acceptSocket = new ServerSocket(port);
      System.out.println("Server listening on port " + port);
	  Socket s=null;
      while (true) {
       s = acceptSocket.accept();
        addConnection(s);
      }
    } catch (IOException e) {
      System.out.println("accept loop IOException: " + e);
    }
  }
  public static void main(String args[]) {
    new Thread(new Server()).start();
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) { }
  }
}