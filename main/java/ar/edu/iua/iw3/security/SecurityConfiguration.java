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

import ar.edu.iua.iw3.auth.IUserBusiness;
import ar.edu.iua.iw3.auth.custom.CustomAuthenticationManager;
import ar.edu.iua.iw3.auth.filters.JWTAuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    private IUserBusiness userBusiness;

    // 1. Define el método bCryptPasswordEncoder como un Bean
    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Define el AuthenticationManager usando el método anterior
    @Bean
    public AuthenticationManager authenticationManager() {
        // Aquí llamamos a bCryptPasswordEncoder() que ya está definido arriba
        return new CustomAuthenticationManager(bCryptPasswordEncoder(), userBusiness);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Obtenemos el manager para el filtro de JWT
        AuthenticationManager authManager = authenticationManager();

        http.csrf(csrf -> csrf.disable());
        
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/register").permitAll()
                
                // Restricción por roles
                .requestMatchers(HttpMethod.DELETE, "/api/v1/ordenes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/ordenes/**").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes/**").hasAnyRole("ADMIN", "OPERADOR")

                // Requiere JWT para el resto de la API
                .requestMatchers("/api/v1/**").authenticated() 
                .anyRequest().authenticated());

        http.sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        // Agregar el filtro de JWT
        http.addFilter(new JWTAuthorizationFilter(authManager));

        return http.build();
    }
}