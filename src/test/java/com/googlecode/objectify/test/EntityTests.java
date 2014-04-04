/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of basic entity manipulation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(EntityTests.class.getName());

	/**
	 * A fruit.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	static abstract class Fruit
	{
		@Id Long id;
		String color;
		String taste;

		/** Default constructor must always exist */
		protected Fruit() {}

		/** Constructor*/
		protected Fruit(String color, String taste)
		{
			this.color = color;
			this.taste = taste;
		}

		public String getColor()
		{
			return this.color;
		}

		public String getTaste()
		{
			return this.taste;
		}
	}

	/**
	 * A fruit, an apple.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	static class Apple extends Fruit
	{
		public static final String COLOR = "red";
		public static final String TASTE = "sweet";

		private String size;

		/** Default constructor must always exist */
		public Apple() {}

		/** Constructor*/
		public Apple(String color, String taste)
		{
			super(color,taste);
			this.size = "small";
		}

		public String getSize()
		{
			return this.size;
		}
	}

	/** */
	@Test
	public void testApple() throws Exception
	{
		fact().register(Apple.class);

		Apple a = new Apple(Apple.COLOR, Apple.TASTE);
		Key<Apple> aKey = ofy().save().entity(a).now();
		Apple a2 = ofy().load().key(aKey).now();
		assert a2.getColor().equals(a.getColor()) : "Colors were different after stored/retrieved";
		assert a2.getSize().equals(a.getSize()) : "Sizes were different after stored/retrieved";
		assert a2.getTaste().equals(a.getTaste()) : "Tastes were different after stored/retrieved";
	}

	/**
	 * A banana fruit.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	static class Banana extends Fruit
	{
		public static final String COLOR = "yellow";
		public static final String TASTE = "sweet";

		private String shape;

		/** Default constructor must always exist */
		public Banana() {}

		/** Constructor*/
		public Banana(String color, String taste)
		{
			super(color,taste);
			this.shape = "like a banana";
		}

		public String getShape()
		{
			return this.shape;
		}
	}

	/** */
	@Test
	public void testBanana() throws Exception
	{
		fact().register(Banana.class);

		Banana b = new Banana(Banana.COLOR, Banana.TASTE);
		Key<Banana> bKey = ofy().save().entity(b).now();
		Banana b2 = ofy().load().key(bKey).now();
		assert b2.getColor().equals(b.getColor()) : "Colors were different after stored/retrieved";
		assert b2.getShape().equals(b.getShape()) : "Shapes were different after stored/retrieved";
		assert b2.getTaste().equals(b.getTaste()) : "Tastes were different after stored/retrieved";
	}

	/**
	 * A holder of a <T>hing.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	static abstract class Holder<T>
	{
		@Id Long id;
		T thing;

		/** Default constructor must always exist */
		protected Holder() {}
		protected Holder(T t) {this.thing = t;}

		public T getThing()
		{
			return this.thing;
		}
		public void setThing(T t)
		{
			this.thing = t;
		}
	}

	/**
	 * A holder of a string.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	static class HolderOfString extends Holder<String>
	{
		/** Default constructor must always exist */
		public HolderOfString() {}

		public HolderOfString(String s) {super(s);}

		public void setMyThing(String s)
		{
			this.thing = s;
		}

		public String getMyThing()
		{
			return this.thing;
		}

	}

	/** */
	@Test
	public void testStringHolder() throws Exception
	{
		fact().register(HolderOfString.class);

		String s = "my secret";
		HolderOfString hos = new HolderOfString(s);
		Key<HolderOfString> hosKey = ofy().save().entity(hos).now();
		HolderOfString hos2 = ofy().load().key(hosKey).now();

		assert hos.getThing().equals(hos2.getMyThing()) : "Strings were different after stored/retrieved";
		assert hos.getThing().getClass().equals(hos2.getMyThing().getClass()) : "Classes were differnt";
	}

	/**
	 * A holder of a string, and a Long.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	static class HolderOfStringAndLong extends HolderOfString
	{
		protected Long myPrecious;

		/** Default constructor must always exist */
		public HolderOfStringAndLong() {}

		public HolderOfStringAndLong(String s, Long l) {super(s); this.myPrecious = l; }

		public Long getMyPrecious()
		{
			return this.myPrecious;
		}
	}

	/** */
	@Test
	public void testStringHolderWithALong() throws Exception
	{
		fact().register(HolderOfStringAndLong.class);

		String s = "my secret";
		HolderOfStringAndLong hosal = new HolderOfStringAndLong(s,2L);
		Key<HolderOfStringAndLong> hosKey = ofy().save().entity(hosal).now();
		HolderOfStringAndLong hosal2 = ofy().load().key(hosKey).now();

		assert hosal.getMyPrecious().equals(hosal2.getMyPrecious()) : "Longs were different after stored/retrieved";
		assert hosal.getThing().equals(hosal2.getMyThing()) : "Strings were different after stored/retrieved";
		assert hosal.getThing().getClass().equals(hosal2.getMyThing().getClass()) : "Classes were differnt";
	}

	/** */
	@Test
	public void testToPojoAndBack() throws Exception
	{
		fact().register(Trivial.class);

		Trivial triv = new Trivial(123L, "blah", 456);

		com.google.appengine.api.datastore.Entity ent = ofy().save().toEntity(triv);
		assert ent.getKey().getId() == 123L;
		assert ent.getProperty("someString").equals("blah");
		assert ent.getProperty("someNumber").equals(456L);

		Trivial converted = ofy().load().fromEntity(ent);
		assert converted.getId().equals(triv.getId());
		assert converted.getSomeString().equals(triv.getSomeString());
		assert converted.getSomeNumber() == triv.getSomeNumber();
	}

}