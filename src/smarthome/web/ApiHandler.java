package smarthome.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import smarthome.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The REST back-end. Every request: parse -> check login -> call the Controller (OOP model) -> JSON reply.
 */
public class ApiHandler implements HttpHandler {
    private static final String COOKIE = "HSSESSION";
    private static final int MAX_BODY = 64 * 1024;
    private static final int MAX_DEVICES = 40;
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final long LOCKOUT_MILLIS = 60_000;

    private record Reply(int status, Object body) { }

    private final UserStore users;
    private final SessionManager sessions;
    private final HomeService homes;
    private final Map<String, long[]> failures = new ConcurrentHashMap<>();   // username -> {count, firstFailureTime}

    public ApiHandler(UserStore users, SessionManager sessions, HomeService homes) {
        this.users = users;
        this.sessions = sessions;
        this.homes = homes;
    }

    // ------------------------------------------------------------ entry
    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            route(ex);
        } catch (ApiException e) {
            send(ex, e.getStatus(), Json.obj("error", e.getMessage()));
        } catch (DeviceNotFoundException e) {
            send(ex, 404, Json.obj("error", e.getMessage()));
        } catch (InvalidSettingException | IllegalArgumentException e) {
            send(ex, 400, Json.obj("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, Json.obj("error", "Something went wrong on the server"));
        } finally {
            ex.close();
        }
    }

    private void route(HttpExchange ex) throws Exception {
        String method = ex.getRequestMethod().toUpperCase();
        String rest = ex.getRequestURI().getPath().substring("/api/".length());
        if (rest.endsWith("/")) rest = rest.substring(0, rest.length() - 1);
        String[] p = rest.isEmpty() ? new String[0] : rest.split("/");

        Map<String, Object> body = Map.of();
        if (!method.equals("GET") && !method.equals("HEAD")) body = readBody(ex);

        // ---- public endpoints
        if (p.length == 1 && method.equals("POST")) {
            switch (p[0]) {
                case "signup" -> { signup(ex, body); return; }
                case "login" -> { login(ex, body); return; }
                case "logout" -> { logout(ex); return; }
                default -> { }
            }
        }

        if (p.length == 1 && p[0].equals("me") && method.equals("GET")) {
            User current = users.find(sessions.lookup(readCookie(ex, COOKIE)));
            send(ex, 200, Json.obj("user", current == null ? null : userJson(current)));
            return;
        }

        // ---- everything else needs a valid session
        String username = sessions.lookup(readCookie(ex, COOKIE));
        User user = users.find(username);
        if (user == null) throw new ApiException(401, "Please sign in");

        Controller home = homes.homeFor(user.getUsername());
        Reply reply;
        synchronized (home) {                       // one request at a time per house
            reply = dispatch(method, p, body, user, home);
        }
        send(ex, reply.status(), reply.body());
    }

    // ------------------------------------------------------------ authentication
    private void signup(HttpExchange ex, Map<String, Object> body) throws IOException {
        User user = users.register(str(body, "username"), str(body, "name"), str(body, "password"));
        startSession(ex, user, 201);
    }

    private void login(HttpExchange ex, Map<String, Object> body) throws IOException {
        String username = str(body, "username").trim().toLowerCase();
        long now = System.currentTimeMillis();
        long[] f = failures.get(username);
        if (f != null && now - f[1] < LOCKOUT_MILLIS && f[0] >= MAX_LOGIN_FAILURES)
            throw new ApiException(429, "Too many failed attempts. Try again in a minute.");

        User user = users.authenticate(username, str(body, "password"));
        if (user == null) {
            failures.compute(username, (k, old) ->
                    (old == null || now - old[1] >= LOCKOUT_MILLIS) ? new long[]{1, now} : new long[]{old[0] + 1, old[1]});
            throw new ApiException(401, "Incorrect username or password");
        }
        failures.remove(username);
        startSession(ex, user, 200);
    }

    private void startSession(HttpExchange ex, User user, int status) throws IOException {
        String token = sessions.create(user.getUsername());
        ex.getResponseHeaders().add("Set-Cookie",
                COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + (SessionManager.TTL_MILLIS / 1000));
        send(ex, status, Json.obj("user", userJson(user)));
    }

    private void logout(HttpExchange ex) throws IOException {
        sessions.destroy(readCookie(ex, COOKIE));
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        send(ex, 200, Json.obj("ok", true));
    }

    private Map<String, Object> userJson(User u) {
        return Json.obj("username", u.getUsername(), "displayName", u.getDisplayName());
    }

    // ------------------------------------------------------------ authenticated routes
    private Reply dispatch(String method, String[] p, Map<String, Object> body, User user, Controller home)
            throws DeviceNotFoundException {
        String r = p.length > 0 ? p[0] : "";
        boolean get = method.equals("GET"), post = method.equals("POST"), del = method.equals("DELETE");

        if (r.equals("me") && get && p.length == 1) return ok(Json.obj("user", userJson(user)));
        if (r.equals("summary") && get && p.length == 1) return ok(summary(home));
        if (r.equals("report") && get && p.length == 1) return ok(report(home));
        if (r.equals("log") && get && p.length == 1) return ok(Json.obj("entries", recentLog(home)));

        if (r.equals("settings") && post && p.length == 1) {
            if (body.containsKey("rate")) home.setRatePerKwh(num(body, "rate"));
            if (body.containsKey("limit")) home.setPowerLimitWatts(num(body, "limit"));
            return ok(summary(home));
        }

        if (r.equals("time") && post && p.length == 2 && p[1].equals("advance")) {
            home.advanceTime((int) num(body, "hours"));
            return ok(summary(home));
        }

        if (r.equals("scenes")) {
            if (get && p.length == 1) {
                List<Object> list = new ArrayList<>();
                home.getScenes().forEach((key, s) ->
                        list.add(Json.obj("key", key, "name", s.getName(), "description", s.getDescription())));
                return ok(Json.obj("scenes", list));
            }
            if (post && p.length == 2 && p[1].equals("run")) {
                home.runScene(str(body, "key"));
                return ok(Json.obj("ok", true));
            }
        }

        if (r.equals("rooms") && post && p.length == 2 && p[1].equals("power")) {
            int n = home.switchRoom(str(body, "room"), bool(body, "on"));
            return ok(Json.obj("updated", n));
        }

        if (r.equals("devices")) {
            if (p.length == 1 && get) {
                List<Object> list = new ArrayList<>();
                for (Device d : home.getDevices()) list.add(deviceJson(d));
                return ok(Json.obj("devices", list, "rooms", new ArrayList<>(home.getRooms())));
            }
            if (p.length == 1 && post) {
                if (home.getDevices().size() >= MAX_DEVICES)
                    throw new ApiException(400, "Device limit reached (" + MAX_DEVICES + ")");
                String name = str(body, "name").trim();
                String room = str(body, "room").trim();
                if (name.length() > 30 || room.length() > 30)
                    throw new ApiException(400, "Name and room must be 30 characters or fewer");
                Device d = DeviceFactory.create(str(body, "type"), name, room);
                home.addDevice(d);
                return new Reply(201, deviceJson(d));
            }
            if (p.length >= 2) {
                int id = parseId(p[1]);
                if (p.length == 2 && del) {
                    home.removeDevice(id);
                    return ok(Json.obj("ok", true));
                }
                if (p.length == 3 && post) {
                    switch (p[2]) {
                        case "power" -> home.switchDevice(id, bool(body, "on"));
                        case "config" -> home.configure(id, str(body, "setting"), str(body, "value"));
                        case "schedule" -> home.setSchedule(id, (int) num(body, "on"), (int) num(body, "off"));
                        case "motion" -> {
                            String msg = home.simulateMotion(id);
                            return ok(Json.obj("message", msg, "device", deviceJson(home.getDevice(id))));
                        }
                        default -> throw new ApiException(404, "Unknown endpoint");
                    }
                    return ok(deviceJson(home.getDevice(id)));
                }
                if (p.length == 3 && del && p[2].equals("schedule")) {
                    home.clearSchedule(id);
                    return ok(deviceJson(home.getDevice(id)));
                }
            }
        }
        throw new ApiException(404, "Unknown endpoint");
    }

    private static Reply ok(Object body) { return new Reply(200, body); }

    // ------------------------------------------------------------ JSON views of the model
    private Map<String, Object> deviceJson(Device d) {
        Map<String, Object> m = Json.obj(
                "id", d.getId(), "name", d.getName(), "type", d.getType(), "room", d.getRoom(),
                "on", d.isOn(), "watts", d.getWattsNow(), "status", d.getStatus(),
                "energyKwh", d.getEnergyKwh(), "hoursOn", d.getHoursOn(), "settings", d.getSettings());
        boolean schedulable = d instanceof Schedulable;
        m.put("schedulable", schedulable);
        if (d instanceof Schedulable s)
            m.put("schedule", s.hasSchedule() ? Json.obj("on", s.getOnHour(), "off", s.getOffHour()) : null);
        return m;
    }

    private Map<String, Object> summary(Controller home) {
        EnergyReport rep = home.generateReport();
        int active = 0;
        for (Device d : home.getDevices()) if (d.isOn()) active++;
        double watts = home.totalWattsNow();
        return Json.obj(
                "day", home.getDay(), "hour", home.getHour(), "clock", home.getClock(),
                "totalWatts", watts, "powerLimit", home.getPowerLimitWatts(),
                "overload", watts > home.getPowerLimitWatts(),
                "deviceCount", home.getDevices().size(), "activeCount", active,
                "totalKwh", rep.totalKwh(), "totalCost", rep.totalCost(), "rate", home.getRatePerKwh());
    }

    private Map<String, Object> report(Controller home) {
        EnergyReport rep = home.generateReport();
        List<Object> rows = new ArrayList<>();
        for (Device d : rep.getDevices())
            rows.add(Json.obj("id", d.getId(), "name", d.getName(), "type", d.getType(), "room", d.getRoom(),
                    "hours", d.getHoursOn(), "kwh", d.getEnergyKwh(), "cost", d.getEnergyKwh() * rep.getRatePerKwh()));
        Map<String, Object> top = null;
        Optional<Device> t = rep.topConsumer();
        if (t.isPresent())
            top = Json.obj("name", t.get().getName(), "room", t.get().getRoom(),
                    "share", t.get().getEnergyKwh() / rep.totalKwh() * 100);
        return Json.obj("totalKwh", rep.totalKwh(), "totalCost", rep.totalCost(), "rate", rep.getRatePerKwh(),
                "byRoom", rep.byRoom(), "byType", rep.byType(), "devices", rows,
                "top", top, "suggestion", rep.suggestion());
    }

    private List<Object> recentLog(Controller home) {
        List<String> all = home.getLog();
        List<Object> out = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && out.size() < 100; i--) out.add(all.get(i));
        return out;
    }

    // ------------------------------------------------------------ helpers
    private static String str(Map<String, Object> b, String key) {
        Object v = b.get(key);
        if (v == null) throw new ApiException(400, "Missing field: " + key);
        return v instanceof Double dd && dd == Math.rint(dd) ? String.valueOf(dd.longValue()) : String.valueOf(v);
    }

    private static double num(Map<String, Object> b, String key) {
        Object v = b.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) { }
        }
        throw new ApiException(400, "Field '" + key + "' must be a number");
    }

    private static boolean bool(Map<String, Object> b, String key) {
        Object v = b.get(key);
        if (v instanceof Boolean x) return x;
        throw new ApiException(400, "Field '" + key + "' must be true or false");
    }

    private static int parseId(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { throw new ApiException(400, "Invalid device id"); }
    }

    private static String readCookie(HttpExchange ex, String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String h : headers)
            for (String part : h.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(name)) return kv[1];
            }
        return null;
    }

    private static Map<String, Object> readBody(HttpExchange ex) throws IOException {
        String type = ex.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.toLowerCase().startsWith("application/json"))
            throw new ApiException(415, "Content-Type must be application/json");   // also blocks cross-site form posts
        byte[] data;
        try (InputStream in = ex.getRequestBody()) {
            data = in.readNBytes(MAX_BODY + 1);
        }
        if (data.length > MAX_BODY) throw new ApiException(413, "Request too large");
        String text = new String(data, StandardCharsets.UTF_8).trim();
        return text.isEmpty() ? Map.of() : Json.parseObject(text);
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
