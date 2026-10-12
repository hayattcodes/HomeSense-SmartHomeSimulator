package smarthome;

import java.util.ArrayList;
import java.util.List;

/** A named bundle of actions (e.g. "Night Mode") that the Controller can run in one go. */
public class Scene {
    @FunctionalInterface
    public interface Action {
        void run(Controller controller);
    }

    private final String name;
    private final String description;
    private final List<Action> actions = new ArrayList<>();

    public Scene(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Scene addAction(Action a) {
        actions.add(a);
        return this;
    }

    public void execute(Controller c) {
        for (Action a : actions) a.run(c);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
