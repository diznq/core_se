package sim.stock_exchange.prng;

import java.util.Random;

public class PRNG {
    public static Random random = new Random(1234567);

    public static long nextLong() {
        return random.nextLong();
    }

    public static long nextLong(long origin, long bound) {
        return random.nextLong(origin, bound);
    }

    public static double nextDouble() {
        return random.nextDouble();
    }

    public static boolean nextBoolean() {
        return random.nextBoolean();
    }

    public static int nextInt() {
        return random.nextInt();
    }

    public static int nextInt(int origin, int bound) {
        return random.nextInt(origin, bound);
    }
}
