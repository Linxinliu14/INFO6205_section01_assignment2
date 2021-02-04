/*
 * Copyright (c) 2018. Phasmid Software
 */

package edu.neu.coe.info6205.util;

import edu.neu.coe.info6205.sort.simple.InsertionSort;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static edu.neu.coe.info6205.util.Utilities.formatWhole;

/**
 * This class implements a simple Benchmark utility for measuring the running time of algorithms.
 * It is part of the repository for the INFO6205 class, taught by Prof. Robin Hillyard
 * <p>
 * It requires Java 8 as it uses function types, in particular, UnaryOperator&lt;T&gt; (a function of T => T),
 * Consumer&lt;T&gt; (essentially a function of T => Void) and Supplier&lt;T&gt; (essentially a function of Void => T).
 * <p>
 * In general, the benchmark class handles three phases of a "run:"
 * <ol>
 *     <li>The pre-function which prepares the input to the study function (field fPre) (may be null);</li>
 *     <li>The study function itself (field fRun) -- assumed to be a mutating function since it does not return a result;</li>
 *     <li>The post-function which cleans up and/or checks the results of the study function (field fPost) (may be null).</li>
 * </ol>
 * <p>
 * Note that the clock does not run during invocations of the pre-function and the post-function (if any).
 *
 * @param <T> The generic type T is that of the input to the function f which you will pass in to the constructor.
 */
public class Benchmark_Timer<T> implements Benchmark<T> {

    /**
     * Calculate the appropriate number of warmup runs.
     *
     * @param m the number of runs.
     * @return at least 2 and at most m/10.
     */
    static int getWarmupRuns(int m) {
        return Integer.max(2, Integer.min(10, m / 10));
    }

    /**
     * Run function f m times and return the average time in milliseconds.
     *
     * @param supplier a Supplier of a T
     * @param m        the number of times the function f will be called.
     * @return the average number of milliseconds taken for each run of function f.
     */
    @Override
    public double runFromSupplier(Supplier<T> supplier, int m) {
        logger.info("Begin run: " + description + " with " + formatWhole(m) + " runs");
        // Warmup phase
        final Function<T, T> function = t -> {
            fRun.accept(t);
            return t;
        };
        new Timer().repeat(getWarmupRuns(m), supplier, function, fPre, null);

        // Timed phase
        return new Timer().repeat(m, supplier, function, fPre, fPost);
    }

    /**
     * Constructor for a Benchmark_Timer with option of specifying all three functions.
     *
     * @param description the description of the benchmark.
     * @param fPre        a function of T => T.
     *                    Function fPre is run before each invocation of fRun (but with the clock stopped).
     *                    The result of fPre (if any) is passed to fRun.
     * @param fRun        a Consumer function (i.e. a function of T => Void).
     *                    Function fRun is the function whose timing you want to measure. For example, you might create a function which sorts an array.
     *                    When you create a lambda defining fRun, you must return "null."
     * @param fPost       a Consumer function (i.e. a function of T => Void).
     */
    public Benchmark_Timer(String description, UnaryOperator<T> fPre, Consumer<T> fRun, Consumer<T> fPost) {
        this.description = description;
        this.fPre = fPre;
        this.fRun = fRun;
        this.fPost = fPost;
    }

    /**
     * Constructor for a Benchmark_Timer with option of specifying all three functions.
     *
     * @param description the description of the benchmark.
     * @param fPre        a function of T => T.
     *                    Function fPre is run before each invocation of fRun (but with the clock stopped).
     *                    The result of fPre (if any) is passed to fRun.
     * @param fRun        a Consumer function (i.e. a function of T => Void).
     *                    Function fRun is the function whose timing you want to measure. For example, you might create a function which sorts an array.
     */
    public Benchmark_Timer(String description, UnaryOperator<T> fPre, Consumer<T> fRun) {
        this(description, fPre, fRun, null);
    }

    /**
     * Constructor for a Benchmark_Timer with only fRun and fPost Consumer parameters.
     *
     * @param description the description of the benchmark.
     * @param fRun        a Consumer function (i.e. a function of T => Void).
     *                    Function fRun is the function whose timing you want to measure. For example, you might create a function which sorts an array.
     *                    When you create a lambda defining fRun, you must return "null."
     * @param fPost       a Consumer function (i.e. a function of T => Void).
     */
    public Benchmark_Timer(String description, Consumer<T> fRun, Consumer<T> fPost) {
        this(description, null, fRun, fPost);
    }

    /**
     * Constructor for a Benchmark_Timer where only the (timed) run function is specified.
     *
     * @param description the description of the benchmark.
     * @param f           a Consumer function (i.e. a function of T => Void).
     *                    Function f is the function whose timing you want to measure. For example, you might create a function which sorts an array.
     */
    public Benchmark_Timer(String description, Consumer<T> f) {
        this(description, null, f, null);
    }

