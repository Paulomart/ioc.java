package de.paulomart.ioc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

class ClassDiscoverHelper {

	private final Set<Class<?>> result = new HashSet<>();
	private final ClassLoader classLoaderToSearchIn;
	private final Predicate<String> classNamePredicate;

	ClassDiscoverHelper(ClassLoader classLoaderToSearchIn, Predicate<String> classNamePredicate) {
		this.classLoaderToSearchIn = classLoaderToSearchIn;
		this.classNamePredicate = classNamePredicate;
	}

	void search() throws Exception {
		for (URL url : getRootUrls()) {
			File f = new File(url.getPath());
			if (f.isDirectory()) {
				visitFile(f);
			} else {
				visitJar(url);
			}
		}
	}

	Collection<Class<?>> getResult() {
		return result;
	}

	private List<URL> getRootUrls() {
		List<URL> result = new ArrayList<>();
		ClassLoader cl = classLoaderToSearchIn;
		while (cl != null) {
			if (cl instanceof URLClassLoader) {
				URL[] urls = ((URLClassLoader) cl).getURLs();
				result.addAll(Arrays.asList(urls));
			}
			cl = cl.getParent();
		}
		return result;
	}

	private void visitFile(File f) throws Exception {
		if (f.isDirectory()) {
			File[] children = f.listFiles();
			if (children == null) {
				return;
			}
			for (File child : children) {
				visitFile(child);
			}
		} else if (f.getName().endsWith(".class")) {
			try (FileInputStream in = new FileInputStream(f)) {
				handleClass(in);
			}
		}
	}

	private void visitJar(URL url) throws Exception {
		try (InputStream urlIn = url.openStream();
				JarInputStream jarIn = new JarInputStream(urlIn)) {
			JarEntry entry;
			while ((entry = jarIn.getNextJarEntry()) != null) {
				if (entry.getName().endsWith(".class")) {
					handleClass(jarIn);
				}
			}
		}
	}

	private void handleClass(InputStream in) throws IOException {
		DiscoverClassVisitor cv = new DiscoverClassVisitor();
		new ClassReader(in).accept(cv, 0);
		if (!classNamePredicate.test(cv.className)) {
			return;
		}
		if (cv.hasAnnotation) {
			try {
				result.add(Class.forName(cv.className, true, classLoaderToSearchIn));
			} catch (ClassNotFoundException e) {
				// can not happen
			}
		}
	}

	private static final String IOC_DISCOVER_DESC = "L" + AutoDiscover.class.getName().replace('.', '/') + ";";

	private class DiscoverClassVisitor extends ClassVisitor {

		private boolean hasAnnotation;
		private String className;

		DiscoverClassVisitor() {
			super(Opcodes.ASM5);
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = name.replace('/', '.');
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if (desc.equals(IOC_DISCOVER_DESC)) {
				hasAnnotation = true;
			}
			return null;
		}
	}
}
