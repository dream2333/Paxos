import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


// According to the definition of paxos, a node can be either acceptor or proposer. 
// In this project, member has the functions of proposing, electing and accepting election results at the same time, 
// so we will use member class to represent both acceptor and proposer
public class Member {

    // Server node properties
    public int serverID;

    public String name;

    public int serverPort;

    private int timeout;

    private int lag;

    private double loss;

    // lag
    public static int IMMEDIATE = 0;
    public static int MEDIUM = 500;
    public static int LATE = 900;
    public static int NEVER = -1;

    // Current proposal version information
    private long promisedProposal = -1;

    private long acceptedProposal = -1;

    private String acceptedValue = Message.NULL;

    // Number of majority nodes
    private int majority;

    // Node address
    private ArrayList<Integer> serverPortList;

    // Server and client sockets
    private DatagramSocket acceptor;
    private DatagramSocket proposer;


    // Initializing member properties and start a new socketserver to listen for socket messages
    // *port: start port
    // *serverID: server id, used to generate proposal ID
    // *timeout: timeout wait time
    // *lag: network delay
    // *loss: packet loss rate
    public Member(int port, int serverID, int timeout, int lag, double loss) {
        this.serverID = serverID;
        this.name = "M" + serverID;
        this.timeout = timeout;
        this.serverPort = port;
        this.lag = lag;
        this.loss = loss;
        try {
            this.proposer = new DatagramSocket();
            proposer.setSoTimeout(timeout);
            this.acceptor = new DatagramSocket(port);
            print("Server M" + serverID + " started at : " + port);
        } catch (SocketException e) {
            print("[ERROR] Server Start Error");
            e.printStackTrace();
        }
        startAcceptor();
    }

    // Save server nodes and the number of majorities
    public void setServerPortList(ArrayList<Integer> ports) {
        serverPortList = ports;
        majority = serverPortList.size() / 2 + 1;
    }

    // Start proposer thread
    public void startProposer() {
        Thread t = new Thread(() -> {
            this.sendProposal(1);
        });
        t.start();
    }

