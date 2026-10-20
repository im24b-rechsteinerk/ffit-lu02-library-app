package ch.bzz;

import ch.bzz.db.BookPersistor;
import ch.bzz.io.UserRepository;
import ch.bzz.model.User;
import ch.bzz.security.JwtHandler;
import ch.bzz.security.PasswordHandler;

import io.javalin.Javalin;

import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavalinMain {

    private static final Logger log = LoggerFactory.getLogger(JavalinMain.class);
    private static final int PORT = 7070;

    private static final BookPersistor bookPersistor = new BookPersistor();
    private static final UserRepository userRepository = new UserRepository();

    public static void main(String[] args) {
        Javalin app = Javalin.create();

        app.get("/books", ctx -> {
            int limit = 0;
            String limitParam = ctx.queryParam("limit");
            if (limitParam != null && !limitParam.isBlank()) {
                limit = Integer.parseInt(limitParam);
            }
            ctx.json(bookPersistor.getAll(limit));
        });

        app.post("/auth/login", ctx -> {
            var json = ctx.bodyValidator(Map.class)
                    .check(m -> m.containsKey("email"), "email is required")
                    .check(m -> m.containsKey("password"), "password is required")
                    .get();

            String inputEmail = (String) json.get("email");
            String inputPassword = (String) json.get("password");

            if (inputEmail == null || inputPassword == null) {
                ctx.status(400).json(Map.of("error", "email and password are required"));
                return;
            }

            User user = userRepository.getByEmail(inputEmail);

            if (user != null) {
                byte[] storedSalt = Base64.getDecoder().decode(user.getPasswordSalt());
                byte[] storedHash = Base64.getDecoder().decode(user.getPasswordHash());

                if (PasswordHandler.verifyPassword(inputPassword, storedHash, storedSalt)) {
                    String jwt = JwtHandler.createJwt(inputEmail, user.getId());
                    ctx.json(Map.of("token", jwt));
                    return;
                }
            }

            log.info("Failed login attempt for email {}", inputEmail);
            ctx.status(401).json(Map.of("error", "Invalid email or password"));
        });

        app.start(PORT);
        log.info("Javalin server started on port {}", PORT);
    }
}
