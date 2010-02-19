/*
 * $Id: Apple.java 319 2010-02-09 02:33:41Z lhoriman $
 * $URL: https://objectify-appengine.googlecode.com/svn/trunk/src/com/googlecode/objectify/test/entity/Apple.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Embedded;
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
public class EmbeddedIndexedPojo
{
	@Id Long id;

	@Unindexed 				private boolean aProp = true;
	
	@Indexed 	@Embedded 	private IndexedDefaultPojo[] indexed = {new IndexedDefaultPojo()};
	@Unindexed 	@Embedded 	private IndexedDefaultPojo[] unindexed = {new IndexedDefaultPojo()};
				@Embedded 	private IndexedDefaultPojo[] def = {new IndexedDefaultPojo()};

// Fundamentally broken; how to test bad-hetro behavior?

//	@Indexed 	@Embedded 	private List indexedHetro = new ArrayList();
//	@Unindexed 	@Embedded 	private List unindexedHetro = new ArrayList();
//				@Embedded 	private List defHetro = new ArrayList();
//	
//	public EmbeddedIndexedPojo(){
//		indexedHetro.add(new IndexedDefaultPojo());
//		indexedHetro.add(new IndexedPojo());
//		
//		unindexedHetro.addAll(indexedHetro);
//		defHetro.addAll(indexedHetro);
//	}
}