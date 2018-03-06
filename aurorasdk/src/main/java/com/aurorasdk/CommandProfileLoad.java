package com.aurorasdk;

/**
 * Created by jayalfredprufrock on 1/26/18.
 */

public class CommandProfileLoad extends Command {

    private String profileName;

    public CommandProfileLoad(String profileName) {

        this.profileName = profileName;
    }

    @Override
    public String getCommandString() {

        return "prof-load " + profileName;
    }
}
