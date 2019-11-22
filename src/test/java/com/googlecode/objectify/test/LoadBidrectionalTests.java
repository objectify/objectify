/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * What happens when we @Load entities in two directions
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadBidrectionalTests extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Top {
		private @Id long id;
		private @Load Ref<Bottom> bottom;

		private Top(long id) { this.id = id; }
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Bottom {
		private @Id long id;
		private @Load Ref<Top> top;

		private Bottom(long id) { this.id = id; }
	}

	/** */
	@Test
	void testBidirectional() throws Exception {
		factory().register(Top.class);
		factory().register(Bottom.class);

		final Top top = new Top(123);
		final Bottom bottom = new Bottom(456);

		top.bottom = Ref.create(bottom);
		bottom.top = Ref.create(top);

		ofy().save().entities(top, bottom).now();
		ofy().clear();

		final Top topFetched = ofy().load().entity(top).now();
		final Bottom bottomFetched = ofy().load().entity(bottom).now();

		assertThat(topFetched.bottom.get()).isSameInstanceAs(bottomFetched);
		assertThat(bottomFetched.top.get()).isSameInstanceAs(topFetched);
	}

	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class TopWithEmbed {
		private @Id long id;
		private BottomEmbed bottom;

		private TopWithEmbed(long id) { this.id = id; }
	}

	/** */
	@Data
	private static class BottomEmbed {
		private @Load Ref<TopWithEmbed> top;
	}

	/** */
	@Test
	void testBidirectionalEmbed() throws Exception {
		factory().register(TopWithEmbed.class);

		final TopWithEmbed top = new TopWithEmbed(123);
		top.bottom = new BottomEmbed();
		top.bottom.top = Ref.create(top);

		ofy().save().entity(top).now();
		ofy().clear();

		final TopWithEmbed topFetched = ofy().load().entity(top).now();

		assertThat(topFetched.bottom.top.get()).isSameInstanceAs(topFetched);
	}

}