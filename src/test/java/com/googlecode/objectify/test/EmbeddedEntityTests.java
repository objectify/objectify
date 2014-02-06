package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.googlecode.objectify.IEmbeddedEntity;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.translate.IEmbeddedEntityTranslatorFactory;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests that bulk gets return results having the same order as the argument
 */
public class EmbeddedEntityTests extends TestBase {


    @BeforeMethod
    public void setUp() {
        super.setUp();


    }

    /**
     * Shows that one can't save House like entities without translator
     */
    @Test(expectedExceptions = SaveException.class)
    public void testWithoutTranslator() {
        fact().register(House.class);

        House h = getHouse();
        ofy().put(h);

    }

    /**
     * Test's loading and saving enitities with IEmbeddedEntity fields,
     * both standard fields and Lists of them.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testLoadSave() {

        fact().getTranslators().add(new IEmbeddedEntityTranslatorFactory());

        fact().register(House.class);

        House h = getHouse();

        ofy().put(h);

        final House house = ofy().load().type(House.class).first().now();

        assert house.name.equals(h.name);
        assert house.room.owner.equals(h.room.owner);
        assert house.rooms.size() == h.rooms.size();
        assert house.equals(h);

    }

    private House getHouse() {
        House h = new House();
        h.name = "Blue House";
        Room r = new Room();
        r.number = 2;
        r.owner = "mr Smith";
        h.room = r;

        Room r2 = new Room();
        r2.number = 4;
        r2.owner = "Samantha";
        h.rooms.add(r2);

        Room r3 = new Room();
        r3.number = 3;
        r3.owner = "JOhn";
        h.rooms.add(r3);

        return h;
    }

    @Entity
    public static class House {
        @Id
        public Long id;

        public String name;

        public List<Room> rooms = new ArrayList<>();

        public Room room;


    }

    public static class Room implements IEmbeddedEntity<Room> {

        public long number;

        public String owner;

        public Room() {
        }

        @Override
        public EmbeddedEntity createEmbeddedEntity() {
            final EmbeddedEntity embeddedEntity = new EmbeddedEntity();
            embeddedEntity.setProperty("number", this.number);
            embeddedEntity.setProperty("owner", this.owner);
            return embeddedEntity;
        }

        @Override
        public Room setFieldsFrom(EmbeddedEntity embeddedEntity) {
            number = (Long) embeddedEntity.getProperty("number");
            owner = (String) embeddedEntity.getProperty("owner");
            return this;
        }
    }

}
