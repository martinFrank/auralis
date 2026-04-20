package com.github.martinfrank.elitegames.auralis.adventure;

public enum HeadingLevel {
    H1(1),
    H2(2),
    H3(3),
    H4(4),
    H5(5),
    H6(6);

    private final int hashCount;

    HeadingLevel(int hashCount) {
        this.hashCount = hashCount;
    }

    public int getHashCount() {
        return hashCount;
    }

    public static HeadingLevel ofHashCount(int hashCount) {
        for (HeadingLevel level : values()) {
            if (level.hashCount == hashCount) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unsupported markdown heading hash count: " + hashCount);
    }
}
