package smarthome.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/** Entry point of the web application: run this class, then open http://localhost:8080 */
public class WebApp {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path publicDir = locatePublicDir();

        UserStore users = new UserStore(publicDir.getParent().resolve("data").resolve("users.db"));
        SessionManager sessions = new SessionManager();
        HomeService homes = new HomeService();

        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/api/", new ApiHandler(users, sessions, homes));
        server.createContext("/", new StaticHandler(publicDir));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("HomeSense is running.");
        System.out.println("Open  http://localhost:" + port + "  in your browser.   (Ctrl+C to stop)");
        System.out.println("Front-end files: " + publicDir);
    }

    /** Finds the public/ folder whether you run from the project root, VS Code or the out/ folder. */
    private static Path locatePublicDir() {
        List<Path> candidates = new ArrayList<>();
        Path cwd = Path.of("").toAbsolutePath();
        candidates.add(cwd.resolve("public"));
        candidates.add(cwd.resolve("SmartHomeSimulator").resolve("public"));
        candidates.add(cwd.getParent() == null ? cwd : cwd.getParent().resolve("public"));
        try {
            Path classes = Path.of(WebApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (classes.getParent() != null) candidates.add(classes.getParent().resolve("public"));
        } catch (URISyntaxException | RuntimeException ignored) { }
        for (Path p : candidates)
            if (Files.isRegularFile(p.resolve("index.html"))) return p;
        throw new IllegalStateException("Could not find the 'public' folder. Run the app from the project root "
                + "(the folder that contains 'public' and 'src'). Looked in: " + candidates);
    }
}
