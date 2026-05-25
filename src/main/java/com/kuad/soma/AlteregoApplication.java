package com.kuad.soma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@EnableJpaAuditing(dateTimeProviderRef = "kstDateTimeProvider")
@SpringBootApplication
public class AlteregoApplication {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	public static void main(String[] args) {
		SpringApplication.run(AlteregoApplication.class, args);
	}

	@Bean
	public DateTimeProvider kstDateTimeProvider() {
		return () -> Optional.of(LocalDateTime.now(KOREA_ZONE_ID));
	}

}
