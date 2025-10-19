package com.toxicstoxm.YAJSI.upgrading;

import org.jetbrains.annotations.NotNull;

public record ConfigVersion(int major, int minor, int patch) implements Version {

    @Override
    public Version getVersion() {
        return this;
    }

    @Override
    public int compareTo(Version other) throws IllegalArgumentException {
        if (other instanceof ConfigVersion(int major1, int minor1, int patch1)) {
            if (this.major != major1)
                return Integer.compare(this.major, major1);
            if (this.minor != minor1)
                return Integer.compare(this.minor, minor1);
            return Integer.compare(this.patch, patch1);
        } else
            throw new IllegalArgumentException("Cannot compare class '" + other.getClass().getName() + "' to '" + this.getClass().getName() + "'!");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConfigVersion(int major1, int minor1, int patch1))) return false;
        return major == major1 && minor == minor1 && patch == patch1;
    }

    @Override
    public @NotNull String toString() {
        return major + "." + minor + "." + patch;
    }

    public @NotNull Version fromString(@NotNull String versionString) {
        if (versionString.isBlank())
            throw new IllegalArgumentException("Version string cannot be null or blank");

        String[] parts = versionString.trim().split("\\.");
        if (parts.length != 3)
            throw new IllegalArgumentException("Invalid version format: expected 'major.minor.patch'");

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            return new ConfigVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version number format: " + versionString, e);
        }
    }
}
