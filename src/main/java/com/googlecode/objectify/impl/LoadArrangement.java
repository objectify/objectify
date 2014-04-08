package com.googlecode.objectify.impl;

import com.googlecode.objectify.annotation.Load;

import java.util.HashSet;
import java.util.Set;

/** 
 * Simple typedef to keep the code sane. A set of load groups that has been used to load entities.
 */
public class LoadArrangement extends HashSet<Class<?>>
{
}