    private final String description;
    private final UnaryOperator<T> fPre;
    private final Consumer<T> fRun;
    private final Consumer<T> fPost;

    final static LazyLogger logger = new LazyLogger(Benchmark_Timer.class);
    public static class Timer {

        /**
         * Construct a new Timer and set it running.
         */
        public Timer() {
            resume();
        }

        /**
         * Run the given function n times, once per "lap" and then return the result of calling stop().
         *
         * @param n        the number of repetitions.
         * @param function a function which yields a T (T may be Void).
         * @return the average milliseconds per repetition.
         */
        public <T> double repeat(int n, Supplier<T> function) {
            for (int i = 0; i < n; i++) {
                function.get();
                lap();
            }
            pause();
            return meanLapTime();
        }

        /**
         * Run the given functions n times, once per "lap" and then return the result of calling stop().
         *
         * @param n        the number of repetitions.
         * @param supplier a function which supplies a different T value for each repetition.
         * @param function a function T=>U and which is to be timed (U may be Void).
         * @return the average milliseconds per repetition.
         */
        public <T, U> double repeat(int n, Supplier<T> supplier, Function<T, U> function) {
            return repeat(n, supplier, function, null, null);
        }

        /**
         * Pause (without counting a lap); run the given functions n times while being timed, i.e. once per "lap", and finally return the result of calling meanLapTime().
         *
         * @param n            the number of repetitions.
         * @param supplier     a function which supplies a T value.
         * @param function     a function T=>U and which is to be timed.
         * @param preFunction  a function which pre-processes a T value and which precedes the call of function, but which is not timed (may be null).
         * @param postFunction a function which consumes a U and which succeeds the call of function, but which is not timed (may be null).
         * @return the average milliseconds per repetition.
         */
        public <T, U> double repeat(int n, Supplier<T> supplier, Function<T, U> function, UnaryOperator<T> preFunction, Consumer<U> postFunction) {
            logger.trace("repeat: with " + n + " runs");
            // TO BE IMPLEMENTED: note that the timer is running when this method is called and should still be running when it returns.
            T t=supplier.get();
            pause();
            for(int i=0; i<n; i++){
                if(preFunction!=null){t= preFunction.apply(t);}
                resume();
                U u=function.apply(t);
                pauseAndLap();
                if(postFunction!=null){postFunction.accept(u);}

            }
            double meantime=meanLapTime();
            resume();
            return meantime;
        }

        /**
         * Stop this Timer and return the mean lap time in milliseconds.
         *
         * @return the average milliseconds used by each lap.
         * @throws edu.neu.coe.info6205.util.Timer.TimerException if this Timer is not running.
         */
        public double stop() {
            pauseAndLap();
            return meanLapTime();
        }

        /**
         * Return the mean lap time in milliseconds for this paused timer.
         *
         * @return the average milliseconds used by each lap.
         * @throws edu.neu.coe.info6205.util.Timer.TimerException if this Timer is running.
         */
        public double meanLapTime() {
            if (running) throw new edu.neu.coe.info6205.util.Timer.TimerException();
            return toMillisecs(ticks) / laps;
        }

        /**
         * Pause this timer at the end of a "lap" (repetition).
         * The lap counter will be incremented by one.
         *
         * @throws edu.neu.coe.info6205.util.Timer.TimerException if this Timer is not running.
         */
        public void pauseAndLap() {
            lap();
            ticks += getClock();
            running = false;
        }

        /**
         * Resume this timer to begin a new "lap" (repetition).
         *
         * @throws edu.neu.coe.info6205.util.Timer.TimerException if this Timer is already running.
         */
        public void resume() {
            if (running) throw new edu.neu.coe.info6205.util.Timer.TimerException();
            ticks -= getClock();
            running = true;
        }

        /**
         * Increment the lap counter without pausing.
         * This is the equivalent of calling pause and resume.
         *
         * @throws edu.neu.coe.info6205.util.Timer.TimerException if this Timer is not running.
         */
        public void lap() {
            if (!running) throw new edu.neu.coe.info6205.util.Timer.TimerException();
            laps++;
        }

        /**
         * Pause this timer during a "lap" (repetition).
         * The lap counter will remain the same.
         *
         * @throws edu.neu.coe.info6205.util.Timer.TimerException if this Timer is not running.
         */
        public void pause() {
            pauseAndLap();
            laps--;
        }

        /**
         * Method to yield the total number of milliseconds elapsed.
         * NOTE: an exception will be thrown if this is called while the timer is running.
         *
         * @return the total number of milliseconds elapsed for this timer.
         */
        public double millisecs() {
            if (running) throw new edu.neu.coe.info6205.util.Timer.TimerException();
            return toMillisecs(ticks);
        }

        @Override
        public String toString() {
            return "Timer{" +
                    "ticks=" + ticks +
                    ", laps=" + laps +
                    ", running=" + running +
                    '}';
        }

