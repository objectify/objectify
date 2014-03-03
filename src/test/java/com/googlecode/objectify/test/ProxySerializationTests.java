/*
 */

package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Make sure that the proxies we return can be serialized sanely.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ProxySerializationTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(ProxySerializationTests.class.getName());
	
	/** */
	ByteArrayOutputStream bytesOut;
	ObjectOutputStream objectOut;
	
	/** */
	@BeforeMethod
	void setUpOutput() throws Exception {
		bytesOut = new ByteArrayOutputStream();
		objectOut = new ObjectOutputStream(bytesOut);
	}
	
	private void serialize(Object o) throws Exception {
		objectOut.writeObject(o);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T deserialize() throws Exception {
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
		return (T)in.readObject();
	}

	/** */
	@Test
	public void queryListCanBeSerialized() throws Exception {
		fact().register(Trivial.class);

		Trivial triv = new Trivial("foo", 5);
		ofy().save().entity(triv).now();

		List<Trivial> trivs = ofy().load().type(Trivial.class).list();
		
		serialize(trivs);
		
		List<Trivial> back = deserialize();
		
		assert back.size() == 1;
		assert back.get(0).getId().equals(triv.getId());
	}
	
	/** */
	@Test
	public void loadMapCanBeSerialized() throws Exception {
		fact().register(Trivial.class);

		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = ofy().save().entity(triv).now();

		@SuppressWarnings("unchecked")
		Map<Key<Trivial>, Trivial> trivs = ofy().load().keys(k);
		
		serialize(trivs);
		
		Map<Key<Trivial>, Trivial> back = deserialize();
		
		assert back.size() == 1;
		assert back.get(k).getId().equals(triv.getId());
	}
}