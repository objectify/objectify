/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

/**
 * A child entity which references a parent in the same entity group.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
public class Child
{
	@Id
	private Long id;
	public Long getId() { return this.id; }
	public void setId(Long value) { this.id = value; }

	@Parent
	private Key<Trivial> parent;
	public Key<Trivial> getParent() { return this.parent; }
	public void setParent(Key<Trivial> value) { this.parent = value; }

	private String childString;
	public String getChildString() { return this.childString; }
	public void setChildString(String value) { this.childString = value; }

	/** Default constructor must always exist */
	public Child() {}

	/** Constructor to use when autogenerating an id */
	public Child(Key<Trivial> parent, String childString)
	{
		this.parent = parent;
		this.childString = childString;
	}
}
