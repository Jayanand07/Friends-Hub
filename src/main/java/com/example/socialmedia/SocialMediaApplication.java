package com.example.socialmedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.socialmedia.repository")
@EnableCaching
@EnableScheduling
@EnableAsync
public class SocialMediaApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(SocialMediaApplication.class, args);
	}

	private static void loadDotEnv() {
		java.io.File envFile = new java.io.File(".env");
		if (envFile.exists()) {
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) continue;
					int idx = line.indexOf('=');
					if (idx > 0) {
						String key = line.substring(0, idx).trim();
						String value = line.substring(idx + 1).trim();
						if (System.getProperty(key) == null && System.getenv(key) == null) {
							System.setProperty(key, value);
						}
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
	}

}
