/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Translate;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.impl.translate.TranslatorFactory;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of the @Translate annotation
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class TranslateAnnotationTests extends TestBase {

	/** Random translator that just prepends some junk and does an uppercase conversion on save so we know it was executed */
	private static class FunkyStringTranslatorFactory extends ValueTranslatorFactory<String, String> {
		public FunkyStringTranslatorFactory() {
			super(String.class);
		}

		@Override
		protected ValueTranslator<String, String> createValueTranslator(TypeKey tk, CreateContext ctx, Path path) {
			return new ValueTranslator<String, String>(String.class) {
				@Override
				protected String loadValue(String value, LoadContext ctx, Path path) throws SkipException {
					return value.substring("FOO".length());
				}

				@Override
				protected String saveValue(String value, boolean index, SaveContext ctx, Path path) throws SkipException {
					return "FOO" + value.toUpperCase();
				}
			};
		}
	}

	/** Translates String collections to comma separated lists of strings, not really intended to be used (no escaping) */
	private static class CommaSeparatedStringCollectionTranslatorFactory implements TranslatorFactory<Collection<String>, String> {
		@Override
		public Translator<Collection<String>, String> create(TypeKey<Collection<String>> tk, CreateContext ctx, Path path) {
			if (!tk.isAssignableTo(Collection.class))
				return null;

			if (GenericTypeReflector.getTypeParameter(tk.getType(), Collection.class.getTypeParameters()[0]) != String.class)
				return null;

			return new ValueTranslator<Collection<String>, String>(String.class) {
				@Override
				protected Collection<String> loadValue(String value, LoadContext ctx, Path path) throws SkipException {
					String[] split = value.split(",");
					return Arrays.asList(split);
				}

				@Override
				protected String saveValue(Collection<String> value, boolean index, SaveContext ctx, Path path) throws SkipException {
					StringBuilder bld = new StringBuilder();
					boolean afterFirst = false;

					for (String str: value) {
						if (afterFirst)
							bld.append(',');
						else
							afterFirst = true;

						bld.append(str.toUpperCase());
					}

					return bld.toString();
				}
			};
		}
	}

	/** */
	@Entity
	@Data
	private static class HasTranslateLate {
		@Id
		Long id;

		@Translate(FunkyStringTranslatorFactory.class)
		String string;
	}

	/**
	 */
	@Test
	void basicTranslationWorks() throws Exception {
		factory().register(HasTranslateLate.class);

		final HasTranslateLate ht = new HasTranslateLate();
		ht.string = "bar";

		final HasTranslateLate fetched = saveClearLoad(ht);

		assertThat(fetched.getString()).isEqualTo("BAR");

		final com.google.appengine.api.datastore.Entity entity = ds().get(Key.create(ht).getRaw());
		assertThat(entity.getProperty("string")).isEqualTo("FOOBAR");
	}

	/** */
	@Entity
	@Data
	private static class HasTranslateEarly {
		@Id
		Long id;

		@Translate(value=CommaSeparatedStringCollectionTranslatorFactory.class, early=true)
		List<String> strings;
	}

	/**
	 */
	@Test
	void earlyTranslationHappensBeforeCollections() throws Exception {
		factory().register(HasTranslateEarly.class);

		final HasTranslateEarly ht = new HasTranslateEarly();
		ht.strings = Arrays.asList("foo", "bar");

		final HasTranslateEarly fetched = saveClearLoad(ht);

		assertThat(fetched.strings).isEqualTo(Arrays.asList("FOO", "BAR"));

		final com.google.appengine.api.datastore.Entity entity = ds().get(Key.create(ht).getRaw());
		assertThat(entity.getProperty("strings")).isEqualTo("FOO,BAR");
	}
}