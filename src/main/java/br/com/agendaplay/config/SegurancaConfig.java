package br.com.agendaplay.config;

import br.com.agendaplay.repository.UsuarioRepository;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Locale;

@Configuration
public class SegurancaConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService usuarios(UsuarioRepository usuarios) {
        return email ->
                usuarios.buscarPorEmail(email.trim().toLowerCase(Locale.ROOT))
                        .map(UsuarioAutenticado::new)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("E-mail ou senha inválidos."));
    }

    @Bean
    SecurityFilterChain seguranca(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        rotas ->
                                rotas.requestMatchers(
                                                "/",
                                                "/login",
                                                "/cadastro/**",
                                                "/css/**",
                                                "/js/**",
                                                "/error")
                                        .permitAll()
                                        .requestMatchers("/proprietario/**")
                                        .hasRole("PROPRIETARIO")
                                        .requestMatchers("/cliente/**")
                                        .hasRole("CLIENTE")
                                        .anyRequest()
                                        .authenticated())
                .formLogin(
                        login ->
                                login.loginPage("/login")
                                        .usernameParameter("email")
                                        .passwordParameter("senha")
                                        .defaultSuccessUrl("/painel", true)
                                        .failureUrl("/login?erro"))
                .logout(
                        logout ->
                                logout.logoutUrl("/sair")
                                        .logoutSuccessUrl("/login?saiu")
                                        .invalidateHttpSession(true)
                                        .clearAuthentication(true)
                                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(erros -> erros.accessDeniedPage("/acesso-negado"))
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                        csp ->
                                                csp.policyDirectives(
                                                        "default-src 'self'; style-src 'self';"
                                                            + " script-src 'self'; img-src 'self'"
                                                            + " data:; form-action 'self';"
                                                            + " frame-ancestors 'none'")))
                .build();
    }
}
