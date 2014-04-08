package com.googlecode.objectify.impl;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.IgnoreLoad;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

/** 
 * Some common behavior of properties
 */
abstract public class AbstractProperty implements Property
{
	String name;
	String[] names;
	Annotation[] annotations;
	
	/** */
	public AbstractProperty(String name, Annotation[] annotations, Object thingForDebug) {
		this.name = name;
		this.annotations = annotations;

		// Figure out names from the @IgnoreLoad and @AlsoLoad annotations
		Set<String> nameSet = new LinkedHashSet<String>();
		
		// If we have @IgnoreLoad, don't add priamry name to the names collection (which is used for loading)
		if (this.getAnnotation(IgnoreLoad.class) == null)
			nameSet.add(name);
		
		// Now any additional names
		AlsoLoad alsoLoad = this.getAnnotation(AlsoLoad.class);
		if (alsoLoad != null)
			if (alsoLoad.value() == null || alsoLoad.value().length == 0)
				throw new IllegalStateException("If specified, @AlsoLoad must specify at least one value: " + thingForDebug);
			else
				for (String value: alsoLoad.value())
					if (value == null || value.trim().length() == 0)
						throw new IllegalStateException("Illegal empty value in @AlsoLoad for " + thingForDebug);
					else
						nameSet.add(value);
		
		names = nameSet.toArray(new String[nameSet.size()]);
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String[] getLoadNames() {
		return names;
	}
	
	@Override
	public <A extends Annotation> A getAnnotation(Class<A> annoClass) {
		return TypeUtils.getAnnotation(annotations, annoClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return annotations;
	}
}