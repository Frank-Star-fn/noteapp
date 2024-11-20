package com.example.noteapp;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FileUtils {
    public static long[] readLongArray(Context context, String fileName) {
        long[] array = null;
        try (FileInputStream fis = context.openFileInput(fileName);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            array = (long[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return array;
    }

    public static void writeLongArray(Context context, String fileName, long[] array){
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(array);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从文件中读取整型数组
    public static int[] readIntArray(Context context, String fileName){
        int[] intArray = null;
        try (FileInputStream fis = context.openFileInput(fileName);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            intArray = (int[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return intArray;
    }

    // 将整型数组写入文件
    public static void writeIntArray(Context context, String fileName, int[] intArray) {
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(intArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
