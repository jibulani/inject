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
        final boolean[] isCircle = new boolean[1];
        isCircle[0] = false;
        Arrays.stream(currClasses).forEach(currClass -> {
            if (initClass.toString().equals(currClass.toString())) {
                isCircle[0] = true;
            }
            else if (currClass.getConstructors().length > 0) {
                Arrays.stream(currClass.getConstructors()).forEach(constructor -> {
                    isCircle[0] = isCircle(initClass, constructor.getParameterTypes());
                });
            }
        });
        return isCircle[0];
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
                ArrayList<Object> objects = new ArrayList<>();

                Class[] paramClasses = constructors[0].getParameterTypes();

                if (isCircle(clazz, paramClasses)) {
                    throw new IllegalStateException();
                }

                Annotation[][] annotations2 = constructors[0].getParameterAnnotations();
                IntStream.range(0, paramClasses.length).forEach(idx -> {
                    if (paramClasses[idx].isInterface() && annotations2[idx].length == 0) {
                        throw new IllegalStateException();
                    }
                    final Object[] obj = {new Object()};
                    if (annotations2[idx].length != 0) {
                        Arrays.stream(annotations2[idx]).forEach(annotation -> {
                            if (annotation.annotationType().toString().equals(Named.class.toString())) {
                                Named a2 = (Named) annotation;
                                obj[0] = getInstance(a2.value(), paramClasses[idx]);
                            }
                        });
                    }
                    else {
                        obj[0] = getInstance(paramClasses[idx]);
                    }
                    objects.add(obj[0]);
                });

                Object[] objects1 = objects.toArray();
                if (isSingletonNecessary) {
                    return (T) singletons.computeIfAbsent(clazz, Object -> createObject(clazz.getConstructors()[0], objects1));
                }
                return createObject(constructors[0], objects1);
            }
            Object obj = getInstance(clazz);
            return (T) obj;
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T createObject(Constructor constructor, Object[] args) {
        try {
            return (T) constructor.newInstance(args);
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
            Object obj = requiredType.newInstance();
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
