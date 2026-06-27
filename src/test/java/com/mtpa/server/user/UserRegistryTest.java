package com.mtpa.server.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRegistryTest {

    private final UserRegistry registry = new UserRegistry();

    @Test
    void registraUnUsuarioYLeAsignaUnaClave() {
        User user = registry.register("ana123");

        assertEquals("ana123", user.getUsername());
        assertTrue(registry.exists("ana123"));
    }

    @Test
    void doRegistrosGeneranClavesDistintas() {
        User primero = registry.register("ana123");
        User segundo = registry.register("bob456");

        assertNotEquals(primero.getAccessKey(), segundo.getAccessKey());
    }

    @Test
    void rechazaUnNombreDeUsuarioYaRegistrado() {
        registry.register("ana123");

        assertThrows(UsernameAlreadyExistsException.class, () -> registry.register("ana123"));
    }

    @Test
    void rechazaUnNombreDeUsuarioConFormatoInvalido() {
        assertThrows(InvalidUsernameException.class, () -> registry.register("a"));
        assertThrows(InvalidUsernameException.class, () -> registry.register("nombre con espacios"));
        assertThrows(InvalidUsernameException.class, () -> registry.register(null));
    }

    @Test
    void permiteLoginConCredencialesCorrectas() {
        User registrado = registry.register("ana123");

        User logueado = registry.login("ana123", registrado.getAccessKey());

        assertEquals(registrado.getUsername(), logueado.getUsername());
    }

    @Test
    void rechazaLoginConClaveIncorrecta() {
        User registrado = registry.register("ana123");

        assertThrows(InvalidCredentialsException.class,
                () -> registry.login("ana123", registrado.getAccessKey() + 1));
    }

    @Test
    void rechazaLoginConUsuarioInexistente() {
        assertThrows(InvalidCredentialsException.class, () -> registry.login("fantasma", 1000L));
    }

    @Test
    void restaurarUnUsuarioAvanzaElContadorDeClavesParaEvitarColisiones() {
        registry.restore(new User("restaurado", 5000L));

        User nuevo = registry.register("nuevoUser");

        assertTrue(nuevo.getAccessKey() > 5000L);
        assertTrue(registry.exists("restaurado"));
    }
}
