package com.mtpa.server.user;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Alta y autenticacion de usuarios. El nombre de usuario es unico porque se
 * inserta de forma atomica ({@code putIfAbsent}), y la clave de acceso es
 * unica porque es un contador siempre creciente.
 */
public class UserRegistry {

    private static final long FIRST_ACCESS_KEY = 1000L;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final AtomicLong nextAccessKey = new AtomicLong(FIRST_ACCESS_KEY);

    public User register(String username) {
        validateUsername(username);

        User user = new User(username, nextAccessKey.getAndIncrement());
        User previous = usersByUsername.putIfAbsent(username, user);
        if (previous != null) {
            throw new UsernameAlreadyExistsException(username);
        }
        return user;
    }

    /** Repone un usuario ya existente (cargado de persistencia) sin validar ni reasignar clave. */
    public void restore(User user) {
        usersByUsername.put(user.getUsername(), user);
        nextAccessKey.updateAndGet(current -> Math.max(current, user.getAccessKey() + 1));
    }

    public User login(String username, long accessKey) {
        User user = username == null ? null : usersByUsername.get(username);
        if (user == null || user.getAccessKey() != accessKey) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    public boolean exists(String username) {
        return username != null && usersByUsername.containsKey(username);
    }

    public Collection<User> allUsers() {
        return usersByUsername.values();
    }

    private void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new InvalidUsernameException(username);
        }
    }
}
