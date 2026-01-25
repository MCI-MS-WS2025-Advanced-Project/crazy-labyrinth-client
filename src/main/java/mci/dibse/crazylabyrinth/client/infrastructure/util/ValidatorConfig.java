package mci.dibse.crazylabyrinth.client.infrastructure.util;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * @author Julian Janetschek
 */
public class ValidatorConfig {

    public static Validator createBeanValidator() {
        try (ValidatorFactory factory =
                Validation.byProvider(HibernateValidator.class)
                        .configure()
                        .messageInterpolator(
                                new ParameterMessageInterpolator()) // avoid jakarta.el dependency
                        .buildValidatorFactory()) {
            return factory.getValidator();
        }
    }
}
