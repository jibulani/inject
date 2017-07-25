package ru.qiwi.academy.inject;

import org.reflections.Reflections;

import javax.inject.Named;
import javax.xml.stream.events.Namespace;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

    HashMap<Class, Object> singletons;

    Object singletonObj;

    ContainerImpl(String packageName) {
        reflections = new Reflections(packageName);
        singletons = new HashMap<>();
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

    public <T> T getInstance(Class<T> clazz) {
        try {
            Constructor[] constructors = clazz.getConstructors();

            if (constructors.length > 1) {
                throw new IllegalArgumentException();
            }
            else if (constructors.length == 0) {
                Annotation[] annotations = clazz.getAnnotations();
                for (Annotation a : annotations) {
                    if (a.annotationType().getSimpleName().equals("Singleton")) {
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
                        if (singletons != null) {
                            if (singletons.get(clazz) == null) {
                                ArrayList<Object> objects = new ArrayList<>();

                                Class[] paramClasses = constructors[0].getParameterTypes();

                                for (Class paramClass : paramClasses) {
                                    if (paramClass.getConstructors().length > 0) {
                                        Constructor paramClassConstr = paramClass.getConstructors()[0];
                                        Class[] paramClasses2 = paramClassConstr.getParameterTypes();
                                        System.out.println("got param types of constr of param type");
                                        for (Class paramClass2 : paramClasses2) {
                                            System.out.println(clazz.toString());
                                            System.out.println(paramClass2.toString());
                                            if (clazz.toString().equals(paramClass2.toString()) && !clazz.isInterface()) {
                                                System.out.println("In circle");
                                                throw new IllegalStateException();
                                            }
                                        }
                                    }

                                }


                                Annotation[][] annotations2 = constructors[0].getParameterAnnotations();
                                int i = 0;
                                for (Class paramClass : paramClasses) {

                                    if (paramClass.isInterface() && annotations2[0].length == 0) {
                                        throw new IllegalStateException();
                                    }



                                    Object obj;// = new Object();
                                    if (annotations2[0].length != 0) {
                                        Named a2 = (Named) annotations2[i][0];
                                        obj = this.getInstance(a2.value(), paramClass);

                                    }
                                    else {
                                        obj = this.getInstance(paramClass);
                                    }
                                    objects.add(obj);
                                    i++;


//                                    Constructor paramClassConstr = paramClass.getConstructors()[0];
//                                    Class[] paramClasses2 = paramClassConstr.getParameterTypes();
//                                    System.out.println("got param types of constr of param type");
//                                    for (Class paramClass2 : paramClasses2) {
//                                        System.out.println(clazz.toString());
//                                        System.out.println(paramClass2.toString());
//                                        if (clazz.toString().equals(paramClass2.toString())) {
//
//                                            singletons.put(clazz, clazz.newInstance());
//                                            return clazz.newInstance();
//                                        }
//                                    }
                                }
                                Object[] objects1 = objects.toArray();
                                T newSingleton = (T) constructors[0].newInstance(objects1);
                                singletons.put(clazz, newSingleton);
                                return newSingleton;
                            }
                            return (T) singletons.get(clazz);
                        }

                        else {

                            ArrayList<Object> objects = new ArrayList<>();

                            Class[] paramClasses = constructors[0].getParameterTypes();


                            Annotation[][] annotations2 = constructors[0].getParameterAnnotations();
                            int i = 0;
                            for (Class paramClass : paramClasses) {

                                if (paramClass.isInterface() && annotations2[0].length == 0) {
                                    throw new IllegalStateException();
                                }
                                Object obj; // = new Object();
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
                            T newSingleton = (T) constructors[0].newInstance(objects1);
                            singletons.put(clazz, newSingleton);
                            return newSingleton;




//                            ArrayList<Object> objects = new ArrayList<>();
//                            Class[] paramClasses = constructors[0].getParameterTypes();
//                            for (Class paramClass : paramClasses) {
//                                Object obj = this.getInstance(paramClass);
//                                objects.add(obj);
//                            }
//                            Object[] objects1 = objects.toArray();
//                            T newSingleton = (T) constructors[0].newInstance(objects1);
//                            singletons.put(clazz, newSingleton);
//                            return newSingleton;
                        }
                    }
                }
                ArrayList<Object> objects = new ArrayList<>();
                Class[] paramClasses = constructors[0].getParameterTypes();
                for (Class paramClass : paramClasses) {
                    Object obj = this.getInstance(paramClass.getName());
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
            Class clazz = Class.forName(name);
            Constructor[] constructors = clazz.getConstructors();

            if (constructors.length > 1) {
                throw new IllegalArgumentException();
            }
            else if (constructors.length == 0) {
                Annotation[] annotations = clazz.getAnnotations();
                for (Annotation a : annotations) {
                    if (a.annotationType().getSimpleName().equals("Singleton")) {
                        if (singletons != null) {
                            if (singletons.get(clazz) == null) {
                                Object currSingleton = clazz.newInstance();
                                singletons.put(clazz, currSingleton);
                                return currSingleton;
                            }
                            return singletons.get(clazz);
                        }
                        else {
                            Object currSingleton = clazz.newInstance();
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
                        if (singletons != null) {
                            if (singletons.get(clazz) == null) {
                                ArrayList<Object> objects = new ArrayList<>();
                                Class[] paramClasses = constructors[0].getParameterTypes();
                                for (Class paramClass : paramClasses) {
                                    Object obj = this.getInstance(paramClass);
                                    objects.add(obj);
                                }
                                Object[] objects1 = objects.toArray();
                                Object newSingleton = constructors[0].newInstance(objects1);
                                singletons.put(clazz, newSingleton);
                                return newSingleton;
                            }
                            return singletons.get(clazz);
                        }
                        else {
                            ArrayList<Object> objects = new ArrayList<>();
                            Class[] paramClasses = constructors[0].getParameterTypes();
                            for (Class paramClass : paramClasses) {
                                Object obj = this.getInstance(paramClass);
                                objects.add(obj);
                            }
                            Object[] objects1 = objects.toArray();
                            Object newSingleton = constructors[0].newInstance(objects1);
                            singletons.put(clazz, newSingleton);
                            return newSingleton;
                        }
                    }
                }
                ArrayList<Object> objects = new ArrayList<>();
                Class[] paramClasses = constructors[0].getParameterTypes();
                for (Class paramClass : paramClasses) {
                    Object obj = this.getInstance(paramClass.getName());
                    objects.add(obj);
                }
                Object[] objects1 = objects.toArray();
                Object obj = constructors[0].newInstance(objects1);
                return obj;
            }

            Class c = Class.forName(name);
            Object obj = c.newInstance();
            return obj;
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
