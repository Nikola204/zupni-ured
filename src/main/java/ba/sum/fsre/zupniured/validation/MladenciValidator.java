package ba.sum.fsre.zupniured.validation;

import ba.sum.fsre.zupniured.dto.VjencanjeForma;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MladenciValidator implements ConstraintValidator<IspravniMladenci, VjencanjeForma> {
    @Override
    public boolean isValid(VjencanjeForma forma, ConstraintValidatorContext context) {
        if (forma == null) return true;
        context.disableDefaultConstraintViolation();
        boolean ispravno = true;
        if (forma.getMladozenjaZupljaninId() == null) {
            ispravno &= obavezno(forma.getImeMladozenje(), "imeMladozenje", "Ime mladoženje je obavezno.", context);
            ispravno &= obavezno(forma.getPrezimeMladozenje(), "prezimeMladozenje", "Prezime mladoženje je obavezno.", context);
        }
        if (forma.getNevjestaZupljaninId() == null) {
            ispravno &= obavezno(forma.getImeNevjeste(), "imeNevjeste", "Ime nevjeste je obavezno.", context);
            ispravno &= obavezno(forma.getPrezimeNevjeste(), "prezimeNevjeste", "Prezime nevjeste je obavezno.", context);
        }
        if (forma.getMladozenjaZupljaninId() != null
                && forma.getMladozenjaZupljaninId().equals(forma.getNevjestaZupljaninId())) {
            context.buildConstraintViolationWithTemplate("Mladoženja i nevjesta ne mogu biti isti župljanin.")
                    .addPropertyNode("nevjestaZupljaninId").addConstraintViolation();
            ispravno = false;
        }
        return ispravno;
    }

    private boolean obavezno(String tekst, String polje, String poruka, ConstraintValidatorContext context) {
        if (tekst != null && !tekst.isBlank()) return true;
        context.buildConstraintViolationWithTemplate(poruka).addPropertyNode(polje).addConstraintViolation();
        return false;
    }
}
