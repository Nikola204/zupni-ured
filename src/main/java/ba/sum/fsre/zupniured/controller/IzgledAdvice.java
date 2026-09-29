package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.IzgledPrikaz;
import ba.sum.fsre.zupniured.service.PregledService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "ba.sum.fsre.zupniured.controller")
public class IzgledAdvice {
    private final PregledService pregledService;
    public IzgledAdvice(PregledService pregledService) { this.pregledService = pregledService; }

    @ModelAttribute("izgled")
    public IzgledPrikaz izgled(Authentication authentication, HttpServletRequest request) {
        String putanja = request.getRequestURI().substring(request.getContextPath().length());
        return pregledService.izgled(authentication == null ? null : authentication.getName(), putanja);
    }
}
