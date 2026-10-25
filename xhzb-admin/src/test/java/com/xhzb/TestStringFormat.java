package com.xhzb;

public class TestStringFormat {

    public static void main(String[] args) {
        String template = "xhzb:ai:%s";
        String formattedString = String.format(template, "1234567890");
        System.out.println(formattedString);
    }
}
