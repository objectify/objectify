package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Indicates that a class is part of a polymorphic persistence hierarchy. Subclasses
 * of an @Entity should be flagged with this annotation.</p>
 * 
 * <p>This is used for Objectify's implementation of polymorphism.  Place this on any
 * class in an inheritance hierarchy that should be queryable <strong>except the root</strong>.
 * For example, in the hierarchy Animal->Mammal->Cat, annotations should be:</p>
 * <ul>
 * <li>@Entity Animal</li>
 * <li>@Subclass(index=true) Mammal</li>
 * <li>@Subclass(index=true) Cat</li>
 * </ul>
 * 
 * <p>The @Entity annotation must be present on the class that identifies the root of the
 * hierarchy.  This class will define the <em>kind</em> of the entire hierarchy.
 * The @Entity annotation must NOT be present on any subclasses.</p>
 * 
 * <p>Actual Java subclasses are not required to have @Subclass, but only Java classes
 * which have @Subclass can be persisted and queried for.  Note that subclass discriminators
 * are not indexed by default, so if you want to query for specific types of subclasses, use @Subclass(index=true).</p>
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
	 * <p>If true, the discriminator will be indexed, and a query for the specific subclass will
	 * return results.  However, superclasses and further subclasses may be have different index states.</p>
	 * 
	 * <p>This is NOT the same as putting @Index on a class; that sets the default index state of the
	 * fields of that class.  This attribute only controls indexing of the discriminator.</p>
	 */
	boolean index() default false;
	
	/**
	 * Additional discriminators which, when encountered, will be interpreted as indicating
	 * this subclass.  Facilitates schema changes in a way analagous to @AlsoLoad.
	 */
	String[] alsoLoad() default {};
}