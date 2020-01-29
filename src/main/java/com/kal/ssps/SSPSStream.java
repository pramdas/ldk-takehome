package com.kal.ssps;

import com.kal.ssps.model.DataRecord;
import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SSPSStream {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Please provide command line arguments: configPath");
            System.exit(1);
        }
        /**
         *
         Load properties from a configuration file
         The configuration properties defined in the configuration file are assumed to include:
              ssl.endpoint.identification.algorithm=https
              sasl.mechanism=PLAIN
              bootstrap.servers=<CLUSTER_BOOTSTRAP_SERVER>
              sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="<CLUSTER_API_KEY>" password="<CLUSTER_API_SECRET>";
              security.protocol=SASL_SSL*/
        final Properties props = loadConfig(args[0]);
        stream(props);
    }

    public static void stream(final Properties props) {
        System.out.println("\n\n\nStarting Streaming SSPS with interval 1 second, Please wait few min for initializing ...\n\n");
        final String topic = props.getProperty("kafka.source.topic");
        // Add additional properties
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ssps-streams-1");
        // Disable caching to print the aggregation value after each record
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG,0);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG,3);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1* 1000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final Serde<DataRecord> DataRecord = getJsonSerde();

        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, DataRecord> records = builder.stream(topic, Consumed.with(Serdes.String(), DataRecord));
//        records.print(Printed.<String, DataRecord>toSysOut().withLabel("Consumed Original Record "));
        final KStream<DataRecord, DataRecord> ssps = records
                .map((k,v) -> {
                    final DataRecord clone = new DataRecord(v.getDevice(), v.getTitle(), v.getCountry());
                    return new KeyValue<>(clone, clone);
                });
        // Group all record by time windows and map to final result
        final KStream<String, DataRecord> results = ssps
                .groupByKey(Grouped.with(DataRecord, DataRecord))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(1)))
                .count()
                .toStream()
                .map((k,v) -> {
                    final DataRecord data = k.key();
                    data.setSps(v);
                    return new KeyValue<>("", data);
                });
        results.print(Printed.<String, DataRecord>toSysOut().withLabel("Final Counted: "));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static Serde<DataRecord> getJsonSerde() {
        Map<String, Object> serdeProps = new HashMap<>();
        serdeProps.put("json.value.type", DataRecord.class);

        final Serializer<DataRecord> mySerializer = new KafkaJsonSerializer<>();
        mySerializer.configure(serdeProps, false);

        final Deserializer<DataRecord> myDeserializer = new KafkaJsonDeserializer<>();
        myDeserializer.configure(serdeProps, false);

        return Serdes.serdeFrom(mySerializer, myDeserializer);
    }
    private static Properties loadConfig(final String configFile) throws IOException {
        if (!Files.exists(Paths.get(configFile))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }
}
