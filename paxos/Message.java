import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

// Message object for storing message state, serializing and deserializing message content
public class Message {

    // The body of the Message
    public long id;
    public String value;
    public String msgType;
    public String from;

    // Constants
    public static final String NULL = "NULL";
    public static final String PREPARE = "PREPARE";
    public static final String PROMISE = "PROMISE";
    public static final String ACCEPT = "ACCEPT";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String REJECT = "REJECT";

    public Message(long id, String value, String type, String from) {
        this.id = id;
        this.value = value;
        this.msgType = type;
        this.from = from;
    }

    // Creating message from packet
    public Message(DatagramPacket packet) {
        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
        String[] temp = msg.split(",");
        this.id = Long.parseLong(temp[0]);
        this.value = temp[1];
        this.msgType = temp[2];
        this.from = temp[3];
    }

    @Override
    public String toString() {
        return msgType + "(" + id + ", " + value + ")";
    }

    public byte[] toBytes() {
        return (id + "," + value + "," + msgType+ "," + from).getBytes();
    }

    public DatagramPacket toPacket() {
        byte[] data = toBytes();
        return new DatagramPacket(data, data.length);
    }

    public boolean isNull() {
        return value.equals(NULL);
    }

    public boolean isReject() {
        return msgType.equals(REJECT);
    }

    public boolean isPrepare() {
        return msgType.equals(PREPARE);
    }

    public boolean isAccept() {
        return msgType.equals(ACCEPT);
    }

    public boolean isAccepted() {
        return msgType.equals(ACCEPTED);
    }

    public boolean isPromise() {
        return msgType.equals(PROMISE);
    }

}
