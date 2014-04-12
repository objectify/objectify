package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 */
public class EmbedFormatTests extends TestBase
{
	/** */
	@Embed
	public static class Inner {
		String stuff;
		public Inner() { }
		public Inner(String stuff) {
			this.stuff = stuff;
		}

		@Override
		public boolean equals(Object obj) {
			return Objects.equals(stuff, ((Inner)obj).stuff);
		}
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class Outer {
		@Id Long id;

		Inner inner;
		public Outer() { }
		public Outer(Inner inner) {
			this.inner = inner;
		}
	}
	
	/** */
	@Test
	public void v1FormatIsCorrect() throws Exception {
		fact().register(Outer.class);
		fact().setSaveWithNewEmbedFormat(false);
		
		Inner inner = new Inner("stuff");
		Outer outer = new Outer(inner);
		
		Key<Outer> key = ofy().save().entity(outer).now();
		
		Entity entity = ds().get(key.getRaw());
		assert entity.getProperty("inner.stuff").equals("stuff");
	}
	
	/** */
	@Test
	public void writtenAsV1ReadsInV2Mode() throws Exception {
		fact().register(Outer.class);
		fact().setSaveWithNewEmbedFormat(false);
		
		Inner inner = new Inner("stuff");
		Outer outer = new Outer(inner);
		
		Key<Outer> key = ofy().save().entity(outer).now();
		
		ofy().clear();
		fact().setSaveWithNewEmbedFormat(true);
		
		Outer fetched = ofy().load().key(key).now();
		assert fetched.inner.stuff.equals(inner.stuff);
	}
	
