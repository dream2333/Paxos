import java.util.Random;

public class TestCase {
    public static void main(String[] args) {
        if (args.length > 0) {
            int testCase = Integer.parseInt(args[0]);
            System.out.println(
                    "\n****************************** TEST " + testCase + " BEGIN ******************************");
            switch (testCase) {
                case 0:
                    test0();
                    break;
                case 1:
                    test1();
                    break;
                case 2:
                    test2();
                    break;
                case 3:
                    test3();
                    break;
                case 4:
                    test4();
                    break;
            }
        } else {
            System.out.println("Please Enter the Case Number");
        }
    }

    // M1 and M2 voting for themselves at the same time.
    // All members have IMMEDIATE responses
    public static void test0() {
        Council council = new Council();
        System.out.println("======Start======");
        // M1-M3
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        // M4-M9
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.updateMemberList();
        // M1 and M2 voting for themselves
        council.getMember(0).startProposer();
        council.getMember(1).startProposer();
    }

    // M1-M9 all voting for themselves
    // All members have IMMEDIATE responses
    public static void test1() {
        Council council = new Council();
        System.out.println("======Start======");
        // M1-M3
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        // M4-M9
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.IMMEDIATE, 0.0);
        council.updateMemberList();
        // M1 and M2 voting for themselves
        council.getMember(0).startProposer();
        council.getMember(1).startProposer();
        council.getMember(2).startProposer();
        council.getMember(3).startProposer();
        council.getMember(4).startProposer();
        council.getMember(5).startProposer();
        council.getMember(6).startProposer();
        council.getMember(7).startProposer();
        council.getMember(8).startProposer();
    }

    // M1 has IMMEDIATE responses
    // M2 or M3 propose and then go offline
    // Other members have varied responses, never become unavailable
    public static void test2() {
        Council council = new Council();
        System.out.println("======Start======");
        // M1-M3
        council.addMember(Member.IMMEDIATE, 0.0);
        council.addMember(Member.NEVER, 1);
        council.addMember(Member.NEVER, 1);
        // M4-M9
        for (int i = 4; i < 9; i++) {
            // lag for [100,900) ms
            int lag = (int) (Math.random() * 800 + 100);
            council.addMember(lag, 0.0);
        }
        council.updateMemberList();
        // M1 and M2 voting for themselves
        council.getMember(0).startProposer();
        council.getMember(1).startProposer();
        council.getMember(2).startProposer();
    }

    // M1 has IMMEDIATE responses
    // M2 has LATE responses, He often miss most of the emails (0.80 loss), and
    // sometimes he receives them all and replies IMMEDIATELY.
    // M3 has MEDIUM responses, but sometimes emails completely do not get to M3
    // Other members have varied responses, never become unavailable
    public static void test3() {
        Council council = new Council();
        System.out.println("======Start======");
        // M1 has IMMEDIATE responses
        council.addMember(Member.IMMEDIATE, 0.0);
        // M2
        if (Math.random() > 0.5) {
            // default LATE responses with 0.8 loss
            council.addMember(Member.LATE, 0.8);
        } else {
            // sometimes IMMEDIATELY
            council.addMember(Member.IMMEDIATE, 0.0);
        }
        // M3
        if (Math.random() > 0.5) {
            // default MEDIUM responses
            council.addMember(Member.MEDIUM, 0);
        } else {
            // sometimes emails completely do not get to M3
            council.addMember(Member.NEVER, 1);
        }
        // M4-M9
        for (int i = 4; i < 9; i++) {
            // lag for [100,900) ms
            int lag = (int) (Math.random() * 800 + 100);
            council.addMember(lag, 0.0);
        }

        council.updateMemberList();
        // M1 and M2 voting for themselves
        council.getMember(0).startProposer();
        council.getMember(1).startProposer();
        council.getMember(2).startProposer();
    }

    // Paxos implementation works with a number 'n' of councilors with four
    // profiles of response times: immediate; medium; late; never
    public static void test4() {
        Council council = new Council();
        System.out.println("======Start======");
        int[] resTime = { Member.IMMEDIATE, Member.MEDIUM, Member.LATE, Member.NEVER };
        String[] resTimeName = { "IMMEDIATE", "MEDIUM", "LATE", "NEVER" };
        Random rand = new Random();
        // Generate a random number 'n' greater than 0 and less than 29
        int n = 9 + rand.nextInt(20);
        System.out.println("The value of the random number n : " + n);
        // The test node cannot be NEVER
        int index = rand.nextInt(3);
        System.out.println("[M1] Start with " + resTimeName[index] + " profile");
        council.addMember(Member.IMMEDIATE, 0.0);
        for (int i = 0; i < n - 1; i++) {
            index = rand.nextInt(4);
            System.out.println("[M" + (i + 2) + "] Start with " + resTimeName[index] + " profile");
            council.addMember(resTime[index], 0.0);
        }
        council.updateMemberList();
        council.getMember(0).startProposer();

    }
}
