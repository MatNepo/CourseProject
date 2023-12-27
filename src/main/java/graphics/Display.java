package graphics;


import controller.CLint;
import controller.Instruction;
import engine.Particle;
import engine.Settings;
import engine.WorldMap;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

//класс для отображения графического окна
public class Display {
    private long window;//идентификатор
    public final int width;//ширина окна
    public final int height;//высота окна
    public final String name;//название окна

    private final WorldMap worldMap;//ссылка на движок симуляции для получения значений
    private final CLint cLint;//ссылка на контроллер для взаимодействия с ним

    private long pTime = 0L;
    private int fCount = 0, last_fCount = 0;

    //функция для расчёта FPS
    private void frameRate() {
        long cTime = System.currentTimeMillis();
        if (cTime - pTime >= 1000L) {
            last_fCount = (int) ((float) fCount / (cTime - pTime) * 1000.0);
            fCount = 0;
            pTime = cTime;
        }
        fCount++;
    }

    //конструктор графического окна, идёт привязка к контроллеру и движку симуляции
    public Display(CLint cLint, WorldMap worldMap) {
        this.cLint = cLint;
        this.worldMap = worldMap;
        width = Settings.display_width;
        height = Settings.display_height;
        name = "debug";
        System.out.println(getDescription() + " created");
    }

    private String getDescription() {
        return String.format("graphics.Display (%s, %s) \"%s\" fps=%s tps=%s status=%s",
                width, height, name, last_fCount, worldMap.getTPS(), cLint.getStatus());
    }


    //инициализация OpenGL через lwjgl
    private void initialize() {
        // Set up an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(width, height, name, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");


        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (videoMode == null) throw new RuntimeException("Failed ti get resolution of the primary monitor");

            // Center the window
            glfwSetWindowPos(
                    window,
                    (videoMode.width() - pWidth.get(0)) / 2,
                    (videoMode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
        System.out.println(getDescription() + " initialized");
    }

    //функция запуска отрисовки
    public void run() {
        System.out.println(getDescription() + " launched with LWJGL " + Version.getVersion());

        initialize();//инициализация
        processGUI();//цикл отрисовки

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();

        System.out.println(getDescription() + " stopped");
    }


    //функция отрисовки текущего состояния клеток
    private void drawField() {
        for (int i = 0; i < worldMap.width; i++) {
            for (int j = 0; j < worldMap.height; j++) {
                drawElem(i, j,
                        worldMap.get(i, j),
                        worldMap.get_oxygen(i, j),
                        worldMap.get_minerals(i, j),
                        worldMap.get_energy(i, j),
                        worldMap.get_ore(i, j));
            }
        }
    }

    //пересчёт координат из системы движка в систему для OpenGL
    private float fXCord(int x) {
        return -1.0f + (float) x / this.worldMap.width * 2;
    }
    private float fYCord(int y) {
        return -1.0f + (float) y / this.worldMap.height * 2;
    }

    //отрисовка одной клетки
    private void drawElem(int x, int y, Particle particle, int oxygen, int minerals, int energy, int ore) {
        final float fx = fXCord(x), fxp = fXCord(x + 1);
        final float fy = fYCord(y), fyp = fYCord(y + 1);

        //отрисовка состояния клетки
        glBegin(GL_TRIANGLE_STRIP);
        Particle.setParticleColor(particle);
        glVertex2f(fx, fy);
        glVertex2f(fxp, fy);
        glVertex2f(fx, fyp);
        glVertex2f(fxp, fyp);
        glEnd();

        //отрисовка состояния запасов ресурсов(опционально)
        if (Settings.draw_resources.get()) {
            glBegin(GL_TRIANGLES);
            Particle.setOxygenColor(oxygen);
            glVertex2f(fx, fyp);
            glVertex2f(fx, (fyp + fy) / 2.0f);
            glVertex2f((fxp + fx) / 2.0f, fyp);


            Particle.setMineralsColor(minerals);
            glVertex2f(fxp, fy);
            glVertex2f(fxp, (fyp + fy) / 2.0f);
            glVertex2f((fxp + fx) / 2.0f, fy);


            Particle.setEnergyColor(energy);
            glVertex2f(fxp, fyp);
            glVertex2f(fxp, (fyp + fy) / 2.0f);
            glVertex2f((fxp + fx) / 2.0f, fyp);

            Particle.setOreColor(ore);
            glVertex2f(fx, fy);
            glVertex2f(fx, (fyp + fy) / 2.0f);
            glVertex2f((fxp + fx) / 2.0f, fy);
            glEnd();
        }
    }

    //функция с циклом отрисовки
    private void processGUI() {
        GL.createCapabilities();
        glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        //цикл отрисовки
        while (!glfwWindowShouldClose(window) && cLint.getStatus() != Instruction.STOP) {
            cLint.handleInput(window);//обработка ввода
            frameRate();//расчёт FPS
            drawField();//отрисовка клеток

            glfwSetWindowTitle(window, getDescription());//изменения отображаемого текста
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        cLint.setExitStatus();//команда к завершению исполнения программы
    }
}
