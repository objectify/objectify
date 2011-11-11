/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests simple use of the getRef() methods on Objectify
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RefTests extends TestBase
{
	Trivial t1;
	Trivial t2;
	Key<Trivial> k1;
	Key<Trivial> k2;
	Key<Trivial> kNone;
	
	/** */
	@BeforeTest
	public void createTwo() {
		Objectify ofy = fact.begin();
		
		t1 = new Trivial("foo", 11);
		k1 = ofy.put(t1);
		
		t2 = new Trivial("bar", 22);
		k2 = ofy.put(t2);
		
		kNone = Key.create(Trivial.class, 12345L);
	}
	
//	/** */
//	@Test
//	public void testGet() throws Exception {
//		Objectify ofy = fact.begin();
//		
//		Ref<Trivial> ref = Ref.create(k1);
//
//		ofy.getRef(ref);
//		assert ref.value().getSomeString().equals(t1);
//		
//		try {
//			ofy.getRef(Ref.create(kNone));
//			assert false;
//		} catch (NotFoundException ex) {}
//	}

	/** */
	@Test
	public void testFind() throws Exception {
		Objectify ofy = fact.begin();
		
		Ref<Trivial> ref = Ref.create(k1);

		ofy.findRef(ref);
		assert ref.value().getSomeString().equals(t1.getSomeString());
		
		Ref<Trivial> refNone = Ref.create(kNone);
		assert refNone.value() == null;
	}

	/** */
	@Test
	public void testGetRefsVarargs() throws Exception {
		Objectify ofy = fact.begin();
		
		Ref<Trivial> ref1 = Ref.create(k1);
		Ref<Trivial> ref2 = Ref.create(k2);
		Ref<Trivial> refNone = Ref.create(kNone);

		ofy.getRefs(ref1, ref2, refNone);
		
		assert ref1.value().getSomeString().equals(t1.getSomeString());
		assert ref2.value().getSomeString().equals(t2.getSomeString());
		assert refNone.value() == null;
	}

	/** */
	@Test
	public void testGetRefsIterable() throws Exception {
		Objectify ofy = fact.begin();
		
		Ref<Trivial> ref1 = Ref.create(k1);
		Ref<Trivial> ref2 = Ref.create(k2);
		Ref<Trivial> refNone = Ref.create(kNone);

		List<Ref<Trivial>> list = new ArrayList<Ref<Trivial>>();
		list.add(ref1);
		list.add(ref2);
		list.add(refNone);
		
		ofy.getRefs(list);
		
		assert ref1.value().getSomeString().equals(t1.getSomeString());
		assert ref2.value().getSomeString().equals(t2.getSomeString());
		assert refNone.value() == null;
	}
}