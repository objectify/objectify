/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.ClassTranslator;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of the translators.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class TranslationTests extends TestBase
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
		final CreateContext createCtx = new CreateContext(factory());
		final ClassTranslator<SimpleEntityPOJO> translator = new ClassTranslator<>(SimpleEntityPOJO.class, createCtx, Path.root());

		final SimpleEntityPOJO pojo = new SimpleEntityPOJO();
		pojo.id = 123L;
		pojo.foo = "bar";

		final SaveContext saveCtx = new SaveContext();
		final FullEntity<?> ent = translator.save(pojo, false, saveCtx, Path.root()).get();
		final Key key = (Key)ent.getKey();

		assertThat(key.getKind()).isEqualTo(SimpleEntityPOJO.class.getSimpleName());
		assertThat(key.getId()).isEqualTo(pojo.id);
		assertThat(ent.getNames()).hasSize(1);
		assertThat(ent.getString("foo")).isEqualTo("bar");
	}

	private static class Thing {
		public String foo;
	}

	/**
	 */
	@Test
	void simplePOJOTranslates() throws Exception {
		final Path thingPath = Path.root().extend("somewhere");

		final CreateContext createCtx = new CreateContext(factory());
		final ClassTranslator<Thing> translator = new ClassTranslator<>(Thing.class, createCtx, thingPath);

		final Thing thing = new Thing();
		thing.foo = "bar";

		final SaveContext saveCtx = new SaveContext();
		final FullEntity<?> ent = translator.save(thing, false, saveCtx, thingPath).get();

		assertThat(ent.getKey()).isNull();
		assertThat(ent.getNames()).hasSize(1);
		assertThat(ent.getString("foo")).isEqualTo("bar");
	}
}