	/** */
	@Test
	public void v2FormatIsCorrect() throws Exception {
		fact().register(Outer.class);
		fact().setSaveWithNewEmbedFormat(true);
		
		Inner inner = new Inner("stuff");
		Outer outer = new Outer(inner);
		
		Key<Outer> key = ofy().save().entity(outer).now();
		
		Entity entity = ds().get(key.getRaw());
		
		EmbeddedEntity entityInner = (EmbeddedEntity)entity.getProperty("inner");
		assert entityInner.getProperty("stuff").equals("stuff");
		
		ofy().clear();
		Outer fetched = ofy().load().key(key).now();
		assert fetched.inner.stuff.equals(inner.stuff);
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class OuterWithList {
		@Id Long id;
		List<Inner> inner = Lists.newArrayList();
		public OuterWithList() { }
	}
	
	/** */
	@Test
	public void v2FormatIsCorrect_WithList() throws Exception {
		fact().register(OuterWithList.class);
		fact().setSaveWithNewEmbedFormat(true);
		
		OuterWithList outer = new OuterWithList();
		outer.inner.add(new Inner("stuff0"));
		outer.inner.add(new Inner("stuff1"));
		
		Key<OuterWithList> key = ofy().save().entity(outer).now();
		
		Entity entity = ds().get(key.getRaw());
		
		@SuppressWarnings("unchecked")
		List<EmbeddedEntity> entityInner = (List<EmbeddedEntity>)entity.getProperty("inner");
		assert entityInner.size() == 2;
		assert entityInner.get(0).getProperty("stuff").equals("stuff0");
		assert entityInner.get(1).getProperty("stuff").equals("stuff1");

		ofy().clear();
		OuterWithList fetched = ofy().load().key(key).now();
		assert fetched.inner.get(0).stuff.equals(outer.inner.get(0).stuff);
		assert fetched.inner.get(1).stuff.equals(outer.inner.get(1).stuff);
	}

	/** */
	@Test
	public void readsV1Format_WithList() throws Exception {
		fact().register(OuterWithList.class);
		fact().setSaveWithNewEmbedFormat(false);

		OuterWithList outer = new OuterWithList();
		outer.inner.add(new Inner("stuff0"));
		outer.inner.add(new Inner("stuff1"));

		Key<OuterWithList> key = ofy().save().entity(outer).now();

		ofy().clear();
		OuterWithList fetched = ofy().load().key(key).now();
		assert fetched.inner.get(0).stuff.equals(outer.inner.get(0).stuff);
		assert fetched.inner.get(1).stuff.equals(outer.inner.get(1).stuff);
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasEmbeddedEntity {
		@Id Long id;
		EmbeddedEntity normal;
		public HasEmbeddedEntity() { }
	}
	
	/** */
	@Test
	public void normalEmbeddedEntityFieldWorksFine() throws Exception {
		fact().register(HasEmbeddedEntity.class);
		fact().setSaveWithNewEmbedFormat(true);

		HasEmbeddedEntity h = new HasEmbeddedEntity();
		h.normal = new EmbeddedEntity();
		h.normal.setProperty("stuff", "stuff");
		
		HasEmbeddedEntity fetched = this.putClearGet(h);
		assert fetched.normal.getProperty("stuff").equals("stuff");
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasEmbeddedEntityList {
		@Id Long id;
		List<EmbeddedEntity> list = Lists.newArrayList();
		public HasEmbeddedEntityList() { }
	}
	
	/** */
	@Test
	public void listEmbeddedEntityFieldWorksFine() throws Exception {
		fact().register(HasEmbeddedEntityList.class);
		fact().setSaveWithNewEmbedFormat(true);

		HasEmbeddedEntityList h = new HasEmbeddedEntityList();
		
		EmbeddedEntity emb0 = new EmbeddedEntity();
		emb0.setProperty("stuff", "stuff0");
		h.list.add(emb0);
		
		HasEmbeddedEntityList fetched = this.putClearGet(h);
		assert fetched.list.size() == 1;
		assert fetched.list.get(0).getProperty("stuff").equals("stuff0");
	}

	/** */
	@Test
	public void emptyListSaves() throws Exception {
		fact().register(HasEmbeddedEntityList.class);
		fact().setSaveWithNewEmbedFormat(true);

		HasEmbeddedEntityList outer = new HasEmbeddedEntityList();

		Key<HasEmbeddedEntityList> key = ofy().save().entity(outer).now();

		Entity entity = ds().get(key.getRaw());
		assert entity.getProperties().size() == 0;
	}

	/** */
	@Embed
	public static class InnerIndexed {
		@Index String stuff;
		public InnerIndexed() { }
		public InnerIndexed(String stuff) {
			this.stuff = stuff;
		}
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class OuterWithIndex {
		@Id Long id;

		InnerIndexed inner;
		public OuterWithIndex() { }
		public OuterWithIndex(InnerIndexed inner) {
			this.inner = inner;
		}
	}
	
	/** */
	@Test
	public void v2IndexFormatIsCorrect() throws Exception {
		fact().register(OuterWithIndex.class);
		fact().setSaveWithNewEmbedFormat(true);
		
		InnerIndexed inner = new InnerIndexed("stuff");
		OuterWithIndex outer = new OuterWithIndex(inner);
		
		Key<OuterWithIndex> key = ofy().save().entity(outer).now();
		
		Entity entity = ds().get(key.getRaw());
		assert entity.getProperties().size() == 2;
		assert entity.getProperty("inner.stuff").equals(Collections.singletonList("stuff"));
		assert !entity.isUnindexedProperty("inner.stuff");
		
		ofy().clear();
		OuterWithIndex fetched = ofy().load().type(OuterWithIndex.class).filter("inner.stuff", "stuff").iterator().next();
		assert fetched.inner.stuff.equals(inner.stuff);
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class OuterWithListIndex {
		@Id Long id;

		List<InnerIndexed> inner = Lists.newArrayList();
		public OuterWithListIndex() { }
		public OuterWithListIndex(InnerIndexed inner) {
			this.inner.add(inner);
		}
	}

	/** */
	@Test
	public void v2ListIndexFormatIsCorrect() throws Exception {
		fact().register(OuterWithListIndex.class);
		fact().setSaveWithNewEmbedFormat(true);

		InnerIndexed inner = new InnerIndexed("stuff0");
		OuterWithListIndex outer = new OuterWithListIndex(inner);

		Key<OuterWithListIndex> key = ofy().save().entity(outer).now();

		Entity entity = ds().get(key.getRaw());
		assert entity.getProperties().size() == 2;
		assert entity.getProperty("inner.stuff").equals(Collections.singletonList("stuff0"));
		assert !entity.isUnindexedProperty("inner.stuff");

		ofy().clear();
		OuterWithListIndex fetched = ofy().load().type(OuterWithListIndex.class).filter("inner.stuff", "stuff0").iterator().next();
		assert fetched.inner.size() == 1;
		assert fetched.inner.get(0).stuff.equals(inner.stuff);
	}

	/** */
	@Test
	public void batchSaveDoesNotCrossContaminateIndexes() throws Exception {
		fact().register(OuterWithIndex.class);
		fact().setSaveWithNewEmbedFormat(true);

		InnerIndexed inner0 = new InnerIndexed("stuff0");
		OuterWithIndex outer0 = new OuterWithIndex(inner0);

		InnerIndexed inner1 = new InnerIndexed("stuff1");
		OuterWithIndex outer1 = new OuterWithIndex(inner1);

		ofy().save().entities(outer0, outer1).now();

		ofy().clear();
		List<OuterWithIndex> fetched = ofy().load().type(OuterWithIndex.class).filter("inner.stuff", inner0.stuff).list();
		assert fetched.size() == 1;
		assert fetched.get(0).inner.stuff.equals(inner0.stuff);
	}

	/** */
	@Embed
	public static class Middle {
		Inner inner;
		public Middle() { }
		public Middle(Inner inner) {
			this.inner = inner;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Middle)
				return Objects.equals(this.inner, ((Middle)obj).inner);
			else
				return false;
		}
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasMiddle {
		@Id Long id;

		List<Middle> middles = new ArrayList<>();
		public HasMiddle() { }
		public HasMiddle(Middle middle) {
			this.middles.add(middle);
		}
	}

	/** */
	@Test
	public void withMiddleButNullInnerWorks() throws Exception {
		fact().register(HasMiddle.class);
		fact().setSaveWithNewEmbedFormat(false);

		Middle middle = new Middle(null);
		HasMiddle outer = new HasMiddle(middle);

		Key<HasMiddle> key = ofy().save().entity(outer).now();

		ofy().clear();

		HasMiddle fetched = ofy().load().key(key).now();
		assert fetched.middles.equals(outer.middles);
	}

}
