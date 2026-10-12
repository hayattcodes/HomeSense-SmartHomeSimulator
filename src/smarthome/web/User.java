package smarthome.web;

/** An account. The password is never stored, only a salted PBKDF2 hash. */
public class User {
    private final String username;
    private final String displayName;
    private final String salt;
    private final String hash;

    public User(String username, String displayName, String salt, String hash) {
        this.username = username;
        this.displayName = displayName;
        this.salt = salt;
        this.hash = hash;
    }

    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getSalt() { return salt; }
    public String getHash() { return hash; }
}
