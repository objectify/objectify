/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.EmbeddedIndexedPojo;
import com.googlecode.objectify.test.entity.IndexedDefaultPojo;
import com.googlecode.objectify.test.entity.IndexedPojo;
import com.googlecode.objectify.test.entity.UnindexedPojo;

/**
 * Tests of various queries
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IndexingTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(IndexingTests.class);

	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		Objectify ofy = this.fact.begin();
		ofy.put(new IndexedPojo());
		ofy.put(new IndexedDefaultPojo());
		ofy.put(new UnindexedPojo());
		ofy.put(new EmbeddedIndexedPojo());
	}	
	
	/** */
	@Test
	public void testIndexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();

		assert ofy.query(IndexedPojo.class).filter("indexed =", true).fetch().iterator().hasNext();
		assert ofy.query(IndexedPojo.class).filter("def =", true).fetch().iterator().hasNext();
		assert !ofy.query(IndexedPojo.class).filter("unindexed =", true).fetch().iterator().hasNext();
		
	}
	/** */
	@Test
	public void testUnindexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();

		assert ofy.query(UnindexedPojo.class).filter("indexed =", true).fetch().iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("def =", true).fetch().iterator().hasNext();
		assert !ofy.query(UnindexedPojo.class).filter("unindexed =", true).fetch().iterator().hasNext();
		
	}
	/** */
	@Test
	public void testIndexedDefaultPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();

		assert ofy.query(IndexedDefaultPojo.class).filter("indexed =", true).fetch().iterator().hasNext();
		assert ofy.query(IndexedDefaultPojo.class).filter("def =", true).fetch().iterator().hasNext();
		assert !ofy.query(IndexedDefaultPojo.class).filter("unindexed =", true).fetch().iterator().hasNext();
		
	}
	/** */
	@Test
	public void testEmbeddedIndexedPojo() throws Exception
	{
		Objectify ofy = this.fact.begin();

		assert  ofy.query(EmbeddedIndexedPojo.class).filter("indexed.indexed =", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("indexed.def =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("indexed.unindexed=", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("def.indexed =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("def.unindexed =", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("def.def =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.unindexed =", true).fetch().iterator().hasNext();
		assert  ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.indexed =", true).fetch().iterator().hasNext();
		assert !ofy.query(EmbeddedIndexedPojo.class).filter("unindexed.def =", true).fetch().iterator().hasNext();
		
	}

}