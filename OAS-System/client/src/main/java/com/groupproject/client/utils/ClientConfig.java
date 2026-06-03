package com.groupproject.client.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {

    private static final Properties properties = new Properties();

    static {
        String fileName = "client.properties";
        
        // Use try-with-resources to automatically prevent memory/file lock leaks
        try (InputStream classpathStream = ClientConfig.class.getResourceAsStream("/" + fileName);
             InputStream classLoaderStream = ClientConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            
            if (classpathStream != null) {
                properties.load(classpathStream);
                System.out.println("Loaded config from Classpath.");
            } else if (classLoaderStream != null) {
                properties.load(classLoaderStream);
                System.out.println("Loaded config from ClassLoader.");
            } else {
                // Fallback to external file
                try (InputStream fileStream = new FileInputStream(fileName)) {
                    properties.load(fileStream);
                    System.out.println("Loaded config from external file.");
                }
            }
        } catch (Exception e) {
            System.err.println("CRITICAL: Error reading " + fileName);
            e.printStackTrace();
        }
    }

    public static String getServerIp() {
        return properties.getProperty("SERVER_IP", "localhost");
    }

    public static int getServerPort() {
        return Integer.parseInt(properties.getProperty("SERVER_PORT", "8080"));
    }
}