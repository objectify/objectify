package com.googlecode.objectify.test;

import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.test.entity.EntityWithValidation;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;
import java.util.logging.Logger;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.testng.Assert.*;

/**
 * Tests of entity validation
 *
 * @author Hendrik Pilz <hepisec@gmail.com>
 */
public class ValidationTest extends TestBase {

    /**
     *      
     */
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(ValidationTest.class.getName());

    @Test(expectedExceptions = SaveException.class)
    public void notNullValidation() {
        fact().register(EntityWithValidation.class);
        EntityWithValidation ent = new EntityWithValidation();
        ent.setNotNull(null);
        ofy().save().entity(ent).now();
    }

    @Test(expectedExceptions = SaveException.class)
    public void notEmptyValidation() {
        fact().register(EntityWithValidation.class);
        EntityWithValidation ent = new EntityWithValidation();
        ent.setNotNull("");
        ofy().save().entity(ent).now();
    }    
    
    @Test
    public void notNullOrEmptyValidationWithValue() {
        fact().register(EntityWithValidation.class);
        assertEquals(0, ofy().load().type(EntityWithValidation.class).count());
        EntityWithValidation ent = new EntityWithValidation();
        ent.setNotNull("Name");
        ent.setNumber("one");
        ofy().save().entity(ent).now();
        assertEquals(1, ofy().load().type(EntityWithValidation.class).count());
    }    
    
    @Test(expectedExceptions = SaveException.class)
    public void possibleValueValidation() {
        fact().register(EntityWithValidation.class);
        EntityWithValidation ent = new EntityWithValidation();
        ent.setNotNull("Name");
        ent.setNumber("4");
        ofy().save().entity(ent).now();
    }
    
    @Test
    public void possibleValueValidationWithCorrectValue() {
        fact().register(EntityWithValidation.class);
        assertEquals(0, ofy().load().type(EntityWithValidation.class).count());
        EntityWithValidation ent = new EntityWithValidation();
        ent.setNotNull("Name");
        ent.setNumber("one");
        ofy().save().entity(ent).now();
        assertEquals(1, ofy().load().type(EntityWithValidation.class).count());
    }    
}
