/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.NamedTrivial;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of basic entity manipulation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BasicTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(BasicTests.class.getName());

	/** */
	@Test
	public void idIsGenerated() throws Exception {
		fact().register(Trivial.class);

		// Note that 5 is not the id, it's part of the payload
		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = ofy().save().entity(triv).now();

		assert k.getKind().equals(triv.getClass().getSimpleName());
		assert k.getId() == triv.getId();

		Key<Trivial> created = Key.create(Trivial.class, k.getId());
		assert k.equals(created);

		Trivial fetched = ofy().load().key(k).now();

		assert fetched.getId().equals(k.getId());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}
	
	/** */
	@Test(expectedExceptions=SaveException.class)
	public void savingNullNamedIdThrowsException() throws Exception {
		fact().register(NamedTrivial.class);
		
		NamedTrivial triv = new NamedTrivial(null, "foo", 5);
		ofy().save().entity(triv).now();
	}

	/** */
	@Test
	public void savingEntityWithSameIdOverwritesData() throws Exception {
		fact().register(Trivial.class);

		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = ofy().save().entity(triv).now();

		Trivial triv2 = new Trivial(k.getId(), "bar", 6);
		Key<Trivial> k2 = ofy().save().entity(triv2).now();

		assert k2.equals(k);

		Trivial fetched = ofy().load().key(k).now();

		assert fetched.getId() == k.getId();
		assert fetched.getSomeNumber() == triv2.getSomeNumber();
		assert fetched.getSomeString().equals(triv2.getSomeString());
	}

	/** */
	@Test
	public void savingEntityWithNameIdWorks() throws Exception {
		fact().register(NamedTrivial.class);

		NamedTrivial triv = new NamedTrivial("first", "foo", 5);
		Key<NamedTrivial> k = ofy().save().entity(triv).now();

		assert k.getName().equals("first");

		Key<NamedTrivial> createdKey = Key.create(NamedTrivial.class, "first");
		assert k.equals(createdKey);

		NamedTrivial fetched = ofy().load().key(k).now();

		assert fetched.getName().equals(k.getName());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}

	/** */
	@Test
	public void testBatchOperations() throws Exception {
		fact().register(Trivial.class);

		Trivial triv1 = new Trivial("foo", 5);
		Trivial triv2 = new Trivial("foo2", 6);

		List<Trivial> objs = new ArrayList<>();
		objs.add(triv1);
		objs.add(triv2);

		Map<Key<Trivial>, Trivial> map = ofy().save().entities(objs).now();
		List<Key<Trivial>> keys = new ArrayList<>(map.keySet());

		// Verify the put keys
		assert keys.size() == objs.size();
		for (int i=0; i<objs.size(); i++)
		{
			assert keys.get(i).getId() == objs.get(i).getId();
		}

		// Now fetch and verify the data
		Map<Key<Trivial>, Trivial> fetched = ofy().load().keys(keys);

		assert fetched.size() == keys.size();
		for (Trivial triv: objs)
		{
			Trivial fetchedTriv = fetched.get(Key.create(triv));
			assert triv.getSomeNumber() == fetchedTriv.getSomeNumber();
			assert triv.getSomeString().equals(fetchedTriv.getSomeString());
		}
	}

	/** */
	@Test
	public void testManyToOne() throws Exception {
		fact().register(Employee.class);

		Employee fred = new Employee("fred");
		ofy().save().entity(fred).now();

		Key<Employee> fredKey = Key.create(fred);

		List<Employee> employees = new ArrayList<>(100);
		for (int i = 0; i < 100; i++)
		{
			Employee emp = new Employee("foo" + i, fredKey);
			employees.add(emp);
		}

		ofy().save().entities(employees).now();

		assert employees.size() == 100;

		int count = 0;
		for (Employee emp: ofy().load().type(Employee.class).filter("manager", fred))
		{
			emp.getName(); // Just to make eclipse happy
			count++;
		}
		assert count == 100;
	}

	/** */
	@Test
	public void testKeyToString() throws Exception {
		Key<Trivial> trivKey = Key.create(Trivial.class, 123);

		String stringified = trivKey.getString();

		Key<Trivial> andBack = Key.create(stringified);

		assert trivKey.equals(andBack);
	}

	/**
	 */
	@Test
	public void testPutNothing() throws Exception {
		ofy().save().entities(Collections.emptyList()).now();
	}

	/** */
	@Test
	public void deleteBatch() throws Exception {
		fact().register(Trivial.class);

		Trivial triv1 = new Trivial("foo5", 5);
		Trivial triv2 = new Trivial("foo6", 6);

		ofy().save().entities(triv1, triv2).now();

		assert ofy().load().entities(triv1, triv2).size() == 2;

		ofy().delete().entities(triv1, triv2).now();

		Map<Key<Trivial>, Trivial> result = ofy().load().entities(triv1, triv2);
		System.out.println("Result is " + result);
		assert result.size() == 0;
	}

	/** */
	@Test
	public void loadWithManuallyCreatedKey() throws Exception {
		fact().register(Trivial.class);

		Trivial triv1 = new Trivial(123L, "foo5", 5);
		ofy().save().entity(triv1).now();

		Trivial fetched = ofy().load().key(Key.create(Trivial.class, 123L)).now();
		assert fetched.toString().equals(triv1.toString());
	}

	/** */
	@SuppressWarnings("unchecked")
	@Test
	public void loadNonexistant() throws Exception {
		fact().register(Trivial.class);

		Trivial triv1 = new Trivial("foo5", 5);
		ofy().save().entity(triv1).now();

		Key<Trivial> triv1Key = Key.create(triv1);
		Key<Trivial> triv2Key = Key.create(Trivial.class, 998);
		Key<Trivial> triv3Key = Key.create(Trivial.class, 999);

		LoadResult<Trivial> res = ofy().load().key(triv2Key);
		assert res.now() == null;

		Map<Key<Trivial>, Trivial> result = ofy().load().keys(triv2Key, triv3Key);
		assert result.size() == 0;

		Map<Key<Trivial>, Trivial> result2 = ofy().load().keys(triv1Key, triv2Key);
		assert result2.size() == 1;
	}

	/** */
	@SuppressWarnings("unchecked")
	@Test
	public void loadNonexistantWithoutSession() throws Exception {
		fact().register(Trivial.class);

		Trivial triv1 = new Trivial("foo5", 5);
		ofy().save().entity(triv1).now();

		Key<Trivial> triv1Key = Key.create(triv1);
		Key<Trivial> triv2Key = Key.create(Trivial.class, 998);
		Key<Trivial> triv3Key = Key.create(Trivial.class, 999);

		ofy().clear();
		LoadResult<Trivial> res = ofy().load().key(triv2Key);
		assert res.now() == null;

		ofy().clear();
		Map<Key<Trivial>, Trivial> result = ofy().load().keys(triv2Key, triv3Key);
		assert result.size() == 0;

		ofy().clear();
		Map<Key<Trivial>, Trivial> result2 = ofy().load().keys(triv1Key, triv2Key);
		assert result2.size() == 1;
	}

	/** */
	@Test
	public void simpleFetchById() throws Exception {
		fact().register(Trivial.class);

		Trivial triv1 = new Trivial("foo5", 5);

		ofy().save().entity(triv1).now();

		ofy().clear();

		Trivial fetched = ofy().load().type(Trivial.class).id(triv1.getId()).now();

		assert fetched.getSomeString().equals(triv1.getSomeString());
	}
	
	
	@Entity
	static class HasParent {
		@Parent Key<Trivial> parent;
		@Id long id;
	}
	
	/** Simply delete an entity which has a parent */
	@Test
	public void deleteAnEntityWithAParent() throws Exception {
		fact().register(Trivial.class);
		fact().register(HasParent.class);
		
		HasParent hp = new HasParent();
		hp.parent = Key.create(Trivial.class, 123L);
		hp.id = 456L;
		
		Key<HasParent> hpKey = ofy().save().entity(hp).now();
		ofy().clear();
		assert ofy().load().key(hpKey).now() != null;
		ofy().delete().entity(hp).now();
		ofy().clear();
		assert ofy().load().key(hpKey).now() == null;
	}

	@Entity
	static class HasIndexedBlob {
		@Id Long id;
		@Index Blob blob;
	}

	@Test
	public void indexSomethingThatCannotBeIndexed() throws Exception {
		fact().register(HasIndexedBlob.class);

		byte[] stuff = "asdf".getBytes();

		HasIndexedBlob hib = new HasIndexedBlob();
		hib.blob = new Blob(stuff);

		HasIndexedBlob fetched = ofy().saveClearLoad(hib);
		byte[] fetchedStuff = fetched.blob.getBytes();

		assert Arrays.equals(fetchedStuff, stuff);
	}

}