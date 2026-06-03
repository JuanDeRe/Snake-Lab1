package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

public class PrimeFinderThread extends Thread{

	
	int a,b;
	
	private List<Integer> primes;

    private boolean paused;
	
	public PrimeFinderThread(int a, int b) {
		super();
                this.primes = new LinkedList<>();
		this.a = a;
		this.b = b;
        this.paused = false;
	}

        @Override
	public void run(){
            for (int i= a;i < b;i++){
                pauseIfNeeded();
                if (isPrime(i)){
                    primes.add(i);
                }
            }
	}


    private void pauseIfNeeded() {
        synchronized (this) {
            while (paused) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public synchronized void pauseThread() {
        paused = true;
    }

    public synchronized void resumeThread() {
        paused = false;
        notify();
    }



    boolean isPrime(int n) {
	    boolean ans;
            if (n > 2) { 
                ans = n%2 != 0;
                for(int i = 3;ans && i*i <= n; i+=2 ) {
                    ans = n % i != 0;
                }
            } else {
                ans = n == 2;
            }
	    return ans;
	}

	public List<Integer> getPrimes() {
		return primes;
	}
	
}
