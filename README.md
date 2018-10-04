# jackson-utils

Utilities for reading and writing JSON and XML using Jackson.

## Serialization and Deserialization

### Enumerated Types

When deserializating strings into Java `Enum` types, it's useful to be able to accept lowercase, uppercase and mixed-cased string input.

This deserializer attempts to coerce the string value using the provided case first, but then falls back to UPPERCASE or lowercase if needed.

```java
objectMapper.registerModule(new SimpleModule()
        .setDeserializerModifier(new EnumUppercaseDeserializerModifier()));
```


### Trimming Whitespace

When deserializing string values, it's useful to automatically trim leading and trailing whitespace including newlines, tabs, etc.

This deserializer uses the `Character.isWhitespace(char)` function to determine what is whitespace.

```java
objectMapper.registerModule(new SimpleModule()
        .addDeserializer(String.class, TrimWhitespaceDeserializer.SINGLETON));
```


### ZonedDateTime

When serializing and deserializing ZonedDateTime values, it's useful to accept a wide variety of string formats.

In addition to the widely-supported ISO date format, you also get variety of other common formats:
* yyyy-MM-dd
* yyyyMMdd
* yyyyMMMddd
* MM/dd/yyyy
* MM-dd-yyyy
* MM.dd.yyyy
* dd-MMM-yyyy
* MMM dd, YYYY
* MMMM dd, yyyy

```java
objectMapper.registerModule(new SimpleModule()
        .addSerializer(new ZonedDateTimeSerializer()) //
        .addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
```


### GeoPoint

This library exposes an immutable `GeoPoint` class that represents a lat/lon coordinate.  `GeoPoint` supports a
variety of serialization and deserialization options, which happen to be compatible with those used by
[Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-point.html).

#### Latitude and longitude fields

```json
"location": {
  "lat": 41.12,
  "lon": -71.34
}
```

#### Comma separated latitude and longitude

```json
"location": "41.12,-71.34"
```

#### Geohash

```json
"location": "drm3btev3e86"
```


## MapPath

When deserializing JSON that does not conform to a fixed schema, it's sometimes useful to deserialize the JSON to native Java objects like `Map` and `List`
instead using Jackson's tree structure (e.g. `JsonNode`).

When we do this however we lose the ability to traverse the data in an easy way and null-safe manner. Enter `MapPath`, which is loosely 
inspired by [JsonPath](https://github.com/json-path/JsonPath).

Let's assume the following JSON input:

```java
{
  "store": {
    "book": [
      {
        "category": "reference",
        "author": "Nigel Rees",
        "title": "Sayings of the Century",
        "price": 8.95
      },
      {
        "category": "fiction",
        "author": "Evelyn Waugh",
        "title": "Sword of Honour",
        "price": 12.99
      },
      {
        "category": "fiction",
        "author": "Herman Melville",
        "title": "Moby Dick",
        "isbn": "0-553-21311-3",
        "price": 8.99
      },
      {
        "category": "fiction",
        "author": "J. R. R. Tolkien",
        "title": "The Lord of the Rings",
        "isbn": "0-395-19395-8",
        "price": 22.99
      }
    ],
    "bicycle": {
      "color": "red",
      "price": 19.95
    }
  },
  "expensive": 10
}
```

Let's use Jackson to parse this JSON into a Map.

```java
String json = "...see above...";
Map map = objectMapper.readValue(json, Map.class);
```

Let's wrap the Map using `MapPath`

```java
MapPath mapPath = MapPath.of(map);
```


Now we can traverse the properties of the map very easily:

```java
   mapPath.getString("store.bicycle.color")    ' red
   mapPath.getDouble("store.bicycle.price")    ' 19.95
   mapPath.getInt("expensive")                 ' 10
```


## Installation

The library is available on [Maven Central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.arakelian%22%20AND%20a%3A%22jackson-utils%22).

### Maven

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central</id>
        <name>Central Repository</name>
        <url>http://repo.maven.apache.org/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
    </repository>
</repositories>

...

<dependency>
    <groupId>com.arakelian</groupId>
    <artifactId>jackson-utils</artifactId>
    <version>1.10.1</version>
    <scope>compile</scope>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
repositories {
  mavenCentral()
}

dependencies {
  compile 'com.arakelian:jackson-utils:1.10.1'
}
```

## Licence

Apache Version 2.0
