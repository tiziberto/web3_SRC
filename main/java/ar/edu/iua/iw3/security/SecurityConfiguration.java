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

    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new CustomAuthenticationManager(bCryptPasswordEncoder(), userBusiness);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authManager = authenticationManager();

        http.csrf(csrf -> csrf.disable());
        
        http.authorizeHttpRequests(auth -> auth
                // 1. Permitir acceso público a login y registro
                .requestMatchers(HttpMethod.POST, "/api/v1/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/register").permitAll()
                
                .requestMatchers(HttpMethod.DELETE, "/api/v1/ordenes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/ordenes/**").hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/ordenes/**").hasAnyRole("ADMIN", "OPERADOR")

                // 3. El resto de las funciones (GET, etc.) para CUALQUIER rol autenticado
                .requestMatchers("/api/v1/**").authenticated()
                
                // 3. Cualquier otra ruta (como Swagger o errores) se puede denegar o permitir
                .anyRequest().authenticated()); // Cambiado de permitAll a authenticated para máxima seguridad

        // Configuración de sesión sin estado (JWT)
        http.sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        // Agregar el filtro que procesa el token en cada petición
        http.addFilter(new JWTAuthorizationFilter(authManager));

        return http.build();
    }
}