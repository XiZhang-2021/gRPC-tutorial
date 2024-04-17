package org.xizhang.rpc.grpc.sample;

import com.google.protobuf.Timestamp;
import com.xizhang.rpc.grpc.pcbook.pb.*;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class Generator {
    private Random rand;
    public Generator(){
        this.rand = new Random();
    }
    public Keyboard NewKeyboard(){
        return Keyboard.newBuilder()
                .setLayout(randomKeyboardLayout())
                .setBacklit(rand.nextBoolean())
                .build();
    }

    public CPU NewCPU(){
        String brand = randomCPUBrand();
        String name = randomCPUName(brand);
        int numberCores = randomInt(2, 8);
        int numberThreads = randomInt(numberCores, 12);
        double minGhz = randomDouble(2.0, 3.5);
        double maxGhz = randomDouble(minGhz, 5.0);

        return CPU.newBuilder()
                .setBrand(brand)
                .setName(name)
                .setNumberCores(numberCores)
                .setNumberThreads(numberThreads)
                .setMinGhz(minGhz)
                .setMaxGhz(maxGhz)
                .build();
    }

    public GPU NewGPU(){
        String brand = randomGPUBrand();
        String name = randomGPUName(brand);
        double minGhz = randomDouble(1.0, 2.0);
        double maxGhz = randomDouble(minGhz, 3.0);

        Memory memory = Memory.newBuilder()
                .setValue(randomInt(2, 6))
                .setUnit(Memory.Unit.GIGABYTE)
                .build();

        return GPU.newBuilder()
                .setBrand(brand)
                .setName(name)
                .setMinGhz(minGhz)
                .setMaxGhz(maxGhz)
                .setMemory(memory)
                .build();
    }

    public Memory NewRam(){
        return Memory.newBuilder()
                .setValue(randomInt(4, 64))
                .setUnit(Memory.Unit.GIGABYTE)
                .build();
    }

    public Storage NewSSD(){
        Memory memory = Memory.newBuilder()
                .setValue(randomInt(128, 1024))
                .setUnit(Memory.Unit.GIGABYTE)
                .build();
        return Storage.newBuilder()
                .setDriver(Storage.Driver.SSD)
                .setMemory(memory)
                .build();
    }

    public Storage NewHDD(){
        Memory memory = Memory.newBuilder()
                .setValue(randomInt(1, 6))
                .setUnit(Memory.Unit.TERABYTE)
                .build();
        return Storage.newBuilder()
                .setDriver(Storage.Driver.HDD)
                .setMemory(memory)
                .build();
    }

    public Screen NewScreen(){
        int height = randomInt(1080, 4320);
        int width = height*16/9;

        Screen.Resolution resolution = Screen.Resolution.newBuilder()
                .setHeight(height)
                .setWidth(width)
                .build();
        return Screen.newBuilder()
                .setSizeInch(randomFloat(13, 17))
                .setResolution(resolution)
                .setPanel(randomScreenPanel())
                .setMultitouch(rand.nextBoolean())
                .build();
    }

    public Laptop NewLaptop(){
        String brand = randomLaptopBrand();
        String name = randomLaptopName(brand);
        double weightKg = randomDouble(1.0, 3.0);
        double priceUSD = randomDouble(1500, 2000);
        int releaseYear = randomInt(2023, 2024);
        return Laptop.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setBrand(brand)
                .setName(name)
                .setCpu(NewCPU())
                .setRam(NewRam())
                .addGpus(NewGPU())
                .addStorages(NewSSD())
                .addStorages(NewHDD())
                .setScreen(NewScreen())
                .setKeyboard(NewKeyboard())
                .setWeightKg(weightKg)
                .setPriceUsd(priceUSD)
                .setReleaseYear(releaseYear)
                .setUpdatedAt(timestampNow())
                .build();
    }

    public double NewLaptopScore(){
        return randomInt(1, 10);
    }

    private Timestamp timestampNow() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }

    private String randomLaptopName(String brand) {
        switch (brand){
            case "Apple":
                return randomStringFromSet("Macbook Air", "Macbook Pro");
            case "Dell":
                return  randomStringFromSet("xps 14", "xps 15");
            default:
                return randomStringFromSet("omen 1", "omen 2");
        }
    }

    private String randomLaptopBrand() {
        return randomStringFromSet("apple", "dell", "hp");
    }

    private Screen.Panel randomScreenPanel() {
        if (rand.nextBoolean()){
            return Screen.Panel.IPS;
        }
        return Screen.Panel.OLED;
    }

    private float randomFloat(int min, int max) {
        return min + rand.nextFloat() * (max - min);
    }

    private String randomGPUName(String brand) {
        if(brand == "NVIDIA"){
            return randomStringFromSet(
                    "RTX 4090",
                    "RTX 4070TI",
                    "RTX 3080TI",
                    "RTX 3080"
            );
        }
        return randomStringFromSet(
                "AMD Readon 7900",
                "AMD Readon 7700",
                "AMD Readon 6700",
                "AMD Readon 6600"
        );
    }

    private String randomGPUBrand() {
        return randomStringFromSet("NVIDIA", "AMD");
    }

    private double randomDouble(double min, double max) {
        return min + rand.nextDouble() * (max - min);
    }

    private int randomInt(int min, int max) {
        return min + rand.nextInt(max-min+1);
    }

    private String randomCPUName(String brand) {
        if(brand == "Intel"){
            return randomStringFromSet("Xeon E-2286M",
                    "Core i9-9980HK",
                    "Core i7-9750H",
                    "Core i5-9400F",
                    "Core i3-1005G1");
        }else{
            return randomStringFromSet("Ryzen 7 PRO 2700U",
                    "Ryzen 5 PRO 3500U",
                    "Ryzen 3 PRO 3200GE");
        }
    }

    private String randomCPUBrand() {
        return randomStringFromSet("Intel", "AMD");
    }

    private String randomStringFromSet(String... a) {
        int n = a.length;
        if(n == 0){
            return "";
        }
        return a[rand.nextInt(n)];
    }

    private Keyboard.Layout randomKeyboardLayout() {
        switch (rand.nextInt(3)){
            case 1:
                return Keyboard.Layout.QWERTY;
            case 2:
                return Keyboard.Layout.QWERTZ;
            default :
                return Keyboard.Layout.AZERTY;
        }
    }

    public static void main(String[] args) {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        System.out.println(laptop);
    }
}
