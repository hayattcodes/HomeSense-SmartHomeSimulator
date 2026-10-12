package smarthome.web;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory login sessions identified by a random token stored in an HttpOnly cookie. */
public class SessionManager {
    public static final long TTL_MILLIS = 8L * 60 * 60 * 1000;   // 8 hours

    private static final class Session {
        final String username;
        final long expiresAt;
        Session(String username, long expiresAt) { this.username = username; this.expiresAt = expiresAt; }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String create(String username) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(username, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /** Returns the username for a valid token, or null. */
    public String lookup(String token) {
        if (token == null) return null;
        Session s = sessions.get(token);
        if (s == null) return null;
        if (s.expiresAt < System.currentTimeMillis()) {
            sessions.remove(token);
            return null;
        }
        return s.username;
    }

    public void destroy(String token) {
        if (token != null) sessions.remove(token);
    }
}
