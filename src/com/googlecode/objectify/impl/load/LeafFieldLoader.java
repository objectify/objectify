package com.googlecode.objectify.impl.load;

import java.lang.reflect.Field;

import com.googlecode.objectify.impl.conv.StandardConversions;
import com.googlecode.objectify.impl.save.Path;

/**
 * <p>Loader which knows how to load basic leaf values. Leaf values are things that
 * go into the datastore: basic types or collections of basic types.  Basically
 * anything except an @Embed.</p>
 */
public class LeafFieldLoader extends FieldLoader
{
	/** */
	StandardConversions conversions;
	
	/**
	 */
	public LeafFieldLoader(StandardConversions conv, Field field) {
		super(field);
		
		this.conversions = conv;
	}

	@Override
	public void load(Object value, Object pojo, Path path) {
	}
}
