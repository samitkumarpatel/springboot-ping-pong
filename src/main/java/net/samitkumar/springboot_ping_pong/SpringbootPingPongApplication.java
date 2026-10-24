package net.samitkumar.springboot_ping_pong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

@SpringBootApplication
public class SpringbootPingPongApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootPingPongApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes() {
		return RouterFunctions
				.route()
				.GET("/ping", request -> ServerResponse.ok().body(Map.of("message", "pong")))
				.build();
	}

}
