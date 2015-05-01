package AzureTest;

public class Helpers {
	public static void sleep(int seconds) {
		try {
			System.out.println("Sleeping for " + seconds + " seconds Zzz...");
		    Thread.sleep(seconds);
		} catch(InterruptedException ex) {
			System.out.println("Interrupt caught while sleeping.");
		}
	}
}
