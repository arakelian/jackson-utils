# jackson-utils

Utilities for reading and writing JSON and XML using Jackson.


## Enumerated Types

When deserializating strings into Java enumerated types (`Enum`), it's useful to accept lowercase, uppercase and mixed cased string value.

```java
    objectMapper.registerModule(new SimpleModule()
                .setDeserializerModifier(new EnumUppercaseDeserializerModifier()));
``` 


## Trimming Whitespace

When deserializing string values, it's useful to automatically trim leading and trailing whitespace including newlines, tabs, etc.

```java
    objectMapper.registerModule(new SimpleModule()
                .addDeserializer(String.class, TrimWhitespaceDeserializer.SINGLETON));
```

 
## ZonedDateTime

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


## GeoPoint

This library exposes an immutable `GeoPoint` class that represents a lat/lon coordinate.  `GeoPoint` supports a
variety of serialization and deserialization options, which happen to be compatible with those used by 
[Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-point.html)

### Object with latitude and longitude fields

```json
"location": { 
  "lat": 41.12,
  "lon": -71.34
}
```

### Comma separated latitude and longitude

```json
"location": "41.12,-71.34"
```

### Geohash

```json
"location": "drm3btev3e86"
```

## Installation

The library is available on Maven Central

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
    <version>1.8.2</version>
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
  compile 'com.arakelian:jackson-utils:1.8.2'
}
```

## Licence

Apache Version 2.0