        private long ticks = 0L;
        private int laps = 0;
        private boolean running = false;

        // NOTE: Used by unit tests
        private long getTicks() {
            return ticks;
        }

        // NOTE: Used by unit tests
        private int getLaps() {
            return laps;
        }

        // NOTE: Used by unit tests
        private boolean isRunning() {
            return running;
        }

        /**
         * Get the number of ticks from the system clock.
         * <p>
         * NOTE: (Maintain consistency) There are two system methods for getting the clock time.
         * Ensure that this method is consistent with toMillisecs.
         *
         * @return the number of ticks for the system clock. Currently defined as nano time.
         */
        private static long getClock() {
            // TO BE IMPLEMENTED

            return System.nanoTime();
        }

        /**
         * NOTE: (Maintain consistency) There are two system methods for getting the clock time.
         * Ensure that this method is consistent with getTicks.
         *
         * @param ticks the number of clock ticks -- currently in nanoseconds.
         * @return the corresponding number of milliseconds.
         */
        private static double toMillisecs(long ticks) {
            // TO BE IMPLEMENTED
            double ret = ticks / 100000;
            return ret;
        }

        final static LazyLogger logger = new LazyLogger(edu.neu.coe.info6205.util.Timer.class);

        static class TimerException extends RuntimeException {
            public TimerException() {
            }

            public TimerException(String message) {
                super(message);
            }

            public TimerException(String message, Throwable cause) {
                super(message, cause);
            }

            public TimerException(Throwable cause) {
                super(cause);
            }
        }
    }
    public static void main(String[] args) {
        int[] nRuns = new int[]{1,2,4,8,16};//numbers of runs
        int n = 10000;//length of arrays
        Benchmark_Timer<Boolean> bmt = new Benchmark_Timer<Boolean>("InsertionSort", new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {

            }
        });
        //Reversed array
        for(int i=0; i<nRuns.length;i++){
        System.out.println("Reversed array with 10000 element "+bmt.reversed(nRuns[i],n));}
        System.out.println("\n");

        //Ordered array
        for(int i=0; i<nRuns.length;i++){
        System.out.println("Ordered array with 10000 element "+bmt.ordered(nRuns[i],n));}
        System.out.println("\n");

        //Partially ordered array
        for(int i=0; i<nRuns.length;i++){
        System.out.println("Partially ordered array with 10000 element "+bmt.partially(nRuns[i],n));}
        System.out.println("\n");

        //Random ordered array
        for(int i=0; i<nRuns.length;i++){
        System.out.println("Random ordered array with 10000 element "+bmt.random(nRuns[i],n));}
        System.out.println("\n");
    }


    //Created an ordered array
    private double ordered(int nRuns, int n){
        Integer[] ordered = new Integer[n];
        for (int i = 0; i < n ; i++) {
            ordered[i] = i;
        }
        Benchmark<Boolean> bm = new Benchmark_Timer<>(
                "testInsertionSortTimer",
                null,
                b -> {
                    new InsertionSort<Integer>().sort(ordered,
                            0,ordered.length);
                },
                null
        );
        double x = bm.run(true,nRuns);
        return x;
    }
//created an array with reversed order
    private double reversed(int nRuns, int n){
        Integer[] reversed = new Integer[n];
        for (int i = 0; i < n ; i++) {
            reversed[i] = n-i-1;
        }
        Benchmark<Boolean> bm = new Benchmark_Timer<>(
                "testInsertionSortTimer",
                null,
                b -> {
                    new InsertionSort<Integer>().sort(reversed,
                            0,reversed.length);
                },
                null
        );
        double x = bm.run(true,nRuns);
        return x;
    }
//created an array with partially order
    private double partially(int nRuns, int n){
        Integer[] partially = new Integer[n];
        Random ran = new Random();
        for (int i = 0; i < n/2 ; i++) {
            partially[i] = ran.nextInt(n/2);
        }
        for (int i = n/2; i < n ; i++) {
            partially[i] = i;
        }

        Benchmark<Boolean> bm = new Benchmark_Timer<>(
                "testInsertionSortTimer",
                null,
                b -> {
                    new InsertionSort<Integer>().sort(partially,
                            0,partially.length);
                },
                null
        );
        double x = bm.run(true,nRuns);
        return x;
    }
//created an array without an order
    private double random(int nRuns, int n){
        Integer[] random = new Integer[n];
        Random ran = new Random();
        for (int i = 0; i < n ; i++) {
            random[i] = ran.nextInt(n);
        }

        Benchmark<Boolean> bm = new Benchmark_Timer<>(
                "testInsertionSortTimer",
                null,
                b -> {
                    new InsertionSort<Integer>().sort(random,
                            0,random.length);
                },
                null
        );
        double x = bm.run(true,nRuns);
        return x;
    }
}
