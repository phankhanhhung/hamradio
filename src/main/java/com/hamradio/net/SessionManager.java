package com.hamradio.net;

import java.util.*;

public class SessionManager {

    public static class Session {
        private final String id;
        private final String station1;
        private final String station2;
        private final long startTime;
        private boolean active;

        public Session(String station1, String station2) {
            this.id = station1 + "-" + station2 + "-" + System.currentTimeMillis();
            this.station1 = station1;
            this.station2 = station2;
            this.startTime = System.currentTimeMillis();
            this.active = true;
        }

        public String getId() { return id; }
        public String getStation1() { return station1; }
        public String getStation2() { return station2; }
        public long getStartTime() { return startTime; }
        public boolean isActive() { return active; }
        public void close() { this.active = false; }
    }

    private final Map<String, Session> sessions = new LinkedHashMap<>();

    public Session createSession(String station1, String station2) {
        Session session = new Session(station1, station2);
        sessions.put(session.getId(), session);
        return session;
    }

    public void closeSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.close();
        }
    }

    public void closeAll() {
        sessions.values().forEach(Session::close);
    }

    public List<Session> getActiveSessions() {
        List<Session> active = new ArrayList<>();
        for (Session s : sessions.values()) {
            if (s.isActive()) active.add(s);
        }
        return active;
    }

    public Session getSession(String id) { return sessions.get(id); }
}
