package ar.edu.iua.iw3.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import ar.edu.iua.iw3.auth.IUserBusiness;
import ar.edu.iua.iw3.auth.custom.CustomAuthenticationManager;
import ar.edu.iua.iw3.auth.filters.JWTAuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    private IUserBusiness userBusiness;

    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new CustomAuthenticationManager(bCryptPasswordEncoder(), userBusiness);
    }

    // En SecurityConfiguration.java
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authManager = authenticationManager();

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                // Estas rutas DEBEN ser públicas y estar ANTES que el filtro de JWT
                .requestMatchers(HttpMethod.POST, "/api/v1/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/register").permitAll()

                // 1. Crear orden (POST /api/v1/ordenes) -> Solo SAP
                .requestMatchers(HttpMethod.POST, "/api/v1/ordenes").hasRole("SAP")
                
                // 2. Registro de tara (PUT /api/v1/ordenes/{numeroOrden}/tara) -> Solo TMS
                .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes/*/tara").hasRole("TMS")
                
                // 3. Recepción de datos (POST /api/v1/ordenes/flow) -> Solo CLI
                // Nota: En tu controlador es POST, si quieres que sea PUT debes cambiarlo allí también.
                .requestMatchers(HttpMethod.POST, "/api/v1/ordenes/flow").hasRole("CLI")
                
                // 4. Cierre de orden (PUT /api/v1/ordenes/{numeroOrden}/close) -> Solo CLI
                .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes/*/close").hasRole("CLI")
                
                // 5. Pesaje final (PUT /api/v1/ordenes/{numeroOrden}/final-weighing) -> Solo TMS
                .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes/*/final-weighing").hasRole("TMS")
                
                // 6. Conciliación (GET /api/v1/ordenes/{numeroOrden}/conciliacion) -> Solo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/ordenes/*/conciliacion").hasRole("ADMIN")

                .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes/*/aceptar-alarma").hasRole("ADMIN")

                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated());

        // Agregar el filtro
        http.addFilter(new JWTAuthorizationFilter(authManager));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // URL de tu frontend Vue
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}