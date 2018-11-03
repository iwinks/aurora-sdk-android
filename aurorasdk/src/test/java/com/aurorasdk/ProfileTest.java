package com.aurorasdk;
import org.junit.Test;

import static org.junit.Assert. *;
public class ProfileTest {

    private String testProfile = "stream config 18 16 {stim-delay:14400000}\n " +
                                                   "stream config 18 17 {stim-interval:300000} \n" +
                                                   "strean config  18 19 {stim-enabled:0}";
    @Test
    public void setContent_isCorrect() {

        Profile profile = new Profile(testProfile);

        assertEquals("14400000", profile.getOptions().get(Profile.Option.STIM_DELAY));
        assertEquals("300000", profile.getOptions().get(Profile.Option.STIM_INTERVAL));
    }

    @Test
    public void setOptionValueString_isCorrect(){

        Profile profile = new Profile(testProfile);
        profile.setOptionValue(Profile.Option.STIM_DELAY, "200");

        assertEquals("200", profile.getOptions().get(Profile.Option.STIM_DELAY));
    }

    @Test
    public void setOptionValueLong_isCorrect() {

        Profile profile = new Profile(testProfile);
        profile.setOptionValue(Profile.Option.STIM_INTERVAL, 100);

        assertEquals(new Long("100"), profile.getOptions().get(Profile.Option.STIM_INTERVAL));
    }

    @Test
    public  void setOptionValueBool_isCorrect(){

        Profile profile = new Profile(testProfile);
        profile.setOptionValue(Profile.Option.STIM_ENABLED, true);

        assertEquals(true, profile.getOptions().get(Profile.Option.STIM_ENABLED));
    }

    @Test
    public void toString_isCorrect() {

        Profile profile = new Profile(testProfile);
        assertEquals(testProfile, profile.toString());
    }

    @Test
    public void option_isCorrect() {

        assertEquals("stim-delay", Profile.Option.STIM_DELAY.getOptionName());
        assertNull(Profile.Option.getOption("test"));
    }
}