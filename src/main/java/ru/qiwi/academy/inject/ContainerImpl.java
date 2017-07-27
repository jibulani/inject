package ru.qiwi.academy.inject;

import org.reflections.Reflections;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Created by etrofimov on 24.07.17.
 */
public class ContainerImpl implements Container {

    Reflections reflections;
    Map<Class, Object> singletons;

    ContainerImpl(String packageName) {
        reflections = new Reflections(packageName);
        singletons = new ConcurrentHashMap<>();
        Set<Class<?>> eagers= reflections.getTypesAnnotatedWith(Eager.class);
        eagers.forEach((eager) -> singletons.put(eager, getInstance(eager)));
    }

    public Map<Class, Object> getAllSingletonObj() {
        return singletons;
    }

    public boolean isCircle(Class initClass, Class[] currClasses) {
        return Arrays.stream(currClasses).anyMatch(currClass -> {
            if (initClass.toString().equals(currClass.toString())) {
                return true;
            }
            else if (currClass.getConstructors().length > 0) {
                return  Arrays.stream(currClass.getConstructors()).anyMatch(constructor ->
                        isCircle(initClass, constructor.getParameterTypes())
                );
            }
            return false;
        });
    }


    public <T> T getInstance(Class<T> clazz) {
        try {
            Constructor[] constructors = clazz.getConstructors();
            if (constructors.length > 1) {
                throw new IllegalArgumentException();
            }
            if (constructors.length == 0) {
                if (clazz.getAnnotation(Singleton.class) != null) {
                    if (singletons.get(clazz) == null) {
                        T currSingleton = clazz.newInstance();
                        singletons.put(clazz, currSingleton);
                        return currSingleton;
                    }
                    return (T) singletons.get(clazz);
                }
                return clazz.newInstance();
            }
            else if (constructors.length == 1) {
                boolean isSingletonNecessary = false;
                if (clazz.getAnnotation(Singleton.class) != null) {
                    isSingletonNecessary = true;
                    if (singletons.get(clazz) != null) {
                        return (T) singletons.get(clazz);
                    }
                }
                List<Object> objects = new ArrayList<>();

                Class[] paramClasses = constructors[0].getParameterTypes();

                if (isCircle(clazz, paramClasses)) {
                    throw new IllegalStateException();
                }

                Annotation[][] annotations2 = constructors[0].getParameterAnnotations();
                IntStream.range(0, paramClasses.length).forEach(idx -> {
                    if (paramClasses[idx].isInterface() && annotations2[idx].length == 0) {
                        throw new IllegalStateException();
                    }
                    if (annotations2[idx].length != 0) {
                        Arrays.stream(annotations2[idx]).forEach(annotation -> {
                            if (annotation.annotationType().equals(Named.class)) {
                                Named a2 = (Named) annotation;
                                objects.add(getInstance(a2.value(), paramClasses[idx]));
                            }
                        });
                    }
                    else {
                        objects.add(getInstance(paramClasses[idx]));
                    }
                });

                if (isSingletonNecessary) {
                    return (T) singletons.computeIfAbsent(clazz, Object -> createObject(clazz.getConstructors()[0], objects));
                }
                return createObject(constructors[0], objects);
            }
            Object obj = getInstance(clazz);
            return (T) obj;
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T createObject(Constructor constructor, List<Object> args) {
        try {
            return (T) constructor.newInstance(args.toArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> T getInstance(String name, Class<T> requiredType) {
        try {

            if (requiredType.isInterface()) {
                Set<Class<? extends T>> classes = reflections.getSubTypesOf(requiredType);
                Class<? extends T> class1 = classes
                        .stream()
                        .filter((class2) -> class2.getAnnotation(Named.class) != null && class2.getAnnotation(Named.class).value().equals(name))
                        .findFirst()
                        .get();
                Named a = class1.getAnnotation(Named.class);
                Object obj = this.getInstance(class1);
                return (T) obj;
            }
            Object obj = getInstance(requiredType);
            return (T) obj;

        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public Object getInstance(String name) throws NotImplementedException {
        throw new NotImplementedException();
    }
}
