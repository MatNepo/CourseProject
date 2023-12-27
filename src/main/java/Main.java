import controller.CLint;
import engine.Settings;
import engine.WorldMap;
import graphics.Display;


//main-функция, из которой происходит запуск программы
public class Main {
    public static void main(String[] args) throws InterruptedException {

        Settings.init(); //инициализация параметров из config.cfg

        System.out.println("Start");

        CLint cLint = new CLint(); //создание контроллера

        //создание движка симуляции с привязкой к контроллеру
        WorldMap worldMap = new WorldMap(cLint, Settings.map_width, Settings.map_height, Settings.thread_count);

        //создание основного потока обработки симуляции, последующий запуск
        Thread worldmap_thread = new Thread(worldMap::process_world);
        worldmap_thread.start();

        //создание графического интерфейса с привязкой к контроллеру и движку симуляции, последующий запуск
        Display display = new Display(cLint, worldMap);
        Thread display_thread = new Thread(display::run);
        display_thread.start();

        //синхронизация завершения программы
        worldmap_thread.join();
        display_thread.join();
        System.out.println("Finish");
    }
}