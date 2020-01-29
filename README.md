# Stream-Starts-Per-Second
SPS: the Pulse of Netflix Streaming

The propose solution: 

The Producer will consume the source event from SSE and push to the kafka topic. The Kafka Stream will consume from this topic and group by time window is 1 second and print out the stream to system out or (push to other topic).

A. Run Standalone Machine:
The Producer and Consumer with embedding inside SSPDriverStandalone Class

```
java -cp target/Stream-Starts-Per-Second-1.0-SNAPSHOT-jar-dependencies.jar com.kal.ssps.SSPDriverStandalone </path/to/file/kafkaconfig>
```
Ps: Due to connection to Confluent Cluster, initialize take 1-2mins ( I will check the reason slow init)

B. Scalability

 Questions: If all events could not be processed on a single processor, or a single machine?

In order to scale out the system to consumer from multipe source : Server Sent Event 
The source will consume from Server Sent Event writen by rxJava and sent to kafka cluster with specific kafka topic 'ssps_stat' or can change in configFile. We can run the producer in multiple node to get the resources in distributed system.

   I. Run Producer in multiple Machine:

```
java -cp target/Stream-Starts-Per-Second-1.0-SNAPSHOT-jar-dependencies.jar com.kal.ssps.SSPSProducer </path/to/file/kafkaconfig>
```

   II. Run Kafka Stream in one Node to count and print result:

Capture the result by Kafka Stream and aggregate on one second intervals grouped by device, title, country to compute counts for these combinations. Events with sev = “success” count as a successful stream start, this is what we want to count.

In this, the input stream reads from a topic named "ssps_stat" provided by configFile, where the values of
   - messages represent lines of serialization of object; and the histogram output is written to system out
   - OR I can write to other topic "ssps_-count-output" if need multiple consumer result (not implement), where each record is an updated count of each event
     

```
java -cp target/Stream-Starts-Per-Second-1.0-SNAPSHOT-jar-dependencies.jar com.kal.ssps.SSPSStream </path/to/file/kafkaconfig>
```

Question: How can your solution handle variations in data volume throughout the day?
- I can change to use Avro schema. 
- We can have fexible data format and serialization in Generic.


Example Output Kakfa Stream Count SPSS:

![stream count](https://github.com/ldkhanh/Stream-Starts-Per-Second/blob/master/Sample%20Output%20From%20KafkaStream%20Count.png)
