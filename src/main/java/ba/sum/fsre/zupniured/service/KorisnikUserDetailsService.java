package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.repository.KorisnikRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KorisnikUserDetailsService implements UserDetailsService {

    private final KorisnikRepository korisnikRepository;

    public KorisnikUserDetailsService(KorisnikRepository korisnikRepository) {
        this.korisnikRepository = korisnikRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        var korisnik = korisnikRepository.findByEmailIgnoreCase(email.strip())
                .orElseThrow(() -> new UsernameNotFoundException("Neispravni podaci za prijavu."));

        return User.withUsername(korisnik.getEmail())
                .password(korisnik.getLozinka())
                .roles(korisnik.getUloga().name())
                .disabled(!korisnik.isAktivan())
                .build();
    }
}
