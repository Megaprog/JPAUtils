package util;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * User: Tomas
 * Date: 24.06.13
 * Time: 14:11
 */
public class HibernateUtil {

    /**
     * Initialize and unproxy Hibernate entity. Only entity itself not its collections.
     * @param maybeProxy entity object
     * @param <T> entity type
     * @return real non proxy entity
     */
    @SuppressWarnings("unchecked")
    public static <T> T unProxy(T maybeProxy) {
        if (maybeProxy == null) {
            throw new IllegalArgumentException("Entity passed for unproxy is null");
        }

        Hibernate.initialize(maybeProxy);
        if (maybeProxy instanceof HibernateProxy){
            return (T) ((HibernateProxy) maybeProxy).getHibernateLazyInitializer().getImplementation();
        }
        else {
            return maybeProxy;
        }
    }

    /**
     * Creates initialized and non proxy copy of Hibernate entity and all its fields.
     * @param maybeProxy entity object
     * @param <T> entity type
     * @return real non proxy copy of entity
     */
    public static <T> T deepUnProxy(T maybeProxy) {
        return deepUnproxy(maybeProxy, new HashSet<Object>());
    }

    @SuppressWarnings("unchecked")
    private static <T> T deepUnproxy(T maybeProxy, Set<Object> visited) {
        final T nonProxy = unProxy(maybeProxy);

        final T copy;
        Class<?> clazz = nonProxy.getClass();
        try {
            copy = (T) clazz.newInstance();
            if (visited.contains(copy)) {
                return copy;
            }
            visited.add(copy);

            do {
                processFields(nonProxy, copy, clazz.getDeclaredFields(), visited);
                clazz = clazz.getSuperclass();
            } while (clazz != null);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return copy;
    }

    @SuppressWarnings("unchecked")
    private static void processFields(Object owner, Object copy, Field[] fields, Set<Object> visited) throws IllegalAccessException {
        for (Field field : fields) {
            final int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers)) {
                continue;
            }

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            Object value = field.get(owner);

            if (value instanceof HibernateProxy) {
                value = deepUnproxy(value, visited);
            }
            else if (value instanceof Object[]) {
                Object[] valueArray = (Object[]) value;
                Object[] result = (Object[]) Array.newInstance(value.getClass(), valueArray.length);
                for (int i = 0; i < valueArray.length; i++) {
                    result[i] = deepUnproxy(valueArray[i], visited);
                }
                value = result;
            }
            else if (value instanceof Set) {
                Set valueSet = (Set) value;
                Set result = new HashSet();
                for (Object object : valueSet) {
                    result.add(deepUnproxy(object, visited));
                }
                value = result;
            }
            else if (value instanceof Map) {
                Map<Object, Object> valueMap = (Map) value;
                Map result = new HashMap();
                for (Map.Entry<Object, Object> entry : valueMap.entrySet()) {
                    result.put(deepUnproxy(entry.getKey(), visited), deepUnproxy(entry.getValue(), visited));
                }
                value = result;
            }
            else if (value instanceof List) {
                List valueList = (List) value;
                List result = new ArrayList(valueList.size());
                for (Object object : valueList) {
                    result.add(deepUnproxy(object, visited));
                }
                value = result;
            }

            field.set(copy, value);
        }
    }
}