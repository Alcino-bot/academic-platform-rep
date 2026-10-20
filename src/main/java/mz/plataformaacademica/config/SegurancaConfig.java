package mz.plataformaacademica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import mz.plataformaacademica.autenticacao.FiltroDeAutoridades;
import mz.plataformaacademica.autenticacao.ServicoDeAutenticacao;

@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtrosDeSeguranca(
            HttpSecurity http, ServicoDeAutenticacao servico, CsrfTokenRepository repositorioCsrf) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(repositorioCsrf)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(pedidos -> pedidos
                .requestMatchers("/api/estado", "/api/auth/csrf", "/api/auth/entrar").permitAll()
                .requestMatchers("/api/contas/registar", "/api/contas/activar", "/api/contas/reenviar-verificacao").permitAll()
                .requestMatchers("/api/administracao/**").hasAuthority("PAPEL_GLOBAL_ADMIN_GERAL")
                .anyRequest().authenticated())
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterAfter(new FiltroDeAutoridades(servico), SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    public static PasswordEncoder codificadorDePalavrasPasse() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public static CsrfTokenRepository repositorioDeTokensCsrf() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public UserDetailsService semUtilizadoresPorOmissao() {
        return nome -> {
            throw new UsernameNotFoundException("A autenticação é feita pelo serviço próprio");
        };
    }
}
