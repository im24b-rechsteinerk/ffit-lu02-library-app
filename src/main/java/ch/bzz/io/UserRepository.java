package ch.bzz.io;

import ch.bzz.Config;
import ch.bzz.model.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("localPU", Config.getProperties());

    public void save(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                em.persist(user);
                em.getTransaction().commit();
                log.info("Saved user to database: {}", user.getEmail());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                log.error("Error during saving of user to the database:", e);
                throw e;
            }
        }
    }
    public User getByEmail(String email) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.error("Error during getting user by email from the database:", e);
            throw e;
        }
    }
    public void close() {
        emf.close();
    }
}
