package com.es.esdemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class UpdatePickTest {
    public static void main(String[] args) throws FileNotFoundException {
        updateFileName( "D:\\PPT背景图", "菜花");
    }

    public static void updateFileName(String path,String fileName){
        File f = new File(path);
        if (!f.exists()){
            System.out.println("文件不存在");
            return;
        }
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if(files[i].isDirectory()){
                File[] files1 = files[i].listFiles();
                for (int j = 0; j < files1.length; j++) {
                    if(files1[j].isDirectory()){
                        updateFileName(files1[j].getPath(),fileName);
                    }
                    if(files1[j].isFile()){
                        //修改文件名
                        String newFilePath = files1[j].getPath().substring(0, files1[j].getPath().lastIndexOf(File.separator)) + File.separator + fileName
                                  + files1[j].getPath().substring(files1[j].getPath().lastIndexOf("."));
                        System.out.println(newFilePath);
                        files1[j].renameTo(new File(newFilePath));
                    }
                }
            }
            if(files[i].isFile()){
                //修改文件名
                String newFilePath = files[i].getPath().substring(0, files[i].getPath().lastIndexOf(File.separator)) + File.separator + fileName
                          + files[i].getPath().substring(files[i].getPath().lastIndexOf("."));
                System.out.println(newFilePath);
                files[i].renameTo(new File(newFilePath));
            }
        }
    }
}
