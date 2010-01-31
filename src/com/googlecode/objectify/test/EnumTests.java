/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.HasEnums;
import com.googlecode.objectify.test.entity.HasEnums.Color;

/**
 * Tests of Enums, including Enums in arrays and lists
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EnumTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(EnumTests.class);
	
	/** */
	@Test
	public void testSimpleEnum() throws Exception
	{
		Objectify ofy = this.fact.begin();

		HasEnums he = new HasEnums();
		he.color = Color.RED;
		Key<HasEnums> key = ofy.put(he);
		
		he = ofy.get(key);
		assert he.color == Color.RED;
	}

	/** */
	@Test(groups={"now"})
	public void testEnumsList() throws Exception
	{
		Objectify ofy = this.fact.begin();

		HasEnums he = new HasEnums();
		he.colors = Arrays.asList(Color.RED, Color.GREEN);
		Key<HasEnums> key = ofy.put(he);
		
		he = ofy.get(key);
		assert he.colors.get(0).equals(Color.RED) : "Expected RED got " + he.colors.get(0);
		assert he.colors.get(1).equals(Color.GREEN) : "Expected GREEN got " + he.colors.get(1);
	}

	/** */
	@Test
	public void testEnumsArray() throws Exception
	{
		Objectify ofy = this.fact.begin();

		HasEnums he = new HasEnums();
		he.colorsArray = new Color[] { Color.RED, Color.GREEN };
		Key<HasEnums> key = ofy.put(he);
		
		he = ofy.get(key);
		assert he.colorsArray[0] == Color.RED;
		assert he.colorsArray[1] == Color.GREEN;
	}
}