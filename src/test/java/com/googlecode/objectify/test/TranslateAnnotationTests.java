/*
 */

package com.googlecode.objectify.test;

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
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of the @Translate annotation
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TranslateAnnotationTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TranslateAnnotationTests.class.getName());

	/** Random translator that just prepends some junk and does an uppercase conversion on save so we know it was executed */
	public static class FunkyStringTranslatorFactory extends ValueTranslatorFactory<String, String> {
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
	public static class CommaSeparatedStringCollectionTranslatorFactory implements TranslatorFactory<Collection<String>, String> {
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
	public static class HasTranslateLate
	{
		@Id
		public Long id;

		@Translate(FunkyStringTranslatorFactory.class)
		public String string;
	}

	/**
	 */
	@Test
	public void testTranslateLate() throws Exception
	{
		fact().register(HasTranslateLate.class);

		HasTranslateLate ht = new HasTranslateLate();
		ht.string = "bar";

		HasTranslateLate fetched = ofy().saveClearLoad(ht);

		assert fetched.string.equals(ht.string.toUpperCase());
	}

	/** */
	@Entity
	public static class HasTranslateEarly
	{
		@Id
		public Long id;

		@Translate(value=CommaSeparatedStringCollectionTranslatorFactory.class, early=true)
		public List<String> strings;
	}

	/**
	 */
	@Test
	public void testTranslateEarly() throws Exception
	{
		fact().register(HasTranslateEarly.class);

		HasTranslateEarly ht = new HasTranslateEarly();
		ht.strings = Arrays.asList(new String[] { "foo", "bar" });

		HasTranslateEarly fetched = ofy().saveClearLoad(ht);

		assert fetched.strings.get(0).equals(ht.strings.get(0).toUpperCase());
		assert fetched.strings.get(1).equals(ht.strings.get(1).toUpperCase());
	}
}
