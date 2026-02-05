import java.util.Scanner;
import java.util.Random;

public class happy {
    public static int roll() {
        Random roller = new Random();
        int roll = roller.nextInt(5) + 1;
        return roll;
    }
    public static void main(String[] args){
        System.out.println("hello whats your name");
        Scanner input = new Scanner(System.in); 
        String name = input.nextLine();

        System.out.println();
        System.out.println("nice to meetcha, " + name);
        int chips = 100;
        System.out.println("well," + name + " you start with " + chips + " chips. ");
      
      for (int i=0; i < 20; i++ ) {
            System.out.println ("rolling two dice...🎲 ");
             int roll = roll() + roll();
             System.out.println(" you rolled a : " + roll);
        }
        input.close();


    }
}
