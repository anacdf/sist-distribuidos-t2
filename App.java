import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import threads.ReceiveUDP;

public class App {
    private static int myProcessId;
    private static String myIp;
    private static int myPort;
    private static Double myChance;
    private static int myEvents;
    private static String myMinDelay;
    private static String myMaxDelay;
    private static List<String> otherHosts;
    private static List<Process> processes;
    private static int[] clock;
    private static String fileName;
    private static DatagramSocket socket;
    private static DatagramPacket packet;

    private static ReceiveUDP receiveUDP;

    public static void main(String[] args) throws FileNotFoundException, SocketException {

        if (args.length < 2) {
            System.out.println("Missing parameters");
            return;
        }

        set_up(args);

        try {
            initialize(fileName);
            set_socket();
            multicast_start();
        } catch (Exception e) {
            System.out.println(e);
        }

        start_local_clock();

        receive_udp_thread();

        run();

        // new ReceiveUDP(myPort).start();

        // debug_method();

        // start_multicast();
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
                        // System.out.println(e);
                    }
                }
            }
        };

        thread.start();
    }

    private static void received_event(String data) {
        String[] splitedData = data.split(" ");

        String receivedId = splitedData[1];
        String receivedClock = splitedData[3];

        clock[Integer.parseInt(receivedId)] = Integer.parseInt(receivedClock);

        print_vetorial_clock("R", null, receivedId, receivedClock);
    }

    private static void multicast_start() throws IOException, InterruptedException {
        MulticastSocket multicast_socket = new MulticastSocket(5000);
        byte[] resource = new byte[1024];
        InetAddress address = InetAddress.getByName("230.0.0.1");
        DatagramPacket packet;

        int totalProcess = otherHosts.size() + 1;
        int[] ready = new int[totalProcess];
        Arrays.fill(ready, 0);

        ready[myProcessId] = 1;

        multicast_socket.joinGroup(address);

        while (true) {
            System.out.println("Waiting processes answer multicast...");

            packet = new DatagramPacket(resource, resource.length);

            try {
                multicast_socket.setSoTimeout(5000);
                multicast_socket.receive(packet);
            } catch (Exception e) {
                // TODO: handle exception
            }

            String received_process_id = new String(packet.getData(), 0, packet.getLength());

            System.out.println("received_process_id " + received_process_id);

            try {
                int received_process_id_int = Integer.parseInt(received_process_id);

                ready[received_process_id_int] = 1;
            } catch (Exception e) {
                // TODO: handle exception
            }

            boolean match = Arrays.stream(ready).allMatch(s -> s == 1);

            if (match) {
                System.out.println("MATCH");
                DatagramPacket packetID = new DatagramPacket(Integer.toString(myProcessId).getBytes(), Integer.toString(myProcessId).getBytes().length, address, 5000);
                socket.send(packetID);
                multicast_socket.leaveGroup(address);
                multicast_socket.close();
                return;
            } else {
                System.out.println("NOT MATCH");
                DatagramPacket packetID = new DatagramPacket(Integer.toString(myProcessId).getBytes(), Integer.toString(myProcessId).getBytes().length, address, 5000);
                socket.send(packetID);
                Thread.sleep(3000);
            }
        }
    }

    private static void local_inc() {
        clock[myProcessId]++;

        print_vetorial_clock("L", null, null, null);
    }

    private static void external_inc(int id) {
        clock[myProcessId]++;

        Process p = processes.get(id);

        String message = "id " + id + " clock " + clock[id];

        try {
            send_udp_message(message, p.getAddress(), p.getPort());
        } catch (IOException e) {
            System.out.println("Error send UDP message!");
            System.out.println(e);
        }

        print_vetorial_clock("S", String.valueOf(id), null, null);
    }

    public static void send_udp_message(String message, String ip, String port) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(ip),
                Integer.parseInt(port));
        socket.send(packet);
    }

    public static void run() {
        int countEvent = 0;
        // while (countEvent < myEvents) {
        while (countEvent < 10) {
            float rnd = random_func(0.1, 0.9);
            if (rnd < myChance) {
                int rndId = new Random().ints(0, (otherHosts.size())).findFirst().getAsInt();
                external_inc(rndId);
            } else {
                local_inc();
            }
            countEvent++;
        }
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
    }

    public static void set_socket() {
        try {
            socket = new DatagramSocket(myPort);
        } catch (Exception e) {
            System.out.println(e);
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
                myMinDelay = processLine[5];
                myMaxDelay = processLine[6];

            } else {
                otherHosts.add(processLine[0]);
                processes.add(new Process(processLine[0], processLine[1], processLine[2]));
            }
        }

        br.close();
    }

    public static void debug_method() {
        System.out.println("processes");
        for (Process i : processes) {
            System.out.println(i);
        }

        System.out.println();

        System.out.println("print other hosts");
        for (String i : otherHosts) {
            System.out.println(i);
        }

        System.out.println("clock");
        for (int i : clock) {
            System.out.print(i);
        }

        System.out.println();

        System.out.println("Test print_vetorial_clock ");
        print_vetorial_clock("L", null, null, null);
        print_vetorial_clock("S", "1", null, null);
        print_vetorial_clock("R", null, "3", "28");
    }
}