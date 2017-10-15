/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Make sure that the proxies we return can be serialized sanely.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class ProxySerializationTests extends TestBase {

	@SuppressWarnings("unchecked")
	private <T> T roundtrip(final T input) throws Exception {
		final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		final ObjectOutputStream objectOut = new ObjectOutputStream(bytesOut);

		objectOut.writeObject(input);

		final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
		return (T)in.readObject();
	}

	/** */
	@Test
	void queryListCanBeSerialized() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		ofy().save().entity(triv).now();

		final List<Trivial> trivs = ofy().load().type(Trivial.class).list();
		final List<Trivial> back = roundtrip(trivs);

		assertThat(back).containsExactly(triv);
	}
	
	/** */
	@Test
	void loadMapCanBeSerialized() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> k = ofy().save().entity(triv).now();

		@SuppressWarnings("unchecked")
		final Map<Key<Trivial>, Trivial> trivs = ofy().load().keys(k);
		final Map<Key<Trivial>, Trivial> back = roundtrip(trivs);

		assertThat(back).containsExactly(k, triv);
	}
}