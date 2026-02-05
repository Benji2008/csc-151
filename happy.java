import java.util.Scanner;

public class happy {
    public static void main(String[] args){
        System.out.println("hello whats your name");
        Scanner input = new Scanner(System.in); 
        String name = input.nextLine();

        System.out.println();
        System.out.println("nice to meetcha, " + name);

}
}
