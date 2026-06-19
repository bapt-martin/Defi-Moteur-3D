package engines.graphicEngine.input;

import engines.graphicEngine.core.GraphicEngine;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class MouseInput implements MouseListener, MouseWheelListener {
    private final GraphicEngine graphicEngine;
    private final InputManager inputManager;

    public MouseInput(GraphicEngine graphicEngine, InputManager inputManager) {
        this.graphicEngine = graphicEngine;
        this.inputManager = inputManager;
    }

    // Action mouse listener methods
    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("CLICKED : " + e.getX() + ";" + e.getY());
        graphicEngine.requestFocusInWindow();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        System.out.println("PRESSED : " + e.getX() + ";" + e.getY());
        graphicEngine.requestFocusInWindow();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        System.out.println("REALEASED : " + e.getX() + ";" + e.getY());
        graphicEngine.requestFocusInWindow();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        System.out.println("ENTER : " + e.getX() + ";" + e.getY());
        graphicEngine.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        System.out.println("EXIT : " + e.getX() + ";" + e.getY());
        graphicEngine.requestFocusInWindow();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        inputManager.handleMouseWheelInput(e);
    }
}
