/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * https://github.com/objectify/objectify/issues/397
 */
class IndexingEmbeddedTests2 extends TestBase {

	/** */
	@Entity
	@Cache
	@Data
	private static class EmbeddedEntity {
		@Id Long id;
		boolean foo;
	}

	/** */
	@Entity
	@Cache
	@Index
	@Data
	private static class UnindexEmbeddedEntity {
		@Id Long id;
		@Unindex EmbeddedEntity embeddedEntity;
	}

	/** */
	//@Test
	void embeddedEntityFollowsNormalIndexingRules() throws Exception {
		factory().register(UnindexEmbeddedEntity.class);

		final EmbeddedEntity embeddedEntity = new EmbeddedEntity();
		embeddedEntity.id = 1L;
		embeddedEntity.foo = true;

		final UnindexEmbeddedEntity thing = new UnindexEmbeddedEntity();
		thing.id = 2L;
		thing.embeddedEntity = embeddedEntity;

		final Key raw = ofy().save().entity(thing).now().getRaw();

		com.google.cloud.datastore.Entity datastoreEntity = datastore().get(raw);

		final Value<?> datastoreEmbeddedEntity = datastoreEntity.getValue("embeddedEntity");
		assertThat(datastoreEmbeddedEntity).isNotNull();

		// The following line fails. Apparently, the embedded entity is indexed, despite the @Unindex annotation
		assertThat(datastoreEmbeddedEntity.excludeFromIndexes()).isTrue();

		// JMS: This is unfortunately necessary because if you don't include the embedded entity in the index,
		// then google's SDK de-indexes any children (even if they are marked as indexed). On the plus side,
		// this shouldn't actually create an index or cost anything - only actual properties are indexed.
		// See DatastoreTests.intermediateEmbeddedEntitiesAlwaysNeedToBeIndexed()

		// I'm leaving this test intact (but disabled) here because we might revisit it later if Google ever changes
		// the SDK behavior - although that's unlikely since it would probably break backwards compatibility.
	}}