package ba.sum.fsre.zupniured.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

// Promjena uloge ili deaktivacija vrijedi i za već prijavljene korisnike.
public class KorisnikStatusFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    public KorisnikStatusFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            boolean valjanaPrijava;
            try {
                var korisnik = userDetailsService.loadUserByUsername(authentication.getName());
                valjanaPrijava = korisnik.isEnabled()
                        && uloge(korisnik.getAuthorities()).equals(uloge(authentication.getAuthorities()));
            } catch (UsernameNotFoundException e) {
                valjanaPrijava = false;
            }
            if (!valjanaPrijava) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private Set<String> uloge(Collection<? extends GrantedAuthority> ovlasti) {
        // Spring Security pri prijavi dodaje i FACTOR_PASSWORD, koji nije korisnička uloga.
        return ovlasti.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(ovlast -> ovlast.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }
}
