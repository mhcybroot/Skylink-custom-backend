package root.cyb.mh.attendancesystem.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitorWebSocketHandler extends BinaryWebSocketHandler {

    // employeeId -> session
    private final Map<String, WebSocketSession> employeeSessions = new ConcurrentHashMap<>();
    // employeeId -> admin session watching them
    private final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.split("token=")[1];
            // Simplification: In a real app we'd validate the token and get the employee/admin ID.
            // For now, we assume token is the employee ID for employees, and "admin_" + employeeId for admins watching.
            if (token.startsWith("admin_")) {
                String targetEmployeeId = token.replace("admin_", "");
                adminSessions.put(targetEmployeeId, session);
                WebSocketSession empSession = employeeSessions.get(targetEmployeeId);
                if (empSession != null && empSession.isOpen()) {
                    empSession.sendMessage(new TextMessage("START_STREAM"));
                }
            } else {
                employeeSessions.put(token, session);
                // Check if admin is already waiting for this employee
                WebSocketSession adminSession = adminSessions.get(token);
                if (adminSession != null && adminSession.isOpen()) {
                    session.sendMessage(new TextMessage("START_STREAM"));
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String token = getToken(session);
        if (token != null) {
            if (token.startsWith("admin_")) {
                String targetEmployeeId = token.replace("admin_", "");
                WebSocketSession empSession = employeeSessions.get(targetEmployeeId);
                if (empSession != null && empSession.isOpen()) {
                    try {
                        empSession.sendMessage(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                WebSocketSession adminSession = adminSessions.get(token);
                if (adminSession != null && adminSession.isOpen()) {
                    try {
                        adminSession.sendMessage(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // Find which employee this is
        String employeeId = getEmployeeId(session);
        if (employeeId != null) {
            WebSocketSession adminSession = adminSessions.get(employeeId);
            if (adminSession != null && adminSession.isOpen()) {
                adminSession.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String token = getToken(session);
        if (token != null) {
            if (token.startsWith("admin_")) {
                String targetEmployeeId = token.replace("admin_", "");
                adminSessions.remove(targetEmployeeId);
                WebSocketSession empSession = employeeSessions.get(targetEmployeeId);
                if (empSession != null && empSession.isOpen()) {
                    empSession.sendMessage(new TextMessage("STOP_STREAM"));
                }
            } else {
                employeeSessions.remove(token);
            }
        }
    }

    private String getToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1];
        }
        return null;
    }

    private String getEmployeeId(WebSocketSession session) {
        for (Map.Entry<String, WebSocketSession> entry : employeeSessions.entrySet()) {
            if (entry.getValue().getId().equals(session.getId())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
