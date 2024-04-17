package org.xizhang.rpc.grpc.serializer;

import com.google.protobuf.Descriptors;
import com.google.protobuf.util.JsonFormat;
import com.xizhang.rpc.grpc.pcbook.pb.Laptop;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Serializer {
    public void writeBinaryFile(Laptop laptop, String filename) {
        try(FileOutputStream outputStream = new FileOutputStream(filename);){
            laptop.writeTo(outputStream);
        }catch(IOException e){
            e.printStackTrace();
        }

    }
    public Laptop readBinaryFile (String filename){
        try(FileInputStream inputStream = new FileInputStream(filename);){
            Laptop laptop = Laptop.parseFrom(inputStream);
            return laptop;
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public void writeJSONFile(Laptop laptop, String filename){
        try(FileOutputStream outputStream = new FileOutputStream(filename);){
            JsonFormat.Printer printer = JsonFormat.printer()
                    .includingDefaultValueFields()
                    .preservingProtoFieldNames();
            String jsonString = printer.print(laptop);
            System.out.println(jsonString);
            outputStream.write(jsonString.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        Serializer serializer = new Serializer();
        Laptop laptop = serializer.readBinaryFile("laptop.bin");
        serializer.writeJSONFile(laptop, "laptop.json");

    }
}
