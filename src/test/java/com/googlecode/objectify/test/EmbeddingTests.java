package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 */
class EmbeddingTests extends TestBase {

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Data
	private static class HasBigDecimal {
		@Id Long id;
		BigDecimal data;
	}

	/**
	 * This is actually a problem to implement because we might have a polymorphic class wherein the base class
	 * does not have a noarg constructor. Old versions of objectify didn't really test this properly. What actually
	 * happens if you use BigDecimal is that the data is written but load fails because missing no-args.
	 * TODO: come up with a better story here
	 */
	//@Test
	void embeddedClassesMustHaveNoArgConstructors() throws Exception {
		assertThrows(Exception.class, () -> factory().register(HasBigDecimal.class));
	}

	@Data
	@NoArgsConstructor
	private static class PartiallyIndexedStruct {
		@Index Someone indexedPerson;
		@Unindex Someone unindexedPerson;

		@Index String indexedString;
		@Unindex String unidexedString;

		PartiallyIndexedStruct(Someone indexedPerson, Someone unindexedPerson, String indexedString, String unidexedString) {
			this.indexedPerson = indexedPerson;
			this.unindexedPerson = unindexedPerson;
			this.indexedString = indexedString;
			this.unidexedString = unidexedString;
		}
	}

	@Entity
	@Cache
	@Data
	@NoArgsConstructor
	private static class PartiallyIndexedEntity {
		@Id Long id;

		@Index PartiallyIndexedStruct indexed;
		@Unindex PartiallyIndexedStruct unindexed;

		PartiallyIndexedEntity(PartiallyIndexedStruct indexed, PartiallyIndexedStruct unindexed) {
			this.indexed = indexed;
			this.unindexed = unindexed;
		}
	}
	
	@Subclass
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class PartiallyIndexedStructSubclass extends PartiallyIndexedStruct {

		public PartiallyIndexedStructSubclass(Someone indexedPerson, Someone unindexedPerson, String indexedString, String unidexedString) {
			super(indexedPerson, unindexedPerson, indexedString, unidexedString);
		}
		
	}

	@Data
	private static class Names {
		Name[] names;
	}

	@Data
	private static class Team {
		Names members;
	}

	@Entity
	@Cache
	@Data
	@EqualsAndHashCode(callSuper = true)
	private static class TeamEntity extends Team {
		@Id
		Long id;
	}

	@Data
	private static class League {
		Team[] teams;
	}

	@Test
	void testNullHandling() throws Exception {
		factory().register(Town.class);

		// null mayor
		Town t1 = new Town();
		t1.mayor = new Someone(null, 30);

		Key<Town> t1Key = ofy().save().entity(t1).now();

		Town t2 = ofy().load().key(t1Key).now();

		assertThat(t2.mayor).isNotNull();
		assertThat(t2.mayor.name).isNull();

		// mayor with null names
		t1 = new Town();
		t1.mayor = new Someone(new Name(null, null), 30);

		t1Key = ofy().save().entity(t1).now();

		t2 = ofy().load().key(t1Key).now();

		assertThat(t2.mayor).isNotNull();

		assertThat(t2.mayor.name).isNotNull();
		assertThat(t2.mayor.name.getFirstName()).isNull();
		assertThat(t2.mayor.name.getLastName()).isNull();
		assertThat(t2.mayor.age).isEqualTo(30);
	}

	@Test
	void testUnindexed() throws Exception {
		factory().register(PartiallyIndexedEntity.class);

		final PartiallyIndexedEntity obj = new PartiallyIndexedEntity(
				new PartiallyIndexedStruct(
						new Someone(new Name("A", "B"), 30),
						new Someone(new Name("C", "D"), 31), "1", "2"),
				new PartiallyIndexedStruct(
						new Someone(new Name("a", "b"), 32),
						new Someone(new Name("c", "d"), 33), "3", "4")
		);

		checkUnindexed(obj);
	}

	@Test
	void testUnindexedPolymorphic() throws Exception {
		factory().register(PartiallyIndexedEntity.class);
		factory().register(PartiallyIndexedStructSubclass.class);
		
		final PartiallyIndexedEntity obj = new PartiallyIndexedEntity(
				new PartiallyIndexedStructSubclass(
						new Someone(new Name("A", "B"), 30),
						new Someone(new Name("C", "D"), 31), "1", "2"),
				new PartiallyIndexedStructSubclass(
						new Someone(new Name("a", "b"), 32),
						new Someone(new Name("c", "d"), 33), "3", "4")
		);
		
		checkUnindexed(obj);
	}

