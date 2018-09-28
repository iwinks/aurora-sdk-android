package com.aurorasdk;

/**
 * Created by jayalfredprufrock on 1/26/18.
 */

public class CommandProfileLoad extends Command {

    public CommandProfileLoad(String profileName) {

        super("prof-load " + profileName);
    }
}
