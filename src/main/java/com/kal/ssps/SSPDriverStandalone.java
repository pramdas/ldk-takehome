package com.kal.ssps;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSPDriverStandalone {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Please provide command line arguments: configPath");
            System.exit(1);
        }
       /***
        *
         Load properties from a configuration file
         The configuration properties defined in the configuration file are assumed to include:
              ssl.endpoint.identification.algorithm=https
              sasl.mechanism=PLAIN
              bootstrap.servers=<CLUSTER_BOOTSTRAP_SERVER>
              sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username = "<CLUSTER_API_KEY" password="<CLUSTER_API_SECRET>";
              security.protocol=SASL_SSL*/

        try {
            final Properties props = loadConfig(args[0]);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.execute(() -> SSPSProducer.producer(props, false));
            executor.execute(() -> SSPSStream.stream(props));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
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
