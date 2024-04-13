package nl.picobello.basecamp.shared;

import java.util.Map;

public class Session {
    private static final Session instance = new Session();
    private String username;

    private Map<String, String> incomingChallenge;

    private Session() {
    }

    public static Session getInstance() {
        return instance;
    }

    public Map<String, String> getIncomingChallenge() {
        return incomingChallenge;
    }

    public void setIncomingChallenge(Map<String, String> newMap) {
        this.incomingChallenge = newMap;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}
