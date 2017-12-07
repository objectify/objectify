/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Translate;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.impl.translate.TranslatorFactory;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
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
	private static class FunkyStringTranslatorFactory extends SimpleTranslatorFactory<String, String> {
		public FunkyStringTranslatorFactory() {
			super(String.class, ValueType.STRING);
		}

		@Override
		protected String toPojo(final Value<String> value) {
			return value.get().substring("FOO".length());
		}

		@Override
		protected Value<String> toDatastore(final String value) {
			return StringValue.of("FOO" + value.toUpperCase());
		}
	}

	/** Translates String collections to comma separated lists of strings, not really intended to be used (no escaping) */
	private static class CommaSeparatedStringCollectionTranslatorFactory implements TranslatorFactory<Collection<String>, String> {
		@Override
		public Translator<Collection<String>, String> create(final TypeKey<Collection<String>> tk, final CreateContext ctx, final Path path) {
			if (!tk.isAssignableTo(Collection.class))
				return null;

			if (GenericTypeReflector.getTypeParameter(tk.getType(), Collection.class.getTypeParameters()[0]) != String.class)
				return null;

			return new ValueTranslator<Collection<String>, String>(ValueType.STRING) {
				@Override
				protected Collection<String> loadValue(final Value<String> value, final LoadContext ctx, final Path path) throws SkipException {
					final String[] split = value.get().split(",");
					return Arrays.asList(split);
				}

				@Override
				protected Value<String> saveValue(final Collection<String> value, final SaveContext ctx, final Path path) throws SkipException {
					final StringBuilder bld = new StringBuilder();
					boolean afterFirst = false;

					for (String str: value) {
						if (afterFirst)
							bld.append(',');
						else
							afterFirst = true;

						bld.append(str.toUpperCase());
					}

					return StringValue.of(bld.toString());
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

		final com.google.cloud.datastore.Entity entity = datastore().get(Key.create(ht).getRaw());
		assertThat(entity.getString("string")).isEqualTo("FOOBAR");
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

		final com.google.cloud.datastore.Entity entity = datastore().get(Key.create(ht).getRaw());
		assertThat(entity.getString("strings")).isEqualTo("FOO,BAR");
	}
}