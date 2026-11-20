package dev.foabic.test;

import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;

final class VersionTest {

    private VersionTest() {
    }

    static void run() {
        Version shortVersion = Version.parse("1.2");
        Assertions.assertEquals("1.2.0", shortVersion.toString(), "short version canonical form");
        Assertions.assertEquals(Version.parse("1.2.0"), shortVersion, "short version equality");

        Assertions.assertTrue(Version.parse("1.0.0").compareTo(Version.parse("1.0.0-rc.1")) > 0,
                "release is newer than prerelease");
        Assertions.assertTrue(Version.parse("1.0.0-rc.2").compareTo(Version.parse("1.0.0-rc.1")) > 0,
                "prerelease ordering");
        Assertions.assertTrue(Version.parse("1.0.0-alpha").compareTo(Version.parse("1.0.0-alpha.1")) < 0,
                "shorter prerelease is lower");

        Assertions.assertTrue(VersionRange.parse("^1.2.3").test(Version.parse("1.9.9")), "caret in range");
        Assertions.assertFalse(VersionRange.parse("^1.2.3").test(Version.parse("2.0.0")), "caret upper bound");
        Assertions.assertTrue(VersionRange.parse("^0.1.2").test(Version.parse("0.1.9")), "0.x caret");
        Assertions.assertFalse(VersionRange.parse("^0.1.2").test(Version.parse("0.2.0")), "0.x caret bound");
        Assertions.assertTrue(VersionRange.parse("~1.2.3").test(Version.parse("1.2.99")), "tilde in range");
        Assertions.assertFalse(VersionRange.parse("~1.2.3").test(Version.parse("1.3.0")), "tilde bound");
        Assertions.assertTrue(VersionRange.parse(">=1.0 <2.0").test(Version.parse("1.5.0")), "compound range");
        Assertions.assertFalse(VersionRange.parse(">=1.0 <2.0").test(Version.parse("2.0.0")), "compound upper");
        Assertions.assertTrue(VersionRange.parse("*").test(Version.parse("99.0.0")), "any range");

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Version.parse("1.2.3.4"), "reject four components");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> VersionRange.parse("^"), "reject empty caret");
    }
}
