/*
 * $Id$
 * $URL$
 */

package com.googlecode.objectify.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.Apple;
import com.googlecode.objectify.test.entity.Banana;
import com.googlecode.objectify.test.entity.HolderOfString;
import com.googlecode.objectify.test.entity.HolderOfStringAndLong;

/**
 * Tests of basic entity manipulation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(EntityTests.class);

	/** */
	@Test
	public void testApple() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Apple a = new Apple(Apple.COLOR, Apple.TASTE);
		Key<Apple> aKey = ofy.put(a);
		Apple a2 = ofy.get(aKey);
		assert a2.getColor().equals(a.getColor()) : "Colors were different after stored/retrieved";
		assert a2.getSize().equals(a.getSize()) : "Sizes were different after stored/retrieved";
		assert a2.getTaste().equals(a.getTaste()) : "Tastes were different after stored/retrieved";
	}
	/** */
	@Test
	public void testBanana() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Banana b = new Banana(Banana.COLOR, Banana.TASTE);
		Key<Banana> bKey = ofy.put(b);
		Banana b2 = ofy.get(bKey);
		assert b2.getColor().equals(b.getColor()) : "Colors were different after stored/retrieved";
		assert b2.getShape().equals(b.getShape()) : "Shapes were different after stored/retrieved";
		assert b2.getTaste().equals(b.getTaste()) : "Tastes were different after stored/retrieved";
	}

	/** */
	@Test
	public void testStringHolder() throws Exception
	{
		Objectify ofy = this.fact.begin();
		String s = "my secret";
		HolderOfString hos = new HolderOfString(s);
		Key<HolderOfString> hosKey = ofy.put(hos);
		HolderOfString hos2 = ofy.get(hosKey);
		
		assert hos.getThing().equals(hos2.getMyThing()) : "Strings were different after stored/retrieved";
		assert hos.getThing().getClass().equals(hos2.getMyThing().getClass()) : "Classes were differnt";
	}

	/** */
	@Test
	public void testStringHolderWithALong() throws Exception
	{
		Objectify ofy = this.fact.begin();
		String s = "my secret";
		HolderOfStringAndLong hosal = new HolderOfStringAndLong(s,2L);
		Key<HolderOfStringAndLong> hosKey = ofy.put(hosal);
		HolderOfStringAndLong hosal2 = ofy.get(hosKey);
		
		assert hosal.getMyPrecious().equals(hosal2.getMyPrecious()) : "Longs were different after stored/retrieved";
		assert hosal.getThing().equals(hosal2.getMyThing()) : "Strings were different after stored/retrieved";
		assert hosal.getThing().getClass().equals(hosal2.getMyThing().getClass()) : "Classes were differnt";
	}

}