	private void checkUnindexed(final PartiallyIndexedEntity obj) {
		final Key<PartiallyIndexedEntity> key = ofy().save().entity(obj).now();

		subtestFoundByQuery(true, key, "indexed.indexedPerson.name.firstName", "A");
		subtestFoundByQuery(true, key, "indexed.indexedPerson.name.lastName", "B");
		subtestFoundByQuery(true, key, "indexed.indexedPerson.age", 30);

		subtestFoundByQuery(false, key, "indexed.unindexedPerson.name.firstName", "C");
		subtestFoundByQuery(false, key, "indexed.unindexedPerson.name.lastName", "D");
		subtestFoundByQuery(false, key, "indexed.unindexedPerson.age", 31);

		subtestFoundByQuery(true, key, "indexed.indexedString", "1");
		subtestFoundByQuery(false, key, "indexed.unindexedString", "2");

		subtestFoundByQuery(true, key, "unindexed.indexedPerson.name.firstName", "a");
		subtestFoundByQuery(true, key, "unindexed.indexedPerson.name.lastName", "b");
		subtestFoundByQuery(true, key, "unindexed.indexedPerson.age", 32);

		subtestFoundByQuery(false, key, "unindexed.unindexedPerson.name.firstName", "c");
		subtestFoundByQuery(false, key, "unindexed.unindexedPerson.name.lastName", "d");
		subtestFoundByQuery(false, key, "unindexed.unindexedPerson.age", 33);

		subtestFoundByQuery(true, key, "unindexed.indexedString", "3");
		subtestFoundByQuery(false, key, "unindexed.unindexedString", "4");
	}

	private void subtestFoundByQuery(final boolean expected, final Key<?> key, final String filter, final Object value) {
		final Query<PartiallyIndexedEntity> q = ofy().load()
				.type(PartiallyIndexedEntity.class)
				.filter(filter + " =", value);

		final Iterator<PartiallyIndexedEntity> results = q.iterator();

		if (expected) {
			assertThat(results.hasNext()).isTrue();
			final PartiallyIndexedEntity result = results.next();
			assertThat(result.id).isEqualTo(key.getId());
			assertThat(results.hasNext()).isFalse();
		} else {
			assertThat(results.hasNext()).isFalse();
		}
	}

	@Test
	void testDeepEmbeddedArrays() throws Exception {
		factory().register(TeamEntity.class);

		final TeamEntity t = new TeamEntity();
		t.members = new Names();
		t.members.names = new Name[]{new Name("Joe", "Smith"), new Name("Jane", "Foo")};

		final TeamEntity fetched = saveClearLoad(t);

		assertThat(fetched).isNotNull();
		assertThat(fetched.members).isNotNull();
		assertThat(fetched.members.names).isNotNull();
		assertThat(fetched.members.names).hasLength(2);
		assertThat(fetched.members.names[0].getFirstName()).isEqualTo("Joe");
		assertThat(fetched.members.names[0].getLastName()).isEqualTo("Smith");
		assertThat(fetched.members.names[1].getFirstName()).isEqualTo("Jane");
		assertThat(fetched.members.names[1].getLastName()).isEqualTo("Foo");
	}

	@Data
	@SuppressWarnings({"serial", "unused"})
	private static class KensMailingListEntry implements Serializable {
		private Key<Trivial> clientKey;
		private String emailAddr;
		private Integer mailOffset;
	}

	@SuppressWarnings({"serial", "unused"})
	@Entity
	@Data
	private static class KensClientListName implements Serializable {
		@Id
		private Long id;
		private Key<Trivial> orgKey;
		private String listName;
		private List<KensMailingListEntry> listMembers = new ArrayList<>();
	}

	@Test
	void kensTest() throws Exception {
		factory().register(KensClientListName.class);

		final List<KensMailingListEntry> listMembers = new ArrayList<>();
		final KensMailingListEntry mle = new KensMailingListEntry();
		listMembers.add(mle);

		final KensClientListName clientlistname = new KensClientListName();
		clientlistname.listMembers = listMembers;

		ofy().save().entity(clientlistname).now();
	}

	@Entity
	private static class EntityEmbedsOtherEntity {
		@Id Long id;
		Trivial other;
	}

	@Test
	void testEntityEmbedsOtherEntity() throws Exception {
		factory().register(EntityEmbedsOtherEntity.class);

		final EntityEmbedsOtherEntity embeds = new EntityEmbedsOtherEntity();
		embeds.other = new Trivial(123L, "blah", 7);

		final EntityEmbedsOtherEntity fetched = saveClearLoad(embeds);

		assertThat(embeds.other.getId()).isEqualTo(fetched.other.getId());
		assertThat(embeds.other.getSomeString()).isEqualTo(fetched.other.getSomeString());
	}

	/**
	 */
	@Entity
	@Cache
	@Data
	private static class Town {
		@Id
		Long id;

		String name;

		Someone mayor;

		Someone[] folk;
	}

	/**
	 */
	@Cache
	@Data
	@NoArgsConstructor
	private static class Someone {
		Name name;
		int age;

		Someone(Name name, int age) {
			this.name = name;
			this.age = age;
		}
	}
}
