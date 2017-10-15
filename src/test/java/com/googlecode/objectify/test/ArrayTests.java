/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * Tests of persisting arrays
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class ArrayTests extends TestBase {

	/** */
	@BeforeEach
	void setUpArrayTests()
	{
		factory().register(HasArrays.class);
	}

	@Entity
	@Cache
	@Data
	private static class HasArrays {
		@Id
		private Long id;

		private String[] strings;

		@Unindex
		private long[] longs;

		@Unindex
		private int[] ints;

		@Unindex
		private Integer[] integers;
	}

	/** */
	@Test
	void stringArraysWork() throws Exception {
		final HasArrays hasa = new HasArrays();
		hasa.strings = new String[] { "red", "green" };

		final HasArrays fetched = saveClearLoad(hasa);

		assertThat(fetched.strings).isEqualTo(hasa.strings);
	}

	/** */
	@Test
	void intArraysWork() throws Exception {
		final HasArrays hasa = new HasArrays();
		hasa.ints = new int[] { 5, 6 };

		final HasArrays fetched = saveClearLoad(hasa);

		assertThat(fetched.ints).isEqualTo(hasa.ints);
	}

	/** */
	@Test
	void integerArraysWork() throws Exception {
		final HasArrays hasa = new HasArrays();
		hasa.integers = new Integer[] { 5, 6 };

		final HasArrays fetched = saveClearLoad(hasa);

		assertThat(fetched.integers).isEqualTo(hasa.integers);
	}

	/** */
	@Test
	void longArraysWork() throws Exception {
		final HasArrays hasa = new HasArrays();
		hasa.longs = new long[] { 5, 6 };

		final HasArrays fetched = saveClearLoad(hasa);

		assertThat(fetched.longs).isEqualTo(hasa.longs);
	}
}