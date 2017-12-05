/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of queries of odd field types.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@SuppressWarnings("MagicConstant")
class QueryExoticTypesTests extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class HasDate {
		@Id Long id;
		@Index Date when;

		HasDate(final Date when) {
			this.when = when;
		}
	}

	/** */
	@Test
	void dateFiltering() throws Exception {
		factory().register(HasDate.class);

		final long later = System.currentTimeMillis();
		final Date earlier = new Date(later - 1);

		final HasDate laterEntity = new HasDate(new Date(later));
		final HasDate earlierEntity = new HasDate(earlier);

		ofy().save().entities(laterEntity, earlierEntity).now();

		final List<HasDate> result = ofy().load().type(HasDate.class).filter("when >", earlier).list();
		assertThat(result).containsExactly(laterEntity);
	}

	/**
	 * @author aswath satrasala
	 */
	@Entity
	@Data
	private static class HasFromThruDate {
		@Id Long id;
		@Index List<Date> dateList = new ArrayList<>();
	}

	/**
	 * @author aswath satrasala
	 */
	@Test
	void fromThruDateFiltering() throws Exception {
		factory().register(HasFromThruDate.class);

		final HasFromThruDate h1 = new HasFromThruDate();
		final Calendar cal1 = Calendar.getInstance();
		cal1.set(2010, 7, 25);
		h1.dateList.add(cal1.getTime());
		cal1.set(2010, 7, 25);
		h1.dateList.add(cal1.getTime());

		final HasFromThruDate h2 = new HasFromThruDate();
		cal1.set(2010, 7, 26);
		h2.dateList.add(cal1.getTime());
		cal1.set(2010, 7, 26);
		h2.dateList.add(cal1.getTime());

		final HasFromThruDate h3 = new HasFromThruDate();
		cal1.set(2010, 7, 27);
		h3.dateList.add(cal1.getTime());
		cal1.set(2010, 7, 27);
		h3.dateList.add(cal1.getTime());

		ofy().save().entities(h1, h2, h3).now();

		cal1.set(2010, 7, 25);
		final Date fromDate = cal1.getTime();

		cal1.set(2010, 7, 26);
		final Date thruDate = cal1.getTime();
		
//		List<com.google.cloud.datastore.Entity> ents = new ArrayList<>(ds().prepare(new com.google.cloud.datastore.Query()).asList(FetchOptions.Builder.withDefaults()));

		final Query<HasFromThruDate> q =
			ofy().load().type(HasFromThruDate.class)
				.filter("dateList >=", fromDate).filter("dateList <=", thruDate);

		final List<HasFromThruDate> listresult = q.list();
		assertThat(listresult).hasSize(2);
	}

	/** */
	@Entity
	private static class HasRef {
		@Id Long id;
		@Index Ref<HasRef> ref;
	}

	/** */
	@Test
	void refsFilterAsKeys() throws Exception {
		factory().register(HasRef.class);

		final Key<HasRef> someKey = Key.create(HasRef.class, 123L);

		final HasRef hr = new HasRef();
		hr.ref = Ref.create(someKey);

		ofy().save().entity(hr).now();

		List<HasRef> result = ofy().load().type(HasRef.class).filter("ref", someKey).list();
		assertThat(result).containsExactly(hr);
	}
}
