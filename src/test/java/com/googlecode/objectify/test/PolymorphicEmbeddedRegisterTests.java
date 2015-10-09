package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;
import org.testng.collections.Lists;

import com.google.common.collect.ImmutableMap;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.mapper.Mapper;
import com.googlecode.objectify.stringifier.Stringifier;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Check if the polymorphic embedded subclasses cause any exceptions depending on the usage place and the registration order.
 * See #196
 *
 * @author Tamas Tozser <ttozser@gmail.com>
 */
public class PolymorphicEmbeddedRegisterTests extends TestBase {

	@Entity
	static class EnityWithField {
		@Id
		long id = 1;
		EmbeddedSuperclass field;
	}

	@Entity
	static class EnityWithList {
		@Id
		long id = 1;
		List<EmbeddedSuperclass> fields;
	}

	@Entity
	static class EnityWithMap {
		@Id
		long id = 1;
		Map<String, EmbeddedSuperclass> map;
	}

	@Entity
	static class EnityWithStringify {
		@Id
		long id = 1;
		@Stringify(SuperEmbeddedClassStringifier.class)
		Map<EmbeddedSuperclass, EmbeddedSuperclass> map;

		static class SuperEmbeddedClassStringifier implements Stringifier<EmbeddedSuperclass> {
			@Override
			public String toString(EmbeddedSuperclass obj) {
				return obj.id;
			}

			@Override
			public EmbeddedSuperclass fromString(String str) {
				return new EmbeddedSuperclass();
			}
		}
	}

	@Entity
	static class EnityWithMapify {
		@Id
		long id = 1;
		@Mapify(SuperClassMapper.class)
		Map<String, EmbeddedSuperclass> map;

		static class SuperClassMapper implements Mapper<String, EmbeddedSuperclass> {
			@Override
			public String getKey(EmbeddedSuperclass superClass) {
				return superClass.id;
			}
		}
	}

	static class EmbeddedSuperclass {
		String id = "foo";
	}

	@Subclass
	static class EmbeddedSubclass extends EmbeddedSuperclass {
	}

	@Subclass
	static class EmbeddedSubSubclass extends EmbeddedSubclass {
	}

	@Test
	public void testPolymorphicField() {
		fact().register(EnityWithField.class);
		fact().register(EmbeddedSubSubclass.class);

		checkField();
	}

	@Test
	public void testPolymorphicFieldReverseRegistration() {
		fact().register(EmbeddedSubSubclass.class);
		fact().register(EnityWithField.class);

		checkField();
	}

	private void checkField() {
		EnityWithField enityWithField = new EnityWithField();
		enityWithField.field = new EmbeddedSubSubclass();

		ofy().saveClearLoad(enityWithField);
	}

	@Test
	public void testPolymorphicList() {
		fact().register(EnityWithList.class);
		fact().register(EmbeddedSubSubclass.class);

		checkList();
	}

	@Test
	public void testPolymorphicListReverseRegistration() {
		fact().register(EmbeddedSubSubclass.class);
		fact().register(EnityWithList.class);

		checkList();
	}

	private void checkList() {
		EnityWithList enityWithlist = new EnityWithList();
		enityWithlist.fields = Lists.<EmbeddedSuperclass> newArrayList(new EmbeddedSubSubclass());

		ofy().saveClearLoad(enityWithlist);
	}

	@Test
	public void testPolymorphicMap() {
		fact().register(EnityWithMap.class);
		fact().register(EmbeddedSubSubclass.class);

		checkMap();
	}

	@Test
	public void testPolymorphicMapReverseRegistration() {
		fact().register(EmbeddedSubSubclass.class);
		fact().register(EnityWithMap.class);

		checkMap();
	}

	private void checkMap() {
		EnityWithMap enityWithMap = new EnityWithMap();
		enityWithMap.map = ImmutableMap.<String, EmbeddedSuperclass>of("foo", new EmbeddedSubSubclass());

		ofy().saveClearLoad(enityWithMap);
	}

	@Test
	public void testPolymorphicStringify() {
		fact().register(EnityWithStringify.class);
		fact().register(EmbeddedSubSubclass.class);

		checkStringify();
	}

	@Test
	public void testPolymorphicStringifyReverseRegistration() {
		fact().register(EmbeddedSubSubclass.class);
		fact().register(EnityWithStringify.class);

		checkStringify();
	}

	private void checkStringify() {
		EnityWithStringify enityWithStringify = new EnityWithStringify();
		enityWithStringify.map = ImmutableMap.<EmbeddedSuperclass, EmbeddedSuperclass>of(new EmbeddedSubSubclass(), new EmbeddedSubSubclass());

		ofy().saveClearLoad(enityWithStringify);
	}

	@Test
	public void testPolymorphicMapify() {
		fact().register(EnityWithMapify.class);
		fact().register(EmbeddedSubSubclass.class);

		checkMapify();
	}

	@Test
	public void testPolymorphicMapifyReverseRegistration() {
		fact().register(EmbeddedSubSubclass.class);
		fact().register(EnityWithMapify.class);

		checkMapify();
	}

	private void checkMapify() {
		EnityWithMapify enityWithMapify = new EnityWithMapify();
		enityWithMapify.map = ImmutableMap.<String, EmbeddedSuperclass>of("foo", new EmbeddedSubSubclass());

		ofy().saveClearLoad(enityWithMapify);
	}

}
