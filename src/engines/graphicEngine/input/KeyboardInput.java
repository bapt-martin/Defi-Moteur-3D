package engines.graphicEngine.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyboardInput implements KeyListener {
    private final InputManager inputManager;
    private final boolean[] keysPressed = new boolean[256];

    public KeyboardInput(InputManager inputManager) {
        this.inputManager = inputManager;
    }

    // Action listener methods
    @Override
    public void keyPressed(KeyEvent e) {
        keysPressed[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keysPressed[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public boolean[] getKeysPressed() {
        return keysPressed;
    }
}
