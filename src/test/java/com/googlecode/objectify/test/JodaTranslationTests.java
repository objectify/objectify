/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.testng.annotations.Test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Tests of type conversions.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class JodaTranslationTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(JodaTranslationTests.class.getName());

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasJoda
	{
		public @Id Long id;
		public LocalTime localTime;
		public LocalDate localDate;
		public LocalDateTime localDateTime;
		public DateTime dateTime;
		public DateTimeZone dateTimeZone;
	}

	/** */
	@Test
	public void joda() throws Exception
	{
		JodaTimeTranslators.add(this.fact);
		this.fact.register(HasJoda.class);

		HasJoda hj = new HasJoda();
		hj.localTime = new LocalTime();
		hj.localDate = new LocalDate();
		hj.localDateTime = new LocalDateTime();
		hj.dateTime = new DateTime();
		hj.dateTimeZone = DateTimeZone.forID("America/Los_Angeles");

		HasJoda fetched = this.putClearGet(hj);
		assert hj.localTime.equals(fetched.localTime);
		assert hj.localDate.equals(fetched.localDate);
		assert hj.localDateTime.equals(fetched.localDateTime);
		assert hj.dateTime.equals(fetched.dateTime);
		assert hj.dateTimeZone.equals(fetched.dateTimeZone);
	}
}