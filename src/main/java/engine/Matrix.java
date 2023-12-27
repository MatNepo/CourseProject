package engine;

import java.util.concurrent.atomic.AtomicReferenceArray;

//gereric-класс для создвний матриц значений, атомарный
public class Matrix<T> {
    //атомарный массив с размером 2 для хранения значений
    private final AtomicReferenceArray<AtomicReferenceArray<T>> arr; //maybe use 'synchronized' instead

    //конструктор с инициализацией начальных значений
    public Matrix(int width, int height, T value) {
        arr = new AtomicReferenceArray<>(width);
        for (int i = 0; i < width; i++) {
            arr.set(i, new AtomicReferenceArray<T>(height));
            for (int j = 0; j < height; j++){
                arr.get(i).set(j, value);
            }
        }
    }

    public void set(int x, int y, T value){
        arr.get(x).set(y, value);
    }

    public T get(int x, int y){
        return arr.get(x).get(y);
    }
}
