// Pet.java
public abstract class Pet {
    private String name;
    private int age;
    private int energy;

    public Pet(String name, int age) {
        this.name = name;
        this.age = age;
        this.energy = 50;
    }

    public abstract void makeSound();

    public void feed(String food) {
        System.out.println(name + " eats " + food);
        energy += 10;
    }

    public void play() {
        System.out.println(name + " is playing...");
        energy -= 15;
        if (energy < 0) energy = 0;
    }

    public String getName() {
        return name;
    }

    public int getEnergy() {
        return energy;
    }
}
