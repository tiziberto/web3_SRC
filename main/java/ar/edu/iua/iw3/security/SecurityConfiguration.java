package ar.edu.iua.iw3.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ar.edu.iua.iw3.auth.IUserBusiness;
import ar.edu.iua.iw3.auth.custom.CustomAuthenticationManager;
import ar.edu.iua.iw3.auth.filters.JWTAuthorizationFilter;
import ar.edu.iua.iw3.controllers.Constants;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

	/* (Bloque de código de configuración de ejemplo, se mantiene comentado) */

	@Bean
	PasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedMethods("*").allowedHeaders("*").allowedOrigins("*");
			}
		};
	}

	@Autowired
	private IUserBusiness userBusiness;

	@Bean
	AuthenticationManager authenticationManager() {
		return new CustomAuthenticationManager(bCryptPasswordEncoder(), userBusiness);
	}

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CORS: https://developer.mozilla.org/es/docs/Web/HTTP/CORS
        // CSRF: https://developer.mozilla.org/es/docs/Glossary/CSRF
        http.csrf(AbstractHttpConfigurer::disable);
        
        http.authorizeHttpRequests(auth -> auth
                // 1. PERMITIR TODAS LAS PETICIONES OPTIONS (PRE-VUELO CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 2. PERMITIR POST y PUT a la RUTA LITERAL de ÓRDENES y sus sub-recursos (Punto 1 y 2)
                .requestMatchers(HttpMethod.POST, "/api/v1/ordenes/**").permitAll() // Usamos /** para máxima tolerancia
                .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/ordenes/**").permitAll() // Permitimos GET para verificación
                
                // 3. PERMITIR POST a la RUTA LITERAL de LOGIN
                .requestMatchers(HttpMethod.POST, "/api/v1/login").permitAll()
                
                // 4. AGREGAR EXCLUSIÓN PARA MANEJADOR DE ERRORES (Cubre GET y POST /error)
                .requestMatchers("/error").permitAll() 
                
                // 5. RUTAS DE DOCUMENTACIÓN Y DEMO (también públicas)
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/ui/**").permitAll()
                .requestMatchers("/demo/**").permitAll()
                
                // 6. CUALQUIER OTRA PETICIÓN REQUIERE AUTENTICACIÓN (JWT)
                .anyRequest().authenticated());
        
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilter(new JWTAuthorizationFilter(authenticationManager()));
        return http.build();

    }

}