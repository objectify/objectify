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
import com.googlecode.objectify.test.entity.Someone;
import com.googlecode.objectify.test.entity.Town;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 */
public class EmbeddingTests extends TestBase
{
	public static class PartiallyIndexedStruct {
		@Index Someone indexedPerson;
		@Unindex Someone unindexedPerson;

		@Index String indexedString;
		@Unindex String unidexedString;

		public PartiallyIndexedStruct() { }

		PartiallyIndexedStruct(Someone indexedPerson, Someone unindexedPerson, String indexedString, String unidexedString) {
			this.indexedPerson = indexedPerson;
			this.unindexedPerson = unindexedPerson;
			this.indexedString = indexedString;
			this.unidexedString = unidexedString;
		}
	}

	@Entity
	@Cache
	public static class PartiallyIndexedEntity {
		@Id Long id;

		@Index PartiallyIndexedStruct indexed;
		@Unindex PartiallyIndexedStruct unindexed;

		public PartiallyIndexedEntity() { }

		PartiallyIndexedEntity(PartiallyIndexedStruct indexed, PartiallyIndexedStruct unindexed) {
			this.indexed = indexed;
			this.unindexed = unindexed;
		}
	}

	@Subclass
	public static class PartiallyIndexedStructSubclass extends PartiallyIndexedStruct {

		public PartiallyIndexedStructSubclass() { }

		public PartiallyIndexedStructSubclass(Someone indexedPerson, Someone unindexedPerson, String indexedString, String unidexedString) {
			super(indexedPerson, unindexedPerson, indexedString, unidexedString);
		}
	}

	public static class Names {
		Name[] names;
	}

	public static class Team {
		Names members;
	}

	@Entity
	@Cache
	public static class TeamEntity extends Team {
		@Id
		Long id;
	}

	public static class League {
		Team[] teams;
	}

	@Test
	public void testNullHandling() throws Exception {
		fact().register(Town.class);

		// null mayor
		Town t1 = new Town();
		t1.mayor = new Someone(null, 30);

		Key<Town> t1Key = ofy().save().entity(t1).now();

		Town t2 = ofy().load().key(t1Key).now();

		assert t2.mayor != null;
		assert t2.mayor.name == null;

		// mayor with null names
		t1 = new Town();
		t1.mayor = new Someone(new Name(null, null), 30);

		t1Key = ofy().save().entity(t1).now();

		t2 = ofy().load().key(t1Key).now();

		assert t2.mayor != null;

		assert t2.mayor.name != null;
		assert t2.mayor.name.firstName == null;
		assert t2.mayor.name.lastName == null;
		assert t2.mayor.age == 30;
	}

	@Test
	public void testUnindexed() throws Exception {
		fact().register(PartiallyIndexedEntity.class);

		PartiallyIndexedEntity obj = new PartiallyIndexedEntity(
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
	public void testUnindexedPolymorphic() throws Exception {
		fact().register(PartiallyIndexedEntity.class);
		fact().register(PartiallyIndexedStructSubclass.class);

		PartiallyIndexedEntity obj = new PartiallyIndexedEntity(
				new PartiallyIndexedStructSubclass(
						new Someone(new Name("A", "B"), 30),
						new Someone(new Name("C", "D"), 31), "1", "2"),
				new PartiallyIndexedStructSubclass(
						new Someone(new Name("a", "b"), 32),
						new Someone(new Name("c", "d"), 33), "3", "4")
		);

		checkUnindexed(obj);
	}

	private void checkUnindexed(PartiallyIndexedEntity obj) {
		Key<PartiallyIndexedEntity> key = ofy().save().entity(obj).now();

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

	private void subtestFoundByQuery(boolean expected, Key<?> key, String filter, Object value) {
		Query<PartiallyIndexedEntity> q = ofy().load().type(PartiallyIndexedEntity.class);
		q = q.filter(filter + " =", value);
		Iterator<PartiallyIndexedEntity> results = q.iterator();

		if (expected) {
			assert results.hasNext();
			PartiallyIndexedEntity result = results.next();
			assert result.id.equals(key.getId());
			assert !results.hasNext();
		} else {
			assert !results.hasNext();
		}
	}

	@Test
	public void testDeepEmbeddedArrays() throws Exception {
		fact().register(TeamEntity.class);

		TeamEntity t = new TeamEntity();
		t.members = new Names();
		t.members.names = new Name[]{new Name("Joe", "Smith"), new Name("Jane", "Foo")};
		Key<TeamEntity> k = ofy().save().entity(t).now();

		System.out.println(ds().get(k.getRaw()));

		t = ofy().load().key(k).now();
		assert t != null;
		assert t.members != null;
		assert t.members.names != null;
		assert t.members.names.length == 2;
		assert t.members.names[0].firstName.equals("Joe");
		assert t.members.names[0].lastName.equals("Smith");
		assert t.members.names[1].firstName.equals("Jane");
		assert t.members.names[1].lastName.equals("Foo");
	}

	@SuppressWarnings({"serial", "unused"})
	public static class KensMailingListEntry implements Serializable {
		private Key<Trivial> clientKey;
		private String emailAddr;
		private Integer mailOffset;
		public KensMailingListEntry() {}
	}

	@SuppressWarnings({"serial", "unused"})
	@Entity
	public static class KensClientListName implements Serializable {
		@Id
		private Long id;
		private Key<Trivial> orgKey;
		private String listName;
		private List<KensMailingListEntry> listMembers = new ArrayList<>();
		public KensClientListName() {}
	}

	@Test
	public void kensTest() throws Exception {
		fact().register(KensClientListName.class);

		List<KensMailingListEntry> listMembers = new ArrayList<>();
		KensMailingListEntry mle = new KensMailingListEntry();
		listMembers.add(mle);

		KensClientListName clientlistname = new KensClientListName();
		clientlistname.listMembers = listMembers;

		ofy().save().entity(clientlistname).now();
	}

	@Entity
	public static class EntityEmbedsOtherEntity {
		@Id Long id;
		Trivial other;
	}

	@Test
	public void testEntityEmbedsOtherEntity() throws Exception {
		fact().register(EntityEmbedsOtherEntity.class);

		EntityEmbedsOtherEntity embeds = new EntityEmbedsOtherEntity();
		embeds.other = new Trivial(123L, "blah", 7);

		EntityEmbedsOtherEntity fetched = ofy().saveClearLoad(embeds);

		assert embeds.other.getId().equals(fetched.other.getId());
		assert embeds.other.getSomeString().equals(fetched.other.getSomeString());
	}

}
