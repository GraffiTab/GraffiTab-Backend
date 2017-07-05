package com.graffitab.server.service;

import lombok.extern.log4j.Log4j2;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.joda.time.DateTime;
import org.springframework.util.ClassUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class ProxyUtilities {

	private static final Map<Class<?>, List<Method>> GETTERS_CACHE = new ConcurrentHashMap<>();

	public static Class<?> unwrapProxyAndGetClass(Object mayBeProxy) {

        Class<?> entityClass;
        Class<?> objectClass = mayBeProxy.getClass();

        if (mayBeProxy.getClass() == JavassistLazyInitializer.class || mayBeProxy instanceof HibernateProxy) {
            LazyInitializer lazyInitializer;

            if (mayBeProxy instanceof HibernateProxy) {
                lazyInitializer = ((HibernateProxy) mayBeProxy).getHibernateLazyInitializer();
            } else {
                lazyInitializer = (LazyInitializer) mayBeProxy;
            }

            entityClass = lazyInitializer.getImplementation().getClass();

        } else if (objectClass.getName().contains("$$")) {
        	// "$$" => this is a CGLib proxy
        	// CGLib generates subclasses of the desired class, so
        	// pull the superclass to get the actual class.
        	entityClass = objectClass.getSuperclass();

        } else {
            entityClass = mayBeProxy.getClass();
        }

        return entityClass;
    }

	public static Object unwrapProxy(Object proxy) {

		if (proxy == null) {
			return null;
		}

        if (proxy instanceof HibernateProxy) {
            LazyInitializer lazyInitializer = ((HibernateProxy) proxy).getHibernateLazyInitializer();
            return lazyInitializer.getImplementation();
        }
        return proxy;
    }

	public static void initializeObjectWithOneLevelCollections(Object object) {
		initializeObject(object, false);
	}

	private static void initializeObject(Object object, boolean isNestedCollectionElement) {

        if (null == object) {
            return;
        }

        Class<?> clazz = object.getClass();

        if (isPrimitiveType(clazz) || Enum.class.isAssignableFrom(object.getClass())) {
        	// Do nothing
        	return;
        }

        if (!Collection.class.isAssignableFrom(clazz) && !Map.class.isAssignableFrom(object.getClass())) {
            initializeProxy(object, isNestedCollectionElement);
        }
    }

    public static void initializeCollection(Object object) {
        if (object instanceof HibernateProxy && !Hibernate.isInitialized(object)) {
            Hibernate.initialize(object);
        }

        Iterable<?> iterable = (Iterable<?>) object;

        for (Object item : iterable) {
            initializeObject(item, true);
        }
   }

   private static void initializeProxy(Object object, boolean isNestedCollectionElement) {
	   Object proxyObject = object;
       if (proxyObject instanceof HibernateProxy && !Hibernate.isInitialized(proxyObject)) {
           Hibernate.initialize(proxyObject);
       }

       Object getterResult;
       Class<?> entityClass = unwrapProxyAndGetClass(proxyObject);
       List<Method> getters = getGetterMethods(entityClass);

       for (Method method : getters) {

		   try {

		       if (proxyObject.getClass() == JavassistLazyInitializer.class) {

		           JavassistLazyInitializer lazyInitializer = (JavassistLazyInitializer) proxyObject;
		           getterResult = lazyInitializer.invoke(proxyObject, method, method, new Object[]{});
		       } else if (proxyObject instanceof HibernateProxy) {

		           LazyInitializer lazyInitializer = ((HibernateProxy) proxyObject).getHibernateLazyInitializer();
		           proxyObject = lazyInitializer.getImplementation();

		           getterResult = method.invoke(proxyObject);
		       } else {

		           getterResult = method.invoke(proxyObject);
		       }

		       initializeObject(getterResult, isNestedCollectionElement);
		   } catch (Throwable e) {
		       log.fatal("Exception invoking getter. " + "Method name - [" + proxyObject.getClass() + "." + method.getName() + "].", e);
		   }
       }
   }


   private static List<Method> getGetterMethods(Class<?> clazz) {
   	List<Method> getters = GETTERS_CACHE.get(clazz);

   	if (getters == null) {
   		if (log.isTraceEnabled()) {
   			log.trace("Caching getters for class: " + clazz.getCanonicalName());
   		}
   		getters = new ArrayList<>();
   		try {
    	   for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
    		   if (propertyDescriptor.getReadMethod() != null) {
    		       getters.add(propertyDescriptor.getReadMethod());
    		   }
    		}

   		} catch (IntrospectionException ie) {
       	   String message = "Unable to get getter methods for class: " + clazz.getName();
       	   log.error(message, ie);
       	   throw new RuntimeException(message, ie);
   		}

   		GETTERS_CACHE.put(clazz, getters);
   	}
       return getters;
   }

   private static boolean isPrimitiveType(Class<?> clazz) {
	   return ClassUtils.isPrimitiveWrapper(clazz) || clazz.equals(String.class) || clazz.equals(DateTime.class) ||
	           clazz.equals(Class.class);
   }
}
