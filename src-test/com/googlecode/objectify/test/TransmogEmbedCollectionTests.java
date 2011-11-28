/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.Transmog;
import com.googlecode.objectify.impl.node.MapNode;
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

	static class Thing {
		String foo;
		long bar;
		
		public Thing() {}
		public Thing(String foo, long bar) {
			this.foo = foo;
			this.bar = bar;
		}
		
		@Override
		public boolean equals(Object paramObject) {
			Thing other = (Thing)paramObject;
			return this.foo.equals(other.foo) && this.bar == other.bar;
		}
	}
	
	@com.googlecode.objectify.annotation.Entity
	static class HasEmbedColl {
		@Id Long id;
		@Embed List<Thing> things = new ArrayList<Thing>();
	}
	
	/** */
	@Test
	public void testEmbeddedColl() throws Exception
	{
		this.fact.register(HasEmbedColl.class);
		
		Transmog<HasEmbedColl> transmog = getTransmog(HasEmbedColl.class);
		Objectify ofy = fact.begin();
		
		HasEmbedColl pojo = new HasEmbedColl();
		pojo.things.add(new Thing("asdf", 123L));
		pojo.things.add(new Thing("zxcv", 456L));
		
		// Some things we will need for comparison
		List<String> thingsFoo = Arrays.asList(new String[] { "asdf", "zxcv" });
		List<Long> thingsBar = Arrays.asList(new Long[] { 123L, 456L });
		
		// Check the tree structure
		MapNode rootNode = transmog.save(pojo, new SaveContext(ofy));
		MapNode thingsNode;
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", null);
		assert rootNode.entrySet().size() == 2;
		
		thingsNode = rootNode.getMap("things");
		assertChildValue(thingsNode, "foo", thingsFoo);
		assertChildValue(thingsNode, "bar", thingsBar);
		
		// Check the entity structure
		Entity entity = transmog.createEntity(rootNode);
		
		assert entity.getKey().getKind().equals(pojo.getClass().getSimpleName());
		assert entity.getKey().getParent() == null;
		assert entity.getKey().getId() == pojo.id;
		assert entity.getProperty("id") == null;
		assert entity.getProperty("things.foo").equals(thingsFoo);
		assert entity.getProperty("things.bar").equals(thingsBar);
		assert entity.getProperties().size() == 3;
		
		// Go back to the tree structure and run the same tests as before
		rootNode = transmog.createNode(entity);
		
		assert !rootNode.hasPropertyValue();
		assertChildValue(rootNode, "id", null);
		assert rootNode.entrySet().size() == 2;
		
		thingsNode = rootNode.getMap("things");
		assertChildValue(thingsNode, "foo", thingsFoo);
		assertChildValue(thingsNode, "bar", thingsBar);
		
		// Go back to a solid object
		HasEmbedColl pojo2 = transmog.load(rootNode, new LoadContext(entity, ofy));
		
		assert pojo2.id.equals(pojo.id);
		assert pojo2.things.equals(pojo.things);
	}
	
}