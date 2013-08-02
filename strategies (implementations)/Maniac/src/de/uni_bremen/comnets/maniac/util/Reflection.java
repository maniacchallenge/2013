package de.uni_bremen.comnets.maniac.util;

/**
 * Created by Isaac Supeene on 6/12/13.
 */
public class Reflection {

    public static String getPreviousMethodName(int i) {
        StackTraceElement stackTraceElements[] = (new Throwable()).getStackTrace();

        String[] fullName = stackTraceElements[i + 1].getClassName().split("\\.");
        String simpleName = fullName[fullName.length - 1];

        return simpleName + '.' + stackTraceElements[i + 1].getMethodName();
    }
}
