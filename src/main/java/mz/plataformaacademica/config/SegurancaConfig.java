package mz.plataformaacademica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtrosDeSeguranca(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(pedidos -> pedidos
                .requestMatchers("/api/estado").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder codificadorDePalavrasPasse() {
        return new BCryptPasswordEncoder();
    }
}
