package ro.utcn.photoplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FiltruProtectieLogin filtruProtectieLogin;

    public SecurityConfig(FiltruProtectieLogin filtruProtectieLogin) {
        this.filtruProtectieLogin = filtruProtectieLogin;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Înainte de verificarea parolei: vezi FiltruProtectieLogin.
            .addFilterBefore(filtruProtectieLogin, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/inregistrare", "/login", "/error", "/css/**", "/js/**", "/h2-console/**").permitAll()
                // Resetarea parolei e, prin natura ei, pentru cine nu se poate autentifica.
                .requestMatchers("/parola-uitata", "/reseteaza-parola").permitAll()
                .requestMatchers(HttpMethod.GET, "/locatii/noua").authenticated()
                .requestMatchers(HttpMethod.GET, "/locatii/mele").authenticated()
                .requestMatchers(HttpMethod.GET, "/locatii").permitAll()
                .requestMatchers(HttpMethod.GET, "/locatii/{id}").permitAll()
                // Vizibilitatea pozelor e verificată în cod: cele ale locațiilor private dau 404.
                .requestMatchers(HttpMethod.GET, "/poze/**").permitAll()
                // Harta de pe prima pagină; întoarce doar locații publice.
                .requestMatchers(HttpMethod.GET, "/api/locatii-harta").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/locatii/mele", true)
                .permitAll()
            )
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
