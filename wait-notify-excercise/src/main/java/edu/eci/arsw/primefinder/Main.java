package edu.eci.arsw.primefinder;

public class Main {

    public static void main(String[] args) {
        Integer t = Integer.parseInt(System.console().readLine("Ingrese cada cuanto de hara una pausa (en ms): "));
        Control control = Control.newControl(t);
        
        control.start();

    }
	
}
