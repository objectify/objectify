/*
 * $Id: Apple.java 319 2010-02-09 02:33:41Z lhoriman $
 * $URL: https://objectify-appengine.googlecode.com/svn/trunk/src/com/googlecode/objectify/test/entity/Apple.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;


/**
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings("unused")
@Cached
@Unindexed
public class UnindexedPojo
{
	@Id Long id;
	@Indexed private boolean indexed = true;
	@Unindexed private boolean unindexed = true;
	private boolean def = true;
}