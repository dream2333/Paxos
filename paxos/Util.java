import java.util.Arrays;

public class Util {

    // Return the max element in an array
    public static int maxOfArr(int[] arr) {
        Arrays.sort(arr);
        return arr[arr.length - 1];
    }

    // Generate a unique id
    public static long genProposalID(int serverID) {
        // when the serverID is less than 1000 the id is unique
        return System.currentTimeMillis() * 10000 + serverID;
    }
}
