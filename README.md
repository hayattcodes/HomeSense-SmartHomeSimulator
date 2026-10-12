# HomeSense - Smart Home Simulator (Java OOP web app)

Requires JDK 17 or newer. No Maven, no Spring, no external libraries.

## Run
- Windows: double-click `run.bat`
- Mac/Linux: `./run.sh`
- VS Code: open this folder, open `src/smarthome/web/WebApp.java`, click Run

Then open http://localhost:8080 and create an account.

(`smarthome.Main` is the original console version; it uses the same model classes.)

## Architecture
Browser (public/: HTML + CSS + JS)  --JSON over HTTP-->  ApiHandler  -->  Controller  -->  Device objects

| Layer | Classes |
|---|---|
| Front end | public/index.html, css/style.css, js/app.js |
| HTTP server | WebApp (com.sun.net.httpserver), StaticHandler, ApiHandler |
| Security | UserStore, PasswordHasher (PBKDF2 + salt), SessionManager (HttpOnly cookie), login lockout |
| Domain (OOP) | Device, SchedulableDevice, Light, Fan, AC, Camera, Switchable, Schedulable, Scene, Controller, EnergyReport, DeviceFactory |
| Utilities | Json (writer + parser), HomeService (one Controller per user) |

## OOP concepts and where they live
- Abstraction: `Device` (abstract), interfaces `Switchable`, `Schedulable`
- Inheritance: `Device` -> `SchedulableDevice` -> `Light`, `Fan`, `AC`; `Device` -> `Camera`
- Polymorphism: `getCurrentPower()`, `getStatus()`, `configure()`, `getSettings()` differ per subclass
- Encapsulation: private fields, validated setters, `User` keeps only a password hash
- Exception handling: `DeviceNotFoundException` (checked), `InvalidSettingException` (unchecked), `ApiException` -> HTTP status
- Collections / generics / lambdas: `Controller.forEachOfType`, `Scene.Action`, `LinkedHashMap`
- Design pattern: Factory (`DeviceFactory`)

## REST API (all JSON, cookie session)
POST /api/signup, /api/login, /api/logout; GET /api/me
GET /api/summary, /api/report, /api/log, /api/devices, /api/scenes
POST /api/devices | POST /api/devices/{id}/power|config|schedule|motion | DELETE /api/devices/{id}[/schedule]
POST /api/rooms/power, /api/scenes/run, /api/time/advance, /api/settings

Accounts are saved in `data/users.db` (hashed passwords). Devices live in memory and reset when the server restarts.
Run the app with ./run.sh
