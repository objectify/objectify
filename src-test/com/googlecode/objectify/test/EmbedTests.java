package com.googlecode.objectify.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.entity.Someone;
import com.googlecode.objectify.test.entity.Town;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 */
public class EmbedTests extends TestBase
{
	public static class PartiallyIndexedStruct
	{
		@Embed @Index Someone indexedPerson;
		@Embed @Unindex Someone unindexedPerson;

		@Index String indexedString;
		@Unindex String unidexedString;

		public PartiallyIndexedStruct() { }

		PartiallyIndexedStruct(Someone indexedPerson, Someone unindexedPerson, String indexedString, String unidexedString)
		{
			this.indexedPerson = indexedPerson;
			this.unindexedPerson = unindexedPerson;
			this.indexedString = indexedString;
			this.unidexedString = unidexedString;
		}
	}

	@Entity
	@Cache
	public static class PartiallyIndexedEntity
	{
		@Id Long id;

		@Embed @Index PartiallyIndexedStruct indexed;
		@Embed @Unindex PartiallyIndexedStruct unindexed;

		public PartiallyIndexedEntity() { }

		PartiallyIndexedEntity(PartiallyIndexedStruct indexed, PartiallyIndexedStruct unindexed)
		{
			this.indexed = indexed;
			this.unindexed = unindexed;
		}
	}

	public static class Names
	{
		@Embed
		Name[] names;
	}

	public static class Team
	{
		@Embed
		Names members;
	}

	@Entity
	@Cache
	public static class TeamEntity extends Team
	{
		@Id
		Long id;
	}

	public static class League
	{
		@Embed
		Team[] teams;
	}

