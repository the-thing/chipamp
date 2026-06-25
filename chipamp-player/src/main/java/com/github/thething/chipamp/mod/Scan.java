package com.github.thething.chipamp.mod;

import java.io.File;
import java.io.IOException;

public class Scan {

    public static void main(String[] args) throws IOException {


        File dir = new File("C:\\Users\\Marcin\\Downloads\\mod");
        ModLoader loader = new ModLoader();

        for (File file : dir.listFiles()) {
            System.out.println(file.getName());
            Mod mod = loader.load(file);

            //

//            if (mod.getChannelCount() > 4) {
//                System.out.println("More than 4 channels: " + file.getName() + " / " + mod.getTrackerId());
//            }
        }
    }
}
