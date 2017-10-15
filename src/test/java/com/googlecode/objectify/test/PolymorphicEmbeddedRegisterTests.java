package com.googlecode.objectify.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.mapper.Mapper;
import com.googlecode.objectify.stringifier.Stringifier;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Check if the polymorphic embedded subclasses cause any exceptions depending on the usage place and the registration order.
 * See #196
 * 
 * @author Tamas Tozser <ttozser@gmail.com>
 */
class PolymorphicEmbeddedRegisterTests extends TestBase {
	
	@Entity
	@Data
	private static class EnityWithField {
		@Id
		long id = 1;
		EmbeddedSuperclass field;
	}
	
	@Entity
	@Data
	private static class EnityWithList {
		@Id
		long id = 1;
		List<EmbeddedSuperclass> fields;
	}
	
	@Entity
	@Data
	private static class EnityWithMap {
		@Id
		long id = 1;
		Map<String, EmbeddedSuperclass> map;
	}
	
	@Entity
	@Data
	private static class EnityWithStringify {
		@Id
		long id = 1;
		@Stringify(SuperEmbeddedClassStringifier.class)
		Map<EmbeddedSuperclass, EmbeddedSuperclass> map;

		private static class SuperEmbeddedClassStringifier implements Stringifier<EmbeddedSuperclass> {
			@Override
			public String toString(EmbeddedSuperclass obj) {
				return obj.id;
			}
			
			@Override
			public EmbeddedSuperclass fromString(String str) {
				return new EmbeddedSuperclass(str);
			}
		}
	}
	
	@Entity
	@Data
	private static class EnityWithMapify {
		@Id
		long id = 1;
		@Mapify(SuperClassMapper.class)
		Map<String, EmbeddedSuperclass> map;
		
		private static class SuperClassMapper implements Mapper<String, EmbeddedSuperclass> {
			@Override
			public String getKey(EmbeddedSuperclass superClass) {
				return superClass.id;
			}
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class EmbeddedSuperclass {
		String id = "foo";
	}
	
	@Subclass
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class EmbeddedSubclass extends EmbeddedSuperclass {
	}
	
	@Subclass
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class EmbeddedSubSubclass extends EmbeddedSubclass {
	}
	
	@Test
	void testPolymorphicField() {
		factory().register(EnityWithField.class);
		factory().register(EmbeddedSubSubclass.class);
		
		checkField();
	}
	
	@Test
	void testPolymorphicFieldReverseRegistration() {
		factory().register(EmbeddedSubSubclass.class);
		factory().register(EnityWithField.class);
		
		checkField();
	}
	
	private void checkField() {
		final EnityWithField enityWithField = new EnityWithField();
		enityWithField.field = new EmbeddedSubSubclass();

		final EnityWithField fetched = saveClearLoad(enityWithField);
		assertThat(fetched).isEqualTo(enityWithField);
	}
	
	@Test
	void testPolymorphicList() {
		factory().register(EnityWithList.class);
		factory().register(EmbeddedSubSubclass.class);
		
		checkList();
	}
	
	@Test
	void testPolymorphicListReverseRegistration() {
		factory().register(EmbeddedSubSubclass.class);
		factory().register(EnityWithList.class);
		
		checkList();
	}
	
	private void checkList() {
		final EnityWithList enityWithlist = new EnityWithList();
		enityWithlist.fields = Lists.newArrayList(new EmbeddedSubSubclass());

		final EnityWithList fetched = saveClearLoad(enityWithlist);
		assertThat(fetched).isEqualTo(enityWithlist);
	}
	
	@Test
	void testPolymorphicMap() {
		factory().register(EnityWithMap.class);
		factory().register(EmbeddedSubSubclass.class);
		
		checkMap();
	}
	
	@Test
	void testPolymorphicMapReverseRegistration() {
		factory().register(EmbeddedSubSubclass.class);
		factory().register(EnityWithMap.class);
		
		checkMap();
	}
	
	private void checkMap() {
		EnityWithMap enityWithMap = new EnityWithMap();
		enityWithMap.map = ImmutableMap.of("foo", new EmbeddedSubSubclass());

		final EnityWithMap fetched = saveClearLoad(enityWithMap);
		assertThat(fetched).isEqualTo(enityWithMap);
	}

	@Test
	void testPolymorphicStringify() {
		factory().register(EnityWithStringify.class);
		factory().register(EmbeddedSubSubclass.class);
		
		checkStringify();
	}
	
	@Test
	void testPolymorphicStringifyReverseRegistration() {
		factory().register(EmbeddedSubSubclass.class);
		factory().register(EnityWithStringify.class);
		
		checkStringify();
	}
	
	private void checkStringify() {
		final EnityWithStringify enityWithStringify = new EnityWithStringify();
		enityWithStringify.map = ImmutableMap.of(new EmbeddedSuperclass(), new EmbeddedSubSubclass());

		final EnityWithStringify fetched = saveClearLoad(enityWithStringify);
		assertThat(fetched).isEqualTo(enityWithStringify);
	}

	@Test
	void testPolymorphicMapify() {
		factory().register(EnityWithMapify.class);
		factory().register(EmbeddedSubSubclass.class);
		
		checkMapify();
	}
	
	@Test
	void testPolymorphicMapifyReverseRegistration() {
		factory().register(EmbeddedSubSubclass.class);
		factory().register(EnityWithMapify.class);
		
		checkMapify();
	}
	
	private void checkMapify() {
		EnityWithMapify enityWithMapify = new EnityWithMapify();
		enityWithMapify.map = ImmutableMap.of("foo", new EmbeddedSubSubclass());

		final EnityWithMapify fetched = saveClearLoad(enityWithMapify);
		assertThat(fetched).isEqualTo(enityWithMapify);
	}
}
