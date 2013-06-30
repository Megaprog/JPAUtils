package util;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * User: Tomas
 * Date: 20.06.13
 * Time: 10:28
 */
public class JPAUtil {

    public static void executeTransaction(EntityManager entityManager, EntityManagerExecutor executor) {
        executeTransaction(entityManager, executor, true);
    }

    public static void executeTransaction(EntityManager entityManager, EntityManagerExecutor executor, boolean closeEM) {
        final EntityTransaction transaction = entityManager.getTransaction();
        if (transaction.isActive()) {
            try {
                executor.execute(entityManager);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                transaction.begin();

                executor.execute(entityManager);

                if (transaction.isActive()) {
                    if (transaction.getRollbackOnly()) {
                        transaction.rollback();
                    }
                    else {
                        transaction.commit();
                    }
                }
            }
            catch (Exception e) {
                if (transaction.isActive() ) {
                    transaction.rollback();
                }

                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                else {
                    throw new RuntimeException(e);
                }
            }
            finally {
                if (closeEM) {
                    entityManager.close();
                }
            }
        }
    }

    public static <T> T findSafe(EntityManager entityManager, Class<T> entityClass, Object primaryKey) {
        return findSafe(entityManager, entityClass, primaryKey, "Entity");
    }

    public static <T> T findSafe(EntityManager entityManager, Class<T> entityClass, Object primaryKey, String entityName) {
        final T entity = entityManager.find(entityClass, primaryKey);
        if (entity == null) {
            throw new IllegalArgumentException(entityName + " with id " + primaryKey + " was not found");
        }

        return entity;
    }
}
