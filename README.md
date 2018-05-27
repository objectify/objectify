# Objectify

Objectify is a Java data access API specifically designed for the Google Cloud Datastore (aka the Google App Engine Datastore).  It occupies a
"middle ground"; easier to use and more transparent than JDO or JPA, but significantly more convenient than
the low-level API libraries that Google provides.  Objectify is designed to make novices immediately productive yet also expose the full power
of the Datastore.

**Important note about versions**: Objectify v5 and prior use the _Google App Engine API for Java_ and therefore can only be used within Google App Engine Standard. Objectify v6+ uses the _Cloud Datastore API_ and can be used from anywhere - GAE Standard, GAE Flex, GCE, or outside Google Cloud entirely. See the [FAQ](https://github.com/objectify/objectify/wiki/FrequentlyAskedQuestions) for more information.

## Features

  * Objectify lets you persist, retrieve, delete, and query your own **typed objects**.
  ```
  @Entity
  class Car {
      @Id String vin; // Can be Long, long, or String
      String color;
  }
    
  ofy().save().entity(new Car("123123", "red")).now();
  Car c = ofy().load().type(Car.class).id("123123").now();
  ofy().delete().entity(c);
  ```
  * Objectify surfaces **all native datastore features**, including batch operations, queries, transactions, asynchronous operations, and partial indexes.
  * Objectify provides **type-safe key and query classes** using Java generics.
  * Objectify provides a **human-friendly query interface**.
  * Objectify can automatically **cache your data in memcache** for improved read performance.
  * Objectify can store polymorphic entities and perform **true polymorphic queries**.
  * Objectify provides a simple, **easy-to-understand transaction model**.
  * Objectify provides built-in facilities to **help migrate schema changes** forward.
  * Objectify provides **thorough documentation** of concepts as well as use cases.
  * Objectify has an **extensive test suite** to prevent regressions.

## Documentation

Full documentation is available in the [Objectify Wiki](https://github.com/objectify/objectify/wiki).

## Downloads

Objectify is released to the [Maven Central Repository](https://github.com/objectify/objectify/wiki/MavenRepository)
and can be downloaded directly from there.

## Help

Help is provided in the
[Objectify App Engine User Group](https://groups.google.com/forum/?fromgroups#!forum/objectify-appengine)
