package engine;

import controller.CLint;
import controller.Instruction;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldMap {
    //ширина и высота карты симуляции, противоположные края карты соединены
    public final int width, height;
    //матрица состояний клетки
    private final Matrix<Particle> matrix_values;
    //матрица ресурса кислорода
    private final Matrix<Integer> oxygen;
    //матрица ресурса минерала
    private final Matrix<Integer> minerals;
    //матрица ресурса руды
    private final Matrix<Integer> ore;
    //матрица ресурса энергии
    private final Matrix<Integer> energy;
    //матрица потенциальных новых состояний
    private final Matrix<boolean[]> matrix_challengers;


    public int tick;
    private final CLint cLint;

    private final int thread_count;

    private long pTime = 0;
    private int tCount = 0, last_tCount = 0;
    private final int delimiter;

    //конструктор симуляции, основные параметры + 6 матриц(состояние, 4 ресурса и массив потенциальных состояний) + первичное обнуление
    public WorldMap(CLint cLint, int width, int height, int thread_count) {
        this.thread_count = thread_count;
        this.cLint = cLint;
        this.width = width;
        this.height = height;
        this.tick = 0;
        this.delimiter = (int) Math.ceil((double) this.width / (double) thread_count);
        matrix_values = new Matrix<>(width, height, Particle.EMPTY);
        matrix_challengers = new Matrix<>(width, height, null);
        oxygen = new Matrix<>(width, height, 0);
        minerals = new Matrix<>(width, height, 0);
        energy = new Matrix<>(width, height, 0);
        ore = new Matrix<>(width, height, 0);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                matrix_values.set(i, j, Particle.EMPTY);
                boolean[] chal = new boolean[Particle.getParticleCount()];
                Arrays.fill(chal, false);
                matrix_challengers.set(i, j, chal);
            }
        }

    }

    //функция для расчёта тикрейта
    private void tickRate() {
        long cTime = System.currentTimeMillis();
        if (cTime - pTime >= 10000L) {
            last_tCount = (int) ((float) tCount / (cTime - pTime) * 1000.0);// / thread_count;
            tCount = 0;
            pTime = cTime;
            System.out.println(last_tCount);
        }
        tCount++;
    }

    public int getTPS() {
        return last_tCount;
    }

    public Particle get(int x, int y) {
        return matrix_values.get((x + width) % width, (y + height) % height);
    }

    public void set(int x, int y, Particle value) {
        matrix_values.set((x + width) % width, (y + height) % height, value);
    }

    public boolean[] get_challengers(int x, int y) {
        return matrix_challengers.get((x + width) % width, (y + height) % height);
    }

    public void set_challengers(int x, int y, Particle particle, boolean val) {
        matrix_challengers.get((x + width) % width, (y + height) % height)[Particle.getParticleId(particle)] = val;
    }

    public int get_oxygen(int x, int y) {
        return oxygen.get((x + width) % width, (y + height) % height);
    }

    public void set_oxygen(int x, int y, int value) {
        oxygen.set((x + width) % width, (y + height) % height, value);
    }

    public int get_minerals(int x, int y) {
        return minerals.get((x + width) % width, (y + height) % height);
    }

    public void set_minerals(int x, int y, int value) {
        minerals.set((x + width) % width, (y + height) % height, value);
    }

    public int get_energy(int x, int y) {
        return energy.get((x + width) % width, (y + height) % height);
    }

    public void set_energy(int x, int y, int value) {
        energy.set((x + width) % width, (y + height) % height, value);
    }

    public int get_ore(int x, int y) {
        return ore.get((x + width) % width, (y + height) % height);
    }

    public void set_ore(int x, int y, int value) {
        ore.set((x + width) % width, (y + height) % height, value);
    }

    //задание начальных значений ресурсов в клетках
    private void set_random_resources() {
        Random random = new Random();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                set_oxygen(i, j, random.nextInt(Settings.resources_max_value));
                set_minerals(i, j, random.nextInt(Settings.resources_max_value));
                set_ore(i, j, random.nextInt(Settings.resources_max_value));
            }
        }
    }

    //задание начальных значений некоторых клеток для старта симуляции
    private void set_1() {
        set(width / 4, height / 4, Particle.SOLAR);
        set(width / 4 + 1, height / 4, Particle.TRANSPORT);
        set(width / 4 + 2, height / 4, Particle.MINER);
    }

    //вычислительный поток симуляции
    class Thread_wrap {

        final public AtomicBoolean tick_allow;
        final private Thread thread;

        public Thread_wrap(int start_x, int end_x) {
            tick_allow = new AtomicBoolean(true);
            thread = new Thread(() -> process_thread(start_x, end_x));
        }

        public void start() {
            thread.start();
        }

        public void tick() {
            tick_allow.set(true);
        }

        public void join() throws InterruptedException {
            thread.join();
        }

        //функция вычислительного потока симуляции, производит расчёт выделенной области значений
        private void process_thread(int start_x, int end_x) {
            while (cLint.getStatus() != Instruction.STOP) {
                tick_allow.set(false);
                for (int i = start_x; i < end_x; i++) {
                    for (int j = 0; j < height; j++) {
                        calc_challengers(i, j); //расчёт потенциальных новых состояний клетки
                    }
                }
                //синхронизация начала с другими потоками
                while (!tick_allow.get() && cLint.getStatus() != Instruction.STOP) {
                }

                tick_allow.set(false);
                for (int i = start_x; i < end_x; i++) {
                    for (int j = 0; j < height; j++) {
                        perform_challengers(i, j); //выбор состояния клетки из потенциальных расчётных
                    }
                }
                //синхронизация с другими потоками
                while (!tick_allow.get() && cLint.getStatus() != Instruction.STOP) {
                }
            }
        }
    }

    //функция основного потока симуляции, отвечает за инициализацию, запуск и синхронизацию вычислительных потоков
    public void process_world() {
        //set_random();
        set_1();
        set_random_resources();
        Thread_wrap[] threads = new Thread_wrap[thread_count];
        for (int i = 0; i < thread_count; i++) {
            threads[i] = new Thread_wrap(i * delimiter, Math.min((i + 1) * delimiter, this.width));
            threads[i].start();
        }

        //основной цикл, отвечает за синхронизацию вычислительных потоков
        while (cLint.getStatus() != Instruction.STOP) {
            if (cLint.getStatus() != Instruction.PAUSE) {
                for (int i = 0; i < thread_count; i++) threads[i].tick();
                tickRate();
                while (cLint.getStatus() != Instruction.STOP) {
                    boolean b = true;
                    for (int i = 0; i < thread_count; i++) b = !threads[i].tick_allow.get() && b;
                    if (b) {
                        break;
                    }
                }
            }
        }

        //синхронизация завершения вычислений
        for (int i = 0; i < thread_count; i++) {
            try {
                threads[i].tick();
                threads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //расчёт возможных новых состояний клетки
    private void calc_challengers(int x, int y) {
        Particle prop = get(x, y);
        Random random = new Random();
        int rnd_val = random.nextInt(2);
        int rnd_val2 = 1 - random.nextInt(3);
        int rnd_x = x + rnd_val * rnd_val2;
        int rnd_y = y + (1 - rnd_val) * rnd_val2;
        //int rnd_x = (1 - random.nextInt(3)) + x;
        //int rnd_y = (1 - random.nextInt(3)) + y;

        int energy = get_energy(x, y);
        int oxygen = get_oxygen(x, y);
        int minerals = get_minerals(x, y);
        int ore = get_ore(x, y);
        int start_energy = energy;
        int start_oxygen = oxygen;
        int start_minerals = minerals;
        int start_ore = ore;


        if ((prop == Particle.SOLAR || prop == Particle.TRANSPORT) && get(rnd_x, rnd_y) == Particle.EMPTY) {
            if (random.nextDouble() <= 0.0004 * Settings.speedup && energy >= Settings.resources_max_value / 100) {
                energy -= Settings.resources_max_value / 100;
                set_challengers(rnd_x, rnd_y, Particle.EXHAUSTED, true);
                set_energy(rnd_x, rnd_y, get_energy(rnd_x, rnd_y) + Settings.resources_max_value / 100 / 2);
            }
        }

        switch (prop) {
            case SOLAR -> {
                if (oxygen >= 0 && minerals >= 0) {
                    oxygen -= 1;
                    minerals -= 4;
                    energy += 10;
                }
            }
            case MINER -> {
                if (ore >= 0) {
                    ore -= 1;
                    energy -= 1;
                    minerals += 3;
                }
            }
            case TRANSPORT -> {
                energy -= 1;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int other_energy = get_energy(x + i, y + j);
                        int other_minerals = get_minerals(x + i, y + j);
                        //int other_oxygen = get_oxygen(x + i, y + j);
                        Particle other_prop = get(x + i, y + j);
                        if (other_prop == Particle.SOLAR || other_prop == Particle.MINER || other_prop == Particle.TRANSPORT || other_prop == Particle.EXHAUSTED) {
                            if (other_energy < energy && energy >= 8) {
                                set_energy(x + i, y + j, other_energy + 8);
                                energy -= 8;
                            } else if (other_energy >= 8) {
                                set_energy(x + i, y + j, other_energy - 8);
                                energy += 8;
                            }
                            if (other_minerals < minerals && minerals >= 8) {
                                set_minerals(x + i, y + j, other_minerals + 8);
                                minerals -= 8;
                            } else if (other_minerals >= 8) {
                                set_minerals(x + i, y + j, other_minerals - 8);
                                minerals += 8;
                            }
                        }
                    }
                }
            }
            case EXHAUSTED -> {
                if (random.nextDouble() <= 0.004) {
                    set_challengers(x, y, Particle.DEAD, true);
                }
                if (oxygen >= Settings.resources_max_value / 1000 || ore >= Settings.resources_max_value / 1000) {
                    if (oxygen >= Settings.resources_max_value / 1000 && random.nextDouble() <= 0.5) {
                        set_challengers(x, y, Particle.SOLAR, true);
                    } else {
                        set_challengers(x, y, Particle.MINER, true);
                    }
                } else if (random.nextDouble() <= 0.04) {
                    set_challengers(x, y, Particle.DEAD, true);
                }
            }
        }

        if (prop == Particle.SOLAR || prop == Particle.TRANSPORT) {
            if (energy > 0) {
                energy -= 1;
            } else {
                set_challengers(x, y, Particle.EXHAUSTED, true);
            }
        }

        if (prop == Particle.MINER) {
            if (energy > 0) {
                energy -= 1;
            } else {
                set_challengers(x, y, Particle.TRANSPORT, true);
            }
        }

        set_energy(x, y, energy - start_energy + get_energy(x, y));
        set_oxygen(x, y, oxygen - start_oxygen + get_oxygen(x, y));
        set_minerals(x, y, minerals - start_minerals + get_minerals(x, y));
        set_ore(x, y, ore - start_ore + get_ore(x, y));
    }

    boolean check(boolean[] chall, Particle particle) {
        return chall[Particle.getParticleId(particle)];
    }

    //принятие решения к переходу к новому состоянию клетки
    private void perform_challengers(int x, int y) {
        boolean[] chall = get_challengers(x, y);

        if (check(chall, Particle.EMPTY)) {
            set(x, y, Particle.EMPTY);
        }

        if (check(chall, Particle.SOLAR)) {
            set(x, y, Particle.SOLAR);
        }

        if (check(chall, Particle.MINER)) {
            set(x, y, Particle.MINER);
        }

        if (check(chall, Particle.TRANSPORT)) {
            set(x, y, Particle.TRANSPORT);
        }

        if (check(chall, Particle.EXHAUSTED)) {
            set(x, y, Particle.EXHAUSTED);
        }

        if (check(chall, Particle.DEAD)) {
            set(x, y, Particle.DEAD);
        }

        if (check(chall, Particle.TOXIC)) {
            set(x, y, Particle.TOXIC);
        }

        if (check(chall, Particle.FIRE)) {
            set(x, y, Particle.FIRE);
        }

        Arrays.fill(chall, false);
    }
}
