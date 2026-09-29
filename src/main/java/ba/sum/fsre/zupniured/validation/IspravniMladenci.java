package ba.sum.fsre.zupniured.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MladenciValidator.class)
public @interface IspravniMladenci {
    String message() default "Provjerite podatke mladenaca.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
