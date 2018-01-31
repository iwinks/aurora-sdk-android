package com.dreambandsdk;

import java.util.Calendar;

/**
 * Created by jayalfredprufrock on 1/29/18.
 */

public class CommandTimeSync extends Command {

    @Override
    public String getCommandString() {

        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();

        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        long msAfterMidnight = now.getTimeInMillis() - midnight.getTimeInMillis();

        return "clock-set " + now.get(Calendar.YEAR) + " " + (now.get(Calendar.MONTH)+1) + " " + now.get(Calendar.DAY_OF_MONTH) + " " + msAfterMidnight;
    }

}