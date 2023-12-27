package engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.exit;


//класс с параметрами программы
public abstract class Settings {
    public static int display_width, display_height;//ширина и высота графического окна
    public static int map_width, map_height;//ширина и высота карты симуляции

    public static int resources_max_value;//максимальное значение ресурса на клетку

    public static double speedup;//ускорение генерации новых клеток
    public static int thread_count;//количество вычислительных потоков

    public static AtomicBoolean draw_resources = new AtomicBoolean(false);

    private static String get(Properties prop, String name) {
        String value = prop.getProperty(name);
        if (value == null){
            System.out.println("ERROR in loading param");
            exit(1);
        }
        return value;
    }

    //инициализация параметров
    public static void init() {
        System.out.println("init settings");
        Properties prop = new Properties();
        String fileName = "config.cfg";
        try (FileInputStream fis = new FileInputStream(fileName)) {
            prop.load(fis);
        } catch (IOException ex) {
            System.out.println("ERROR in loading settings");
            exit(1);
        }
        display_width = Integer.parseInt(get(prop, "graphics.display_width"));
        display_height = Integer.parseInt(get(prop, "graphics.display_height"));

        map_height = Integer.parseInt(get(prop, "map.height"));
        map_width = Integer.parseInt(get(prop, "map.width"));
        resources_max_value = Integer.parseInt(get(prop, "map.max_resource"));

        speedup = Double.parseDouble(get(prop, "simulation.boost_spawnrate"));
        thread_count = Integer.parseInt(get(prop, "simulation.thread_count"));
    }
}
