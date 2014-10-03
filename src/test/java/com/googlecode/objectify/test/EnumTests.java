/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.test.EnumTests.HasEnums.Color;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of Enums, including Enums in arrays and lists
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EnumTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(EnumTests.class.getName());

	/** */
	@Entity
	@Cache
	@Index
	public static class HasEnums
	{
		public enum Color {
			RED,
			GREEN
		}

		public @Id Long id;

		public Color color;
		public List<Color> colors;
		public Color[] colorsArray;
	}

	/** */
	@BeforeMethod
	public void setUpExtra()
	{
		fact().register(HasEnums.class);
	}

	/** */
	@Test
	public void testSimpleEnum() throws Exception
	{
		HasEnums he = new HasEnums();
		he.color = Color.RED;
		Key<HasEnums> key = ofy().save().entity(he).now();

		he = ofy().load().key(key).now();
		assert he.color == Color.RED;
	}

	/** */
	@Test(groups={"now"})
	public void testEnumsList() throws Exception
	{
		HasEnums he = new HasEnums();
		he.colors = Arrays.asList(Color.RED, Color.GREEN);
		Key<HasEnums> key = ofy().save().entity(he).now();

		he = ofy().load().key(key).now();
		assert he.colors.get(0).equals(Color.RED) : "Expected RED got " + he.colors.get(0);
		assert he.colors.get(1).equals(Color.GREEN) : "Expected GREEN got " + he.colors.get(1);
	}

	/** */
	@Test
	public void testEnumsArray() throws Exception
	{
		HasEnums he = new HasEnums();
		he.colorsArray = new Color[] { Color.RED, Color.GREEN };
		Key<HasEnums> key = ofy().save().entity(he).now();

		he = ofy().load().key(key).now();
		assert he.colorsArray[0] == Color.RED;
		assert he.colorsArray[1] == Color.GREEN;
	}

	/** */
	@Test
	public void testFilterByEnum() throws Exception
	{
		HasEnums he = new HasEnums();
		he.color = Color.GREEN;
		ofy().save().entity(he).now();

		HasEnums fetched = ofy().load().type(HasEnums.class).filter("color =", Color.GREEN).first().now();
		assert fetched.id.equals(he.id);
	}
}