/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Transmog;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TransmogTestBase;

/**
 * Tests the basic low-level functions of the Transmog.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TransmogTests extends TransmogTestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TransmogTests.class.getName());

	/** */
	@Test
	public void testTrivial() throws Exception
	{
		this.fact.register(Trivial.class);
		
		Transmog<Trivial> transmog = getTransmog(Trivial.class);
		Objectify ofy = fact.begin();
		
		Trivial triv = new Trivial(123L, "foo", 456L);
		
		// Check the tree structure
		Node rootNode = transmog.save(triv, new SaveContext(ofy));
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", triv.getId());
		assertChildValue(rootNode, "someString", triv.getSomeString());
		assertChildValue(rootNode, "someNumber", triv.getSomeNumber());
		assert rootNode.size() == 3;
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getKey().getKind().equals(triv.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getKey().getId() == triv.getId();
		assert entity.getProperty("id") == null;
		assert entity.getProperty("someString").equals(triv.getSomeString());
		assert entity.getProperty("someNumber").equals(triv.getSomeNumber());
		assert entity.getProperties().size() == 2;
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", triv.getId());
		assertChildValue(rootNode, "someString", triv.getSomeString());
		assertChildValue(rootNode, "someNumber", triv.getSomeNumber());
		assert rootNode.size() == 3;
		
		// Go back to a solid object
		Trivial triv2 = transmog.load(rootNode, new LoadContext(ofy));
		
		assert triv.getId().equals(triv2.getId());
		assert triv.getSomeNumber() == triv2.getSomeNumber();
		assert triv.getSomeString().equals(triv2.getSomeString());
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	public static class HasSimpleList {
		@Id long id;
		List<String> stuff = new ArrayList<String>();
	}
	
	/** */
	@Test
	public void testSimpleList() throws Exception
	{
		fact.register(HasSimpleList.class);
		
		Transmog<HasSimpleList> transmog = getTransmog(HasSimpleList.class);
		Objectify ofy = fact.begin();
		
		HasSimpleList pojo = new HasSimpleList();
		pojo.id = 123L;
		pojo.stuff.add("foo");
		pojo.stuff.add("bar");
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		Node stuffNode;
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		stuffNode = rootNode.get("stuff");
		assertChildValue(stuffNode, 0, pojo.stuff.get(0));
		assertChildValue(stuffNode, 1, pojo.stuff.get(1));
		assert rootNode.size() == 2;
		assert stuffNode.size() == 2;
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getKey().getId() == pojo.id;
		assert entity.getProperty("id") == null;
		assert entity.getProperty("stuff").equals(pojo.stuff);	// list same size & contents
		assert entity.getProperties().size() == 1;
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		stuffNode = rootNode.get("stuff");
		assertChildValue(stuffNode, 0, pojo.stuff.get(0));
		assertChildValue(stuffNode, 1, pojo.stuff.get(1));
		assert rootNode.size() == 2;
		assert stuffNode.size() == 2;
		
		// Go back to a solid object
		HasSimpleList pojo2 = transmog.load(rootNode, new LoadContext(ofy));
		
		assert pojo.id == pojo2.id;
		assert pojo.stuff.equals(pojo2.stuff);
	}

	/** */
	@Test
	public void testSimpleListEmpty() throws Exception
	{
		fact.register(HasSimpleList.class);
		
		Transmog<HasSimpleList> transmog = getTransmog(HasSimpleList.class);
		Objectify ofy = fact.begin();
		
		HasSimpleList pojo = new HasSimpleList();
		pojo.id = 123L;
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		assert rootNode.size() == 1;
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getKey().getId() == pojo.id;
		assert entity.getProperties().size() == 0;
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		assert rootNode.size() == 1;
		
		// Go back to a solid object
		HasSimpleList pojo2 = transmog.load(rootNode, new LoadContext(ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.stuff.isEmpty(); 
	}
	
	/** */
	@Test
	public void testSimpleListExplicitlySetNull() throws Exception
	{
		fact.register(HasSimpleList.class);
		
		Transmog<HasSimpleList> transmog = getTransmog(HasSimpleList.class);
		Objectify ofy = fact.begin();
		
		HasSimpleList pojo = new HasSimpleList();
		pojo.id = 123L;
		pojo.stuff = null;	// explicitly null it out
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		assert rootNode.size() == 1;
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getKey().getId() == pojo.id;
		assert entity.getProperties().size() == 0;
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		assert rootNode.size() == 1;
		
		// Go back to a solid object
		HasSimpleList pojo2 = transmog.load(rootNode, new LoadContext(ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.stuff.isEmpty(); 
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	public static class HasSimpleListUninitialized {
		@Id long id;
		List<String> stuff;
	}
	
	/** */
	@Test
	public void testSimpleListUninitialized() throws Exception
	{
		fact.register(HasSimpleListUninitialized.class);
		
		Transmog<HasSimpleListUninitialized> transmog = getTransmog(HasSimpleListUninitialized.class);
		Objectify ofy = fact.begin();
		
		HasSimpleListUninitialized pojo = new HasSimpleListUninitialized();
		pojo.id = 123L;
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		assert rootNode.size() == 1;
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getKey().getId() == pojo.id;
		assert entity.getProperties().size() == 0;
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", pojo.id);
		assert rootNode.size() == 1;
		
		// Go back to a solid object
		HasSimpleListUninitialized pojo2 = transmog.load(rootNode, new LoadContext(ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.stuff == null; 
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	public static class HasMap {
		@Id long id;
		Map<String, Long> stuff = new HashMap<String, Long>();
	}
	
	/** */
	@Test
	public void testMap() throws Exception
	{
		fact.register(HasMap.class);
		Transmog<HasMap> transmog = getTransmog(HasMap.class);
		Objectify ofy = fact.begin();
		
		HasMap pojo = new HasMap();
		pojo.id = 123L;
		pojo.stuff.put("foo", 5L);
		
		// Check the tree structure
		Node rootNode = transmog.save(pojo, new SaveContext(ofy));

		// id and foo
		{
			assert rootNode.size() == 2;
			Node stuffNode = rootNode.get("stuff");
			assert stuffNode.size() == 1;
			assertChildValue(stuffNode, "foo", 5L);
		}
		
		// Check the entity structure
		Entity entity = transmog.save(rootNode);
		
		assert entity.getProperties().size() == 1;
		assert entity.getProperty("stuff.foo").equals(5L);
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.load(entity);
		
		// id and foo
		{
			assert rootNode.size() == 2;
			Node stuffNode = rootNode.get("stuff");
			assert stuffNode.size() == 1;
			assertChildValue(stuffNode, "foo", 5L);
		}
		
		// Go back to a solid object
		HasMap pojo2 = transmog.load(rootNode, new LoadContext(ofy));
		
		assert pojo2.id == pojo.id;
		assert pojo2.stuff.equals(pojo.stuff); 
	}
}