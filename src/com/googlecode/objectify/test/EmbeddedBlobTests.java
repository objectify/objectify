/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.annotation.Cached;

/**
 * Testing what you can and can not do with @Embedded blobs like Text and Blob
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedBlobTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(EmbeddedBlobTests.class.getName());
	
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
	
	/** */
	@Cached
	public static class HasEmbeddedText
	{
		public @Id Long id;
		public EmbeddedText stuff;
	}
	
	/** */
	@Cached
	public static class HasEmbeddedBlob
	{
		public @Id Long id;
		public EmbeddedBlob stuff;
	}
	
	/** */
	@Cached
	public static class HasEmbeddedTextArray
	{
		public @Id Long id;
		public EmbeddedText[] stuff;
	}
	
	/** */
	@Cached
	public static class HasEmbeddedBlobArray
	{
		public @Id Long id;
		public EmbeddedBlob[] stuff;
	}
	
	/** */
	@Cached
	public static class HasEmbeddedTextList
	{
		public @Id Long id;
		public List<EmbeddedText> stuff;
	}
	
	/** */
	@Cached
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
		// These are ok
		this.fact.register(HasEmbeddedText.class);
		this.fact.register(HasEmbeddedBlob.class);
		this.fact.register(HasEmbeddedTextArray.class);
		this.fact.register(HasEmbeddedBlobArray.class);
		this.fact.register(HasEmbeddedTextList.class);
		this.fact.register(HasEmbeddedBlobList.class);
		
		// These are not bad anymore...
//		this.assertRegistrationFailure(HasEmbeddedTextArray.class);
//		this.assertRegistrationFailure(HasEmbeddedBlobArray.class);
//		this.assertRegistrationFailure(HasEmbeddedTextList.class);
//		this.assertRegistrationFailure(HasEmbeddedBlobList.class);
	}
}