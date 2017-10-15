/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A child entity which references a parent in the same entity group.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
@Data
@NoArgsConstructor
public class Child {
	@Id
	private Long id;

	@Parent
	private Key<Trivial> parent;

	private String childString;

	/** Constructor to use when autogenerating an id */
	public Child(Key<Trivial> parent, String childString) {
		this.parent = parent;
		this.childString = childString;
	}
}