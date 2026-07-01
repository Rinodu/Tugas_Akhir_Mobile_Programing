package com.campusmate.pro.util;

import java.util.UUID;

public class IdHelper {
    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
