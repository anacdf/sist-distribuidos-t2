import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import model.Process;

public class App {
    private static int myProcessId;
    private static String myIp;
    private static int myPort;
    private static Double myChance;
    private static int myEvents;
    private static float myMinDelay;
    private static float myMaxDelay;
    private static List<String> otherHosts;
    private static List<Process> processes;
    private static int[] clock;
    private static String fileName;
    private static DatagramSocket socket;
    private static DatagramPacket packet;
    private static Semaphore sem;

    public static void main(String[] args) throws FileNotFoundException, SocketException, InterruptedException {

        if (args.length < 2) {
            System.out.println("Missing parameters");
            return;
        }

        set_up(args);

        try {
            initialize(fileName);
            set_socket();
            start();
        } catch (Exception e) {
            // e.printStackTrace();
        }

        start_local_clock();

        receive_udp_thread();

        run();
    }

    private static void receive_udp_thread() {
        byte[] resource = new byte[1024];

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        packet = new DatagramPacket(resource, resource.length);
                        socket.receive(packet);

                        String receivedData = new String(packet.getData(), 0, packet.getLength());
                        received_event(receivedData);
                    } catch (IOException e) {
                        // e.printStackTrace();
                    } catch (InterruptedException e) {
                        // e.printStackTrace();
                    }
                }
            }
        };

        thread.start();
    }

    private static void received_event(String data) throws InterruptedException {
        try {
            String[] splitedData = data.split(" ");
            int receivedId = Integer.parseInt(splitedData[1]);
            int receivedClock = Integer.parseInt(splitedData[3]);

            if(receivedClock == -1){
                socket.close();
                System.out.println("Process " + receivedId + " is done!");
                System.exit(1);
            } else {             
                sem.acquire();
            clock[myProcessId]++;
                clock[receivedId] = receivedClock;
                sem.release();
                print_vetorial_clock("R", null, splitedData[1], splitedData[3]);
            }

        } catch (Exception e) {
            // e.printStackTrace();
        }

    }

    private static void start() throws IOException, InterruptedException {
        byte[] resource = new byte[1024];
        DatagramPacket packet;

        int totalProcess = otherHosts.size() + 1;
        int[] ready = new int[totalProcess];
        Arrays.fill(ready, 0);

        ready[myProcessId] = 1;

        System.out.println("Waiting processes answer...");
        while (true) {
            packet = new DatagramPacket(resource, resource.length);

            try {
                socket.setSoTimeout(1000);
                socket.receive(packet);
            } catch (Exception e) {
                // e.printStackTrace();
            }

            String received_process_id = new String(packet.getData(), 0, packet.getLength());

            try {
                int received_process_id_int = Integer.parseInt(received_process_id);

                ready[received_process_id_int] = 1;
            } catch (Exception e) {
                // e.printStackTrace();
            }

            boolean match = Arrays.stream(ready).allMatch(s -> s == 1);

            if (match) {
                System.out.println("Ready!");
                Thread.sleep(3000);
                return;
            } else {
                for (Process p : processes) {
                    DatagramPacket packetID = new DatagramPacket(Integer.toString(myProcessId).getBytes(),
                            Integer.toString(myProcessId).getBytes().length, InetAddress.getByName(p.getAddress()),
                            Integer.parseInt(p.getPort()));
                    socket.send(packetID);
                }
            }
        }
    }

    private static void local_inc() throws InterruptedException {
        sem.acquire();
        clock[myProcessId]++;
        sem.release();

        print_vetorial_clock("L", null, null, null);
    }

    private static void external_inc(int id) throws InterruptedException {
        sem.acquire();
        clock[myProcessId]++;
        sem.release();

        Process p = processes.get(id);

        try {
            String message = "id " + myProcessId + " clock " + clock[myProcessId];
            send_udp_message(message, p.getAddress(), p.getPort());
            print_vetorial_clock("S", String.valueOf(id), null, null);
        } catch (IOException e) {
            System.out.println("Error send UDP message!");
            e.printStackTrace();
        }

    }

    public static void send_udp_message(String message, String ip, String port) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(ip),
                Integer.parseInt(port));
        socket.send(packet);
    }

    public static void run() throws InterruptedException, IOException {
        int countEvent = 0;
        // while (countEvent < myEvents) {
        while (countEvent < 20) {
            float rnd = random_func(0, 1.0);
            if (rnd < myChance) {
                int rndId = new Random().ints(0, clock.length - 1).findFirst().getAsInt();
                external_inc(rndId);
            } else {
                local_inc();
            }
            countEvent++;

            float rnd_delay = random_func(myMinDelay, myMaxDelay);
            Thread.sleep((long) rnd_delay);
        }

        for (Process p : processes) {
            String message = "id " + myProcessId + " clock " + "-1";
            send_udp_message(message, p.getAddress(), p.getPort());            
        }
        System.out.println("Done!");
        System.exit(1);
    }

    private static void print_vetorial_clock(String event, String nodeTo, String nodeFrom, String clockValue) {
        String vetorialCloclk = myProcessId + " [ ";

        for (int i : clock) {
            vetorialCloclk += i + " ";
        }

        vetorialCloclk += "] ";

        if (event.equals("L")) {
            vetorialCloclk += "L";
        }

        if (event.equals("S")) {
            vetorialCloclk += "S " + nodeTo;
        }

        if (event.equals("R")) {
            vetorialCloclk += "R " + nodeFrom + " " + clockValue;
        }

        System.out.println(vetorialCloclk);
    }

    public static void start_local_clock() {
        int totalProcess = otherHosts.size() + 1; // size of other process + my host

        clock = new int[totalProcess];
        Arrays.fill(clock, 0);
    }

    public static void set_up(String[] args) {
        fileName = args[0];
        myProcessId = Integer.parseInt(args[1]);
        otherHosts = new ArrayList<>();
        processes = new ArrayList<>();
        sem = new Semaphore(1);
    }

    public static void set_socket() {
        try {
            socket = new DatagramSocket(myPort);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public static float random_func(double d, double e) {
        return (float) (d + (Math.random() * e));
    }

    public static void initialize(String fileName) throws IOException {

        File file = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;

        br.readLine(); // skip first line

        while ((st = br.readLine()) != null) {
            String[] processLine = st.split(" ");

            if (processLine[0].equals(Integer.toString(myProcessId))) {
                myIp = processLine[1];
                myPort = Integer.parseInt(processLine[2]);
                myChance = Double.parseDouble(processLine[3]);
                myEvents = Integer.parseInt(processLine[4]);
                myMinDelay = Float.parseFloat(processLine[5]);
                myMaxDelay = Float.parseFloat(processLine[6]);

            } else {
                otherHosts.add(processLine[0]);
                processes.add(new Process(processLine[0], processLine[1], processLine[2]));
            }
        }

        br.close();
    }
}