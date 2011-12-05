package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.IgnoreLoad;
import com.googlecode.objectify.annotation.Load;

/** 
 * Some common behavior of properties
 */
abstract public class AbstractProperty implements Property
{
	String name;
	String[] names;
	Annotation[] annotations;
	
	/** The states are important - null means none, empty means "all" */
	String[] loadGroups;
	
	/** */
	public AbstractProperty(String name, Annotation[] annotations, Object thingForDebug) {
		this.name = name;
		this.annotations = annotations;

		// Figure out names from the @IgnoreLoad and @AlsoLoad annotations
		Set<String> nameSet = new LinkedHashSet<String>();
		
		// If we have @IgnoreLoad, don't add priamry name to the names collection (which is used for loading)
		if (this.getAnnotation(IgnoreLoad.class) == null)
			nameSet.add(name);
		
		// Now any additional names, either @AlsoLoad or the deprecated @OldName
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
		if (load != null)
			loadGroups = load.value();
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
	public String[] getLoadGroups() {
		return loadGroups;
	}
	
	@Override
	public boolean shouldLoad(Set<String> groups) {
		if (loadGroups == null)
			return false;
		
		if (loadGroups.length == 0)
			return true;
		
		for (String loadGroup: loadGroups)
			if (groups.contains(loadGroup))
				return true;
		
		return false;
	}
}