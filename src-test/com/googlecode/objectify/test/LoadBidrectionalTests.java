/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * What happens when we @Load entities in two directions
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadBidrectionalTests extends TestBase
{
	/** */
	@Entity
	public static class Top {
		public @Id long id;
		public @Load Bottom bottom;
		
		public Top() {}
		public Top(long id) { this.id = id; }
	}
	
	/** */
	@Entity
	public static class Bottom {
		public @Id long id;
		public @Load Top top;
		
		public Bottom() {}
		public Bottom(long id) { this.id = id; }
	}
	
	/** */
	@Test
	public void testBidirectional() throws Exception
	{
		fact.register(Top.class);
		fact.register(Bottom.class);
		
		TestObjectify ofy = fact.begin();
		
		Top top = new Top(123);
		Bottom bottom = new Bottom(456);
		
		top.bottom = bottom;
		bottom.top = top;
		
		ofy.put(top, bottom);
		ofy.clear();
		
		Top topFetched = ofy.load().entity(top).get();
		
		assert topFetched.bottom.id == top.bottom.id;
		assert topFetched.bottom.top.id == top.id;
	}

	/** */
	@Entity
	public static class TopWithEmbed {
		public @Id long id;
		public BottomEmbed bottom;
		
		public TopWithEmbed() {}
		public TopWithEmbed(long id) { this.id = id; }
	}
	
	/** */
	@Embed
	public static class BottomEmbed {
		public @Load TopWithEmbed top;
		public BottomEmbed() {}
	}
	
	/** */
	@Test
	public void testBidirectionalEmbed() throws Exception
	{
		fact.register(TopWithEmbed.class);
		
		TestObjectify ofy = fact.begin();
		
		TopWithEmbed top = new TopWithEmbed(123);
		top.bottom = new BottomEmbed();
		top.bottom.top = top;
		
		ofy.put(top);
		ofy.clear();
		
		TopWithEmbed topFetched = ofy.load().entity(top).get();
		
		assert topFetched.bottom.top.id == top.id;
	}

}