/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * A trivial entity with some basic data.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trivial implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private Long id;

	@Index
	private String someString;

	private long someNumber;

	/** Constructor to use when autogenerating an id */
	public Trivial(String someString, long someNumber) {
		this(null, someString, someNumber);
	}
}