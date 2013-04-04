/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of basic entity manipulation variation (this test uses Key<?> and com.google.appengine.api.datastore.Key instances as Ids.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityWithKeyTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(EntityWithKeyTests.class.getName());

	/**
	 * A fruit.
	 * 
	 * @author Bryce Cottam (variant of original tests written by Scott Hernandez)
	 */
	@Entity
	@Cache
	static abstract class Fruit
	{
		// we're mainly testing the Key<?> id's here:
		@Id Key<? extends Fruit> id;
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
		fact.register(Apple.class);
		TestObjectify ofy = this.fact.begin();
		Apple a = new Apple(Apple.COLOR, Apple.TASTE);
		Key<Apple> aKey = ofy.put(a);
		
		assert aKey != null : "Key was not generated for an Apple created without a key";
		Apple a2 = ofy.get(aKey);
		assert a2.getColor().equals(a.getColor()) : "Colors were different after stored/retrieved";
		assert a2.getSize().equals(a.getSize()) : "Sizes were different after stored/retrieved";
		assert a2.getTaste().equals(a.getTaste()) : "Tastes were different after stored/retrieved";
	}

	@Test
	public void testAppleWithPresetStringId() throws Exception
	{
		fact.register(Apple.class);
		TestObjectify ofy = this.fact.begin();
		Apple a = new Apple(Apple.COLOR, Apple.TASTE);
		Key<Apple> preKey = Key.create(Apple.class, "myApple");
		a.id = preKey;
		Key<Apple> aKey = ofy.put(a);
		
		assert preKey.equals(aKey) : "Apple didn't keep the pre-assigned id";
		Apple a2 = ofy.get(aKey);
		assert a2.getColor().equals(a.getColor()) : "Colors were different after stored/retrieved";
		assert a2.getSize().equals(a.getSize()) : "Sizes were different after stored/retrieved";
		assert a2.getTaste().equals(a.getTaste()) : "Tastes were different after stored/retrieved";
	}
	
	@Test
	public void testAppleWithPresetLongId() throws Exception
	{
		fact.register(Apple.class);
		TestObjectify ofy = this.fact.begin();
		Apple a = new Apple(Apple.COLOR, Apple.TASTE);
		Key<Apple> preKey = Key.create(Apple.class, 50);
		a.id = preKey;
		Key<Apple> aKey = ofy.put(a);
		
		assert preKey.equals(aKey) : "Apple didn't keep the pre-assigned id";
		Apple a2 = ofy.get(aKey);
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
		fact.register(Banana.class);
		TestObjectify ofy = this.fact.begin();
		Banana b = new Banana(Banana.COLOR, Banana.TASTE);
		Key<Banana> bKey = ofy.put(b);
		Banana b2 = ofy.get(bKey);
		assert b2.getColor().equals(b.getColor()) : "Colors were different after stored/retrieved";
		assert b2.getShape().equals(b.getShape()) : "Shapes were different after stored/retrieved";
		assert b2.getTaste().equals(b.getTaste()) : "Tastes were different after stored/retrieved";
	}

	/**
	 * A Parent of a <T>hing.
	 * 
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	static class ParentThing
	{	
		@Id Key<ParentThing> id;
		private String name;
		
		public ParentThing() {}
		public ParentThing(String name) { this.name = name; }
		
		public String getName() { return name; }
	}
	
	
	@Entity
	@Cache
	static class Child
	{
		@Id Key<Child> id;
		@Parent Key<ParentThing> parentId;
		
		private String name;
		public Child() {  }
		
		public Child(Key<ParentThing> parentId, String name)
		{
			this.parentId = parentId;
			this.name = name;
		}
		
		public String getName() { return name; }
	}
	
	@Test
	public void testParentAndChild()
	{
		fact.register(ParentThing.class);
		fact.register(Child.class);
		
		TestObjectify ofy = this.fact.begin();
		ParentThing parent = new ParentThing("the parent");
		Key<ParentThing> parentId = ofy.put(parent);
		
		Child child = new Child(parentId, "the child");
		Key<Child> childId = ofy.put(child);
		
		assert childId.getParent() != null : "Child id did not get linked to the Parent";
		assert childId.getRaw().getParent() != null : "Raw Child id did not get linked to the raw Parent";
		assert childId.getParent().equals(parentId) : "Child id got linked to the wrong Parent";
		assert childId.getRaw().getParent().equals(parentId.getRaw()) : "Raw Child id got linked to the wrong raw Parent";
	}
	
	@Test
	public void testParentAndChildWithPreDefinedIds()
	{
		fact.register(ParentThing.class);
		fact.register(Child.class);
		
		TestObjectify ofy = this.fact.begin();
		ParentThing parent = new ParentThing("the parent");
		Key<ParentThing> generatedParentId = Key.create(ParentThing.class, "parentId");
		parent.id = generatedParentId;
		Key<ParentThing> parentId = ofy.put(parent);
		
		Child child = new Child(parentId, "the child");
		Key<Child> generatedChildId = Key.create(generatedParentId, Child.class, 12);
		child.id = generatedChildId;
		
		Key<Child> childId = ofy.put(child);
		
		assert childId.getParent() != null : "Child id did not get linked to the Parent";
		assert childId.getRaw().getParent() != null : "Raw Child id did not get linked to the raw Parent";
		assert childId.getParent().equals(parentId) : "Child id got linked to the wrong Parent";
		assert childId.getRaw().getParent().equals(parentId.getRaw()) : "Raw Child id got linked to the wrong raw Parent";
	}
	
	@Test
	public void testParentAndChildWithPreDefinedIdsWithoutSpecifyingParent()
	{
		fact.register(ParentThing.class);
		fact.register(Child.class);
		
		TestObjectify ofy = this.fact.begin();
		ParentThing parent = new ParentThing("the parent");
		Key<ParentThing> generatedParentId = Key.create(ParentThing.class, "parentId");
		parent.id = generatedParentId;
		Key<ParentThing> parentId = ofy.put(parent);
		
		Child child = new Child(parentId, "the child");
		Key<Child> generatedChildId = Key.create(Child.class, 12);
		child.id = generatedChildId;
		
		Key<Child> childId = ofy.put(child);
		
		assert childId.getParent() != null : "Child id did not get linked to the Parent";
		assert childId.getRaw().getParent() != null : "Raw Child id did not get linked to the raw Parent";
		assert childId.getParent().equals(parentId) : "Child id got linked to the wrong Parent";
		assert childId.getRaw().getParent().equals(parentId.getRaw()) : "Raw Child id got linked to the wrong raw Parent";
	}
	
	@Test(expectedExceptions={SaveException.class})
	public void testParentAndChildWithPreDefinedIdsWithWrongParent()
	{
		fact.register(ParentThing.class);
		fact.register(Child.class);
		
		TestObjectify ofy = this.fact.begin();
		ParentThing parent = new ParentThing("the parent");
		Key<ParentThing> generatedParentId = Key.create(ParentThing.class, "parentId");
		parent.id = generatedParentId;
		Key<ParentThing> parentId = ofy.put(parent);
		
		ParentThing wrongParent = new ParentThing("the wrong parent");
		Key<ParentThing> wrongGeneratedParentId = Key.create(ParentThing.class, "wrongParentId");
		wrongParent.id = wrongGeneratedParentId;
		Key<ParentThing> wrongParentId = ofy.put(wrongParent);
		
		Child child = new Child(parentId, "the child");
		Key<Child> generatedChildId = Key.create(wrongParentId, Child.class, 12);
		child.id = generatedChildId;
		
		Key<Child> childId = ofy.put(child);
		
		assert childId.getParent() != null : "Child id did not get linked to the Parent";
		assert childId.getRaw().getParent() != null : "Raw Child id did not get linked to the raw Parent";
		assert !childId.getParent().equals(wrongParentId) : "Child id got linked to the wrong Parent";
		assert !childId.getRaw().getParent().equals(wrongParentId.getRaw()) : "Raw Child id got linked to the wrong raw Parent";
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
		@Id com.google.appengine.api.datastore.Key id;
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
	public void testStringHolderWithPresetStringId() throws Exception
	{
		fact.register(HolderOfString.class);
		TestObjectify ofy = this.fact.begin();
		String s = "my secret";
		HolderOfString hos = new HolderOfString(s);
		com.google.appengine.api.datastore.Key key = KeyFactory.createKey(HolderOfString.class.getSimpleName(), "theFirstHolder");
		hos.id = key;
		
		Key<HolderOfString> hosKey = ofy.put(hos);
		HolderOfString hos2 = ofy.get(hosKey);
		
		assert key.equals(hosKey.getRaw()) : "The raw Key is not equal to the preset raw key";
		assert hos.getThing().equals(hos2.getMyThing()) : "Strings were different after stored/retrieved";
		assert hos.getThing().getClass().equals(hos2.getMyThing().getClass()) : "Classes were differnt";
	}
	
	/** */
	@Test
	public void testStringHolderWithPresetLongId() throws Exception
	{
		fact.register(HolderOfString.class);
		TestObjectify ofy = this.fact.begin();
		String s = "my secret";
		HolderOfString hos = new HolderOfString(s);
		com.google.appengine.api.datastore.Key key = KeyFactory.createKey(HolderOfString.class.getSimpleName(), 20);
		hos.id = key;
		
		Key<HolderOfString> hosKey = ofy.put(hos);
		HolderOfString hos2 = ofy.get(hosKey);
		
		assert key.equals(hosKey.getRaw()) : "The raw Key is not equal to the preset raw key";
		assert hos.getThing().equals(hos2.getMyThing()) : "Strings were different after stored/retrieved";
		assert hos.getThing().getClass().equals(hos2.getMyThing().getClass()) : "Classes were differnt";
	}
	
	/** */
	@Test
	public void testStringHolder() throws Exception
	{
		fact.register(HolderOfString.class);
		TestObjectify ofy = this.fact.begin();
		String s = "my secret";
		HolderOfString hos = new HolderOfString(s);
		Key<HolderOfString> hosKey = ofy.put(hos);
		HolderOfString hos2 = ofy.get(hosKey);
		
		assert hos.id instanceof com.google.appengine.api.datastore.Key : "the value of HolderOfString.id was changed";
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
		fact.register(HolderOfStringAndLong.class);
		TestObjectify ofy = this.fact.begin();
		String s = "my secret";
		HolderOfStringAndLong hosal = new HolderOfStringAndLong(s,2L);
		Key<HolderOfStringAndLong> hosKey = ofy.put(hosal);
		HolderOfStringAndLong hosal2 = ofy.get(hosKey);
		
		assert hosal.getMyPrecious().equals(hosal2.getMyPrecious()) : "Longs were different after stored/retrieved";
		assert hosal.getThing().equals(hosal2.getMyThing()) : "Strings were different after stored/retrieved";
		assert hosal.getThing().getClass().equals(hosal2.getMyThing().getClass()) : "Classes were differnt";
	}

	/** */
	@Test
	public void testToPojoAndBack() throws Exception
	{
		fact.register(Trivial.class);
		TestObjectify ofy = this.fact.begin();
		
		Trivial triv = new Trivial(123L, "blah", 456);
		
		com.google.appengine.api.datastore.Entity ent = ofy.toEntity(triv);
		assert ent.getKey().getId() == 123L;
		assert ent.getProperty("someString").equals("blah");
		assert ent.getProperty("someNumber").equals(456L);
		
		Trivial converted = ofy.toPojo(ent);
		assert converted.getId().equals(triv.getId());
		assert converted.getSomeString().equals(triv.getSomeString());
		assert converted.getSomeNumber() == triv.getSomeNumber();
	}

}