    // Start acceptor thread
    public void startAcceptor() {
        Thread t = new Thread(() -> {
            try {
                this.onAcceptorReceived();
            } catch (Exception e) {
                print("[ERROR] Network Error");
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // Send proposal to acceptor
    // *round: records the number of rounds to re-initiate a request after an id conflict
    public String sendProposal(int round) {
        // Generates a unique proposal id that is larger than the last used
        long id = Util.genProposalID(serverID);
        print("=====send proposal " + id + "=====");
        // Cache the results of each round
        ArrayList<Message> promisedList = new ArrayList<Message>();
        ArrayList<Integer> promisedAcceptorsPort = new ArrayList<Integer>();
        // Broadcast the message to all acceptors and save the results of this round
        print("send prepare");
        for (int port : serverPortList) {
            Message promise = doPrepare(port, id);
            // If null is returned it means the response timeout or network error
            if (promise != null) {
                promisedList.add(promise);
                promisedAcceptorsPort.add(port);
            }
        }
        // The proposer receives a promise message from the majority and constructs a new proposal (n,value)
        if (promisedList.size() >= majority) {
            long n = id;
            String value = name;
            // If there is a proposal in all these promise messages, update the value to the proposal with the largest number
            // If there is no proposal in all these promise messages, send member's name
            for (int i = 0; i < promisedList.size(); i++) {
                if (!promisedList.get(i).isNull()) {
                    value = promisedList.get(i).value;
                }
            }
            // Proposer sends an ACCEPT message to each node in the acceptor
            String lastValue = null;
            print("send accept");
            for (int port : promisedAcceptorsPort) {
                Message lastAccepted = doAccept(port, n, value);
                // If any rejections or null messages received, resend a new proposal
                if (lastAccepted != null) {
                    if (lastAccepted.isReject()) {
                        // Pause for a random period of time and then retry to avoid live lock
                        try {
                            int interval = (int) (Math.random() * 1000);
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return sendProposal(round + 1);
                    }
                    lastValue = lastAccepted.value;
                }
            }
            print("=====Proposal result=====\n>>> Proposal(" + id + ")  \n>>> " + lastValue
                    + " elected;\n>>> round : "
                    + round);
            return lastValue;
        }
        // If the number of received promises is not more than half, return null
        print("=====Proposal result=====\n>>> Proposal(" + id + ")  \n>>> No majority of responses received;");
        return null;
    }

    // Phase 1:
    // proposer sends a prepare request to the acceptor and decides the next step based on the acceptor's return value
    // *port: the port of the acceptor
    // *proposalID: the proposal id sent
    // return: PROMISE or null
    public Message doPrepare(int port, long proposalID) {
        try {
            proposer.setSoTimeout(timeout);
            proposer.connect(InetAddress.getLocalHost(), port);
            // Send prepare
            // Use the "null" for proposals in the PREPARE stage
            Message prepareMsg = new Message(proposalID, Message.NULL, Message.PREPARE, name);
            DatagramPacket packet = prepareMsg.toPacket();
            proposer.send(packet);
            print(prepareMsg + " -> M" + (port - 60000));
            // receive result
            byte[] buffer = new byte[1024];
            packet = new DatagramPacket(buffer, buffer.length);
            delay();
            proposer.receive(packet);
            // Drop the packet and throw an exception if the simulation situation is packet loss
            if (block()) {
                throw new SocketTimeoutException();
            }
            Message result = new Message(packet);
            print(result + " from " + result.from);
            return result;
        } catch (SocketTimeoutException e) {
            print("[ERROR] Message Sent But Received Timeout");
            return null;
        } catch (Exception e) {
            print("[ERROR] NetWork ERROR");
            return null;
        }
    }

    // Phase 2
    // Send ACCEPT
    // *port: port of acceptor
    // *proposalID: the proposal number to send
    // *value: the value to send
    // return :ACCEPTED or null
    public Message doAccept(int port, long proposalID, String value) {
        try {
            proposer.connect(InetAddress.getLocalHost(), port);
            // Send ACCEPT
            Message acceptMsg = new Message(proposalID, value, Message.ACCEPT, name);
            DatagramPacket packet = acceptMsg.toPacket();
            proposer.send(packet);
            print("send " + acceptMsg + " -> M" + (port - 60000));
            // receive result
            byte[] buffer = new byte[1024];
            packet = new DatagramPacket(buffer, buffer.length);
            delay();
            proposer.receive(packet);
            // Drop the packet and throw an exception if the simulation situation is packet loss
            if (block()) {
                throw new SocketTimeoutException();
            }
            Message result = new Message(packet);
            print("receive " + result + " from " + result.from);
            return result;
        } catch (SocketTimeoutException e) {
            print("[ERROR] Received Timeout");
            return null;
        } catch (Exception e) {
            print("[ERROR] NetWork ERROR");
            return null;
        }
    }

    // Process incoming message packets
    // Acceptor is the server side of member and needs to listen to the port to process the received packets
    public void onAcceptorReceived() throws IOException, InterruptedException {
        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            delay();
            this.acceptor.receive(packet);
            // Drop the packet if the simulation situation is packet loss
            if (block()) {
                continue;
            }
            Message msg = new Message(packet);
            if (msg.isPrepare()) {
                // If the acceptor receives a proposal with a number greater than the promisedProposal, update the promisedProposal with n.
                if (msg.id > promisedProposal) {
                    promisedProposal = msg.id;
                    if (acceptedProposal != -1) {
                        // Return the proposal id and value If other proposals have been accepted 
                        Message promiseMsg = new Message(acceptedProposal, acceptedValue, Message.PROMISE, name);
                        byte[] data = promiseMsg.toBytes();
                        packet.setData(data);
                        acceptor.send(packet);
                        print("return " + promiseMsg + " -> " + msg.from);
                    } else {
                        // Return the the current proposalID and null if no other proposal has been accepted
                        Message promiseMsg = new Message(msg.id, Message.NULL, Message.PROMISE, name);
                        byte[] data = promiseMsg.toBytes();
                        packet.setData(data);
                        acceptor.send(packet);
                        print("return " + promiseMsg + " -> " + msg.from);
                    }
                } else {
                    // If the received id <= promisedProposal, the proposal is rejected
                    Message rejectMsg = new Message(msg.id, Message.NULL, Message.REJECT, name);
                    byte[] data = rejectMsg.toBytes();
                    packet.setData(data);
                    acceptor.send(packet);
                    print("return REJECT -> " + msg.from);
                }
            } else if (msg.isAccept()) {
                // If an acceptor receives an ACCEPT message and received id >= promisedProposal
                // the value in the proposal is accepted and stored persistently.
                if (msg.id >= promisedProposal) {
                    acceptedProposal = promisedProposal = msg.id;
                    acceptedValue = msg.value;
                    Message acceptedMsg = new Message(msg.id, acceptedValue, Message.ACCEPTED, name);
                    byte[] data = acceptedMsg.toBytes();
                    packet.setData(data);
                    acceptor.send(packet);
                    print("return " + acceptedMsg + " -> " + msg.from);
                } else {
                    // If the received id < promisedProposal, the accept is rejected
                    Message rejectMsg = new Message(promisedProposal, Message.NULL, Message.REJECT, name);
                    byte[] data = rejectMsg.toBytes();
                    packet.setData(data);
                    acceptor.send(packet);
                    print("return REJECT -> " + msg.from);
                }
            }
        }
    }

    // Formatting output messages
    private void print(String str) {
        System.out.println("[" + name + "] " + str);
    }

    // Simulation of network latency
    private void delay() throws InterruptedException {
        if(lag>0){
            Thread.sleep(lag);
        }
    }

    // Simulate network packet loss
    // return : packet loss when true is returned, no packet loss when false is returned
    private boolean block() {
        if (lag == Member.NEVER || Math.random() < loss) {
            return true;
        }
        return false;
    }
}
