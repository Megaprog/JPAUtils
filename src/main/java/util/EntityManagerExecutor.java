package util;

import javax.persistence.EntityManager;

/**
 * User: Tomas
 * Date: 20.06.13
 * Time: 10:55
 */
public interface EntityManagerExecutor {

    void execute(EntityManager entityManager) throws Exception;
}
