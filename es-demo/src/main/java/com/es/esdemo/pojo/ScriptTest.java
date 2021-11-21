package com.es.esdemo.pojo;

import java.util.List;

public class ScriptTest {
    private int id;
    private int time ;
    private List<String> identity;
    private String label;
    private String num;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public List<String> getIdentity() {
        return identity;
    }

    public void setIdentity(List<String> identity) {
        this.identity = identity;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "ScriptTest{" +
                  "id=" + id +
                  ", time=" + time +
                  ", identity=" + identity +
                  ", label='" + label + '\'' +
                  ", num='" + num + '\'' +
                  '}';
    }
}
