/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * More tests of using the @AlsoLoad annotation combined with @Load
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadAlsoLoadTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(LoadAlsoLoadTests.class.getName());

	/** */
	@Entity
	@Cache
	static class HasConcrete
	{
		@Id Long id;
		String bar;

		public void cruft(@Load @AlsoLoad("foo") Ref<Trivial> triv) {
			this.bar = triv.get().getSomeString();
		}
	}

	/**
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();

		fact().register(HasConcrete.class);
		fact().register(Trivial.class);
	}

	/** */
	@Test
	public void alsoLoadWorksWithLoad() throws Exception
	{
		Trivial triv = new Trivial("someString", 123L);
		Key<Trivial> trivKey = ofy().save().entity(triv).now();

		com.google.appengine.api.datastore.Entity ent = new com.google.appengine.api.datastore.Entity(Key.getKind(HasConcrete.class));
		ent.setProperty("foo", trivKey.getRaw());
		ds().put(null, ent);

		Key<HasConcrete> key = Key.create(ent.getKey());
		HasConcrete fetched = ofy().load().key(key).now();

		assert fetched.bar.equals(triv.getSomeString());
	}
}