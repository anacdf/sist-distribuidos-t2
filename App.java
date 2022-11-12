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
import java.util.concurrent.Semaphore;

import threads.ReceiveUDP;

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
            // multicast_start();
            start();
        } catch (Exception e) {
            System.out.println(e);
        }

        start_local_clock();

        receive_udp_thread();

        run();
    }

    private static void receive_udp_thread() {
        System.out.println("receive_udp_thread");
        byte[] resource = new byte[1024];

        Thread thread = new Thread() {
            public void run() {
                System.out.println("run");
                while (true) {
                    try {
                        packet = new DatagramPacket(resource, resource.length);
                        socket.receive(packet);

                        String receivedData = new String(packet.getData(), 0, packet.getLength());
                        received_event(receivedData);
                    } catch (IOException e) {
                        // System.out.println(e);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();
    }

    private static void received_event(String data) throws InterruptedException {
        System.out.println("received_event 88 - data " + data);
        try {
            String[] splitedData = data.split(" ");
            int receivedId = Integer.parseInt(splitedData[1]);
            int receivedClock = Integer.parseInt(splitedData[3]);
            System.out.println("received_event - receivedId " + receivedId);
            System.out.println("received_event - receivedClock " + receivedClock);

            if (receivedId == myProcessId) {
                sem.acquire();
                clock[myProcessId]++;
                sem.release();
            } else {
                sem.acquire();
                clock[receivedId] = receivedClock;
                sem.release();
                print_vetorial_clock("R", null, splitedData[1], splitedData[3]);
            }

        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    private static void start() throws IOException, InterruptedException {
        System.out.println("start");
        byte[] resource = new byte[1024];
        DatagramPacket packet;

        int totalProcess = otherHosts.size() + 1;
        int[] ready = new int[totalProcess];
        Arrays.fill(ready, 0);

        System.out.println("start - meu processo começa contando com 1");

        ready[myProcessId] = 1;

        System.out.println("Waiting processes answer multicast...");
        while (true) {
            System.out.println("start - while");
            packet = new DatagramPacket(resource, resource.length);

            try {
                socket.setSoTimeout(1000);
                socket.receive(packet);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }

            String received_process_id = new String(packet.getData(), 0, packet.getLength());

            System.out.println("start - received_process_id " + received_process_id);

            try {
                int received_process_id_int = Integer.parseInt(received_process_id);

            System.out.println("start - received_process_id_int " + received_process_id_int);

                ready[received_process_id_int] = 1;
                System.out.println("start - atualiza ready na posicao " + received_process_id_int);
            } catch (Exception e) {
                // TODO: handle exception
            }

            boolean match = Arrays.stream(ready).allMatch(s -> s == 1);

            if (match) {
                System.out.println("match - Ready!");
                Thread.sleep(3000);
                return;
            } else {
                for (Process p : processes) {
                    System.out.println("Send to " + p.getAddress() + ":" + p.getPort());
                    DatagramPacket packetID = new DatagramPacket(Integer.toString(myProcessId).getBytes(),
                            Integer.toString(myProcessId).getBytes().length, InetAddress.getByName(p.getAddress()),
                            Integer.parseInt(p.getPort()));
                    socket.send(packetID);
                }
            }
        }
    }

    private static void local_inc() throws InterruptedException {
        System.out.println("local_inc");
        sem.acquire();
        clock[myProcessId]++;
        sem.release();

        print_vetorial_clock("L", null, null, null);
    }

    private static void external_inc(int id) throws InterruptedException {
        System.out.println("external_inc");
        sem.acquire();
        clock[myProcessId]++;
        sem.release();

        Process p = processes.get(id);
        
        try {
            System.out.println("ID " + id + " myPRocessID " + myProcessId);
            if (id != myProcessId) {
                String message = "id " + myProcessId + " clock " + clock[myProcessId];
                send_udp_message(message, p.getAddress(), p.getPort());
                print_vetorial_clock("S", String.valueOf(id), null, null);
            }
        } catch (IOException e) {
            System.out.println("Error send UDP message!");
            System.out.println(e);
        }

    }

    public static void send_udp_message(String message, String ip, String port) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(ip),
                Integer.parseInt(port));
        socket.send(packet);
    }

    public static void run() throws InterruptedException {
        System.out.println("run");
        int countEvent = 0;
        while (countEvent < myEvents) {
        // while (countEvent < 10) {   
            float rnd = random_func(0, 1.0);
            if (rnd < myChance) {
                System.out.println("run - if (rnd < myChance)");
                int rndId = new Random().ints(0, clock.length - 1).findFirst().getAsInt();
                external_inc(rndId);
            } else {
                System.out.println("run - else");
                local_inc();
            }
            countEvent++;

            float rnd_delay = random_func(myMinDelay, myMaxDelay);
            Thread.sleep((long) rnd_delay);
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
        System.out.println("start_local_clock");
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
        System.out.println("set_socket");
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
        System.out.println("initialize");
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
                System.out.println(
                    "initialize - otherHosts" + " " + otherHosts
                );
                System.out.println(
                    "initialize - processes" + " " + processes.size()
                );
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