package com.googlecode.objectify.util;

import com.googlecode.objectify.annotation.NotNullOrEmpty;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.PossibleValues;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entities which use validation annotations must extend this class
 * 
 * @author Hendrik Pilz <hepisec@gmail.com>
 */
public abstract class ValidatingEntity {

    private static final Logger log = Logger.getLogger(ValidatingEntity.class.getName());
    
    /**
     * This method is called by Objectify and starts the validation of the entity
     * 
     * @throws ValidationException 
     */
    @OnSave
    public void validate() throws ValidationException {
        log.log(Level.FINE, "Validating {0} entity.", this.getClass().getSimpleName());
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            validate(field);
        }
    }

    /**
     * Validate a certain field
     * 
     * @param field
     * @throws ValidationException 
     */
    public void validate(Field field) throws ValidationException {
        log.log(Level.FINE, "Validating field {0}", field.getName());
        Method getMethod = getGetter(field);

        if (getMethod == null) {
            log.log(Level.FINE, "Field {0} has no associated method {1}", new Object[]{field.getName(), getGetter(field)});
            return;
        }

        try {
            Object value = getMethod.invoke(this);
            validateNotNullOrEmpty(field, value);
            validatePossibleValues(field, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Check whether the property is null or empty
     * 
     * A ValidationException is thrown, if the annotated property is null or empty.
     * 
     * @param field
     * @param value
     * @throws ValidationException 
     */
    public void validateNotNullOrEmpty(Field field, Object value) throws ValidationException {
        if (field.isAnnotationPresent(NotNullOrEmpty.class)) {
            log.log(Level.FINE, "Validating NotNullOrEmpty on {0}", field.getName());
    
            if (value == null) {
                throw new ValidationException(field.getName() + " must not be null!");
            }

            if (field.getType().equals(String.class)) {
                String sValue = (String) value;

                if (sValue.isEmpty()) {
                    throw new ValidationException(field.getName() + " must not be empty!");
                }
            }
        }
    }

    /**
     * Check if the property value is one of the given values
     * 
     * A ValidationException is thrown if the value of the annotated property is not
     * in the list of possible values
     * 
     * @param field
     * @param value
     * @throws ValidationException 
     */
    public void validatePossibleValues(Field field, Object value) throws ValidationException {
        PossibleValues possibleValues = field.getAnnotation(PossibleValues.class);
        
        if (possibleValues == null) {
            return;
        }
        
        log.log(Level.FINE, "Validating PossibleValues on {0}", field.getName());
        String[] values = possibleValues.value();
        
        for (String v : values) {
            if (v.equals(value)) {
                return;
            }
        }
        
        throw new ValidationException("The value " + value.toString() + " is not allowed for " + field.getName());
    }

    private Method getGetter(Field field) {
        String name = field.getName();
        String methodName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
        log.log(Level.FINE, "Check for method {0}", methodName);

        try {
            Method getMethod = this.getClass().getDeclaredMethod(methodName);
            return getMethod;
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }
}
