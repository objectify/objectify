/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of type conversions.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class JodaTranslationTests extends TestBase {

	/** */
	@Entity
	@Cache
	@Data
	private static class HasJoda {
		private @Id Long id;
		private LocalTime localTime;
		private LocalDate localDate;
		private LocalDateTime localDateTime;
		private DateTime dateTime;
		private DateTimeZone dateTimeZone;
		private YearMonth yearMonth;
	}

	/** */
	@Test
	void joda() throws Exception {
		JodaTimeTranslators.add(factory());
		factory().register(HasJoda.class);

		final HasJoda hj = new HasJoda();
		hj.localTime = new LocalTime();
		hj.localDate = new LocalDate();
		hj.localDateTime = new LocalDateTime();
		hj.dateTime = new DateTime();
		hj.dateTimeZone = DateTimeZone.forID("America/Los_Angeles");
		hj.yearMonth = new YearMonth();

		final HasJoda fetched = saveClearLoad(hj);

		assertThat(fetched).isEqualTo(hj);
	}
}