package controller;

import engine.Settings;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;

//контроллер
public class CLint {
    //текущий статус программы
    private final AtomicReference<Instruction> atomicStatus = new AtomicReference<>();

    //локеры для чтения ввода (для удаления залипания/дребезга)
    private final Map<Instruction, Boolean> instructions;

    public CLint() {
        //this.display = display;
        this.atomicStatus.set(Instruction.CONTINUE);
        this.instructions = new HashMap<>();
        for (Instruction i : Instruction.values()) instructions.put(i, true);
    }

    public Instruction getStatus() {
        return atomicStatus.get();
    }

    public void setExitStatus() {
        atomicStatus.set(Instruction.STOP);
    }

    //обработка ввода с клавиатуры
    public void handleInput(long window) {
        keyHandler(window, Instruction.STOP, GLFW_KEY_Q, () -> this.atomicStatus.set(Instruction.STOP));
        keyHandler(window, Instruction.PAUSE, GLFW_KEY_W, () -> this.atomicStatus.set(Instruction.PAUSE));
        keyHandler(window, Instruction.CONTINUE, GLFW_KEY_E, () -> this.atomicStatus.set(Instruction.CONTINUE));
        keyHandler(window, Instruction.AUX1, GLFW_KEY_Z, () -> Settings.draw_resources.set(true));
        keyHandler(window, Instruction.AUX2, GLFW_KEY_X, () -> Settings.draw_resources.set(false));
    }

    //обработка ввода конкретной клавиши
    private void keyHandler(long window, Instruction i, int key, Runnable runnable) {
        if (instructions.get(i) && glfwGetKey(window, key) == GLFW_PRESS) {
            runnable.run();
            instructions.put(i, false);
        }
        if (!instructions.get(i) && glfwGetKey(window, key) == GLFW_RELEASE) {
            instructions.put(i, true);
        }
    }

}
