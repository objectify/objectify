/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Transmog;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.test.util.TransmogTestBase;

/**
 * Tests the basic low-level functions of the Transmog as they relate to @Embed collections.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TransmogEmbedCollectionTests extends TransmogTestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TransmogEmbedCollectionTests.class.getName());
	
	/** */
	static class OneField {
		String foo;
		public OneField() {}
		public OneField(String foo) { this.foo = foo; }
		@Override public boolean equals(Object other) { return this.foo.equals(((OneField)other).foo); }
	}
	@com.googlecode.objectify.annotation.Entity
	static class HasOneFieldColl {
		@Id long id;
		@Embed List<OneField> things = new ArrayList<OneField>();
	}

	/** */
	@Test
	public void testOneFieldCollOneItem() throws Exception
	{
		this.fact.register(HasOneFieldColl.class);
		Transmog<HasOneFieldColl> transmog = getTransmog(HasOneFieldColl.class);
		Objectify ofy = fact.begin();
		
		HasOneFieldColl pojo = new HasOneFieldColl();
		pojo.id = 123L;
		pojo.things.add(new OneField("asdf"));
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		
		// Should look like { id: 123L, things: [ { foo: "asdf" } ] }
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 123L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 1;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");
		}
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		// Should have one property:  "things.foo" = [ "asdf" ]
	
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getProperty("id") == null;
		assert entity.getProperties().size() == 1;
		assert entity.getProperty("things.foo").equals(Collections.singletonList("asdf"));
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		// Should look like { id: 123L, things: [ { foo: "asdf" } ] }
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 123L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 1;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");
		}
		
		// Go back to a solid object
		HasOneFieldColl pojo2 = transmog.load(rootNode, new LoadContext(entity, ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.things.equals(pojo.things);
	}
	
	/** */
	@Test
	public void testOneFieldCollTwoItems() throws Exception
	{
		this.fact.register(HasOneFieldColl.class);
		Transmog<HasOneFieldColl> transmog = getTransmog(HasOneFieldColl.class);
		Objectify ofy = fact.begin();
		
		HasOneFieldColl pojo = new HasOneFieldColl();
		pojo.id = 123L;
		pojo.things.add(new OneField("asdf"));
		pojo.things.add(new OneField("qwer"));
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		
		// Should look like { id: 123L, things: [ { foo: "asdf" }, { foo: "qwer" } ] }
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 123L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 2;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");

			Node thing1 = thingsNode.get(1);
			assertChildValue(thing1, "foo", "qwer");
	}
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		// Should have one property:  "things.foo" = [ "asdf", "qwer" ]
	
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getProperty("id") == null;
		assert entity.getProperties().size() == 1;
		assert entity.getProperty("things.foo").equals(Arrays.asList(new String[] { "asdf", "qwer" }));
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		// Should look like { id: 123L, things: [ { foo: "asdf" }, { foo: "qwer" } ] }
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 123L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 2;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");

			Node thing1 = thingsNode.get(1);
			assertChildValue(thing1, "foo", "qwer");
		}
		
		// Go back to a solid object
		HasOneFieldColl pojo2 = transmog.load(rootNode, new LoadContext(entity, ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.things.equals(pojo.things);
	}

	static class TwoFields {
		String foo;
		long bar;
		
		public TwoFields() {}
		public TwoFields(String foo, long bar) {
			this.foo = foo;
			this.bar = bar;
		}
		
		@Override
		public boolean equals(Object paramObject) {
			TwoFields other = (TwoFields)paramObject;
			return this.foo.equals(other.foo) && this.bar == other.bar;
		}
	}
	
	@com.googlecode.objectify.annotation.Entity
	static class HasTwoFieldsColl {
		@Id long id;
		@Embed List<TwoFields> things = new ArrayList<TwoFields>();
	}
	
	/** */
	@Test
	public void testTwoFieldsCollOneItem() throws Exception
	{
		this.fact.register(HasTwoFieldsColl.class);
		
		Transmog<HasTwoFieldsColl> transmog = getTransmog(HasTwoFieldsColl.class);
		Objectify ofy = fact.begin();
		
		HasTwoFieldsColl pojo = new HasTwoFieldsColl();
		pojo.id = 222;
		pojo.things.add(new TwoFields("asdf", 123L));
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		
		// Should look like: {id='222', things=[{foo='asdf', bar='123'}]}
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 222L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 1;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");
			assertChildValue(thing0, "bar", 123L);
		}
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getProperty("id") == null;
		assert entity.getProperties().size() == 2;
		
		List<String> thingsFoo = Arrays.asList(new String[] { "asdf" });
		assert entity.getProperty("things.foo").equals(thingsFoo);
		
		List<Long> thingsBar = Arrays.asList(new Long[] { 123L });
		assert entity.getProperty("things.bar").equals(thingsBar);
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		// Should look like: {id='222', things=[{foo='asdf', bar='123'}]}
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 222L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 1;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");
			assertChildValue(thing0, "bar", 123L);
		}
		
		// Go back to a solid object
		HasTwoFieldsColl pojo2 = transmog.load(rootNode, new LoadContext(entity, ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.things.equals(pojo.things);
	}

	/** */
	@Test
	public void testTwoFieldsCollTwoItems() throws Exception
	{
		this.fact.register(HasTwoFieldsColl.class);
		
		Transmog<HasTwoFieldsColl> transmog = getTransmog(HasTwoFieldsColl.class);
		Objectify ofy = fact.begin();
		
		HasTwoFieldsColl pojo = new HasTwoFieldsColl();
		pojo.id = 222;
		pojo.things.add(new TwoFields("asdf", 123L));
		pojo.things.add(new TwoFields("zxcv", 456L));
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 222L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 2;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");
			assertChildValue(thing0, "bar", 123L);
			
			Node thing1 = thingsNode.get(1);
			assertChildValue(thing1, "foo", "zxcv");
			assertChildValue(thing1, "bar", 456L);
		}
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getProperty("id") == null;
		assert entity.getProperties().size() == 2;
		
		List<String> thingsFoo = Arrays.asList(new String[] { "asdf", "zxcv" });
		assert entity.getProperty("things.foo").equals(thingsFoo);
		
		List<Long> thingsBar = Arrays.asList(new Long[] { 123L, 456L });
		assert entity.getProperty("things.bar").equals(thingsBar);
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		{
			assert !rootNode.hasPropertyValue();
			assertChildValue(rootNode, "id", 222L);
			assert rootNode.size() == 2;
			
			Node thingsNode = rootNode.get("things");
			assert thingsNode.size() == 2;
			
			Node thing0 = thingsNode.get(0);
			assertChildValue(thing0, "foo", "asdf");
			assertChildValue(thing0, "bar", 123L);
			
			Node thing1 = thingsNode.get(1);
			assertChildValue(thing1, "foo", "zxcv");
			assertChildValue(thing1, "bar", 456L);
		}
		
		// Go back to a solid object
		HasTwoFieldsColl pojo2 = transmog.load(rootNode, new LoadContext(entity, ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.things.equals(pojo.things);
	}
	
}