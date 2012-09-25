/*
 */

package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Absolute simplest parent reference tests.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadParentTestsReallySimple extends TestBase
{
	/** */
	@Entity
	public static class Father {
		public @Id long id;
		
		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + id + ")";
		}
	}
	
	/** */
	@Entity
	public static class Child {
		public @Id long id;
		public @Load @Parent Father father;
		
		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + id + ", " + father + ")";
		}
	}
	
	/** */
	@Test
	public void testParentExists() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);
		
		TestObjectify ofy = fact.begin();
		
		Father f = new Father();
		f.id = 123L;
		ofy.put(f);
		
		Child ch = new Child();
		ch.id = 456L;
		ch.father = f;
		Key<Child> kch = ofy.put(ch);
		
		ofy.clear();
		
		Ref<Child> ref = ofy.load().key(kch);
		Child fetched = ref.get();
		
		assert fetched.father.id == f.id;
	}

	/** */
	@Test
	public void testParentMissing() throws Exception
	{
		fact.register(Father.class);
		fact.register(Child.class);
		
		TestObjectify ofy = fact.begin();
		
		Father f = new Father();
		f.id = 123L;
		// don't put
		
		Child ch = new Child();
		ch.id = 456L;
		ch.father = f;
		Key<Child> kch = ofy.put(ch);
		
		ofy.clear();
		
		Ref<Child> ref = ofy.load().key(kch);
		Child fetched = ref.get();
		
		assert fetched.father.id == f.id;
	}
}