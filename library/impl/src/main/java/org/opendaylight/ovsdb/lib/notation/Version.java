/*
 * Copyright (c) 2014, 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;

import com.google.errorprone.annotations.Var;

/**
 * This class represents a version according to RFC 7047.
 * The default implementation assumes the left-most digit is most significant when performing comparisons.
 * @see <a href="http://tools.ietf.org/html/rfc7047#section-3.1">RFC7047 Section 3.1</a>
 */
public class Version implements Comparable<Version> {
    private static final String FORMAT = "(\\d+)\\.(\\d+)\\.(\\d+)";

    private int major;
    private int minor;
    private int patch;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static final Version NULL = new Version(0,0,0);
    public static final String NULL_VERSION_STRING = "0.0.0";

    public static Version fromString(String version) {
        final int firstDot = version.indexOf('.');
        final int secondDot = version.indexOf('.', firstDot + 1);
        if (firstDot == -1 || secondDot == -1) {
            throw new IllegalArgumentException("<" + version + "> does not match format " + Version.FORMAT);
        }
        final int major = parse(version, 0, firstDot);
        final int minor = parse(version, firstDot + 1, secondDot);
        final int patch = parse(version, secondDot + 1, version.length());
        return new Version(major, minor, patch);
    }

    /**
     * Parses the string argument from position 'start' to 'end' as a signed decimal integer.
     * We use a custom hand written method instead of {@link Integer#parseInt(String)}
     * just to avoid allocating three intermediate String objects in {@link #fromString(String)},
     * as objection allocation data in Java Mission Control from ODL running in a scale lab
     * has identified this as the top #3 (!) memory allocator overall - 1 GB avoidable String.
     * @author Michael Vorburger.ch
     */
    private static int parse(String string, int start, int end) {
        @Var int result = 0;
        for (int i = start; i < end && i < string.length(); i++) {
            char character = string.charAt(i);
            int digit = Character.digit(character, 10);
            if (digit < 0) {
                throw new IllegalArgumentException("Not a digit: " + character);
            }
            result = result * 10 + digit;
        }
        return result;
    }

    @Override
    public String toString() {
        return "" + major + "." + minor + "." + patch;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getPatch() {
        return patch;
    }

    public void setPatch(int patch) {
        this.patch = patch;
    }


    // ToDo: While format is X.X.X semantics are schema dependent.
    // Therefore we should allow equals to be overridden by the schema
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Version version = (Version) object;

        if (major != version.major) {
            return false;
        }
        if (minor != version.minor) {
            return false;
        }
        if (patch != version.patch) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }

    // ToDo: While format is X.X.X semantics are schema dependent
    // Therefore we should allow compareTo to be overridden by the schema
    @Override
    public int compareTo(Version version) {
        if (this.equals(version)) {
            return 0;
        }
        if (this.major > version.major) {
            return 1;
        }
        if (this.major < version.major) {
            return -1;
        }
        // major is equal
        if (this.minor > version.minor) {
            return 1;
        }
        if (this.minor < version.minor) {
            return -1;
        }
        // minor is equal
        if (this.patch > version.patch) {
            return 1;
        }
        // must be less than
        return -1;
    }
}
