package ru.qiwi.academy.inject;

import org.reflections.Reflections;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.events.Namespace;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by etrofimov on 24.07.17.
 */
public class ContainerImpl implements Container {

    Reflections reflections;

    Map<Class, Object> singletons;


    ContainerImpl(String packageName) {
        reflections = new Reflections(packageName);
        singletons = new ConcurrentHashMap<>();
        Set<Class<?>> eagers= reflections.getTypesAnnotatedWith(ru.qiwi.academy.inject.Eager.class);
        for (Class<?> eager : eagers) {
            try {
                singletons.put(eager, eager.newInstance());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Map<Class, Object> getAllSingletonObj() {
        return singletons;
    }

    public boolean isCircle(Class initClass, Class[] currClasses) {
        for (Class paramClass : currClasses) {
            if (paramClass.getConstructors().length > 0) {
                Constructor paramClassConstr = paramClass.getConstructors()[0];
                Class[] paramClasses2 = paramClassConstr.getParameterTypes();
                for (Class paramClass2 : paramClasses2) {
                    if (initClass.toString().equals(paramClass2.toString())) {
                        return true;
                    } else {
                        if (paramClass2.getConstructors().length > 0) {
                            return isCircle(initClass, paramClass2.getConstructors()[0].getParameterTypes());
                        }
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return false;
    }


    public <T> T getInstance(Class<T> clazz) {
        try {
            Constructor[] constructors = clazz.getConstructors();

            if (constructors.length > 1) {
                throw new IllegalArgumentException();
            }
            if (constructors.length == 0) {
                Annotation[] annotations = clazz.getAnnotations();
                for (Annotation a : annotations) {
                    if (a.annotationType().equals(Singleton.class)) {
                        if (singletons != null) {
                            if (singletons.get(clazz) == null) {
                                T currSingleton = clazz.newInstance();
                                singletons.put(clazz, currSingleton);
                                return currSingleton;
                            }
                            return (T) singletons.get(clazz);
                        }
                        else {
                            T currSingleton = clazz.newInstance();
                            singletons.put(clazz, currSingleton);
                            return currSingleton;
                        }
                    }
                }
                return clazz.newInstance();
            }
            else if (constructors.length == 1) {
                Annotation[] annotations = clazz.getAnnotations();
                for (Annotation a : annotations) {

                    if (a.annotationType().getSimpleName().equals("Singleton")) {
                            if (singletons.get(clazz) == null) {
                                ArrayList<Object> objects = new ArrayList<>();

                                Class[] paramClasses = constructors[0].getParameterTypes();

                                if (isCircle(clazz, paramClasses)) {
                                    throw new IllegalStateException();
                                }

                                Annotation[][] annotations2 = constructors[0].getParameterAnnotations();
                                int i = 0;
                                for (Class paramClass : paramClasses) {

                                    if (paramClass.isInterface() && annotations2[0].length == 0) {
                                        throw new IllegalStateException();
                                    }

                                    Object obj;
                                    if (annotations2[0].length != 0) {
                                        Named a2 = (Named) annotations2[i][0];
                                        obj = this.getInstance(a2.value(), paramClass);

                                    }
                                    else {
                                        obj = this.getInstance(paramClass);
                                    }
                                    objects.add(obj);
                                    i++;

                                }
                                Object[] objects1 = objects.toArray();
                                singletons.computeIfAbsent(clazz, Object -> addSingleton(clazz, objects1));
                                return (T) singletons.get(clazz);
                            }
                            return (T) singletons.get(clazz);
                    }
                }
                ArrayList<Object> objects = new ArrayList<>();
                Class[] paramClasses = constructors[0].getParameterTypes();
                for (Class paramClass : paramClasses) {
                    Object obj = this.getInstance(paramClass);
                    objects.add(obj);
                }
                Object[] objects1 = objects.toArray();
                Object obj = constructors[0].newInstance(objects1);
                return (T) obj;
            }
            Object obj = clazz.newInstance();
            return (T) obj;
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw e;
        }
        catch (IllegalStateException e) {
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T addSingleton(Class<T> clazz, Object[] args) {
        try {
            return (T) clazz.getConstructors()[0].newInstance(args);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
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
    public Object getInstance(String name) throws NotImplementedException {
        throw new NotImplementedException();
    }
}
