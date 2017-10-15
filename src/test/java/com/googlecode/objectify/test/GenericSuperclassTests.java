/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 */
class GenericSuperclassTests extends TestBase {

	/**
	 * A holder of a <T>hing.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	@Data
	@NoArgsConstructor
	private static class Holder<T> {
		@Id private Long id;
		private T thing;

		protected Holder(T t) {this.thing = t;}
	}

	/**
	 * A holder of a string.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class HolderOfString extends Holder<String> {
		public HolderOfString(String s) {super(s);}
	}

	/** */
	@Test
	void worksWithGenericSuperclass() throws Exception {
		factory().register(HolderOfString.class);

		final HolderOfString hos = new HolderOfString("foobar");
		final HolderOfString fetched = saveClearLoad(hos);

		assertThat(fetched).isEqualTo(hos);
	}

	/**
	 * A holder of a string, and a Long.
	 *
	 * @author Scott Hernandez
	 */
	@Entity
	@Cache
	@Data
	@EqualsAndHashCode(callSuper = true)
	@NoArgsConstructor
	private static class HolderOfStringAndLong extends HolderOfString {
		private Long myPrecious;

		public HolderOfStringAndLong(String s, Long l) {super(s); this.myPrecious = l; }
	}

	/** */
	@Test
	void worksWithGenericSuperSuperclass() throws Exception {
		factory().register(HolderOfStringAndLong.class);

		final HolderOfStringAndLong hosal = new HolderOfStringAndLong("foobar",2L);
		final HolderOfStringAndLong fetched = saveClearLoad(hosal);

		assertThat(fetched).isEqualTo(hosal);
	}
}