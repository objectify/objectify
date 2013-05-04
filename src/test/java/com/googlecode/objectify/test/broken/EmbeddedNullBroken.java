/**
 *
 */
package com.googlecode.objectify.test.broken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * These are fairly pathological cases WRT nulls in embedded collections.  They're really hard to solve,
 * possibly impossible.  @Embed collections have a lot of limitations; really the whole approach is hackish.
 * It will eventually be deprecated in favor of a solution based on EmbeddedEntity, so these bugs probably
 * won't get fixed.
 */
public class EmbeddedNullBroken extends TestBase {

	/**
	 * @author Brian Chapman
	 */
	@Test
	public void testFooBar() {
		fact().register(FooBar.class);

		FooBar fooBar = createFooBar();
		Result<Key<FooBar>> result = ofy().save().entity(fooBar);
		result.now();

		FooBar retreived = ofy().load().type(FooBar.class).id(fooBar.id).safe();

		assert fooBar.foos.size() == retreived.foos.size();
	}

	private FooBar createFooBar() {
		FooBar fooBar = new FooBar();
		List<Foo> foos = fooBar.foos;
		/* @formatter:off
		 * Here is the root of the issue. foos is an array where the first element is non-null but
		 * the remaining are null. In that case the underlying appengine Entity stores values like this
		 *
		 * fooBar.foos.bar.aField = [aField]
		 * fooBar.foos.bar.bField = [<someRandomNumber>]
		 * fooBar.foos.bar = [null, null, null, null]  <=== Note there are 4 here, not 5
		 *
		 * I would think that the expected behavior would be something like
		 *
		 * fooBar.foos.bar.aField = [aField, aField, aField, aField, aField]
		 * fooBar.foos.bar.bField = [1,2,3,4,5]
		 *
		 * JMS:  this actually should produce a metaproperty .^null = [1,2,3,4]
		 */
		for (int i = 0; i < 5; i++) {
			Foo foo = createFoo();
			if (i != 0) {
				foo.bar = null;
			}
			foos.add(foo);
		}

		return fooBar;
	}

	private Foo createFoo() {
		Bar bar = new Bar();
		Foo foo = new Foo(bar);
		return foo;
	}

	@Embed
	public static class Bar {
		public String aField = "aField";
		public Double bField = Math.random();
	}

	@Embed
	public static class Foo {
		public Bar bar;
		public Foo(Bar bar) { this.bar = bar; }
		public Foo() { }
	}

	@Entity
	public static class FooBar {
		@Id Long id;
		public List<Foo> foos = new ArrayList<Foo>();
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
		fact().register(Form.class);

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
