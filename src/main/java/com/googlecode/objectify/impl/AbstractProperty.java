package com.googlecode.objectify.impl;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.IgnoreLoad;
import com.googlecode.objectify.annotation.Load;

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
	
	/** The states are important - null means none, empty means "all" */
	Class<?>[] loadGroups;
	
	/** This will never be empty - either null or have some values */
	Class<?>[] loadUnlessGroups;
	
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
		
		// Get @Load groups
		Load load = this.getAnnotation(Load.class);
		if (load != null) {
			loadGroups = load.value();
			
			if (load.unless().length > 0)
				loadUnlessGroups = load.unless();
		}
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
		return TypeUtils.getAnnotation(annoClass, annotations);
	}

	@Override
	public Annotation[] getAnnotations() {
		return annotations;
	}

	@Override
	public boolean shouldLoad(Set<Class<?>> groups) {
		if (loadGroups == null)
			return false;
		
		if (loadGroups.length > 0 && !matches(groups, loadGroups))
			return false;

		if (loadUnlessGroups != null && matches(groups, loadUnlessGroups))
			return false;
		
		return true;
	}
	
	private boolean matches(Set<Class<?>> groups, Class<?>[] loadGroups) {
		for (Class<?> propertyGroup: loadGroups)
			for (Class<?> enabledGroup: groups)
				if (propertyGroup.isAssignableFrom(enabledGroup))
					return true;
		
		return false;
	}
}