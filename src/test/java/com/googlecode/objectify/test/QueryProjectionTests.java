/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryProjectionTests extends TestBase {

	/** */
	@Test
	void simpleProjectionWorks() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial(123L, "foo", 12);
		ofy().save().entity(triv).now();
		ofy().clear();

		final List<Trivial> projected = ofy().load().type(Trivial.class).project("someString").list();
		assertThat(projected).hasSize(1);

		final Trivial pt = projected.get(0);
		assertThat(pt.getId()).isEqualTo(triv.getId());
		assertThat(pt.getSomeString()).isEqualTo(triv.getSomeString());
		assertThat(pt.getSomeNumber()).isEqualTo(0);	// default value, not the saved one
	}

	/** */
	@Test
	void projectionDoesNotContaminateSession() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial(123L, "foo", 12);
		final Key<Trivial> trivKey = ofy().save().entity(triv).now();
		ofy().clear();

		final List<Trivial> projected = ofy().load().type(Trivial.class).project("someString").list();
		assertThat(projected).hasSize(1);
		assertThat(ofy().isLoaded(trivKey)).isFalse();

		final Trivial fetched = ofy().load().key(trivKey).now();
		assertThat(fetched).isEqualTo(triv);
	}

	@Entity
	@Data
	private static class HasIndexedNumber {
		@Id Long id;
		@Index int number;
	}

	/** */
	@Test
	void projectionOfNumbersWorks() throws Exception {
		factory().register(HasIndexedNumber.class);

		final HasIndexedNumber hin = new HasIndexedNumber();
		hin.number = 5;

		ofy().save().entity(hin).now();
		ofy().clear();

		final List<HasIndexedNumber> projected = ofy().load().type(HasIndexedNumber.class).project("number").list();
		assertThat(projected).hasSize(1);
		assertThat(projected.get(0).number).isEqualTo(hin.number);
	}

	/** */
	@Test
	void distinctWorks() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial(123L, "foo", 12);
		final Trivial triv2 = new Trivial(456L, "foo", 12);
		ofy().save().entities(triv, triv2).now();
		ofy().clear();

		final List<Trivial> projected = ofy().load().type(Trivial.class).project("someString").distinct(true).list();
		assertThat(projected).hasSize(1);

		final Trivial pt = projected.get(0);
		assertThat(pt.getId()).isEqualTo(triv.getId());
		assertThat(pt.getSomeString()).isEqualTo(triv.getSomeString());
		assertThat(pt.getSomeNumber()).isEqualTo(0);	// default value, not the saved one
	}

	@Entity
	private static class HasBool {
		@Id Long id;
		@Index boolean t = true;
		@Index boolean f = false;
	}

	/**
	 * This caused a LoadException in 5.1.4
	 */
	@Test
	void projectBooleanPrimitiveFields() throws Exception {
		factory().register(HasBool.class);

		ofy().save().entity(new HasBool()).now();
		ofy().clear();

		final HasBool fetched = ofy().load().type(HasBool.class).project("t").first().now();
		assertThat(fetched.t).isTrue();
		assertThat(fetched.f).isFalse();
	}

	@Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class HasDate {
		@Id Long id;
		@Index Date date;
	}

	/** */
	@Test
	void datesCanBeProjected() throws Exception {
		factory().register(HasDate.class);

		final HasDate hd = new HasDate(null, new Date());
		ofy().save().entity(hd).now();
		ofy().clear();

		final List<HasDate> projected = ofy().load().type(HasDate.class).project("date").list();
		assertThat(projected).hasSize(1);

		final HasDate phd = projected.get(0);
		assertThat(phd.getDate()).isEqualTo(hd.getDate());
	}

	@Entity
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class HasBlob {
		@Id Long id;
		@Index byte[] blob;
	}

	/** */
	@Test
	void blobsCanBeProjected() throws Exception {
		factory().register(HasBlob.class);

		final HasBlob hb = new HasBlob(null, new byte[] { 1, 2, 3 });
		ofy().save().entity(hb).now();
		ofy().clear();

		final List<HasBlob> projected = ofy().load().type(HasBlob.class).project("blob").list();
		assertThat(projected).hasSize(1);

		final HasBlob phb = projected.get(0);
		assertThat(phb.getBlob()).isEqualTo(hb.getBlob());
	}
}