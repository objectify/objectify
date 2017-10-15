/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A trivial entity with some basic data.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
@Data
@NoArgsConstructor
public class NamedTrivial {
	@Id private String name;

	private String someString;

	@Unindex
	private long someNumber;

	/** You cannot autogenerate a name */
	public NamedTrivial(String id, String someString, long someNumber) {
		this.name = id;
		this.someNumber = someNumber;
		this.someString = someString;
	}
}