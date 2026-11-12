package com.kolodkingal.enhanced.compat;
//#norenames

import org.lwjgl.PointerBuffer;

/**
 * 26.3 only: 26.3 ships no TinyFileDialogs, so there is no system "open file" dialog: the answer is always "cancelled"
 * (a skin is still added by dropping its file on the window, or into the folder the editor opens).
 */
public final class Dialogs {
    private Dialogs() {
    }

    public static String openFile(String title, String dir, PointerBuffer filters, String description, boolean multiple) {
        return null;
    }
}
