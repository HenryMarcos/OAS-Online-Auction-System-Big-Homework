package com.groupproject.client.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {

    private static final Properties properties = new Properties();

    static {
        String fileName = "client.properties";
        InputStream is = null;
        try {
            
            // Thử lần 1: Tìm trong classpath root(Tốt cho Maven/Gradle "resources" folder)
            is = ClientConfig.class.getResourceAsStream("/" + fileName);

            // Thử lần 2: Tìm trong ClassLoader(Tốt cho VSCode)
            if (is == null) {
                is = ClientConfig.class.getClassLoader().getResourceAsStream(fileName);
            }

            // Thử lần 3: tìm ở bên ngoài file exe
            if (is == null) {
                System.out.println("Property file not in JAR. Searching external folder...");
                is = new FileInputStream(fileName);
            }

            // If we found it in any of the 3 places, load it!
            if (is != null) {
                properties.load(is);
                System.out.println("Successfully loaded " + fileName);
                is.close();
            } else {
                System.err.println("CRITICAL: " + fileName + " not found ANYWHERE!");
                
            }

        } catch (Exception e) {
            System.err.println("Error reading " + fileName);
                e.printStackTrace();
        }
    }


    public static String getServerIp() {
        // Return the IP, or default to localhost if something went terribly wrong
        return properties.getProperty("SERVER_IP", "localhost");
    }

    public static int getServerPort() {
        return Integer.parseInt(properties.getProperty("SERVER_PORT", "8080"));
    }
}
