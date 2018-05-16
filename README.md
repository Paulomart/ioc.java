# ioc.java

Minimalistic IOC container for Java applications

## Usage

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
