package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Embedded;

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
	public Person mayor;

	@Embedded
	public Person[] folk;
}
