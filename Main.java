// Main.java
public class Main {
    public static void main(String[] args) {
        Pet cat = new Cat("Mochi", 2);
        Pet dog = new Dog("Rex", 3);

        cat.makeSound();
        dog.makeSound();

        cat.feed("fish");
        dog.feed("bone");

        System.out.println(cat.getName() + " energy: " + cat.getEnergy());
        System.out.println(dog.getName() + " energy: " + dog.getEnergy());

        cat.play();
        dog.play();

        System.out.println(cat.getName() + " energy: " + cat.getEnergy());
        System.out.println(dog.getName() + " energy: " + dog.getEnergy());
    }
}
