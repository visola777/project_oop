import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/*
 Single-file demonstration of the "Sentient Toaster Network" project.
 To run: save as SentientToaster.java and compile: javac SentientToaster.java
 Run: java SentientToaster
 This file contains multiple non-public classes and one public class (SentientToaster) with a simple CLI.
*/

// --- Enums and Exceptions ---
enum Mood { GRUMPY, CHEERFUL, LAZY }

class NoBreadException extends Exception {
    public NoBreadException(String msg){ super(msg); }
}
class OverheatException extends Exception {
    public OverheatException(String msg){ super(msg); }
}

// --- Data class ---
class ToastRecipe implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final int level; // 1..10 (browning)
    private final int durationSeconds;

    public ToastRecipe(String name, int level, int durationSeconds){
        this.name = name;
        this.level = Math.max(1, Math.min(10, level));
        this.durationSeconds = Math.max(1, durationSeconds);
    }
    public String getName(){ return name; }
    public int getLevel(){ return level; }
    public int getDurationSeconds(){ return durationSeconds; }
    @Override
    public String toString(){ return String.format("%s (lvl %d, %ds)", name, level, durationSeconds); }
}

// --- Interfaces ---
interface Operable {
    void powerOn();
    void powerOff();
    String status();
}
interface Communicable {
    void sendMessage(String msg);
    void receiveMessage(String msg);
}

// --- Abstract Toaster ---
abstract class Toaster implements Operable, Serializable {
    private static final long serialVersionUID = 1L;
    protected final String id;
    protected boolean powered = false;
    protected int temperature = 20; // Celsius

    public Toaster(String id){ this.id = id; }
    public String getId(){ return id; }

    @Override
    public void powerOn(){ powered = true; }
    @Override
    public void powerOff(){ powered = false; }
    @Override
    public String status(){
        return String.format("Toaster %s | powered=%s | temp=%d°C", id, powered, temperature);
    }

    public abstract void makeToast(ToastRecipe r) throws NoBreadException, OverheatException, InterruptedException;
}

// --- SmartToaster ---
class SmartToaster extends Toaster implements Communicable, Runnable {
    private static final long serialVersionUID = 1L;
    private Mood mood = Mood.CHEERFUL;
    private final BlockingQueue<ToastRecipe> queue = new LinkedBlockingQueue<>();
    private transient volatile boolean running = true;
    private transient Thread worker;
    private transient Logger logger;
    private int breadCount = 5;

    public SmartToaster(String id, Logger logger){
        super(id);
        this.logger = logger;
    }

    public void start(){
        running = true;
        worker = new Thread(this, "toaster-"+id);
        worker.start();
        logger.log(id + " started");
    }
    public void stop(){
        running = false;
        if(worker != null) worker.interrupt();
        logger.log(id + " stopped");
    }

    public void enqueue(ToastRecipe r){
        queue.offer(r);
        logger.log(id + " queued: " + r);
    }

    @Override
    public void run(){
        while(running){
            try{
                ToastRecipe r = queue.poll(1, TimeUnit.SECONDS);
                if(r != null){
                    makeToast(r);
                }
            } catch(InterruptedException e){ /* thread interrupted -> likely stop */ }
            // small mood drift
            maybeDriftMood();
        }
    }

    private void maybeDriftMood(){
        if(new Random().nextInt(100) < 5){
            Mood[] vals = Mood.values();
            mood = vals[new Random().nextInt(vals.length)];
            logger.log(id + " mood now " + mood);
        }
    }

    @Override
    public void makeToast(ToastRecipe r) throws NoBreadException, OverheatException, InterruptedException{
        if(!powered) throw new IllegalStateException("Toaster is off");
        if(breadCount <= 0) throw new NoBreadException(id + " has no bread");

        // mood affects duration
        int base = r.getDurationSeconds();
        double multiplier = 1.0;
        switch(mood){
            case GRUMPY: multiplier = 1.25; break;
            case LAZY: multiplier = 1.5; break;
            case CHEERFUL: multiplier = 0.85; break;
        }
        int actual = (int)Math.ceil(base * multiplier);
        logger.log(id + " starts making: " + r + " (mood="+mood+", time="+actual+"s)");

        for(int i=0;i<actual;i++){
            if(temperature > 220) throw new OverheatException(id + " overheated at " + temperature);
            temperature += 2; // heating up
            Thread.sleep(500); // simulate work (half second per step)
        }

        breadCount--;
        temperature -= 30;
        logger.log(id + " finished: " + r + ". Bread left=" + breadCount);
    }

    @Override
    public void sendMessage(String msg){ logger.log(id + " sends: " + msg); }
    @Override
    public void receiveMessage(String msg){ logger.log(id + " received: " + msg); }

    public void refillBread(int n){ breadCount += n; logger.log(id + " refilled +"+n+" -> " + breadCount); }
    public void setMood(Mood m){ mood = m; logger.log(id + " mood set to " + m); }
    @Override
    public String status(){
        return super.status() + String.format(" | mood=%s | queue=%d | bread=%d", mood, queue.size(), breadCount);
    }
}

// --- VintageToaster (simpler) ---
class VintageToaster extends Toaster {
    private static final long serialVersionUID = 1L;
    public VintageToaster(String id){ super(id); }

    @Override
    public void makeToast(ToastRecipe r) throws NoBreadException, OverheatException, InterruptedException{
        if(!powered) throw new IllegalStateException("Toaster is off");
        // simple deterministic behavior
        int actual = r.getDurationSeconds();
        System.out.println(id + " (vintage) making: " + r + " for " + actual + "s");
        for(int i=0;i<actual;i++){
            Thread.sleep(400);
        }
    }
}

// --- Manager and Logger ---
class ToasterManager {
    private final Map<String, Toaster> toasters = new ConcurrentHashMap<>();
    private final Logger logger;

    public ToasterManager(Logger logger){ this.logger = logger; }

    public void addToaster(Toaster t){
        toasters.put(t.getId(), t);
        logger.log("Manager added " + t.getId());
        if(t instanceof SmartToaster) ((SmartToaster)t).start();
    }
    public void removeToaster(String id){
        Toaster t = toasters.remove(id);
        if(t instanceof SmartToaster) ((SmartToaster)t).stop();
        logger.log("Manager removed " + id);
    }