package ba.sum.fsre.zupniured.config;

import ba.sum.fsre.zupniured.service.KorisnikUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final KorisnikUserDetailsService korisnikUserDetailsService;

    public SecurityConfig(KorisnikUserDetailsService korisnikUserDetailsService) {
        this.korisnikUserDetailsService = korisnikUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .addFilterBefore(new KorisnikStatusFilter(korisnikUserDetailsService),
                        AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/error", "/css/**", "/js/**",
                                "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/korisnici", "/korisnici/**").hasRole("ADMIN")
                        .requestMatchers("/dogadjaji", "/dogadjaji/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers("/zupljani", "/zupljani/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers("/krstenja", "/krstenja/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers("/prva-pricest", "/prva-pricest/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers("/krizma", "/krizma/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers("/vjencanja", "/vjencanja/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers("/misne-nakane", "/misne-nakane/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/obavijesti", "/obavijesti/{id}").permitAll()
                        .requestMatchers("/admin/obavijesti", "/admin/obavijesti/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .requestMatchers("/dokumenti", "/dokumenti/**")
                            .hasAnyRole("ADMIN", "ZUPNIK", "DJELATNIK")
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("lozinka")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }
}
