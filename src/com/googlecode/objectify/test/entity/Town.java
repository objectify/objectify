package com.googlecode.objectify.test.entity;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 */
@Entity
public class Town
{
	@Id
	public Long id;

	public String name;
	
	@Embedded
	Industry industry;

	@Embedded
	public Person mayor;

	@Embedded
	public Person[] folk;
}
