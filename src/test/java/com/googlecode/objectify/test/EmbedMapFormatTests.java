package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.EmbedMap;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

/**
 */
public class EmbedMapFormatTests extends TestBase
{
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class OuterWithMap {
		@Id Long id;
		@EmbedMap Map<String, Long> map = Maps.newLinkedHashMap();
	}
	
	/** */
	@Test
	public void v1EmbedMapFormatIsCorrect() throws Exception {
		fact().register(OuterWithMap.class);
		fact().setSaveWithNewEmbedFormat(false);
		
		OuterWithMap outer = new OuterWithMap();
		outer.map.put("asdf", 123L);
		
		Key<OuterWithMap> key = ofy().save().entity(outer).now();
		
		Entity entity = ds().get(key.getRaw());
		assert entity.getProperty("map.asdf").equals(123L);
	}

	/** */
	@Test
	public void v2EmbedMapFormatIsCorrect() throws Exception {
		fact().register(OuterWithMap.class);
		fact().setSaveWithNewEmbedFormat(true);
		
		OuterWithMap outer = new OuterWithMap();
		outer.map.put("asdf", 123L);
		
		Key<OuterWithMap> key = ofy().save().entity(outer).now();
		
		Entity entity = ds().get(key.getRaw());
		
		EmbeddedEntity entityInner = (EmbeddedEntity)entity.getProperty("map");
		assert entityInner.getProperty("asdf").equals(123L);
		
		ofy().clear();
		OuterWithMap fetched = ofy().load().key(key).now();
		assert fetched.map.equals(outer.map);
	}
}
