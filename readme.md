# Paxos Protocol

本项目是一个基于Paxos的分布式董事会投票程序，参考了[Paxos Made Simple ](http://lamport.azurewebsites.net/pubs/paxos-simple.pdf "Paxos Made Simple ")，使用多线程和Socket通信模拟服务器节点，能够正确快速在一个分布式系统中对某个proposal达成一致。
##### Features
- Paxos Compliant
- 每个member可以自由模拟网络质量，包括网络延迟和丢包率
- 具有容错能力，只要大多数服务器还运行，决议就能继续进行
- 当多个member同时发送proposal时，不会产生livelock，程序仍然能够继续运行
- 5个测试用例

##### Assessment
- ✓ Paxos implementation works when two councillors send voting proposals at the same time
- ✓ Paxos implementation works in the case where all M1-M9 have immediate responses to voting queries
- ✓ Paxos implementation works when M1 – M9 have responses to voting queries suggested by the profiles above, including when M2 or M3 propose and then go offline
- ✓ Testing harness for the above scenarios
- ✓ Paxos implementation works with a number ‘n’ of councilors with four profiles of response times: immediate; medium; late; never
- x Fast Byzantine Paxos implementation

### Design
包含四个核心类：

##### Council
保存了服务器集群节点内容，用于创建新的Member
##### Member
包含了proposor和accepter,用于接收和发送proposal,并得出一致性结果
##### Message
用于序列化和反序列化socket消息对象
##### TestCase
测试用例
##### Util
其他的工具类

### Getting Started
1. 编译：

		make

2. 测试

		make test CASE={CASENUMBER}

	例如，我想要运行测试用例0和测试用例2，便可以输入以下命令，测试结果将导出至output文件夹
	> make test CASE=0
	> make test CASE=2


3. 清理

		make clean
输入此命令可以清理所有已经编译过的class文件

### Test Case
##### Description:
一共有5个测试用例，分别对应不同情况下的选举情况，测试结果将导出至output文件夹。

当有新的成员加入董事会时，会显示这样一条消息，代表其对应的socket服务器和acceptor已顺利启动

> [M1] Server M1 started at : 60001

每一个proposal发送后，都会显示详细输出，例如：

> [M2] =====send proposal 16667831854940002=====
[M2] send prepare
[M2] PREPARE(16667831854940002, NULL) -> M1
[M1] return PROMISE(16667831854940002, NULL) -> M2
[M3] return PROMISE(16667822825860002, NULL) -> M2
...
[M2] receive ACCEPTED(16667822825860002, M2) from M9
[M2] =====Proposal result=====
\>>> Proposal(16667822825860002)
\>>> M2 elected;
\>>> round : 1

其中，左侧的[M2]代表这条调试信息来自于哪个Member，右侧的信息包含了提案内容，消息的目的地或来源地，proposalID(proposalID的生成方式为时间戳*10000+serverID)
> [M2] =====send proposal 16667831854940002=====
[M2] PREPARE(16667831854940002, NULL) -> M1

当proposal结果顺利返回的时候，会显示proposal的id，最终的当选人，以及这个proposal提交了几轮（当proposal id发生冲突时，会重新提交id更大的提案，会导致轮数的增加）
> [M2] =====Proposal result=====
\>>> Proposal(16667822825860002)
\>>> M2 elected;
\>>> round : 1

当然，也有另一种情况，即未收到大多数member的回复,这种情况下一般为自身网络情况或多数member的网络情况不佳

> [M1] =====Proposal result=====
\>>> Proposal(16667848252930001)  
\>>> No majority of responses received;

当无法收到回复时，会经常出现以下内容，以方便我们观察网络状况
> [M1] [ERROR] Receive Timeout

##### Case0:
M1 and M2 voting for themselves at the same time. All members have IMMEDIATE responses

**Expected output：**

1. 当同时发出prepare请求时，M2的proposalID永远大于M1,因此选举结果总是M2当选
2. 在不存在延迟的情况下，两个Proposal result都是M2当选
3. M1提交了两轮Proposal，M2提交了一轮Proposal

##### Case1:
M1-M9 all voting for themselves. All members have IMMEDIATE responses

**Expected output：**

1. 当同时发出prepare请求时，M9的proposalID永远大于任何人,因此选举结果总是M9当选
2. 在不存在延迟的情况下，所有Proposal result都是M9当选
3. 其他的Proposer提交的轮数大于1

##### Case2:
M1 has IMMEDIATE responses. M2 or M3 propose and then go offline. Other members have varied responses, never become unavailable

**Expected output：**

1. 选举结果总是M1当选
2. 发送给m1、m2的消息均显示[ERROR] Receive Timeout
3. M1的Proposal result消息显示m1当选，M2/M3由于发出PREPARE后立即下线，Proposal result显示 No majority of responses received

##### Case3:
M1 has IMMEDIATE responses.M2 has LATE responses, He often miss most of the emails, and sometimes he receives them all and replies IMMEDIATELY.M3 has MEDIUM responses, but sometimes emails completely do not get to M3.Other members have varied delay, never become unavailable.

###### The network model for M2 has a 50% probability of 
LATE responses with 0.8 loss （50%）
IMMEDIATELY responses with 0.0 loss （50%）

###### The network model for M3 has a 50% probability of 
MEDIUM responses with 0.0 loss （50%）
NEVER responses with 1.0 loss （50%）


**Expected output：**

1. 多次运行得到的选举结果可能不一致，但一定符合Paxos定义
2. 不同的成员展示了不同的网络状况

##### Case4:
Paxos implementation works with a number 'n' of councilors with four profiles of response times: immediate; medium; late; never；M1 voting for himself. **为了方便测试，我将n设置为9-29之间的一个随机数字**

**Expected output：**

1. M1 elected且展现了正常的Proposal流程
2. M1提交了一轮Proposal
3. 不同的成员展示了不同的网络状况

### Other
我使用了以下环境进行测试，测试结果已保存在 /output 文件夹下：
- SMP Debian 4.19.232-1 x86_64 GNU/Linux with openjdk version "11.0.16"
- Win11 Home Edition 10.0.22000 x86_64 with openjdk version "17.0.2"