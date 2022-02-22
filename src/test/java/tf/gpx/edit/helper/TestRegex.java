/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.helper;

import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test match() and lokAt() for some complex regex
 * See also https://regex101.com/
 * 
 * @author thomas
 */
public class TestRegex {
    private void testMatchLookingAt(final Pattern pattern, final String testString, final int minLength, final boolean matchResult, final boolean lookingAtResult) {
        if (testString != null && testString.length() >= minLength) {
//            System.out.println("testString: \"" + testString + "\"");
            Assert.assertEquals(pattern.matcher(testString).matches(), matchResult);
            Assert.assertEquals(pattern.matcher(testString).lookingAt(), lookingAtResult);
        }
    }
    
    @Test
    public void testMatcherNoDEG() {
        // simpler version of regex WITHOUT DEG char
        final Pattern test1 = Pattern.compile("[NS][ ]?([0-8 ]?[0-9]?)");
        
        // building up correct string chart by char
        // matches should be TRUE since only [NS] is mandatory
        final String testString = "N 11" + LatLonHelper.DEG;

        int end = 0;
        testMatchLookingAt(test1, testString.substring(0, end++), 0, false, false);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, true, true);

        testMatchLookingAt(test1, testString, 0, false, true);
    }
    
    @Test
    public void testMatcherDEG() {
        // simpler version of regex WITH DEG char
        final Pattern test1 = Pattern.compile("[NS][ ]?([0-8 ]?[0-9]?)" + LatLonHelper.DEG);
        
        // building up correct string chart by char
        // matches should be FALSE since [NS] and DEG is mandatory
        final String testString = "N 11" + LatLonHelper.DEG;

        int end = 0;
        testMatchLookingAt(test1, testString.substring(0, end++), 0, false, false);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, false, false);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, false, false);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, false, false);
        testMatchLookingAt(test1, testString.substring(0, end++), 0, false, false);

        // match only at the very end...
        testMatchLookingAt(test1, testString, 0, true, true);

        testMatchLookingAt(test1, testString + " ", 0, false, true);
    }
    
    @Test
    public void testMatcherDEGAndMinLength1() {
        // simpler version of regex WITH DEG char
        final Pattern test1 = Pattern.compile("[NS][ ]?([0-8 ]?[0-9]?)" + LatLonHelper.DEG);
        
        // building up correct string chart by char
        // matches should be FALSE since [NS] and DEG is mandatory
        final String testString = "N 11" + LatLonHelper.DEG;

        int end = 0;
        testMatchLookingAt(test1, testString.substring(0, end++), 4, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 4, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 4, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 4, true, true);
        // testing "N 11" - which fails
        testMatchLookingAt(test1, testString.substring(0, end++), 4, false, false);

        // testing "N 11o" - which works
        testMatchLookingAt(test1, testString, 4, true, true);

        testMatchLookingAt(test1, testString + " ", 4, false, true);
    }
    
    @Test
    public void testMatcherDEGAndMinLength2() {
        // simpler version of regex WITH DEG char
        final Pattern test1 = Pattern.compile("[NS][ ]?([0-8 ]?[0-9]?)" + LatLonHelper.DEG);
        
        // building up correct string chart by char
        // matches should be FALSE since [NS] and DEG is mandatory
        final String testString = "N 11" + LatLonHelper.DEG;

        int end = 0;
        testMatchLookingAt(test1, testString.substring(0, end++), 5, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 5, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 5, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 5, true, true);
        testMatchLookingAt(test1, testString.substring(0, end++), 5, true, true);

        // testing "N 11o" - which works
        testMatchLookingAt(test1, testString, 4, true, true);

        testMatchLookingAt(test1, testString + " ", 4, false, true);
    }
    
    private void testAllLATLON1Combinations(final Pattern pattern, final String dir, final String deg, final String min, final String sec, final boolean doShortCombinations, final boolean result) {
        if (doShortCombinations) {
            logAssert(pattern, dir + deg + LatLonHelper.DEG, result);
            logAssert(pattern, dir + " " + deg + LatLonHelper.DEG, result);
            logAssert(pattern, deg + LatLonHelper.DEG + dir, result);
            logAssert(pattern, deg + LatLonHelper.DEG + " " + dir, result);

            logAssert(pattern, dir + deg + LatLonHelper.DEG + min + LatLonHelper.MIN, result);
            logAssert(pattern, dir + " " + deg + LatLonHelper.DEG + min + LatLonHelper.MIN, result);
            logAssert(pattern, dir + " " + deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN, result);
            logAssert(pattern, dir + deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN, result);
            logAssert(pattern, deg + LatLonHelper.DEG + min + LatLonHelper.MIN + dir, result);
            logAssert(pattern, deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + dir, result);
            logAssert(pattern, deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + " " + dir, result);

            logAssert(pattern, dir + deg + LatLonHelper.DEG + LatLonHelper.MIN + LatLonHelper.SEC, result);
            logAssert(pattern, dir + " " + deg + LatLonHelper.DEG + LatLonHelper.MIN + LatLonHelper.SEC, result);
            logAssert(pattern, deg + LatLonHelper.DEG + LatLonHelper.MIN + LatLonHelper.SEC + dir, result);
            logAssert(pattern, deg + LatLonHelper.DEG + LatLonHelper.MIN + LatLonHelper.SEC + " " + dir, result);
        }
        
        logAssert(pattern, dir + deg + LatLonHelper.DEG + min + LatLonHelper.MIN + sec + LatLonHelper.SEC, result);
        logAssert(pattern, dir + " " + deg + LatLonHelper.DEG + min + LatLonHelper.MIN + sec + LatLonHelper.SEC, result);
        logAssert(pattern, dir + " " + deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + sec + LatLonHelper.SEC, result);
        logAssert(pattern, dir + " " + deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + " " + sec + LatLonHelper.SEC, result);
        logAssert(pattern, dir + deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + sec + LatLonHelper.SEC, result);
        logAssert(pattern, dir + deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + " " + sec + LatLonHelper.SEC, result);
        logAssert(pattern, dir + deg + LatLonHelper.DEG + min + LatLonHelper.MIN + " " + sec + LatLonHelper.SEC, result);
        logAssert(pattern, deg + LatLonHelper.DEG + min + LatLonHelper.MIN + sec + LatLonHelper.SEC + dir, result);
        logAssert(pattern, deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + sec + LatLonHelper.SEC + dir, result);
        logAssert(pattern, deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + sec + LatLonHelper.SEC + " " + dir, result);
        logAssert(pattern, deg + LatLonHelper.DEG + " " + min + LatLonHelper.MIN + " " + sec + LatLonHelper.SEC + " " + dir, result);
    }
    
    private void logAssert(final Pattern pattern, final String testString, final boolean result) {
//        System.out.println("testing: " + testString + " for " + result);
        boolean test = (pattern.matcher(testString).matches() == result);
        if (!test) {
//            System.out.println("  and failing!");
        }
        Assert.assertTrue("testing: " + testString + " for " + result, test);
    }
    
    private void createAllLATLON1Combinations(final Pattern pattern, final String dir, final String deg, final String min, final String sec, final String subsec, final boolean doShortCombinations, final boolean result) {
        if (doShortCombinations) {
            testAllLATLON1Combinations(pattern, dir, deg, "", "", doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, min.substring(0, 1), "", doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, min, "", doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, "", sec.substring(0, 1), doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, "", sec, doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, min, sec.substring(0, 1), doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, min, sec, doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, min, sec + ".", doShortCombinations, result);
            testAllLATLON1Combinations(pattern, dir, deg, min, sec + ",", doShortCombinations, result);

            testAllLATLON1Combinations(pattern, dir, "-" + deg, "", "", doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, "-" +  min.substring(0, 1), "", doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, "-" + min, "", doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, "", "-" + sec.substring(0, 1), doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, "", "-" + sec, doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, min, sec.substring(0, 1), doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, min, sec, doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, min, "-" + sec + ".", doShortCombinations, false);
            testAllLATLON1Combinations(pattern, dir, "-" + deg, min, "-" + sec + ",", doShortCombinations, false);
        }

        testAllLATLON1Combinations(pattern, dir, deg, min, sec + "." + subsec, doShortCombinations, result);
        testAllLATLON1Combinations(pattern, dir, deg, min, sec + "," + subsec, doShortCombinations, result);

        testAllLATLON1Combinations(pattern, "-" + dir, deg, min, sec + "." + subsec, doShortCombinations, false);
        testAllLATLON1Combinations(pattern, "-" + dir, deg, min, sec + "," + subsec, doShortCombinations, false);

        testAllLATLON1Combinations(pattern, "-" + dir, "-" + deg, min, sec + "." + subsec, doShortCombinations, false);
        testAllLATLON1Combinations(pattern, "-" + dir, "-" + deg, min, sec + "," + subsec, doShortCombinations, false);

        testAllLATLON1Combinations(pattern, "-" + dir, "-" + deg, "-" + min, sec + "." + subsec, doShortCombinations, false);
        testAllLATLON1Combinations(pattern, "-" + dir, "-" + deg,"-" +  min, sec + "," + subsec, doShortCombinations, false);

        testAllLATLON1Combinations(pattern, "-" + dir, "-" + deg, "-" + min, "-" + sec + "." + subsec, doShortCombinations, false);
        testAllLATLON1Combinations(pattern, "-" + dir, "-" + deg, "-" + min, "-" + sec + "," + subsec, doShortCombinations, false);
    }
    
    @Test
    public void testLAT1Combinations1() {
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "N", "90", "00", "00", "0", true, true);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "S", "90", "00", "00", "0", true, true);

        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "E", "90", "00", "00", "0", true, false);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "W", "90", "00", "00", "0", true, false);

        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "N", "90", "1", "00", "0", false, false);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "S", "90", "1", "00", "0", false, false);

        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "N", "90", "0", "1", "0", false, false);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "S", "90", "0", "1", "0", false, false);

        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "N", "90", "0", "00", "1", false, false);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "S", "90", "0", "00", "1", false, false);
    }
    
    @Test
    public void testLAT1Combinations2() {
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "N", "89", "59", "59", "9", true, true);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "S", "89", "59", "59", "9", true, true);

        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "E", "89", "59", "59", "9", true, false);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "W", "89", "59", "59", "9", true, false);

        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "N", "89", "61", "59", "9", false, false);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "S", "89", "61", "59", "9", false, false);

        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "N", "89", "59", "61", "9", false, false);
        createAllLATLON1Combinations(LatLonHelper.LAT_PATTERN_1, "S", "89", "59", "61", "9", false, false);
    }
    
    @Test
    public void testLON1Combinations1() {
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "E", "180", "00", "00", "0", true, true);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "W", "180", "00", "00", "0", true, true);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "N", "180", "00", "00", "0", true, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "S", "180", "00", "00", "0", true, false);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "E", "181", "00", "00", "0", false, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "W", "181", "00", "00", "0", false, false);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "E", "180", "1", "00", "0", false, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "W", "180", "1", "00", "0", false, false);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "E", "180", "0", "1", "0", false, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "W", "180", "0", "1", "0", false, false);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "E", "180", "0", "00", "1", false, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "W", "180", "0", "00", "1", false, false);
    }
    
    @Test
    public void testLON1Combinations2() {
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "E", "179", "59", "59", "9", true, true);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "W", "179", "59", "59", "9", true, true);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "N", "179", "59", "59", "9", true, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "S", "179", "59", "59", "9", true, false);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "N", "179", "61", "59", "9", false, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "S", "179", "61", "59", "9", false, false);

        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "N", "179", "59", "61", "9", false, false);
        createAllLATLON1Combinations(LatLonHelper.LON_PATTERN_1, "S", "179", "59", "61", "9", false, false);
    }
    
    @Test
    public void testLAT2Combinations1() {
        logAssert(LatLonHelper.LAT_PATTERN_2, "90", true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "90.0", true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "89.9999", true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "0", true);

        logAssert(LatLonHelper.LAT_PATTERN_2, "-90", true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "-90.0", true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "-89.9999", true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "-0", true);

        logAssert(LatLonHelper.LAT_PATTERN_2, "90" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "90.0" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "89.9999" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "0" + LatLonHelper.DEG, true);

        logAssert(LatLonHelper.LAT_PATTERN_2, "-90" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "-90.0" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "-89.9999" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LAT_PATTERN_2, "-0" + LatLonHelper.DEG, true);

        logAssert(LatLonHelper.LAT_PATTERN_2, "90.1", false);
        logAssert(LatLonHelper.LAT_PATTERN_2, "-90.1", false);
    }
    
    @Test
    public void testLON2Combinations1() {
        logAssert(LatLonHelper.LON_PATTERN_2, "180", true);
        logAssert(LatLonHelper.LON_PATTERN_2, "180.0", true);
        logAssert(LatLonHelper.LON_PATTERN_2, "179.9999", true);
        logAssert(LatLonHelper.LON_PATTERN_2, "0", true);

        logAssert(LatLonHelper.LON_PATTERN_2, "-180", true);
        logAssert(LatLonHelper.LON_PATTERN_2, "-180.0", true);
        logAssert(LatLonHelper.LON_PATTERN_2, "-179.9999", true);
        logAssert(LatLonHelper.LON_PATTERN_2, "-0", true);

        logAssert(LatLonHelper.LON_PATTERN_2, "180" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LON_PATTERN_2, "180.0" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LON_PATTERN_2, "179.9999" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LON_PATTERN_2, "0" + LatLonHelper.DEG, true);

        logAssert(LatLonHelper.LON_PATTERN_2, "-180" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LON_PATTERN_2, "-180.0" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LON_PATTERN_2, "-179.9999" + LatLonHelper.DEG, true);
        logAssert(LatLonHelper.LON_PATTERN_2, "-0" + LatLonHelper.DEG, true);

        logAssert(LatLonHelper.LON_PATTERN_2, "180.1", false);
        logAssert(LatLonHelper.LON_PATTERN_2, "-180.1", false);
    }
}
