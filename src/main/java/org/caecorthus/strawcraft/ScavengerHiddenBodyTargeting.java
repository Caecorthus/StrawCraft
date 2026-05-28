package org.caecorthus.strawcraft;

public final class ScavengerHiddenBodyTargeting {
    private ScavengerHiddenBodyTargeting() {
    }

    public static boolean allowsTargeting(boolean hiddenByScavenger, boolean vanillaAllowed) {
        return !hiddenByScavenger && vanillaAllowed;
    }
}
