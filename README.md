# ioc.java

Minimalistic IOC container for Java applications

## Usage

**Maven Repository**

Add the following maven repository to your pom:

```xml
<repository>
  <id>paul-h.de_repo</id>
  <url>https://mvn.paul-h.de/</url>
</repository>
```

Include the project as dependency:

```xml
<dependency>
  <groupId>de.paulomart</groupId>
  <artifactId>ioc</artifactId>
  <version>2.0.2</version>
</dependency>
```

**Example**

```java
// create a instance of the container
Container container = new Container();
// register a given class to the container
// the class A implements the InterfaceA
container.registerClass(A.class);
// resolve a instance implementing interface
InterfaceA myInstance = container.resolve(InterfaceA.class);
```

For a more detailed example see the [`src/examples/java`](src/examples/java/) Folder.
