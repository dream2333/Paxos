
import java.util.ArrayList;

public class Council {
    static ArrayList<Member> memberList = new ArrayList<Member>();
    static ArrayList<Integer> memberPortList = new ArrayList<Integer>();

    // add a new member to server clusters (council)
    public Member addMember(int lag, double loss) {
        int id = memberPortList.size() + 1;
        int port = 60000 + id;
        Member member = new Member(port, id, 1000, lag, loss);
        memberList.add(member);
        memberPortList.add(port);
        return member;
    }

    public void updateMemberList() {
        for (Member member : memberList) {
            member.setServerPortList(memberPortList);
        }
    }

    public Member getMember(int index){
        return memberList.get(index);
    }
}
