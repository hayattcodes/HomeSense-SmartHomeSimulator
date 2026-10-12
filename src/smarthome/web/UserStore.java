package smarthome.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Registers and authenticates users; persists them to a small text file (data/users.db). */
public class UserStore {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,20}");

    private final Path file;
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserStore(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] f = line.split("\t");
                if (f.length == 4) users.put(f[0].toLowerCase(), new User(f[0], f[1], f[2], f[3]));
            }
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": " + e.getMessage());
        }
    }

    private void save() {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            List<String> lines = new ArrayList<>();
            for (User u : users.values())
                lines.add(String.join("\t", u.getUsername(), u.getDisplayName(), u.getSalt(), u.getHash()));
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(500, "Could not save account");
        }
    }

    public synchronized User register(String username, String displayName, String password) {
        username = username == null ? "" : username.trim();
        displayName = displayName == null ? "" : displayName.replaceAll("[\\p{Cntrl}]", "").trim();
        if (!USERNAME.matcher(username).matches())
            throw new ApiException(400, "Username must be 3-20 letters, numbers or underscores");
        if (displayName.length() < 2 || displayName.length() > 40)
            throw new ApiException(400, "Name must be 2-40 characters");
        if (password == null || password.length() < 8)
            throw new ApiException(400, "Password must be at least 8 characters");
        if (password.length() > 100)
            throw new ApiException(400, "Password is too long");
        if (users.containsKey(username.toLowerCase()))
            throw new ApiException(409, "That username is already taken");

        String salt = PasswordHasher.newSalt();
        User user = new User(username, displayName, salt, PasswordHasher.hash(password, salt));
        users.put(username.toLowerCase(), user);
        save();
        return user;
    }

    /** Returns the user if the credentials are right, otherwise null. */
    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;
        User u = users.get(username.trim().toLowerCase());
        if (u == null) {
            PasswordHasher.hash(password, PasswordHasher.newSalt());   // same cost either way
            return null;
        }
        return PasswordHasher.verify(password, u.getSalt(), u.getHash()) ? u : null;
    }

    public User find(String username) {
        return username == null ? null : users.get(username.toLowerCase());
    }
}
