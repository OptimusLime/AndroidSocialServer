package edu.eplex.androidsocialclient.Utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by paul on 12/8/14.
 */
public class TextValidator {

    Pattern pattern;
    Matcher matcher;

    public enum MatchFailures
    {
        TooShort,
        TooLong,
        Other
    }

    public TextValidator(String patternToMatch){
        pattern = Pattern.compile(patternToMatch);
    }

    public boolean matchPattern(String val)
    {
        matcher = pattern.matcher(val);
        return matcher.matches();
    }
}
