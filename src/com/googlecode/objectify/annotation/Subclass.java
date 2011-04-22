package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Indicates that a class is part of a polymorphic persistence hierarchy.  Subclasses
 * of an @Entity should be flagged with this annotation.</p>
 * 
 * <p>This is used for Objectify's implementation of polymorphism.  Place this on any
 * class in an inheritance hierarchy that should be queryable <strong>except the root</strong>.
 * For example, in the hierarchy Animal->Mammal->Cat, annotations should be:</p>
 * <ul>
 * <li>@Entity Animal</li>
 * <li>@Subclass Mammal</li>
 * <li>@Subclass Cat</li>
 * </ul>
 * 
 * <p>The @Entity annotation must be present on the class that identifies the root of the
 * hierarchy.  This class will define the <em>kind</em> of the entire hierarchy.
 * The @Entity annotation must NOT be present on any subclasses.</p>
 * 
 * <p>Actual Java subclasses are not required to have @Subclass, but only Java classes
 * which have @Subclass can be persisted and queried for.</p>
 * 
 * @author Jeff Schnitzer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Subclass
{
	/** 
	 * Optionally define the discriminator value for the subclass; default is Class.getSimpleName() 
	 */
	String name() default "";
	
	/** 
	 * If true, the discriminator will not be indexed, and a query for the specific subclass will
	 * not return results.  However, superclasses and further subclasses may be indexed.
	 */
	boolean unindexed() default false;
	
	/**
	 * Additional discriminators which, when encountered, will be interpreted as indicating
	 * this subclass.  Facilitates schema changes in a way analagous to @AlsoLoad.
	 */
	String[] alsoLoad() default {};
}