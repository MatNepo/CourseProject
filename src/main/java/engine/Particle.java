package engine;

import static org.lwjgl.opengl.GL11.glColor3d;

//enum-класс состояний клетки
public enum Particle {
    EMPTY, SOLAR, MINER, TRANSPORT, EXHAUSTED, TOXIC, DEAD, FIRE;


    public static int getParticleCount() {
        return values().length;
    }

    public static int getParticleId(Particle particle) {
        return switch (particle) {
            case EMPTY -> 0;
            case SOLAR -> 1;
            case MINER -> 2;
            case TRANSPORT -> 3;
            case DEAD -> 4;
            case EXHAUSTED -> 5;
            case TOXIC -> 6;
            case FIRE -> 7;
        };
    }

    //задание цвета отображения состояния
    public static void setParticleColor(Particle particle) {
        switch (particle) {
            case EMPTY -> glColor3d(0.0, 0.0, 0.0);
            case SOLAR -> glColor3d(0.0, 1.0, 0.0);
            case MINER -> glColor3d(0.0, 0.0, 1.0);
            case TRANSPORT -> glColor3d(0.5, 0.4, 0.3);
            case DEAD -> glColor3d(0.225, 0.0, 0.0);
            case EXHAUSTED -> glColor3d(0.6, 0.6, 0.6);
            case TOXIC -> glColor3d(0.6, 0.9, 0.2);
            case FIRE -> glColor3d(1.0, 0.0, 0.0);
        }
    }

    //задание цвета отображения кислорода
    public static void setOxygenColor(int oxygen) {
        glColor3d(0.0, 0.0, (float) oxygen / Settings.resources_max_value);
    }

    //задание цвета отображения минералов
    public static void setMineralsColor(int minerals) {
        glColor3d((float) minerals / Settings.resources_max_value / 2.0,
                (float) minerals / Settings.resources_max_value / 2.0,
                0.0);
    }

    //задание цвета отображения энергии
    public static void setEnergyColor(int minerals) {
        glColor3d((float) minerals / Settings.resources_max_value / 2.0,
                0.0,
                0.0);
    }

    //задание цвета отображения руды
    public static void setOreColor(int ore) {
        glColor3d((float) ore / Settings.resources_max_value,
                (float) ore / Settings.resources_max_value,
                (float) ore / Settings.resources_max_value);
    }
}
