package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/** 
 * Deliberately confusing name.  It can't be an inner class because inner classes
 * have a different basename, and we had a bug relating to identical basenames for
 * field classes.  
 */
@Entity
public class User
{
	public @Id Long id;
	public com.google.appengine.api.users.User who;
}