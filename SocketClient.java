import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.util.concurrent.*;
import java.util.Scanner;

public class SocketClient {

    private static final Logger LOGGER = Logger.getLogger(SocketClient.class.getName());
    private static final BlockingQueue<String> MESSAGE_QUEUE = new LinkedBlockingQueue<>();
    private static volatile boolean RUNNING = true;

    public static void main(String[] args) {
        // if (args.length < 3) {
        //     System.out.println("Usage: java SocketClient <server> <port> <input>");
        //     return;
        // }
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        // StringBuilder input = new StringBuilder();
        // for (int i = 0; i < args.length; i++) {
        //     input.append(args[i]).append(" ");
        // }

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.INFO);

        if (args.length > 2 && args[2].equals("-w")) {
            LOGGER.setLevel(Level.WARNING);
        } else if (args.length > 2 && args[2].equals("-s")) {
            LOGGER.setLevel(Level.SEVERE);
        }

        try (Socket sock = new Socket(server, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true)) {
                new Thread(() -> {
                try {
                    String line;
                    while (RUNNING && (line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error reading from socket", e);
                }
            }).start();

            // start a separate thread for command-line I/O
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (RUNNING) {
                    String line = scanner.nextLine();
                    MESSAGE_QUEUE.offer(line);
                }
            }).start();

            // main thread handles sending messages down the socket
            while (RUNNING) {
                String message = MESSAGE_QUEUE.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    out.println(message);
                }
            }
        } catch(IOException e){
            LOGGER.log(Level.SEVERE, "Error connecting to server", e);
        } catch(InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}