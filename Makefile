target:
	@javac paxos/*.java
test:
	@cd paxos && java TestCase $(CASE)
clean:
	@rm paxos/*.class