# Objectify

Objectify is a Java data access API specifically designed for the Google App Engine datastore.  It occupies a
"middle ground"; easier to use and more transparent than JDO or JPA, but significantly more convenient than
the Low-Level API.  Objectify is designed to make novices immediately productive yet also expose the full power
of the GAE datastore. This library **does not** currently support the Google AppEngine **Flexible** Environment.

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

Full documentation is availble in the [Objectify Wiki](https://github.com/objectify/objectify/wiki). Be sure to check
out the [FAQ](https://github.com/objectify/objectify/wiki/FrequentlyAskedQuestions), especially if you are using Flexible Runtimes.

## Downloads

Objectify is released to the [Maven Central Repository](https://github.com/objectify/objectify/wiki/MavenRepository)
and can be downloaded directly from there.

## Help

Help is provided in the
[Objectify App Engine User Group](https://groups.google.com/forum/?fromgroups#!forum/objectify-appengine)
