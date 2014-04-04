/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
		public @Load Ref<Bottom> bottom;

		public Top() {}
		public Top(long id) { this.id = id; }
	}

	/** */
	@Entity
	public static class Bottom {
		public @Id long id;
		public @Load Ref<Top> top;

		public Bottom() {}
		public Bottom(long id) { this.id = id; }
	}

	/** */
	@Test
	public void testBidirectional() throws Exception
	{
		fact().register(Top.class);
		fact().register(Bottom.class);

		Top top = new Top(123);
		Bottom bottom = new Bottom(456);

		top.bottom = Ref.create(bottom);
		bottom.top = Ref.create(top);

		ofy().save().entities(top, bottom).now();
		ofy().clear();

		Top topFetched = ofy().load().entity(top).now();

		assert topFetched.bottom.get().id == top.bottom.get().id;
		assert topFetched.bottom.get().top.get().id == top.id;
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
	public static class BottomEmbed {
		public @Load Ref<TopWithEmbed> top;
		public BottomEmbed() {}
	}

	/** */
	@Test
	public void testBidirectionalEmbed() throws Exception
	{
		fact().register(TopWithEmbed.class);

		TopWithEmbed top = new TopWithEmbed(123);
		top.bottom = new BottomEmbed();
		top.bottom.top = Ref.create(top);

		ofy().save().entity(top).now();
		ofy().clear();

		TopWithEmbed topFetched = ofy().load().entity(top).now();

		assert topFetched.bottom.top.get().id == top.id;
	}

}