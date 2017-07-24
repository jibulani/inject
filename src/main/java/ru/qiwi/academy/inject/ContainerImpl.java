package ru.qiwi.academy.inject;

import org.reflections.Reflections;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by etrofimov on 24.07.17.
 */
public class ContainerImpl implements Container {

    Object[] objs;

    Reflections reflections;

    Set<Class<? extends Object>> allClasses;

    Object singletonObj;

    ContainerImpl(String packageName) {
        reflections = new Reflections(packageName);
    }

    public Map<Class, Object> getAllSingletonObj() {
        HashMap<Class, Object> allSingletonObjs = new HashMap<>();
        ContainerImpl c = new ContainerImpl("ru.qiwi.academy");
        allSingletonObjs.put(c.getClass(), c);
        return allSingletonObjs;
    }

    public <T> T getInstance(Class<T> clazz) {
        try {
            Annotation[] annotations = clazz.getAnnotations();
            for (Annotation a : annotations) {
                if (a.annotationType().getSimpleName().equals("Singleton")) {
                    if (singletonObj == null) {
                        singletonObj = clazz.newInstance();
                    }
                    return (T) singletonObj;
                }
            }
            Constructor[] constructors = clazz.getConstructors();

            if (constructors.length > 1) {
                throw new IllegalArgumentException();
            }
            for (Constructor constructor : constructors) {
                ArrayList<Object> objects = new ArrayList<>();
                Class[] paramClasses = constructor.getParameterTypes();
                for (Class paramClass : paramClasses) {
                    Object obj = getInstance(paramClass);
                    objects.add(obj);
                }
                Object[] objects1 = objects.toArray();
                Object obj = constructor.newInstance(objects1);
                return (T) obj;
            }
            Object obj = clazz.newInstance();
            return (T) obj;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException();
        }
    }

    public <T> T getInstance(String name, Class<T> requiredType) {
        try {

            if (requiredType.isInterface()) {
                Set<Class<? extends T>> classes = reflections.getSubTypesOf(requiredType);
                for (Class<? extends T> class1 : classes) {
                    Named a = class1.getAnnotation(Named.class);
                    if (a != null && a.value().equals(name)) {
                        Object obj = class1.newInstance();
                        return (T) obj;
                    }

                }
            }

            Object obj = requiredType.newInstance();
            return (T) obj;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public Object getInstance(String name) {
        try {
            Class c = Class.forName(name);
            Object obj = c.newInstance();
            return obj;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
