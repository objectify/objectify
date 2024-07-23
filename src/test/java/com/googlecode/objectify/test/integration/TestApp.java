package com.googlecode.objectify.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.NullValue;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.util.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This class tests Objectify APIs handling of Embedded NullValue by comparing with Entitites
// written and read using raw Datastore API calls.
public class TestApp {
  private final String PROJECT_ID_ENV_VARIABLE = "PROJECT_ID";
  private final String NAMESPACE = "objectify-test";
  private final String KIND = "Sample";
  private final String DATABASE_ID = ""; // "(default)" DB
  private final String PROJECT_ID = "jimit-test"; // System.getenv(PROJECT_ID_ENV_VARIABLE)
  private DatastoreOptions datastoreOptions;
  private Datastore datastore;
  private ObjectifyFactory factory;

  public TestApp() {
    // Setup
    datastoreOptions =
        DatastoreOptions.newBuilder().setProjectId(PROJECT_ID).setNamespace(NAMESPACE).build();

    datastore = datastoreOptions.getService();
    factory = new ObjectifyFactory(datastore);
    factory.register(Sample.class);
    ObjectifyService.init(factory);
  }

  @Entity(name = "Sample")
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Sample {
    @Id String name;
    Map<String, List<Value>> values;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Value {
    String primitiveField;
    Map<String, String> structuredField;
  }

  // Verifies correctly loading NullValue embedded in a Map
  public void testLoadingNullEmbeddedMapValue() {
    String rawDatastoreDocId = "EmbeddedMapNullValue_rawDatastoreDoc";
    com.google.cloud.datastore.Key rawDatastoreKey = getDatastoreKey(rawDatastoreDocId);

    // embeddedList = {"structuredField":null, "primitiveField":null}
    FullEntity embeddedList =
        com.google.cloud.datastore.Entity.newBuilder()
            .set("structuredField", new NullValue())
            .set("primitiveField", new NullValue())
            .build();

    // embeddedEntity = {"k1":[{"structuredField":null, "primitiveField":null}]}
    FullEntity embeddedEntity =
        com.google.cloud.datastore.Entity.newBuilder()
            .set("k1", Arrays.asList(EntityValue.of(embeddedList)))
            .build();

    // "values" = {"k1":[{"structuredField":null, "primitiveField":null}]}
    com.google.cloud.datastore.Entity entityWithNullValuesInEmbeddedMap =
        com.google.cloud.datastore.Entity.newBuilder(rawDatastoreKey)
            .set("values", embeddedEntity)
            .build();

    // Save to Datastore
    datastore.put(entityWithNullValuesInEmbeddedMap);

    // Verify using Datastore API
    com.google.cloud.datastore.Entity rawDatastoreReadEntity = datastore.get(rawDatastoreKey);
    assertEquals(entityWithNullValuesInEmbeddedMap, rawDatastoreReadEntity);

    // Verify using Objectify `load` API
    try (Closeable ignored = ObjectifyService.begin()) {
      Sample appDocEntity =
          factory.ofy().load().key(Key.create(Sample.class, rawDatastoreDocId)).now();
      assertEquals(rawDatastoreDocId, appDocEntity.name);
      assertNotNull(appDocEntity.values);
      System.out.println("appDoc=" + appDocEntity);
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      System.out.print(sw);
    }
  }

  // Verifies correctly loading map value of NullValue
  public void testLoadingEmbeddedMapNullValue() {
    Map<String, List<Value>> values = new HashMap<>();
    values.put("k1", null);

    String rawDatastoreDocId =
        "EmbeddedMapNullValue_rawDatastoreDoc"; // entered using raw Datastore API
    Sample appDoc = new Sample(rawDatastoreDocId, values);

    // Set up the raw Datastore key
    com.google.cloud.datastore.Key key =
        com.google.cloud.datastore.Key.newBuilder(PROJECT_ID, KIND, rawDatastoreDocId, DATABASE_ID)
            .setNamespace(NAMESPACE)
            .build();
    com.google.cloud.datastore.Entity entityWithNullValue =
        com.google.cloud.datastore.Entity.newBuilder(key).set("k1", new NullValue()).build();

    // Save to Datastore
    datastore.put(entityWithNullValue);

    // Verify using Datastore API
    assertEquals(entityWithNullValue, datastore.get(key));

    // Verify using Objectify `load` API
    try (Closeable ignored = ObjectifyService.begin()) {
      Sample appDocEntity =
          factory.ofy().load().key(Key.create(Sample.class, rawDatastoreDocId)).now();
      assertNotNull(appDocEntity);
      assert (appDocEntity.values.keySet().contains("k1"));
      assertNull(appDocEntity.values.get("k1"));
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      System.out.print(sw);
    }
  }

  // Verifies that saving a null-value results for a list in inconsistency of saved data between
  // Datastore API and Objectify API.
  // POJO:
  // Id => { "values" : { "k1" : null } }
  //
  // Datastore Entity:
  // values = {
  //   "properties": {
  //     "k1": {
  //       "nullValue": null
  //     }
  //   }
  // }
  public void testSavingNullListValue() throws InterruptedException {
    // Set up the raw Datastore key
    String rawDatastoreDocId = "nullValue_rawDatastorePutKey"; // entered using raw Datastore API
    com.google.cloud.datastore.Key rawDatastoreEntityKey = getDatastoreKey(rawDatastoreDocId);

    // Create an Entity resembling the Objectify POJO
    FullEntity embeddedEntity = FullEntity.newBuilder().set("k1", new NullValue()).build();
    com.google.cloud.datastore.Entity entityWithNullValue =
        com.google.cloud.datastore.Entity.newBuilder(rawDatastoreEntityKey)
            .set("values", embeddedEntity)
            .build();
    datastore.put(entityWithNullValue);

    // Now save a POJO
    String objectifyAppDocId = "nullValue_objectifyAppSaveKey"; // entered from test app
    Map<String, List<Value>> values = new HashMap<>();
    values.put("k1", null);
    Sample sampleDoc = new Sample(objectifyAppDocId, values);
    com.google.cloud.datastore.Key objectifyAppSaveKey = getDatastoreKey(objectifyAppDocId);

    try (Closeable ignored = ObjectifyService.begin()) {
      // If an entity is not saved, uncomment the following line and call save() method.
      // And edit the entity directly to `structuredFiled` has null (nullValue) from GCP console
      factory.ofy().save().entity(sampleDoc).now();
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      System.out.print(sw);
    }

    com.google.cloud.datastore.Entity readRawDatastoreEntity = datastore.get(rawDatastoreEntityKey);
    com.google.cloud.datastore.Entity readObjectifyAppDoc = datastore.get(objectifyAppSaveKey);
    assertEquals(readRawDatastoreEntity, readObjectifyAppDoc);
  }

  // This test verifies that saving a POJO with an empty list as a value results in inconsistency of
  // saved data between Datastore API and Objectify API.
  // POJO:
  // Id => { "values" : { "k1" : [] } }
  //
  // Observed Datastore Entity:
  // values = {}
  //
  // Expected Datastore Entity:
  // values = {
  //   "properties": {
  //     "k1": {
  //       "arrayValue": {}
  //     }
  //   }
  // }
  public void testSavingEmptyList() throws InterruptedException {
    // Set up the raw Datastore key
    String rawDatastoreDocId = "nonNullValue_rawDatastorePutKey"; // entered using raw Datastore API
    com.google.cloud.datastore.Key rawDatastoreKey = getDatastoreKey(rawDatastoreDocId);

    // Insert an Entity resembling the Objectify POJO
    FullEntity embeddedEntity = FullEntity.newBuilder().set("k1", new ArrayList<>()).build();
    com.google.cloud.datastore.Entity entityWithNonNullValue =
        com.google.cloud.datastore.Entity.newBuilder(rawDatastoreKey)
            .set("values", embeddedEntity)
            .build();
    datastore.put(entityWithNonNullValue);

    // Now save a POJO
    String objectifyAppDocId = "nonNullValue_objectifyAppSaveKey"; // entered from test app
    com.google.cloud.datastore.Key objectifyAppDocKey = getDatastoreKey(objectifyAppDocId);
    Map<String, List<Value>> values = new HashMap<>();
    values.put("k1", new ArrayList<>());
    // POJO to be saved
    Sample sampleDoc = new Sample(objectifyAppDocId, values);

    try (Closeable ignored = ObjectifyService.begin()) {
      // If an entity is not saved, uncomment the following line and call save() method.
      // And edit the entity directly to `structuredFiled` has null (nullValue) from GCP console
      factory.ofy().save().entity(sampleDoc).now();
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      System.out.print(sw);
    }

    // Read back both entities using Datastore API and compare
    com.google.cloud.datastore.Entity readRawDatastoreEntity = datastore.get(rawDatastoreKey);
    com.google.cloud.datastore.Entity readObjectifyAppDoc = datastore.get(objectifyAppDocKey);
    assertEquals(readRawDatastoreEntity, readObjectifyAppDoc);
  }

  private com.google.cloud.datastore.Key getDatastoreKey(String id) {
    return com.google.cloud.datastore.Key.newBuilder(PROJECT_ID, KIND, id, DATABASE_ID)
        .setNamespace(NAMESPACE)
        .build();
  }

  public static void main(String[] args) throws InterruptedException {
    TestApp test = new TestApp();
    // This verifies fix for https://github.com/objectify/objectify/issues/506
    test.testLoadingNullEmbeddedMapValue();
    // test.testLoadingEmbeddedMapNullValue();
    // test.testSavingNullListValue();
    // test.testSavingEmptyList();
  }
}
