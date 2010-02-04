/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.List;

import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;

/**
 * Testing what you can and can not do with @Embedded blobs like Text and Blob
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedBlobTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(EmbeddedBlobTests.class);
	
	/** */
	public static class EmbeddedText
	{
		public Text text;
	}
	
	/** */
	public static class EmbeddedBlob
	{
		public Blob blob;
	}
	
	/** This should work */
	public static class HasEmbeddedText
	{
		public @Id Long id;
		public EmbeddedText stuff;
	}
	
	/** This should work */
	public static class HasEmbeddedBlob
	{
		public @Id Long id;
		public EmbeddedBlob stuff;
	}
	
	/** This should fail when you try to register it */
	public static class HasEmbeddedTextArray
	{
		public @Id Long id;
		public EmbeddedText[] stuff;
	}
	
	/** This should fail when you try to register it */
	public static class HasEmbeddedBlobArray
	{
		public @Id Long id;
		public EmbeddedBlob[] stuff;
	}
	
	/** This should fail when you try to register it */
	public static class HasEmbeddedTextList
	{
		public @Id Long id;
		public List<EmbeddedText> stuff;
	}
	
	/** This should fail when you try to register it */
	public static class HasEmbeddedBlobList
	{
		public @Id Long id;
		public List<EmbeddedBlob> stuff;
	}
	
	/**
	 * Fails an assertion if the class registers successfully
	 */
	protected void assertRegistrationFailure(Class<?> clazz)
	{
		try
		{
			this.fact.register(clazz);
			assert false : "You should not have been able to register " + clazz;
		}
		catch (IllegalStateException ex)
		{
			// good
		}
	}
	
	/**
	 * Some of these should be registerable, some not
	 */
	@Test
	public void testRegistration() throws Exception
	{
		// These are ok, no collections
		this.fact.register(HasEmbeddedText.class);
		this.fact.register(HasEmbeddedBlob.class);
		
		// These are all bad
		this.assertRegistrationFailure(HasEmbeddedTextArray.class);
		this.assertRegistrationFailure(HasEmbeddedBlobArray.class);
		this.assertRegistrationFailure(HasEmbeddedTextList.class);
		this.assertRegistrationFailure(HasEmbeddedBlobList.class);
	}
}