	@Test
	public void testNullHandling() throws Exception
	{
		fact.register(Town.class);
		TestObjectify ofy = this.fact.begin();

		// null mayor
		Town t1 = new Town();
		t1.mayor = new Someone(null, 30);

		Key<Town> t1Key = ofy.put(t1);

		Town t2 = ofy.get(t1Key);

		assert t2.mayor != null;
		assert t2.mayor.name == null;

		// mayor with null names
		t1 = new Town();
		t1.mayor = new Someone(new Name(null, null), 30);

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
		fact.register(PartiallyIndexedEntity.class);

		TestObjectify ofy = this.fact.begin();

		PartiallyIndexedEntity obj = new PartiallyIndexedEntity(
				new PartiallyIndexedStruct(
						new Someone(new Name("A", "B"), 30),
						new Someone(new Name("C", "D"), 31), "1", "2"),
				new PartiallyIndexedStruct(
						new Someone(new Name("a", "b"), 32),
						new Someone(new Name("c", "d"), 33), "3", "4")
		);

		Key<PartiallyIndexedEntity> key = ofy.put(obj);

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

	private void subtestFoundByQuery(boolean expected, Key<?> key, String filter, Object value)
	{
		Query<PartiallyIndexedEntity> q = fact.begin().load().type(PartiallyIndexedEntity.class);
		q = q.filter(filter + " =", value);
		Iterator<PartiallyIndexedEntity> results = q.iterator();

		if (expected)
		{
			assert results.hasNext();
			PartiallyIndexedEntity result = results.next();
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

		TestObjectify ofy = fact.begin();
		TeamEntity t = new TeamEntity();
		t.members = new Names();
		t.members.names = new Name[]{new Name("Joe", "Smith"), new Name("Jane", "Foo")};
		Key<TeamEntity> k = ofy.put(t);

		System.out.println(ds().get(fact.getRawKey(k)));

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
		private @Embed
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

		TestObjectify ofy = this.fact.begin();
		
		ofy.put(clientlistname);
	}
	
	@Entity
	public static class EntityEmbedsOtherEntity
	{
		@Id Long id;
		@Embed Trivial other;
	}

	@Test
	public void testEntityEmbedsOtherEntity() throws Exception
	{
		this.fact.register(EntityEmbedsOtherEntity.class);
		
		EntityEmbedsOtherEntity embeds = new EntityEmbedsOtherEntity();
		embeds.other = new Trivial(123L, "blah", 7);
		
		EntityEmbedsOtherEntity fetched = this.putClearGet(embeds);
		
		assert embeds.other.getId().equals(fetched.other.getId());
		assert embeds.other.getSomeString().equals(fetched.other.getSomeString());
	}
	
	/**
	 * This is a test case submitted on the objectify google group:
	 * https://groups.google.com/forum/#!topic/objectify-appengine/sITmXfBgOxI
	 * 
I just ran across an anomaly (hesitate to call it a bug yet) re: 
nested @Embedded.  Consider the following: 

@Entity 
class Form 
{ 
  @Embedded 
  Approval[] approvals; 
} 

class Approval 
{ 
  @Embedded 
  AddressBook addressBook; 

  String approvedBy; 
} 

class AddressBook 
{ 
  @Serialized 
  Group[] groups; 

  @Serialized 
  Profile[] profiles; 
} 

class Group 
{ 
  String name; 
} 

class Profile 
{ 
  String name; 
} 

Assume I create a Form with 3 Approval objects in the approvals 
array.  Each Approval has a non-null value for approvedBy. 
Approvals[0] and Approvals[2] have non-null AddressBook value for 
addressBook.  Approvals[1] has a null value for addressBook. 

Now I put the Form. 

The flattened structure in the data store for Form will have the field 
'approvals.addressBook' with a null value in addition to the 
'approvals.addressBook.groups' and 'approvals.addressBook.profiles' 
fields that were expected. 

When I read the Form from the data store I will only get 2 Approval 
objects in the approvals array and the data in those 2 Approval 
objects will be a weird Frankenstein of values from the original 3 
Approval objects I put into the data store. 

If the Form is saved with non-null values in the addressBook field fro 
all 3 Approval objects, no 'approvals.addressBook' field is created in 
the data store and when read, all 3 Approval objects are present with 
no problems.
	 */
	@SuppressWarnings("serial")
	static class Group implements Serializable {
		String name;
		public Group() {}
		public Group(String name) { this.name = name; }
	} 
	@SuppressWarnings("serial")
	static class Profile implements Serializable {
		String name; 
		public Profile() {}
		public Profile(String name) { this.name = name; }
	}
	@Embed
	static class AddressBook {
		@Serialize Group[] groups;
		@Serialize Profile[] profiles;
	} 
	@Embed
	static class Approval {
		AddressBook addressBook;
		String approvedBy;
	}
	@Entity
	static class Form {
		@Id Long id;
		Approval[] approvals;
	}
	
	/** */
	@Test
	public void testNestedEmbeds() {
		fact.register(Form.class);
		
		Approval approval0 = new Approval();
		approval0.approvedBy = "somezero";
		approval0.addressBook = new AddressBook();
		approval0.addressBook.groups = new Group[] { new Group("group0") };
		approval0.addressBook.profiles = new Profile[] { new Profile("profile0") };
		
		Approval approval1 = new Approval();
		approval1.approvedBy = "someone";
		
		Approval approval2 = new Approval();
		approval2.approvedBy = "sometwo";
		approval2.addressBook = new AddressBook();
		approval2.addressBook.groups = new Group[] { new Group("group2") };
		approval2.addressBook.profiles = new Profile[] { new Profile("profile2") };
		
		Form form = new Form();
		form.approvals = new Approval[] { approval0, approval1, approval2 };
		
		Form fetched = this.putClearGet(form);
		
		assert fetched.approvals.length == 3;
		
		assert fetched.approvals[0].approvedBy.equals(approval0.approvedBy);
		assert fetched.approvals[0].addressBook != null;
		assert fetched.approvals[0].addressBook.groups.length == 1;
		assert fetched.approvals[0].addressBook.groups[0].name.equals(approval0.addressBook.groups[0].name);
		assert fetched.approvals[0].addressBook.profiles.length == 1;
		assert fetched.approvals[0].addressBook.profiles[0].name.equals(approval0.addressBook.profiles[0].name);
		
		assert fetched.approvals[1].approvedBy.equals(approval1.approvedBy);
		assert fetched.approvals[1].addressBook == null;
		
		assert fetched.approvals[2].approvedBy.equals(approval2.approvedBy);
		assert fetched.approvals[2].addressBook != null;
		assert fetched.approvals[2].addressBook.groups.length == 1;
		assert fetched.approvals[2].addressBook.groups[0].name.equals(approval2.addressBook.groups[0].name);
		assert fetched.approvals[2].addressBook.profiles.length == 1;
		assert fetched.approvals[2].addressBook.profiles[0].name.equals(approval2.addressBook.profiles[0].name);
	}
}
