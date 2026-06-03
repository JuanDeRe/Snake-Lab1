/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.primefinder;

/**
 *
 */
public class Control extends Thread {
    
    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final Timer timer;

    private final int NDATA = MAXVALUE / NTHREADS;

    private PrimeFinderThread pft[];
    
    private Control(Integer t) {
        super();
        this.pft = new  PrimeFinderThread[NTHREADS];
        int i;
        for(i = 0;i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i*NDATA, (i+1)*NDATA);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i*NDATA, MAXVALUE + 1);
        this.timer = new Timer(t, pft);
    }
    
    public static Control newControl(Integer t) {
        return new Control(t);
    }

    @Override
    public void run() {
        for(int i = 0;i < NTHREADS;i++ ) {
            pft[i].start();
        }
        timer.start();
    }
}
