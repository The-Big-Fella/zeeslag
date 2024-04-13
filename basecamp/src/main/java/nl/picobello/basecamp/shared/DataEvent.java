package nl.picobello.basecamp.shared;

import java.util.EventObject;
import java.util.HashMap;

public class DataEvent extends EventObject {
    private final HashMap<String, String> data;
    private final ServerEvents type;

    public DataEvent(Object source, HashMap<String, String> data, ServerEvents type) {
        super(source);
        this.data = data;
        this.type = type;
    }

    public HashMap<String, String> getData() {
        return this.data;
    }

    public ServerEvents getType() {
        return this.type;
    }
}
