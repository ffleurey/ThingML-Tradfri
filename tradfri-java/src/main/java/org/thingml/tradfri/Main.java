package org.thingml.tradfri;

public class Main {

	protected static String gateway_ip = "10.3.1.180";
	protected static String security_key = "5HV7ibb4brgWL18x";

	
	public static void main(String[] args) {
		
		TradfriGateway gw = new TradfriGateway(gateway_ip, security_key);
                gw.initCoap();
		gw.dicoverBulbs();
		for (LightBulb b : gw.bulbs) {
			//b.updateBulb();
			System.out.println(b.toString());
		}

		System.exit(0);
	}

}
