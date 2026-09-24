package com.pedeja.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String TOKEN_INVALIDO = "Autenticação necessária ou token inválido";

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			SecurityErrorWriter securityErrorWriter
	) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								securityErrorWriter.escrever(request, response, HttpStatus.UNAUTHORIZED, TOKEN_INVALIDO))
						.accessDeniedHandler((request, response, exception) ->
								securityErrorWriter.escrever(request, response, HttpStatus.FORBIDDEN, "Acesso negado")))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								HttpMethod.POST,
								"/api/v1/auth/cadastro",
								"/api/v1/auth/login",
								"/api/v1/restaurantes/cadastro"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/restaurantes", "/api/v1/restaurantes/**").permitAll()
						.requestMatchers("/api/v1/meu-restaurante", "/api/v1/meu-restaurante/**").hasRole("RESTAURANTE")
						.requestMatchers("/api/v1/pedidos", "/api/v1/pedidos/**").hasRole("CLIENTE")
						.requestMatchers(
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/actuator/health"
						).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint((request, response, exception) ->
								securityErrorWriter.escrever(request, response, HttpStatus.UNAUTHORIZED, TOKEN_INVALIDO))
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.build();
	}

	/**
	 * O token carrega o perfil na claim "perfil" (CLIENTE ou RESTAURANTE). O prefixo
	 * ROLE_ permite usar hasRole("CLIENTE") nas regras de acesso.
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("perfil");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}
}
