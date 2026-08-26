package com.oblodai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oblodai.core.AsyncPager;
import com.oblodai.core.Pager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * The asynchronous tree is written by hand, so it is checked by machine: every namespace exposes the
 * same method names with the same parameter types on both clients, and the async side returns a
 * future or an async pager. A method added to one tree and forgotten in the other fails here, before
 * anyone finds out by not being able to call it.
 */
class AsyncParityTest {

    /** One method's identity for comparison: name plus parameter types, return type ignored. */
    private record Shape(String name, List<String> parameters) {

        @Override
        public String toString() {
            return name + "(" + String.join(", ", parameters) + ")";
        }
    }

    private static Set<Shape> shapes(Class<?> namespace) {
        Set<Shape> out = new LinkedHashSet<>();
        for (Method method : namespace.getMethods()) {
            if (method.getDeclaringClass() == Object.class) continue;
            List<String> parameters = new ArrayList<>();
            for (Class<?> parameter : method.getParameterTypes()) parameters.add(parameter.getSimpleName());
            out.add(new Shape(method.getName(), parameters));
        }
        return out;
    }

    /** The namespaces of the blocking client, by accessor name, paired with the async ones. */
    private static Map<String, Class<?>[]> namespaces() {
        Map<String, Class<?>> blocking = new LinkedHashMap<>();
        for (Method method : Oblodai.class.getDeclaredMethods()) {
            if (method.getParameterCount() != 0) continue;
            Class<?> type = method.getReturnType();
            if (type.getPackageName().equals("com.oblodai.resources")) blocking.put(method.getName(), type);
        }
        Map<String, Class<?>> async = new LinkedHashMap<>();
        for (Method method : OblodaiAsync.class.getDeclaredMethods()) {
            if (method.getParameterCount() != 0) continue;
            Class<?> type = method.getReturnType();
            if (type.getPackageName().equals("com.oblodai.resources.async")) async.put(method.getName(), type);
        }
        assertEquals(blocking.keySet(), async.keySet(), "both clients expose the same namespaces");
        assertEquals(16, blocking.size(), "the namespaces of this SDK");

        Map<String, Class<?>[]> pairs = new LinkedHashMap<>();
        blocking.forEach((name, type) -> pairs.put(name, new Class<?>[] {type, async.get(name)}));
        return pairs;
    }

    @TestFactory
    List<DynamicTest> everyNamespaceHasTheSameMethodsOnBothClients() {
        List<DynamicTest> tests = new ArrayList<>();
        namespaces()
                .forEach(
                        (name, pair) ->
                                tests.add(
                                        DynamicTest.dynamicTest(
                                                name,
                                                () -> {
                                                    Set<Shape> blocking = shapes(pair[0]);
                                                    Set<Shape> async = shapes(pair[1]);
                                                    assertEquals(
                                                            blocking,
                                                            async,
                                                            name + ": the two trees must be method-for-method identical");
                                                })));
        return tests;
    }

    @TestFactory
    List<DynamicTest> everyAsyncMethodReturnsAFutureOrAnAsyncPager() {
        List<DynamicTest> tests = new ArrayList<>();
        namespaces()
                .forEach(
                        (name, pair) ->
                                tests.add(
                                        DynamicTest.dynamicTest(
                                                name,
                                                () -> {
                                                    for (Method method : pair[1].getMethods()) {
                                                        if (method.getDeclaringClass() == Object.class) continue;
                                                        Class<?> returned = method.getReturnType();
                                                        assertTrue(
                                                                returned == CompletableFuture.class
                                                                        || returned == AsyncPager.class,
                                                                pair[1].getSimpleName()
                                                                        + "."
                                                                        + method.getName()
                                                                        + " returns "
                                                                        + returned.getSimpleName());
                                                    }
                                                    for (Method method : pair[0].getMethods()) {
                                                        if (method.getDeclaringClass() == Object.class) continue;
                                                        assertTrue(
                                                                method.getReturnType() != CompletableFuture.class,
                                                                pair[0].getSimpleName()
                                                                        + "."
                                                                        + method.getName()
                                                                        + " blocks; it must not return a future");
                                                    }
                                                })));
        return tests;
    }

    @Test
    void everyPublicMethodOfEveryNamespaceAcceptsPerCallOptions() {
        // The documentation says so on its first page: "Every method's optional last argument is a
        // RequestOptions". This is that promise, checked.
        List<String> missing = new ArrayList<>();
        namespaces()
                .forEach(
                        (name, pair) -> {
                            for (Class<?> namespace : pair) {
                                Set<Shape> shapes = shapes(namespace);
                                for (Shape shape : shapes) {
                                    List<String> parameters = shape.parameters();
                                    if (!parameters.isEmpty()
                                            && parameters.get(parameters.size() - 1).equals("RequestOptions")) {
                                        continue;
                                    }
                                    List<String> withOptions = new ArrayList<>(parameters);
                                    withOptions.add("RequestOptions");
                                    if (!shapes.contains(new Shape(shape.name(), withOptions))) {
                                        missing.add(namespace.getName() + "#" + shape);
                                    }
                                }
                            }
                        });
        assertEquals(List.of(), missing, "these methods have no RequestOptions overload");
    }

    @Test
    void listMethodsReturnAPagerOnOneSideAndAnAsyncPagerOnTheOther() {
        for (Map.Entry<String, Class<?>[]> pair : namespaces().entrySet()) {
            for (Method method : pair.getValue()[0].getMethods()) {
                if (method.getReturnType() != Pager.class) continue;
                Method async =
                        java.util.Arrays.stream(pair.getValue()[1].getMethods())
                                .filter(
                                        candidate ->
                                                candidate.getName().equals(method.getName())
                                                        && java.util.Arrays.equals(
                                                                candidate.getParameterTypes(),
                                                                method.getParameterTypes()))
                                .findFirst()
                                .orElse(null);
                assertNotNull(async, pair.getKey() + "." + method.getName() + " has no async form");
                assertEquals(
                        AsyncPager.class,
                        async.getReturnType(),
                        pair.getKey() + "." + method.getName() + " must page without blocking");
            }
        }
    }
}
