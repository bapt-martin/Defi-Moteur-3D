package engines.graphicEngine.input;

import engines.graphicEngine.core.GraphicEngineContext;
import engines.graphicEngine.math.geometry.Vertex3D;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class MouseMotionInput implements MouseMotionListener {
    private final GraphicEngineContext graphicEngineContext;
    private final InputManager inputManager;

    private final double mouseSensitivity = 0.01;
    private Vertex3D winLastMousePosition;
    private boolean firstMouseMove = true;// To input manager?

    public MouseMotionInput(GraphicEngineContext graphicEngineContext, InputManager inputManager) {
        this.inputManager = inputManager;
        this.winLastMousePosition = new Vertex3D(0,0,0);
        this.graphicEngineContext = graphicEngineContext;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        inputManager.handleMouseMoving(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        System.out.println("dragggggged : " + e.getX() + ", " + e.getY());
    }

    public void manageFirstInput() {
        if(this.isFirstMouseMove()) {
            this.setWinLastMousePosition(new Vertex3D(graphicEngineContext.getWindowWidth()/2.0, graphicEngineContext.getWindowHeight()/2.0,0));
            this.setFirstMouseMove(false);
        }
    }

    public double getMouseSensitivity() {
        return mouseSensitivity;
    }

    public Vertex3D getWinLastMousePosition() {
        return winLastMousePosition;
    }

    public void setWinLastMousePosition(Vertex3D winLastMousePosition) {
        this.winLastMousePosition = winLastMousePosition;
    }

    public boolean isFirstMouseMove() {
        return firstMouseMove;
    }

    public void setFirstMouseMove(boolean firstMouseMove) {
        this.firstMouseMove = firstMouseMove;
    }
}
