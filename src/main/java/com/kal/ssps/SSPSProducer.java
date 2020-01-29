package com.kal.ssps;


import com.kal.ssps.model.DataRecord;
import com.kal.ssps.util.JsonUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.TopicExistsException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class SSPSProducer {
    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Please provide command line arguments: configPath");
            System.exit(1);
        }
         /***
         Load properties from a configuration file
         The configuration properties defined in the configuration file are assumed to include:
              ssl.endpoint.identification.algorithm=https
              sasl.mechanism=PLAIN
              bootstrap.servers=<CLUSTER_BOOTSTRAP_SERVER>
              sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username = "<CLUSTER_API_KEY" password="<CLUSTER_API_SECRET>";
              security.protocol=SASL_SSL*/
        final Properties props = loadConfig(args[0]);
        producer(props, true);
    }

    public static void producer(final Properties props, boolean printTrace) {
        final String endpoint = props.getProperty("sse.endpoint");
        final String topic = props.getProperty("kafka.source.topic");
        // Create topic if needed
        createTopic(topic, 10,3, props);

        // Add additional properties
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaJsonSerializer");

        Producer<String, DataRecord> producer = new KafkaProducer<String, DataRecord>(props);

        consume(endpoint).subscribeOn(Schedulers.computation())
                .doOnNext(data -> {
                    String ms = (String) data;
                    if (ms!= null && !ms.isEmpty() && ms.contains("\"sev\":\"success\"")) {
//                        System.out.println(ms);
                        DataRecord record = JsonUtil.readValue(ms, DataRecord.class);
                        if (printTrace) System.out.println(record.toString());
                        producer.send(new ProducerRecord<>(topic, "khanh", record), new Callback(){
                            @Override
                            public void onCompletion(RecordMetadata m, Exception e) {
                                if (e != null) {
//                                    e.printStackTrace();
                                } else {
                                    if (printTrace) System.out.printf("Produced record to topic %s partition [%d] @ offset %d%n", m.topic(), m.partition(), m.offset());
                                }
                            }
                        });
                    }
                })
                .doOnError(System.out::println)
                .doOnComplete(()-> System.out.println("Completed"))
                .blockingSubscribe();

        producer.flush();
        producer.close();
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

    private static Observable consume(final String endpoint) {
        System.out.println("Start Consuming From " + endpoint);
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                try {
                    Client sseClient = ClientBuilder.newClient();
                    WebTarget target = sseClient.target(endpoint);
                    SseEventSource eventSource = SseEventSource.target(target).build();
                    try {
                        eventSource.register((sseEvent) -> {
                            String data = sseEvent.readData();
                            emitter.onNext(data);
                        }, (e) -> {
                            emitter.onError(e);
                        });
                        eventSource.open();
                        System.out.println("Source open ? " + eventSource.isOpen());
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        }).observeOn(Schedulers.newThread());

        return observable;
    }

    private static void createTopic(final String topic, final int partitions, final int replication, final Properties cloudConfig) {
        final NewTopic newTopic = new NewTopic(topic, partitions, (short) replication);
        try (final AdminClient adminClient = AdminClient.create(cloudConfig)) {
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();
        } catch (final InterruptedException | ExecutionException e) {
            // Ignore if TopicExistsException, which may be valid if topic exists
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException(e);
            }
        }
    }
}
