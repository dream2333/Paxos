target:
	@javac paxos/*.java
test:
	@cd paxos && java TestCase $(CASE) | tee ../output/Test$(CASE)Output.txt
clean:
	@rm paxos/*.class