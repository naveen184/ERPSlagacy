package com.telushealth.thcp.pipeline.common.util

import com.cloudbees.groovy.cps.NonCPS

/**
 * Represents a string pattern that may optionally include wildcards ('*', '**' or '?'), and
 * provides an API to determine whether that pattern matches a specified input string.
 * <p/>
 * The wildcard character '*' within the pattern matches a sequence of zero or more characters within a
 * single file or directory name in the input string. It does not match a sequence of two or more
 * dir/file names. For instance, 'a*b' matches 'a12345b' and 'ab', but does NOT match 'a/b' or 'a123/b'.
 * <p/>
 * The '**' wildcard matches any sequence of zero or more characters in the input string, including
 * directory names and separators . It matches any part of the directory tree. For instance, 'a**b'
 * matches 'a12345b', 'ab', 'a/b' and 'a1/a2/a3b'.
 * <p/>
 * The wildcard character '?' within the pattern matches exactly one character in the input string,
 * excluding the normalized file separator character ('/').
 * <p/>
 *
 * @author Tapvir Virk
 */
class WildcardPattern {

    private final List regexes = []
    private final List strings = []
    private final boolean defaultMatches

    /**
     * Construct a new WildcardPattern instance on a single pattern or a comma-separated list of patterns.
     * @param patternString - the pattern string, optionally including wildcard characters ('*' or '?');
     *      may optionally contain more than one pattern, separated by commas; may be null or empty to always match
     * @param defaultMatches - a boolean indicating whether <code>matches()</code> should
     *      return true if the pattern string is either empty or null. This parameter is
     *      optional and defaults to <code>true</code>.
     */
    WildcardPattern(String patternString, boolean defaultMatches=true) {
        this.defaultMatches = defaultMatches
        def patterns = patternString ? patternString.tokenize(',') : []
        patterns.each { pattern ->
            if (containsWildcards(pattern)) {
                regexes << convertStringWithWildcardsToRegex(pattern.trim())
            }
            else {
                strings << pattern.trim()
            }
        }
    }

    /**
     * Return true if the specified String matches the pattern or if the original
     * patternString (specified in the constructor) was null or empty and the
     * value for defaultMatches (also specified in the constructor) was true.
     * @param string - the String to check
     * @return true if the String matches the pattern
     */
    boolean matches(String string) {
        if (regexes.empty && strings.empty) {
            return defaultMatches
        }
        regexes.find { regex -> string ==~ regex } ||
        strings.contains(string)
    }

    /**
     * Return true if the specified String contains one or more wildcard characters ('?' or '*')
     * @param string - the String to check
     * @return true if the String contains wildcards
     */
    @NonCPS
    private static boolean containsWildcards(String string) {
        string =~ /\*|\?/
    }

    /**
     * Convert the specified String, optionally containing wildcards (? or *), to a regular expression String
     *
     * @param stringWithWildcards - the String to convert, optionally containing wildcards (? or *)
     * @return an equivalent regex String
     *
     * @throws AssertionError - if the stringWithWildcards is null
     */
    @NonCPS
    private static String convertStringWithWildcardsToRegex(String stringWithWildcards) {
        assert stringWithWildcards != null

        def result = new StringBuffer()
        def prevCharWasStar = false
        stringWithWildcards.each { ch ->
            switch (ch) {
                case '*':
                // Single '*' matches single dir/file; Double '*' matches sequence of zero or more dirs/files
                    result << (prevCharWasStar ? /.*/ : /[^\/]*/)
                    prevCharWasStar = !prevCharWasStar
                    break
                case '?':
                // Any character except the normalized file separator ('/')
                    result << /[^\/]/
                    break
                case ['$', '|', '[', ']', '(', ')', '.', ':', '{', '}', '\\', '^', '+']:
                    result << '\\' + ch
                    break
                default: result << ch
            }
        }
        result
    }
}