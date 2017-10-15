/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.ClassTranslator;
import com.googlecode.objectify.impl.translate.ClassTranslatorFactory;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of the translators.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TranslationTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	private static class SimpleEntityPOJO {
		public @Id Long id;
		public String foo;
	}

	/**
	 */
	@Test
	void simplePojoEntityTranslates() throws Exception {
		CreateContext createCtx = new CreateContext(factory());
		ClassTranslator<SimpleEntityPOJO> translator = ClassTranslatorFactory.createEntityClassTranslator(SimpleEntityPOJO.class, createCtx, Path.root());

		SimpleEntityPOJO pojo = new SimpleEntityPOJO();
		pojo.id = 123L;
		pojo.foo = "bar";

		SaveContext saveCtx = new SaveContext();
		Entity ent = (Entity)translator.save(pojo, false, saveCtx, Path.root());

		assert ent.getKey().getKind().equals(SimpleEntityPOJO.class.getSimpleName());
		assert ent.getKey().getId() == pojo.id;
		assert ent.getProperties().size() == 1;
		assert ent.getProperty("foo").equals("bar");
	}

	private static class Thing {
		public String foo;
	}

	/**
	 */
	@Test
	void simplePOJOTranslates() throws Exception {
		Path thingPath = Path.root().extend("somewhere");

		CreateContext createCtx = new CreateContext(factory());
		ClassTranslator<Thing> translator = ClassTranslatorFactory.createEmbeddedClassTranslator(Thing.class, createCtx, thingPath);

		Thing thing = new Thing();
		thing.foo = "bar";

		SaveContext saveCtx = new SaveContext();
		EmbeddedEntity ent = (EmbeddedEntity)translator.save(thing, false, saveCtx, thingPath);

		assert ent.getKey() == null;
		assert ent.getProperties().size() == 1;
		assert ent.getProperty("foo").equals("bar");
	}
}