package com.googlecode.objectify.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.entity.Person;
import com.googlecode.objectify.test.entity.Town;
import com.googlecode.objectify.test.entity.Trivial;

/**
 */
public class EmbeddedTests extends TestBase
{
	public static class PartiallyUnindexedStruct
	{
		@Embedded
		Person indexedPerson;
		@Embedded
		@Unindexed
		Person unindexedPerson;

		String indexedString;
		@Unindexed
		String unidexedString;

		public PartiallyUnindexedStruct()
		{
		}

		PartiallyUnindexedStruct(Person indexedPerson, Person unindexedPerson, String indexedString, String unidexedString)
		{
			this.indexedPerson = indexedPerson;
			this.unindexedPerson = unindexedPerson;
			this.indexedString = indexedString;
			this.unidexedString = unidexedString;
		}
	}

	@Cached
	public static class PartiallyUnindexedEntity
	{
		@Id
		Long id;

		@Embedded
		PartiallyUnindexedStruct indexed;

		@Embedded
		@Unindexed
		PartiallyUnindexedStruct unindexed;

		public PartiallyUnindexedEntity()
		{
		}

		PartiallyUnindexedEntity(PartiallyUnindexedStruct indexed, PartiallyUnindexedStruct unindexed)
		{
			this.indexed = indexed;
			this.unindexed = unindexed;
		}
	}

	public static class Names
	{
		@Embedded
		Name[] names;
	}

	public static class Team
	{
		@Embedded
		Names members;
	}

	@Cached
	public static class TeamEntity extends Team
	{
		@Id
		Long id;
	}

	public static class League
	{
		@Embedded
		Team[] teams;
	}

	@Test
	public void testNullHandling() throws Exception
	{
		Objectify ofy = this.fact.begin();

		// null mayor
		Town t1 = new Town();
		t1.mayor = new Person(null, 30);

		Key<Town> t1Key = ofy.put(t1);

		Town t2 = ofy.get(t1Key);

		assert t2.mayor != null;
		assert t2.mayor.name == null;

		// mayor with null names
		t1 = new Town();
		t1.mayor = new Person(new Name(null, null), 30);

		t1Key = ofy.put(t1);

		t2 = ofy.get(t1Key);

		assert t2.mayor != null;

		assert t2.mayor.name != null;
		assert t2.mayor.name.firstName == null;
		assert t2.mayor.name.lastName == null;
		assert t2.mayor.age == 30;
	}


	@Test
	public void testUnindexed() throws Exception
	{
		fact.register(PartiallyUnindexedEntity.class);

		Objectify ofy = this.fact.begin();

		PartiallyUnindexedEntity obj = new PartiallyUnindexedEntity(
				new PartiallyUnindexedStruct(
						new Person(new Name("A", "B"), 30),
						new Person(new Name("C", "D"), 31), "1", "2"),
				new PartiallyUnindexedStruct(
						new Person(new Name("a", "b"), 32),
						new Person(new Name("c", "d"), 33), "3", "4")
		);

		Key<PartiallyUnindexedEntity> key = ofy.put(obj);

		subtestFoundByQuery(true, key, "indexed.indexedPerson.name.firstName", "A");
		subtestFoundByQuery(true, key, "indexed.indexedPerson.name.lastName", "B");
		subtestFoundByQuery(true, key, "indexed.indexedPerson.age", 30);

		subtestFoundByQuery(false, key, "indexed.unindexedPerson.name.firstName", "C");
		subtestFoundByQuery(false, key, "indexed.unindexedPerson.name.lastName", "D");
		subtestFoundByQuery(false, key, "indexed.unindexedPerson.age", 31);

		subtestFoundByQuery(true, key, "indexed.indexedString", "1");
		subtestFoundByQuery(false, key, "indexed.unindexedString", "2");

		subtestFoundByQuery(false, key, "unindexed.indexedPerson.name.firstName", "a");
		subtestFoundByQuery(false, key, "unindexed.indexedPerson.name.lastName", "b");
		subtestFoundByQuery(false, key, "unindexed.indexedPerson.age", 32);

		subtestFoundByQuery(false, key, "unindexed.unindexedPerson.name.firstName", "c");
		subtestFoundByQuery(false, key, "unindexed.unindexedPerson.name.lastName", "d");
		subtestFoundByQuery(false, key, "unindexed.unindexedPerson.age", 33);

		subtestFoundByQuery(false, key, "unindexed.indexedString", "3");
		subtestFoundByQuery(false, key, "unindexed.unindexedString", "4");
	}

	private void subtestFoundByQuery(boolean expected, Key<?> key, String filter, Object value)
	{
		Query<PartiallyUnindexedEntity> q = fact.begin().query(PartiallyUnindexedEntity.class);
		q.filter(filter + " =", value);
		Iterator<PartiallyUnindexedEntity> results = q.iterator();

		if (expected)
		{
			assert results.hasNext();
			PartiallyUnindexedEntity result = results.next();
			assert result.id.equals(key.getId());
			assert !results.hasNext();
		}
		else
		{
			assert !results.hasNext();
		}
	}

	@Test
	public void testDeepEmbeddedArrays() throws Exception
	{
		fact.register(TeamEntity.class);

		Objectify ofy = fact.begin();
		TeamEntity t = new TeamEntity();
		t.members = new Names();
		t.members.names = new Name[]{new Name("Joe", "Smith"), new Name("Jane", "Foo")};
		Key<TeamEntity> k = ofy.put(t);

		System.out.println(ofy.getDatastore().get(fact.getRawKey(k)));

		t = ofy.get(k);
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
	public static class KensMailingListEntry implements Serializable
	{
		private Key<Trivial> clientKey;
		private String emailAddr;
		private Integer mailOffset;
		public KensMailingListEntry() {}
	}

	@SuppressWarnings({"serial", "unused"})
	@Entity
	public static class KensClientListName implements Serializable
	{
		@Id
		private Long id;
		private Key<Trivial> orgKey;
		private String listName;
		private @Embedded
		List<KensMailingListEntry> listMembers = new ArrayList<KensMailingListEntry>();
		public KensClientListName() {}
	}
	
	@Test
	public void kensTest() throws Exception
	{
		this.fact.register(KensClientListName.class);

		List<KensMailingListEntry> listMembers = new ArrayList<KensMailingListEntry>();              
		KensMailingListEntry mle = new KensMailingListEntry();
		listMembers.add(mle);
		               
		KensClientListName clientlistname = new KensClientListName();
		clientlistname.listMembers = listMembers;

		Objectify ofy = this.fact.begin();
		
		ofy.put(clientlistname);
	}
	
	public static class EntityEmbedsOtherEntity
	{
		@Id Long id;
		@Embedded Trivial other;
	}

	@Test
	public void testEntityEmbedsOtherEntity() throws Exception
	{
		this.fact.register(EntityEmbedsOtherEntity.class);
		
		EntityEmbedsOtherEntity embeds = new EntityEmbedsOtherEntity();
		embeds.other = new Trivial(123L, "blah", 7);
		
		EntityEmbedsOtherEntity fetched = this.putAndGet(embeds);
		
		assert embeds.other.getId().equals(fetched.other.getId());
		assert embeds.other.getSomeString().equals(fetched.other.getSomeString());
	}
}
