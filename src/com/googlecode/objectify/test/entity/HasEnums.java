/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import java.util.List;

import javax.persistence.Id;

/**
 * An entity that has an enum type.
 * Left off getters and setters for convenience.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class HasEnums
{
	public enum Color {
		RED,
		GREEN
	}
	
	public @Id Long id;
	
	public Color color;
	public List<Color> colors;
	public Color[] colorsArray;
}