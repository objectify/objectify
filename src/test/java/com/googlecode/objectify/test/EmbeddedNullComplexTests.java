/**
 *
 */
package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Hey, this should work now! These used to break in the old embedded format. In the new EmbeddedEntity-based
 * solution, weird pathological situations are fine.
 */
class EmbeddedNullComplexTests extends TestBase {

	/**
	 * @author Brian Chapman
	 */
	@Test
	void testFooBar() {
		factory().register(FooBar.class);

		final FooBar fooBar = createFooBar();
		final FooBar retreived = saveClearLoad(fooBar);

		assertThat(fooBar.foos).isEqualTo(retreived.foos);
	}

	private FooBar createFooBar() {
		FooBar fooBar = new FooBar();
		List<Foo> foos = fooBar.foos;
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
		return new Foo(new Bar());
	}

	@Data
	private static class Bar {
		private String aField = "aField";
		private Double bField = Math.random();
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class Foo {
		private Bar bar;
	}

	@Entity
	@Data
	private static class FooBar {
		@Id
		private Long id;
		private List<Foo> foos = new ArrayList<>();
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
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@SuppressWarnings("serial")
	private static class Group implements Serializable {
		String name;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@SuppressWarnings("serial")
	private static class Profile implements Serializable {
		String name;
	}

	@Data
	private static class AddressBook {
		@Serialize Group[] groups;
		@Serialize Profile[] profiles;
	}

	@Data
	private static class Approval {
		AddressBook addressBook;
		String approvedBy;
	}

	@Data
	@Entity
	private static class Form {
		@Id Long id;
		Approval[] approvals;
	}

	/** */
	@Test
	void testNestedEmbeds() {
		factory().register(Form.class);

		final Approval approval0 = new Approval();
		approval0.approvedBy = "somezero";
		approval0.addressBook = new AddressBook();
		approval0.addressBook.groups = new Group[] { new Group("group0") };
		approval0.addressBook.profiles = new Profile[] { new Profile("profile0") };

		final Approval approval1 = new Approval();
		approval1.approvedBy = "someone";

		final Approval approval2 = new Approval();
		approval2.approvedBy = "sometwo";
		approval2.addressBook = new AddressBook();
		approval2.addressBook.groups = new Group[] { new Group("group2") };
		approval2.addressBook.profiles = new Profile[] { new Profile("profile2") };

		final Form form = new Form();
		form.approvals = new Approval[] { approval0, approval1, approval2 };

		final Form fetched = saveClearLoad(form);

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
