package edu.eci.arsw.primefinder;

import java.util.Scanner;

public class Timer extends Thread {

    private final Integer t;
    private final PrimeFinderThread[] pft;
    private final Scanner scanner = new Scanner(System.in);

    public Timer(Integer t, PrimeFinderThread[] pft) {
        this.t = t;
        this.pft = pft;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();

        while (checkThreadsRunning()) {
            if (System.currentTimeMillis() - time >= t) {

                stopThreads();
                showStatus();
                continueThreads();

                time = System.currentTimeMillis();
            }
        }
    }

    private void stopThreads() {
        for (PrimeFinderThread p : pft) {
            p.pauseThread();
        }
    }

    private void continueThreads() {
        for (PrimeFinderThread p : pft) {
            p.resumeThread();
        }
    }

    private void showStatus() {
        int i = 1;
        for (PrimeFinderThread p : pft) {
            System.out.println("Numero de primos hilo " + i++ + " : " +p.getPrimes().size());
        }

        System.out.println("Presione ENTER para continuar...");
        scanner.nextLine();
    }

    private boolean checkThreadsRunning() {
        for (PrimeFinderThread p : pft) {
            if (p.isAlive()) {
                return true;
            }
        }
        return false;
    }
}