package com.googlecode.objectify.test.entity;

import java.util.List;
import java.util.Set;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * Entity for testing null/empty embedded arrays and collections
 */
@Entity
@Cache
public class Criminal
{
	@Id
	public Long id;

	public Name[] aliases;

	public List<Name> moreAliases;

	public Set<Name> aliasesSet;

	public Criminal()
	{
	}
}
